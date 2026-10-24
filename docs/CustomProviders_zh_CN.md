# 自定义音乐源

> [!warning] 
> 此文档由GPT-6-Luna生成

使用 `xxx.json` 定义音乐源，文件名即音乐源名称。文件根节点必须是 JSON 对象。

## 请求阶段

一个音乐源可以配置连续的请求阶段。搜索请求使用 `url` 和 `handler`；后续阶段从 `url1`/`handler1`、`url2`/`handler2` 开始。每个 URL 都必须有对应的 handler，阶段编号不能跳过。

1. `url` 是搜索请求。`{0}` 替换为 URL 编码后的关键词，`{1}` 替换为搜索数量上限。解析器使用 `handler.array` 获取结果数组，按 `handler.prerequisite` 过滤，并为每个保留结果创建一条曲目。
2. 对每个后续 URL，解析器会按编号将当前曲目的 MID 值替换到对应占位符中：`mid0` 替换 `{0}`、`mid1` 替换 `{1}`，依此类推。对应的 `handler` 用于筛选和提取响应字段。只有 `url` 中的 `{0}`、`{1}` 分别替换为关键词和结果上限。
3. 每个 handler 中非空的 `name`、`artist`、`url`、`pic` 会更新曲目已有值；空值不会覆盖前面阶段得到的值。每个 handler 都会用本阶段提取到的 MID 列表替换曲目当前 MID 列表。若要把 ID 传给更后续的 URL 阶段，该阶段的 handler 也必须重新提取它。
4. 搜索结束后，仅返回 `name` 非空的曲目；`artist` 可以为空。

如果首轮没有符合条件的结果，后续 URL 阶段不会请求。

## 单次请求模式

关闭**单次请求模式**时，搜索会在返回结果前执行所有已配置阶段。开启时，搜索只请求 `url`，并保留首阶段解析出的曲目信息和 MID 值；后续阶段会延迟到用户试听或下载该曲目时才请求。因此，尚未解析播放地址的曲目也可能出现在结果列表中；使用曲目时会在后台继续请求后续阶段。后续网络请求失败后，再次试听或下载可以重试。若封面只由后续阶段提供，搜索结果列表中可能不会显示封面。

## URL 与 handler 字段

| 字段 | 说明 |
|---|---|
| `url`、`url1`、`url2`…… | 连续的请求 URL。未编号的 `url` 是搜索请求；后续阶段从 `url1` 开始。不能跳过编号；每个 URL 都要有对应 handler。`url` 中 `{0}` 替换为关键词、`{1}` 替换为结果上限；后续 URL 中的 `{0}`、`{1}` 等按顺序替换为当前曲目的 MID 值。 |
| `handler`、`handler1`、`handler2`…… | 描述对应阶段的响应字段。必须配置 `handler.name`；`handler.artist` 可选。 |
| `name` | 歌曲标题字段。可使用 `__` 分隔嵌套路径，例如 `track__title`。 |
| `artist` | 艺术家字段。可使用 `__` 分隔嵌套路径。 |
| `url` | 播放地址字段。可使用 `__` 分隔嵌套路径。设置为 `{0}` 时会直接将响应解析为纯文本URL|
| `pic` | 专辑图片字段。可使用 `__` 分隔嵌套路径；可配合 `picUrl`。 |
| `mid0`、`mid1`…… | MID 字段，从 `mid0` 开始连续编号。`mid0` 用于下一阶段 URL 的 `{0}`，`mid1` 用于 `{1}`，依此类推。每个字段可用 `__` 分隔多个候选字段名，解析器读取第一个非空字段。候选字段必须是当前响应对象的直接子级。配置了 MID 字段但响应中没有对应值时，占位符会替换为空字符串；若没有配置 MID 字段，则跳过下一 URL 阶段。 |
| `array` | 搜索结果数组的路径，各层对象键用 `__` 分隔，例如 `data__items` 表示 `response.data.items`。若响应根节点本身是数组，可省略。 |
| `prerequisite` | 当前阶段响应对象的筛选条件。使用字段名/期望值对，并以 `__` 分隔，例如 `pay__免费`。所有条件都必须匹配；不设置或留空则不筛选。 |
| `replace` | URL 和图片字段的替换文本对，以 `__` 分隔，例如 `old1__new1__old2__new2`。必须提供偶数个值。 |
| `replaceName` | 歌曲标题和艺术家的替换文本对，以 `__` 分隔，例如 `old1__new1__old2__new2`。必须提供偶数个值。 |
| `picUrl` | 图片 URL 模板。设置后，`{0}` 替换为读取并经过 `replace` 处理的图片字段值。 |

