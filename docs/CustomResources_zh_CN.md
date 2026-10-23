# 自定义音乐源

> [!warning] 
> 此文档由GPT-6-Luna生成

使用 `xxx.json` 定义音乐源，文件名即音乐源名称。文件根节点必须是 JSON 对象。

## 请求阶段

一个音乐源可以配置连续的请求阶段：`url1`、`url2`、`url3`……每个 URL 都必须有同编号的 `handler`。例如 `url1` 对应 `handler1`，`url2` 对应 `handler2`。

1. `url1` 是搜索请求。`{0}` 替换为 URL 编码后的关键词，`{1}` 替换为搜索数量上限。解析器使用 `handler1.array` 获取结果数组，按 `handler1.prerequisite` 过滤，并为每个保留结果创建一条曲目。
2. 对每个后续 URL，解析器从当前曲目的 `mid` 读取 ID，将其替换 URL 中的 `{0}`，并分别请求该曲目的详情。对应的 `handler` 用于筛选和提取详情字段。只有 `url1` 会替换 `{1}`；后续阶段不会替换 `{1}`。
3. 每个 handler 中有值的 `name`、`artist`、`url`、`pic`、`mid` 字段都会更新曲目已有值；留空字段不会覆盖前面阶段得到的值。后续阶段可用 `mid` 提供下一阶段的 ID。
4. 搜索结束后，仅返回同时具有非空`name`和`artist`的曲目。

如果首轮没有符合条件的结果，后续 URL 阶段不会请求。

## URL 与 handler 字段

| 字段 | 说明 |
|---|---|
| `url1`、`url2`…… | 连续的请求 URL。不能跳过编号；每个 URL 都要有对应 handler。`url1` 中 `{0}` 替换为关键词、`{1}` 替换为结果上限；后续 URL 中仅 `{0}` 会替换为当前曲目的 `mid`。 |
| `handler1`、`handler2`…… | 描述对应阶段的响应字段。`handler1.name` 和 `handler1.artist` 必须配置。 |
| `name` | 歌曲标题字段。可使用 `__` 分隔嵌套路径，例如 `track__title`。 |
| `artist` | 艺术家字段。可使用 `__` 分隔嵌套路径。 |
| `url` | 播放地址字段。可使用 `__` 分隔嵌套路径。设置为 `{0}` 时会直接将响应解析为纯文本URL|
| `pic` | 专辑图片字段。可使用 `__` 分隔嵌套路径；可配合 `picUrl`。 |
| `mid` | ID 字段。可填写多个候选字段名，用 `__` 分隔；读取第一个非空字段。用于下一阶段 URL 的 `{0}`。候选字段必须位于当前响应对象的直接子级。后续阶段没有读到新的 `mid` 时，会保留之前的非空值。 |
| `array` | 搜索结果数组的路径，各层对象键用 `__` 分隔，例如 `data__items` 表示 `response.data.items`。若响应根节点本身是数组，可省略。 |
| `prerequisite` | 当前阶段响应对象的筛选条件。使用字段名/期望值对，并以 `__` 分隔，例如 `pay__免费`。所有条件都必须匹配；不设置或留空则不筛选。 |
| `replace` | URL 和图片字段的替换文本对，以 `__` 分隔，例如 `old1__new1__old2__new2`。必须提供偶数个值。 |
| `picUrl` | 图片 URL 模板。设置后，`{0}` 替换为读取并经过 `replace` 处理的图片字段值。 |

`name`、`artist`、`url`、`pic` 的路径遇到数组时，解析器会取数组的第一个元素后继续查找。`mid` 不支持此数组/嵌套路径行为。`picUrl` 模板仅在 `pic` 配置为单个字段名时应用；`pic` 使用多段路径时不会应用模板。

## 配置示例

### 搜索响应直接包含播放地址

```json
{
  "url1": "https://example.com/search?q={0}&limit={1}",
  "handler1": {
    "array": "data__items",
    "name": "title",
    "artist": "artist",
    "url": "play_url",
    "pic": "cover"
  }
}
```

### 搜索后逐曲请求详情

```json
{
  "url1": "https://example.com/search?q={0}&limit={1}",
  "handler1": {
    "array": "data__items",
    "prerequisite": "pay__free",
    "mid": "song_id__alternate_id",
    "name": "title",
    "artist": "artist"
  },
  "url2": "https://example.com/song/{0}",
  "handler2": {
    "url": "data__play_url",
    "pic": "data__cover"
  }
}
```

URL2 会为每条搜索结果分别请求。详情响应为可直接由 handler 解析的 JSON 对象。

### 多阶段请求

```json
{
  "url1": "https://example.com/search?q={0}&limit={1}",
  "handler1": {
    "array": "items",
    "mid": "song_id",
    "name": "title",
    "artist": "artist"
  },
  "url2": "https://example.com/song/{0}",
  "handler2": {
    "mid": "stream_id"
  },
  "url3": "https://example.com/stream/{0}",
  "handler3": {
    "url": "play_url"
  }
}
```

URL2 的响应通过 `handler2.mid` 更新 ID，随后 URL3 使用该 ID 请求播放地址。

## 当前音乐源示例

- **网易云（Netease）**：URL1 直接返回数组，曲目字段包含标题、艺术家、图片和播放地址。
- **QQ**：`handler1.mid` 提供 `song_mid`；URL2 响应由 `handler2` 读取播放地址和图片。
- **酷狗（Kugou）**：结果数组路径为 `data__lists`；`mid` 依次尝试 `SQFileHash`、`HQFileHash`、`FileHash`；URL2 返回包含 `url` 字段的 JSON 对象。
- **酷我（Kuwo）**：结果数组路径为 `abslist`；URL2 返回纯文本 URL。

## 注意事项

- 使用有效 JSON，字符串使用双引号，不要添加注释。
- 必须提供 `url1`、`handler1`，且 `handler1.name`、`handler1.artist` 不能为空。
- URL 阶段必须从 `url1` 开始连续编号；每个 URL 必须有相应 handler。
- 配置后续 URL 时，必须确保当前曲目已有正确的非空 `mid`。后续阶段如果未提取到新的 `mid`，会沿用旧值，可能导致下一阶段重复使用旧 ID。
- `array` 只用于首轮搜索响应。响应根节点是对象时必须设置有效路径；路径不存在或结果不是数组时，搜索结果为空。
- `prerequisite`、`replace` 使用 `__` 分隔的字段/值对，需确保值数量为偶数。
- 请求 URL 中的 `{0}` 用于关键词或曲目 ID；handler 中的字段路径使用对象键名，不要把 `{0}` 当作普通 JSON 字段。
- 当前请求方式为 HTTP GET；解析器不支持配置请求方法或请求头。
