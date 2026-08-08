# RedFoxExpand 1.7.10 材质包 API

适用版本：RedFoxExpand `0.1.0`、Minecraft `1.7.10`、Forge `10.13.4.1614`。目录入口见
[RESOURCE_PACK_STRUCTURE.md](RESOURCE_PACK_STRUCTURE.md)。JSON 根可以是一个 definition 对象或最多 1024 个
对象的数组；文件采用原子解析，任意一项错误会跳过该文件全部 definitions。

## Definition

```json
{
  "id": "mytheme:inventory",
  "operation": "append",
  "priority": 0,
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "class_match": "exact",
  "x_offset": 0,
  "y_offset": 0,
  "width_offset": 120,
  "height_offset": 0
}
```

| 字段 | 默认值 | 用途 |
|---|---|---|
| `id` | `<source>#<index>` | 稳定定义 ID；跨文件覆盖必须显式写同一 ID |
| `operation` | `append` | `append` 加入；`replace` 删除同 ID 后加入；`disable` 删除同 ID 且不加入 |
| `priority` | `0` | registry 完成后按升序稳定应用 |
| `target_type` | `container_class` | `screen_class`、`container_class`/`menu_class`、`screen_title` |
| `target` | 无 | 非 `disable` 必需；类名或标题表达式 |
| `class_match` | `exact` | `exact` 或 `assignable`，只用于类目标 |
| `x_offset/y_offset` | `0` | GUI 原点偏移 |
| `width_offset/height_offset` | `0` | GUI 宽高偏移 |
| `title_x_offset/title_y_offset` | `0` | v1 标题兼容偏移 |
| `label_x_offset/label_y_offset` | `0` | v1 标签兼容偏移 |
| `title_color/label_color` | 原版颜色 | `#RRGGBB`、`#AARRGGBB`、`0x...` 或整数 |
| `slot_modifiers` | `[]` | Slot 选择、偏移和高亮 |
| `sprites` | `[]` | legacy/底层贴图字段 |
| `custom_textures` | `[]` | 推荐贴图接口 |
| `texts` | `[]` | 新增前景文字 |
| `font_rules` | `[]` | 显式修改原版文字调用 |

`disable` 只需要 `id`，其余目标与渲染字段会被忽略。未显式写 `id` 时，资源路径和数组序号会成为 ID，
因此通常不能跨不同文件覆盖。

### 目标匹配

类目标接受完整类名、SRG/运行时可重映射类名或简单类名。推荐完整 1.7.10 类名。常见现代 vanilla
`InventoryMenu`、`ChestMenu`、`CraftingMenu`、`FurnaceMenu`、`AnvilMenu`、`MerchantMenu`、
`BrewingStandMenu`、`HopperMenu`、`DispenserMenu`、`BeaconMenu`、`EnchantmentMenu`、`HorseInventoryMenu`
由 `platform/forge1710` 映射到相应 Container；第三方类不猜测。

`screen_title` 比较解析到的翻译键、库存名和显示标题；`*` 是任意字符串通配符，其他字符按字面值处理。

## Slot modifiers

| 字段 | 默认值 | 用途 |
|---|---|---|
| `slots` | 全部 | 索引、索引字符串或 `0-8` 范围 |
| `target_x/target_y` | 不限制 | 匹配修改前基础坐标 |
| `target_class_name` | 不限制 | Slot 类；`target_class` 是别名 |
| `target_class_match` | `exact` | `exact` 或 `assignable` |
| `x_offset/y_offset` | `0` | 累积位置偏移 |
| `highlight_color` | 无 | 第一端 ARGB；`color` 是别名 |
| `color_2` | 第一端颜色 | 第二端 ARGB |

多个选择器同时存在时全部满足才匹配。示例与第三方协作语义见
[break-limit-inventory.md](break-limit-inventory.md)。

## `custom_textures` / `sprites`

`custom_textures` 字段：

| 字段 | 默认值 | 用途 |
|---|---|---|
| `texture_type` | `full` | `full`、`region`、`animation` |
| `texture` | 无 | 必需的 PNG 或动画目录 |
| `resource_type` | `resource_location` | `resource_location`、`gui_sprite`、`auto` |
| `x/y` | `0` | 目标坐标；`screen_x/screen_y` 是别名 |
| `width/height` | `16` | 目标尺寸 |
| `anchor` | `gui` | `gui`、`screen_center`、`screen` |
| `z` | `0` | 同层顺序和旧层推导 |
| `layer` | 负 z 为 underlay，否则 background | `underlay`、`background`、`foreground` |