`name`、`artist`、`url`、`pic` 的路径遇到数组时，解析器会取数组的第一个元素后继续查找。`midN` 不支持此数组/嵌套路径行为。MID 字段必须从 `mid0` 开始连续编号；解析器会在第一个缺失或为空的 `midN` 配置处停止。`picUrl` 模板仅在 `pic` 配置为单个字段名时应用；`pic` 使用多段路径时不会应用模板。

## 配置示例

### 搜索响应直接包含播放地址

```json
{
  "url": "https://example.com/search?q={0}&limit={1}",
  "handler": {
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
  "url": "https://example.com/search?q={0}&limit={1}",
  "handler": {
    "array": "data__items",
    "prerequisite": "pay__free",
    "mid0": "song_id__alternate_id",
    "name": "title",
    "artist": "artist"
  },
  "url1": "https://example.com/song/{0}",
  "handler1": {
    "url": "data__play_url",
    "pic": "data__cover"
  }
}
```

`url1` 会为每条搜索结果分别请求。详情响应为可直接由 handler 解析的 JSON 对象。

### 多阶段请求

```json
{
  "url": "https://example.com/search?q={0}&limit={1}",
  "handler": {
    "array": "items",
    "mid0": "song_id",
    "name": "title",
    "artist": "artist"
  },
  "url1": "https://example.com/song/{0}",
  "handler1": {
    "mid0": "stream_id"
  },
  "url2": "https://example.com/stream/{0}",
  "handler2": {
    "url": "play_url"
  }
}
```

`url1` 的响应通过 `handler1.mid0` 将当前 MID 列表替换为 `stream_id`，随后 `url2` 将其作为 `{0}` 请求播放地址。

### 一次请求使用多个 ID

```json
{
  "url": "https://example.com/search?q={0}&limit={1}",
  "handler": {
    "array": "items",
    "mid0": "song_id",
    "mid1": "source",
    "name": "title",
    "artist": "artist"
  },
  "url1": "https://example.com/play?id={0}&source={1}",
  "handler1": {
    "url": "data__url"
  }
}
```

此例中，`url1` 的 `{0}` 替换为 `mid0` 提取的 `song_id`，`{1}` 替换为 `mid1` 提取的 `source`。

## 当前音乐源示例

- **网易云（Netease）**：搜索 URL（`url`）直接返回数组，曲目字段包含标题、艺术家、图片和播放地址。
- **酷狗（Kugou）**：结果数组路径为 `data__lists`；`mid0` 依次尝试 `SQFileHash`、`HQFileHash`、`FileHash`；`url1` 返回包含 `url` 字段的 JSON 对象。
- **酷我（Kuwo）**：结果数组路径为 `abslist`；`url1` 返回纯文本 URL。

## 注意事项

- 使用有效 JSON，字符串使用双引号，不要添加注释。
- 必须提供 `url`、`handler`，且 `handler.name` 不能为空；`artist` 可选。
- 请求阶段必须从 `url`/`handler` 开始，后续从 `url1`/`handler1` 开始连续编号；每个 URL 必须有相应 handler。
- 每个阶段都会替换当前 MID 列表，不会自动沿用上一阶段的 MID。若后续 URL 要使用某个 ID，必须在紧邻它之前的 handler 中配置相应 `midN`。若响应中缺少已配置的 MID 字段，对应 URL 占位符会替换为空字符串。
- `array` 只用于首轮搜索响应。响应根节点是对象时必须设置有效路径；路径不存在或结果不是数组时，搜索结果为空。
- `prerequisite`、`replace` 使用 `__` 分隔的字段/值对，需确保值数量为偶数。
- `url` 中 `{0}` 是关键词、`{1}` 是结果上限；后续 URL 中的 `{0}`、`{1}` 等分别对应 `mid0`、`mid1` 等。handler 中的字段路径使用对象键名，不要把占位符当作普通 JSON 字段名。
- 当前请求方式为 HTTP GET；解析器不支持配置请求方法或请求头。
