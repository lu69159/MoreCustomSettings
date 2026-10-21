# 自定义音乐源（`resources.json`）
> [!warning] 
> 由GPT-6-Luna生成

`resources.json` 用于定义音乐搜索使用的音乐源。文件根节点必须是 JSON 数组，每个数组项描述一个音乐源。该文件从模组根目录加载。

## 音乐源字段

| 字段 | 必填 | 说明 |
|---|---:|---|
| `name` | 是 | 音乐源 ID/名称。用于日志和音乐源设置键（`MCS-resource-<name>`）。 |
| `url1` | 是 | 搜索接口。`{0}` 会替换为经过 URL 编码的搜索关键词；`{1}` 会替换为请求的结果数量上限。 |
| `url2` | 否 | 可选的单曲详情接口。设置后，解析器会从 `handler1.mid` 获取 ID，将其替换到 `{0}`，然后为每条符合条件的搜索结果请求此接口。 |
| `prerequisite` | 否 | 可选的筛选条件，由 `__` 分隔的字段名/字段值对组成。例如：`"pay__免费"` 表示只保留 `pay` 字段值为 `免费` 的结果。不设置或设为空字符串时不筛选。 |
| `handler1` | 是 | 描述搜索响应中的字段。至少必须配置非空的 `name`。 |
| `handler2` | 视情况而定 | 描述详情响应中的字段。当播放地址或图片从 URL2 响应中读取时，需要配置此字段。 |

## Handler 字段

`handler1` 和 `handler2` 使用相同的字段格式。除特别说明外，各字段值是对应 JSON 对象中的字段名。

| 字段 | 说明 |
|---|---|
| `name` | 歌曲标题字段。曲目标题从 `handler1.name` 读取。当前解析器要求配置此字段。 |
| `artist` | 艺术家字段，通常从搜索结果（`handler1`）读取。 |
| `url` | 播放地址字段。如果设置了 `handler1.url`，则从搜索结果读取；否则从详情响应中通过 `handler2.url` 读取。若 URL2 返回纯文本 URL，使用 `"url": "{0}"`；解析器会将该响应文本存放在特殊的 `{0}` 键下。 |
| `pic` | 专辑图片字段。如果在 `handler1` 中设置，则从搜索结果读取；否则通过 `handler2.pic` 从详情响应读取。 |
| `mid` | 仅在 `handler1` 配合 `url2` 时使用。可以填写一个字段名，也可以用 `__` 分隔多个候选字段名。解析器会取第一个非空值并替换到 URL2 的 `{0}` 占位符中。 |
| `array` | 搜索结果数组的路径，各层对象键用 `__` 分隔。例如 `data__lists` 表示 `response.data.lists`。如果 URL1 在根节点直接返回结果数组，则省略或留空。 |
| `replace` | 替换文本对，用 `__` 分隔，例如 `old1__new1__old2__new2`。替换会应用于选取的播放地址或图片地址。字段数应为偶数。 |
| `picUrl` | 可选的图片 URL 模板。设置后，`{0}` 会替换为经过 `replace` 处理的 `pic` 字段值。 |

### 搜索与详情响应处理流程

1. 解析器请求 `url1`，将 `{0}` 替换为经过 URL 编码的关键词，将 `{1}` 替换为结果数量上限。
2. 解析器从响应根节点读取结果数组，或按照 `handler1.array` 指定的路径查找数组。
3. 如果设置了 `prerequisite`，只保留符合所有字段/字段值条件的结果。
4. 如果设置了 `url2`，解析器从 `handler1.mid` 中获取第一个可用 ID，将其替换到 URL2 的 `{0}` 中并请求详情接口，然后使用 `handler2` 解析详情响应。
5. 曲目标题和艺术家来自 `handler1`；播放地址和图片字段若配置在 `handler1` 中则从其读取，否则从 `handler2` 读取。

URL2 可以返回 JSON 对象，也可以返回纯文本 URL。若返回 JSON 对象，将 `handler2.url` 配置为实际的 URL 字段名；若返回纯文本 URL，则将 `handler2.url` 配置为 `"{0}"`。

## 配置示例

### URL1 直接返回数组

```json
{
  "name": "example-direct",
  "url1": "https://example.com/search?q={0}&limit={1}",
  "handler1": {
    "name": "title",
    "artist": "artist",
    "url": "play_url",
    "pic": "cover"
  }
}
```

### URL1 返回嵌套结果，URL2 返回对象

```json
{
  "name": "example-detail",
  "url1": "https://example.com/search?q={0}&limit={1}",
  "prerequisite": "pay__free",
  "handler1": {
    "array": "data__items",
    "mid": "song_id__alternate_id",
    "name": "title",
    "artist": "artist"
  },
  "url2": "https://example.com/song/{0}",
  "handler2": {
    "url": "play_url",
    "pic": "cover"
  }
}
```

### URL2 返回纯文本 URL

```json
{
  "name": "example-plain-url",
  "url1": "https://example.com/search?q={0}&limit={1}",
  "handler1": {
    "array": "items",
    "mid": "id",
    "name": "title",
    "artist": "artist"
  },
  "url2": "https://example.com/play?id={0}",
  "handler2": {
    "url": "{0}"
  }
}
```

## 当前音乐源示例

- **网易云（Netease）**：URL1 直接返回数组，每条结果自身包含标题、艺术家、图片和播放地址。
- **QQ**：`handler1.mid` 提供 `song_mid`；URL2 返回 JSON 对象，其中播放地址和图片字段分别为 `song_play_url` 和 `album_pic`。
- **酷狗（Kugou）**：结果数组路径为 `data__lists`；`mid` 依次尝试 `SQFileHash`、`HQFileHash`、`FileHash`。URL2 返回包含 `url` 字段的对象。
- **酷我（Kuwo）**：结果数组路径为 `abslist`；URL2 返回纯文本 URL，因此 `handler2.url` 配置为 `{0}`。图片地址由 `picUrl` 模板和 `web_albumpic_short` 字段拼接生成。

## 重要约束

- 必须使用有效 JSON：字符串使用双引号，不要添加注释，字段和数组项之间要有逗号。
- 必须提供 `url1` 和 `handler1`，且 `handler1.name` 不能为空。
- 设置了 `url2` 时，每条结果必须能从 `handler1.mid` 的候选字段中获取非空 ID。如果播放地址或图片来自 URL2，还需根据响应格式配置 `handler2`。
- `array`、`mid`、`replace` 和 `prerequisite` 使用固定分隔符 `__`。
- `prerequisite` 和 `replace` 都按成对值解析，请确保每项中的值数量为偶数。
- 除了 URL2 返回纯文本时专用的 `handler2.url` 配置，不要将 `{0}` 用作字段名。