`region` 增加 `image_x/image_y`（默认 0）、`image_width/image_height`（默认目标尺寸）和必需的
`texture_width/texture_height`。`sprites` 使用等价的 `u/v/source_width/source_height/tex_width/tex_height`，
并用 `full_texture` 指定整图；它的 `resource_type` 默认 `auto`。所有 GUI 浮点数必须有限且绝对值不超过
65536，目标尺寸必须大于 0。

`resource_location` 保留给定路径；v1 无 namespace 路径相对 `assets/Kyeitk/`，v2 无 namespace 路径属于
小写 `kyeitk` domain。`gui_sprite` 是 legacy 图集路径适配，不应用于动画。完整示例见
[custom-textures.md](custom-textures.md)。

## 动画 metadata

动画目录内固定读取 `animation.json`：

| 字段 | 默认值 | 用途 |
|---|---|---|
| `frames` | 无 | 必需，1..4096 个字符串或 `{texture,duration_ms}` |
| `frame_duration_ms` | `100` | 全局默认帧时长，1..600000 ms |
| `loop` | `true` | 到结尾后是否循环 |
| `condition` | `always` | 当前支持 `always`、`never` |
| `default_texture` | `<动画目录>.png` | 默认/静态回退纹理 |
| `missing_frame` | `use_default` | `use_default`、`skip`、`disable` |

相对帧路径从动画目录解析；`textures/...` 从 Kyeitk domain 根解析。v2 带 namespace 的帧只能使用
`Kyeitk:`/`kyeitk:`，跨 domain 帧会拒绝。

## 文字

`texts`：

| 字段 | 默认值 | 用途 |
|---|---|---|
| `text` | `""` | 文本或翻译键 |
| `x/y` | `0` | 相对 GUI 原点；`screen_x/screen_y` 是别名 |
| `color` | `#FFFFFF` | RGB/ARGB |
| `shadow` | `false` | 是否绘制阴影 |
| `translate` | `false` | 用 1.7.10 `I18n.format(text)` 解析 |

`font_rules` 必须至少有一个 selector：

| 字段 | 默认值 | 用途 |
|---|---|---|
| `text` | 不限制 | 精确匹配实际字符串 |
| `translation_key` | 不限制 | 翻译后精确匹配实际字符串 |
| `match_x/match_y` | 不限制 | 匹配原始 drawString 坐标 |
| `ordinal` | 不限制 | 当前前景绘制中的调用序号，范围 0..1024 |
| `x_offset/y_offset` | `0` | 匹配后的偏移 |
| `color` | 原调用颜色 | 替换颜色 |

多个 selector 是 AND 关系，规则按最终 definition 应用顺序匹配。新包优先使用 `font_rules`；标题/标签字段
只为 v1 的调用序号语义保留。

## Native v2 manifest

manifest 固定路径为 `kyeitk:redfoxexpand/index.json`：

```json
{
  "api_version": 2,
  "configs": ["redfoxexpand/config/inventory.json"]
}
```

`api_version` 只接受 `2`；`configs` 最多 1024 项，路径必须位于 `redfoxexpand/config/` 且为 `.json`。

## 预算、错误与回退

| 内容 | 上限 |
|---|---:|
| 单配置 / manifest | 1 MiB |
| animation metadata | 256 KiB |
| 单文件 definitions | 1024 |
| 单数组 entries | 4096 |
| 扫描到的 v1 资源 | 16384 |
| 相对路径长度 | 512 |
| 单 PNG 边长 | 4096 |
| 单 PNG 像素 | 16 MiPixel |
| 单 reload 动态纹理像素 | 64 MiPixel |

JSON 语法/类型、未知枚举、非法路径、缺图、解码和超预算错误均记录来源并跳过所属文件。坏的 v2 manifest
只跳过该 manifest；坏的新格式不会关闭 legacy。F3+T 顶层失败时发布空快照并释放旧动态纹理，避免 stale
状态。Mixin 注入被其他 coremod 改写时相应视觉功能可能降级，必须查看日志和实机表现。

## 兼容性

Kyeitk v1 是与当前 1.8.9 项目共用包的接口；native v2 是本 1.7.10 项目的扩展。第三方 Mod GUI、
OptiFine/coremod、F3+T、药水效果布局和 OpenGL 状态恢复必须在真实客户端组合中验证。
