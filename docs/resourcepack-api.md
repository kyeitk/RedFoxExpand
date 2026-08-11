# RedFoxExpand 材质包开发者 API

本文档对应 RedFoxExpand `0.2.0`、Minecraft Java Edition `1.7.10` 和 Forge
`10.13.4.1614`。它既是 API 参考，也是资源包迁移与排错手册。后续修改解析器字段、默认值、
合并顺序或示例时，必须同步维护本文档。

## 0.2.0 Schema 选择

RedFoxExpand 1.7.10 同时提供三条兼容入口：

| Schema | 发现入口 | 用途 | 兼容行为 |
|---|---|---|---|
| v1 | `assets/Kyeitk/config/**/*.json` | 既有 1.7.10 Kyeitk 格式 | 字段与 0.1.0 保持兼容 |
| v2 | `assets/kyeitk/redfoxexpand/index.json`，`api_version: 2` | 严格 Definition/manifest | 未知字段、错误类型直接拒绝 config |
| v3 | 同一 manifest，`api_version: 3` | v2 + Reactive UI | v2 Sprite 必须增加稳定 `id` 才能成为 target |

大写 v1、旧 Polytone 与小写 v2/v3 会同时成为候选；任何一种格式都不会全局关闭另一种格式。
重复规则应通过稳定 Definition ID 与 `append/replace/disable` 管理。v2/v3 之间不做字段猜测或自动升级。

### Native manifest

```json
{
  "api_version": 3,
  "configs": ["redfoxexpand/config/inventory.json"]
}
```

- `api_version`：必填 integer，仅允许 `2` 或 `3`；
- `configs`：必填 string array，默认无，最多 256 项；
- 路径必须安全、为小写 `kyeitk:redfoxexpand/config/*.json`，并从声明它的同一个资源包读取；
- 1.7.10 的 `IResource` 不公开资源包名称，因此实现直接保留实际 `IResourcePack` 所有权并从该 pack
  打开 config，不按 `getAllResources()` 索引猜测来源；
- manifest 错误只隔离该 manifest；config 错误隔离该 config；纹理错误隔离对应 Definition；
- 顶层 reload 构建失败时保留上一 immutable generation；成功时再原子替换并销毁旧运行态。
- v2/v3 Sprite 实际首次绑定且 TextureManager 中尚无同 ID 对象时，该 `SimpleTexture` 归当前
  RedFoxExpand native 缓存所有；下一次资源 reload 在原版遍历纹理缓存前删除该对象及 map entry，
  因而从 v2/v3 包切到另一包不会继续请求上一包 PNG。原版或其他 Mod 已缓存的同 ID 纹理不会被认领。

### v2/v3 Definition 公共字段

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---:|---|
| `id` | string | 必填 | 小写 namespaced ID，如 `example:inventory` |
| `operation` | string | `append` | `append` / `replace` / `disable` |
| `priority` | integer | `0` | 同一 pack priority 内的 Definition 顺序 |
| `match` | object | 必填 | 严格 matcher；对象必须恰好有一个 operator |
| `geometry` | object | 全 0 | `x/y/width/height_offset` |
| `slot_modifiers` | array | `[]` | Slot 选择、位移与高亮 |
| `sprites` | array | `[]` | 贴图、UV、颜色、层、锚点、可选帧动画；v3 要求 `id` |
| `texts` | array | `[]` | 固定/翻译文字、颜色、阴影、层与锚点 |
| `text_rules` | array | `[]` | `title` / `player_inventory` 位移、颜色或隐藏 |
| `bindings` | array | `[]` | 仅 v3 |
| `animations` | array | `[]` | 仅 v3 Property Animation，不是纹理帧动画 |
| `behaviors` | array | `[]` | 仅 v3 |

候选应用顺序为 `pack priority -> definition priority -> source path -> array index`。`replace`/`disable`
先按稳定 ID 移除较低候选；`disable` 本身不进入 active 列表。

### 1.7.10 matcher

可用 operator：

```text
all / any / not
exact_screen_class / assignable_screen_class
exact_screen_simple_class / assignable_screen_simple_class
exact_menu_class / assignable_menu_class
exact_menu_simple_class / assignable_menu_simple_class
screen_title_key / screen_title_text
```

full class matcher 必须写全限定类名，simple matcher 必须显式使用 `*_simple_class`。现代
`net.minecraft.world.inventory.InventoryMenu` 兼容映射到 1.7.10 的
`net.minecraft.inventory.ContainerPlayer`。1.7.10 没有对应注册表数据，因此 `menu_type`、
`resource_location`、`mod_namespace` 会在重载时明确报错，不会静默为 false。

### Texture object 与帧动画

```json
{
  "texture": {
    "type": "pack_resource",
    "location": "textures/gui/character.png"
  },
  "animation": {
    "frame_duration_ms": 100,
    "loop": true,
    "condition": "always",
    "missing_frame": "use_default",
    "frames": [{
      "texture": {"type":"pack_resource","location":"textures/gui/frame_0.png"},
      "duration_ms": 120
    }]
  }
}
```

`texture.type` 为 `resource_location`、`gui_sprite` 或 `pack_resource`。1.7.10 没有 GUI atlas，
`gui_sprite` 兼容解析为 `textures/gui/sprites/<path>.png`。帧 `duration_ms` 默认继承
`frame_duration_ms`（100），范围 1..600000；`condition` 为 `always/never`；`missing_frame` 为
`use_default/skip/disable`。资源在 reload 阶段校验 PNG 签名、单边 4096、单图/动画/generation 像素预算；
render tick 不读文件或解析 JSON。

### Schema v3 Reactive API

Schema v3 的完整变量、表达式语法、Binding、Event、Property、Animation、Behavior、Action、优先级、预算和
生命周期见 [`SCHEMA_V3.md`](SCHEMA_V3.md)。公开扩展摘要：

- `bindings[]`：`target`、`property`、`value`，numeric property 可选 `smoothing_ms`，默认 `0`；
- `animations[]`：稳定 `id`、`duration_ms`、`loop`（默认 false）、property tracks/keyframes；
- `behaviors[]`：`on.event`，health event 可选 `every`，`mode` 仅 `coalesce`，`if` 默认 `true`；
- action：`play_animation`、`stop_animation`、`set_visible`、`set_alpha`；
- property：`visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z`；
- Runtime Context 包含 player、screen、gui 与 `mouse.x/y/gui_x/gui_y/left_down/right_down`。

Reactive target 目前只能是同一 Definition 中的 v3 Sprite ID。表达式、事件、Action、Property 或 target
引用错误会在 reload 阶段拒绝 config；运行时表达式错误会按来源 key 限流记录（每个 GUI 同 key 一次、最多 64 个），只回退该次属性求值并保留基础值。所有 transform
均为临时合成，不修改基础 x/y/width/height；动画停止、GUI 关闭或 generation 替换后返回基础值。

以下各节保留 v1 API 的完整字段参考。

## 1. v1 适配契约

0.2.0 继续保留玩家背包的 v1 兼容语义：Kyeitk 相对路径、`screen_center`、`underlay`、浮点尺寸与
完整纹理取样均与 0.1.0 一致。公开仓库不包含内部测试代码、参考材质包、第三方图片或其 ZIP；
全新克隆仍可独立构建，且不会改变或扩大任何第三方素材授权。

## 2. 文件位置与加载

配置文件必须放在已启用资源包的固定目录中，可继续建立子目录：

```text
assets/Kyeitk/config/**/*.json
```

纹理放在：

```text
assets/Kyeitk/textures/gui/picture.png
```

对应 JSON：

```json
"texture": "textures/gui/picture.png"
```

第三方 Mod 专用配置和图片分别放在
`assets/Kyeitk/compatibility/<modid>/config/` 与该目录的 `textures/gui/` 下。目录和文件名建议只使用
小写 ASCII、数字、下划线、短横线和 `/`。JSON 必须是严格 JSON，不能包含注释或尾随逗号。
根节点可以是一个对象，也可以是由多个 definition 对象组成的数组。

加载发生在游戏启动和资源重载（通常为 `F3+T`）时。扫描、JSON 解析、PNG 解码和动画帧缓存都在
这一阶段完成。重载会建立新的不可变配置/纹理快照并刷新当前容器 GUI；GUI 尺寸和槽位先恢复基础值
再重应用，避免累计漂移。路径不得是系统绝对路径或包含 `.`/`..` 段。

## 3. 最小配置

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "anchor": "screen_center",
      "x": -200,
      "y": -100,
      "width": 150,
      "height": 200,
      "layer": "underlay"
    }
  ]
}
```

`target_type` 和 `target` 决定配置应用到哪个界面，其余字段均为修改内容。

## 4. 顶层字段

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `target_type` | 字符串 | `container_class` | `screen_class`、`container_class`、`menu_class` 或 `screen_title` |
| `target` | 字符串 | 无 | 必需，目标类名或标题匹配式 |
| `class_match` | 字符串 | `exact` | 类目标使用 `exact` 或 `assignable`；`screen_title` 不使用此字段 |
| `x_offset` | 整数 | `0` | GUI 整体水平偏移 |
| `y_offset` | 整数 | `0` | GUI 整体垂直偏移 |
| `width_offset` | 整数 | `0` | GUI 逻辑宽度增量 |
| `height_offset` | 整数 | `0` | GUI 逻辑高度增量 |
| `title_x_offset` | 整数 | `0` | 原版标题水平偏移 |
| `title_y_offset` | 整数 | `0` | 原版标题垂直偏移 |
| `label_x_offset` | 整数 | `0` | 原版玩家物品栏标签水平偏移 |
| `label_y_offset` | 整数 | `0` | 原版玩家物品栏标签垂直偏移 |
| `title_color` | 颜色 | 未覆盖 | 原版标题颜色 |
| `label_color` | 颜色 | 未覆盖 | 原版玩家物品栏标签颜色 |
| `slot_modifiers` | 数组 | `[]` | 槽位位置和悬停高亮规则 |
| `sprites` | 数组 | `[]` | Polytone/旧格式兼容贴图 |
| `custom_textures` | 数组 | `[]` | 推荐的新格式任意位置贴图 |
| `texts` | 数组 | `[]` | 前景文字叠加 |
| `font_rules` | 数组 | `[]` | v1 显式按文字/翻译结果/坐标/调用序号调整原版文字 |

尺寸变更后的容器原点公式为：

```text
guiLeft = baseGuiLeft + x_offset - width_offset / 2
guiTop  = baseGuiTop  + y_offset - height_offset / 2
```

除法为 Java 整数除法。需要精确对称扩展时，建议使用偶数尺寸增量。

在 1.7.10 玩家背包中，原版 `InventoryEffectRenderer.initGui` 会根据药水效果重算并左移水平原点。
Mod 会在初始化后恢复修改尺寸对应的居中原点并保存最终 `guiLeft`。因此背景、槽位、人物模型、文字和
`anchor: gui` 贴图在有/无药水时使用相同位置，配置作者不需要增加第二份补偿。

药水效果列表保留原版内容和垂直排列，但水平位置改为 `guiLeft + xSize + 4`。若右侧空间不足，
140 像素宽的列表会钳制到屏幕右边界内；极窄窗口下可能与 GUI 重叠，但不会继续把 GUI 本身推离中心。

## 5. 目标匹配

### `screen_class`

匹配 `GuiContainer` 界面类，可写完整类名或简单类名。完整类名只匹配该完整名称，不会退化为简单
类名；简单类名仍只比较简单名称，存在跨 Mod 同名碰撞风险：

```json
{
  "target_type": "screen_class",
  "target": "GuiInventory"
}
```

### `container_class` / `menu_class`

两者等价，匹配 1.7.10 的 `Container` 类。推荐写 1.7.10 原生完整类名：

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer"
}
```

为迁移现代 Polytone 包，以下名称会自动映射到 `ContainerPlayer`：

```text
net.minecraft.world.inventory.InventoryMenu
InventoryMenu
```

上述 `InventoryMenu` 映射同时适用于 strict v2/v3。v1 兼容层还显式维护 Chest、Crafting、Furnace、
Anvil、Merchant、BrewingStand、Hopper、Dispenser、Beacon、Enchantment 与 HorseInventory 的常用
现代 `*Menu` 名；strict v2/v3 不会猜测这些额外名称，推荐直接写 1.7.10 Container 完整类名。

类目标的 `class_match` 默认为 `exact`，只匹配运行时类本身。需要让基类或接口规则覆盖子类时显式启用：

```json
{
  "target_type": "container_class",
  "target": "example.api.BaseContainer",
  "class_match": "assignable"
}
```

`assignable` 会遍历运行时类的父类和接口；找不到匹配类时返回不匹配，不会退回标题或简单路径猜测。

### `screen_title`

匹配实际标题、未翻译文本或翻译键，支持 `*` 通配符：

```json
{
  "target_type": "screen_title",
  "target": "container.*"
}
```

所有命中的目标都会参与合并，固定由低到高按以下顺序应用：

```text
screen_title -> container_class/menu_class -> screen_class
```

因此通用标题规则可以与容器规则叠加，最具体的界面类规则最后应用。偏移字段仍累加，颜色由合并顺序中
最后一个非空值覆盖，列表按该顺序追加。同一目标类型内保持配置来源顺序。

## 6. `custom_textures`：推荐贴图 API

### 6.1 通用字段

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `texture_type` | 字符串 | `full` | `full` 整图、`region` 局部取图或 `animation` 动画目录 |
| `texture` | 字符串 | 无 | 必需；静态图为 Kyeitk 相对 PNG，动画为含 `animation.json` 的目录 |
| `resource_type` | 字符串 | `resource_location` | `resource_location`、`gui_sprite` 或 `auto`；动画不允许 `gui_sprite` |
| `anchor` | 字符串 | `gui` | `gui`、`screen_center` 或 `screen` |
| `x` | 小数 | `0` | 相对锚点的显示 X |
| `y` | 小数 | `0` | 相对锚点的显示 Y |
| `width` | 小数 | `16` | 游戏内最终显示宽度，必须大于 0 |
| `height` | 小数 | `16` | 游戏内最终显示高度，必须大于 0 |
| `layer` | 字符串 | `background` | `underlay`、`background` 或 `foreground` |

位置和尺寸单位都是 Minecraft GUI 逻辑像素，支持负数位置和小数尺寸，并随游戏 GUI 缩放变化。

资源解析语义：

- `resource_location`：原样解析 Kyeitk 相对路径或 `namespace:path`，不根据扩展名猜测；
- `gui_sprite`：将 `namespace:id` 映射为 `namespace:textures/gui/sprites/id.png`；
- `auto`：保留旧规则——路径不以 `textures/` 开头且不以 `.png` 结尾时按 GUI sprite 处理。

推荐新配置使用默认 `resource_location`，仅在确实填写 GUI sprite ID 时显式使用 `gui_sprite`。

### 6.2 坐标锚点

| `anchor` | `(0,0)` 所在位置 | 适用场景 |
|---|---|---|
| `gui` | 修改后 GUI 左上角 | 与槽位、标题或面板绑定的装饰 |
| `screen_center` | 当前游戏窗口逻辑中心 | 左右立绘、居中背景、超限边框 |
| `screen` | 当前游戏窗口逻辑左上角 | 固定屏幕位置的装饰 |

以 `screen_center` 为例，宽 `150` 的图片水平居中应设置 `x=-75`。

### 6.3 渲染层

```text
underlay custom texture
  -> 原版容器背景
  -> background custom texture
  -> 槽位与物品
  -> 原版标题和标签
  -> foreground custom texture
  -> texts
```

- `underlay`：适合人物立绘、扩展外框以及需要被原版背包遮挡的图片；
- `background`：适合原版面板上方、物品下方的背景细节；
- `foreground`：适合提示和遮罩，可能遮住物品或文字。

绘制时会临时解除 scissor/depth 与 Alpha Test 限制并启用标准 Alpha 混合，使图片能显示局部
半透明和原版 GUI 矩形外像素；结束后恢复调用前的 RGBA、Alpha 函数/阈值、纹理绑定、混合因子及
texture/blend/alpha/depth/scissor 开关。超出实际游戏窗口的部分仍会被 OpenGL 视口裁切。

### 6.4 整图模式 `full`

`full` 始终采样整张 PNG，原图物理尺寸与游戏显示尺寸互不绑定：

```json
{
  "texture_type": "full",
  "texture": "textures/gui/inventory.png",
  "anchor": "screen_center",
  "x": -245,
  "y": -109,
  "width": 150,
  "height": 218,
  "layer": "underlay"
}
```

此例把 `600×872` 图片缩放为 `150×218`，即 25%。

### 6.5 局部取图模式 `region`

```json
{
  "texture_type": "region",
  "texture": "textures/gui/picture.png",
  "anchor": "gui",
  "x": 180,
  "y": 10,
  "width": 72,
  "height": 81,
  "image_x": 17,
  "image_y": 67,
  "image_width": 288,
  "image_height": 324,
  "texture_width": 600,
  "texture_height": 872,
  "layer": "background"
}
```

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `image_x` | 小数 | `0` | 原图取样区域左上角 X |
| `image_y` | 小数 | `0` | 原图取样区域左上角 Y |
| `image_width` | 小数 | `width` | 原图取样区域宽度，必须大于 0 |
| `image_height` | 小数 | `height` | 原图取样区域高度，必须大于 0 |
| `texture_width` | 小数 | 无 | 整张 PNG 物理宽度，`region` 必需且必须大于 0 |
| `texture_height` | 小数 | 无 | 整张 PNG 物理高度，`region` 必需且必须大于 0 |

UV 计算公式：

```text
minU = image_x / texture_width
minV = image_y / texture_height
maxU = (image_x + image_width) / texture_width
maxV = (image_y + image_height) / texture_height
```

### 6.6 动画模式 `animation`

`animation` 的 `texture` 指向含 `animation.json` 的目录：

```json
{
  "texture_type": "animation",
  "texture": "textures/gui/inventory",
  "anchor": "gui",
  "x": 0,
  "y": 0,
  "width": 176,
  "height": 166,
  "layer": "background"
}
```

对应 `assets/Kyeitk/textures/gui/inventory/animation.json`：

```json
{
  "frames": [
    "frame_0.png",
    { "texture": "frame_1.png", "duration_ms": 150 }
  ],
  "frame_duration_ms": 100,
  "loop": true,
  "condition": "always",
  "default_texture": "textures/gui/inventory.png",
  "missing_frame": "use_default"
}
```

- `frames` 必须有 1–4096 项；裸文件名相对于动画目录。
- `duration_ms` / `frame_duration_ms` 范围为 1–600000 毫秒。
- `condition` 当前为 `always` 或 `never`。
- `loop=false` 播完后显示默认图。
- `missing_frame` 为 `use_default`、`skip` 或 `disable`。
- `default_texture` 省略时自动使用动画目录同名 PNG，例如目录 `inventory/` 对应 `inventory.png`。

完整回退语义与 Alpha 制作要求见 [`GUI_RESOURCE_FORMAT.md`](GUI_RESOURCE_FORMAT.md)。

## 7. `sprites`：旧格式兼容 API

新资源包优先使用 `custom_textures`。`sprites` 用于兼容旧 Slotify/Polytone 字段，在 Kyeitk 配置内
也可以继续使用：

| 字段 | 默认值 | 说明 |
|---|---|---|
| `texture` | 无 | 必需；可以是完整 PNG 路径或现代 GUI 图集 ID |
| `resource_type` | `auto` | `resource_location`、`gui_sprite` 或旧启发式 `auto` |
| `x` / `y` | `0` | 显示位置；`screen_x/screen_y` 是高优先级别名 |
| `z` | `0` | 未显式写 `layer` 时，负值映射为 `underlay` |
| `width` / `height` | `16` | 游戏显示尺寸 |
| `anchor` | 见下文 | 与新 API 相同 |
| `layer` | 见下文 | 与新 API 相同 |
| `u` / `v` | `0` | 区域取样左上角 |
| `source_width` / `source_height` | 显示尺寸 | 区域取样宽高 |
| `tex_width` / `tex_height` | `256` | 整张纹理尺寸；`texture_width/texture_height` 是别名 |
| `full_texture` | 自动判断 | `true` 强制整图，`false` 使用区域取样 |

现代图集 ID 如 `minecraft:inventory` 会解析为：

```text
minecraft:textures/gui/sprites/inventory.png
```

此类 ID 未写 `anchor` 时默认 `screen_center`；显式 `textures/...png` 路径默认 `gui`。未写
`layer` 时，负 `z` 为 `underlay`，其他为 `background`。

## 8. `slot_modifiers`

```json
{
  "slots": [0, "1-4", "8-5"],
  "target_x": 124,
  "target_y": 35,
  "target_class_name": "SlotCrafting",
  "x_offset": 2,
  "y_offset": 0,
  "highlight_color": "#80FF4040",
  "color_2": "#20302020"
}
```

### 选择字段

| 字段 | 说明 |
|---|---|
| `slots` | 槽位索引：单个数字、数组、字符串数字或包含首尾的范围；正序/倒序均支持 |
| `target_x` | 匹配槽位修改前的基础 X |
| `target_y` | 匹配槽位修改前的基础 Y |
| `target_class_name` | 匹配 Slot 完整类名或简单类名；`target_class` 是别名 |
| `target_class_match` | `exact`（默认）只匹配该 Slot 类；`assignable` 同时匹配其子类/接口实现 |

多个选择字段同时存在时使用 AND 逻辑。没有任何选择字段时匹配该容器的全部槽位。

### 修改字段

| 字段 | 默认值 | 说明 |
|---|---|---|
| `x_offset` | `0` | 槽位水平偏移，整数 |
| `y_offset` | `0` | 槽位垂直偏移，整数 |
| `highlight_color` | 未覆盖 | 悬停渐变第一颜色；`color` 是别名 |
| `color_2` | 第一颜色 | 悬停渐变第二颜色 |

多个规则命中同一槽位时，位置偏移会依次累加；高亮使用第一个含颜色的命中规则。

## 9. `texts`

```json
{
  "text": "RedFoxExpand",
  "x": 92,
  "y": 6,
  "color": "#FFFFFF",
  "shadow": true,
  "translate": false
}
```

| 字段 | 默认值 | 说明 |
|---|---|---|
| `text` | 空字符串 | 要绘制的文本或翻译键 |
| `x` / `y` | `0` | 相对 GUI 左上角的整数位置；`screen_x/screen_y` 是高优先级别名 |
| `color` | `#FFFFFF` | 文本颜色 |
| `shadow` | `false` | 是否绘制原版文字阴影 |
| `translate` | `false` | `true` 时通过 1.7.10 `I18n.format` 解析 `text` |

文字在前景贴图之后绘制。

## 10. v1 `font_rules`

`font_rules` 至少需要一个 selector：`text`、`translation_key`、`match_x`、`match_y` 或 `ordinal`。
多个 selector 为 AND；匹配后应用 `x_offset/y_offset` 和可选 `color`。它比旧标题/标签调用序号字段更稳定，
但仍只属于 v1；strict v2/v3 使用 `text_rules` 的 `title/player_inventory` selector。

## 11. 颜色格式

支持：

```text
十进制整数
#RRGGBB
#AARRGGBB
0xRRGGBB
0xAARRGGBB
```

下划线会被忽略，例如 `0x80_FF_40_40`。槽位渐变建议始终使用 `#AARRGGBB` 明确 alpha。

## 12. 多文件与资源包优先级

- 同一 Kyeitk 相对路径存在于多个资源包时，由 RedFoxExpand 按 Minecraft 已启用资源包顺序选择
  最高优先级版本；
- 不同目标类型按 `screen_title`、`container_class/menu_class`、`screen_class` 合并；
- 同一匹配级别命中的不同资源路径按完整资源位置的字典序合并；
- `x/y/width/height` 等偏移字段累加；
- `title_color/label_color` 由合并顺序中最后一个非空值覆盖；
- 槽位、贴图和文字列表按合并顺序追加；
- 单个 modifier 内，`sprites` 总是在 `custom_textures` 之前加入渲染列表；
- 同一渲染层中的图片按列表顺序绘制，后绘制的透明图片覆盖先绘制图片的重叠像素。
- legacy Polytone、v1 与 native v2/v3 会共同产生候选；同 ID 的 `replace/disable` 决定覆盖，不使用
  “发现新格式就关闭旧格式”的全局开关。

为了让层级和覆盖关系容易维护，建议一个界面的背景与立绘放在同一个 JSON 中，并显式填写
`texture_type`、`anchor` 和 `layer`。

## 13. 完整玩家背包示例

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "anchor": "screen_center",
      "x": -210,
      "y": -88,
      "width": 307.5,
      "height": 180,
      "layer": "underlay"
    },
    {
      "texture_type": "full",
      "texture": "textures/gui/rw.png",
      "anchor": "screen_center",
      "x": -245,
      "y": -109,
      "width": 150,
      "height": 218,
      "layer": "underlay"
    }
  ]
}
```

只修改 `x/y` 可以移动图片，只修改 `width/height` 可以缩放图片，不需要重新编译 Mod。

## 14. 错误处理与排错

以下情况会在日志中记录错误并拒绝相应配置：

- 缺少非空 `target` 或 `texture`；
- 未知 `target_type`、`class_match`、`target_class_match`、`texture_type`、`resource_type`、`anchor` 或 `layer`；
- 动画贴图使用 `resource_type: gui_sprite`；
- 显示宽高或取样宽高不为正数；
- `region` 缺少有效的 `texture_width/texture_height`；
- 颜色不是合法整数、6 位 RGB 或 8 位 ARGB；
- 槽位范围或 JSON 类型非法；
- 绝对路径、`.`/`..` 路径段、Kyeitk PNG 缺失或无法解码；
- 动画帧数、时长、条件、默认图或缺帧策略非法。

排错顺序：

1. 检查游戏日志中的 `RedFoxExpand` / `Invalid Kyeitk GUI config`；
2. 检查 JSON 能否被严格解析；
3. 检查物理目录是否为 `assets/Kyeitk/`，相对纹理路径是否与大小写完全一致；
4. 临时只保留一张 `custom_textures` 图片；
5. 使用 `screen_center` 和明显的 `x/y` 验证坐标；
6. 执行 `F3+T` 后重新打开目标 GUI。

## 15. 当前限制

- 仅作用于客户端 Minecraft 1.7.10 Forge 容器 GUI；
- 不支持 HUD、按钮/widget、通用 Component 或 Semantic Slot；
- v1 目录帧动画条件仍只支持 `always/never`；Schema v3 的状态表达式和属性动画应使用
  `bindings/animations/behaviors`；
- 不支持独立调整玩家 3D 模型位置；
- native v2/v3 检查 PNG/像素预算，但不会要求 JSON 声明尺寸等于 PNG 原始尺寸；
- `menu_type`、`resource_location`、`mod_namespace` matcher 在 1.7.10 明确不可用；
- Reactive target 仅限 v3 Sprite；text/Slot target 尚未实现；
- width、height、color Binding、Texture State、自定义旋转枢轴、custom variable/event、Timer、
  User Function、loop/recursion、`every.mode=repeat` 与 Inspector 尚未实现；
- 无法取得后备文件的自定义 `IResourcePack` 不能提供大写 Kyeitk 目录；
- 游戏内视觉效果、不同 GUI 缩放及与其他 Mod 的组合效果需由使用者验证。

## 16. Java 扩展接口

这些接口面向后续 RedFoxExpand 模块复用，使用 Java 8；0.2.0 尚不承诺跨大版本二进制稳定性：

| 接口 | 方法 | 用途与默认行为 |
|---|---|---|
| `client.render.GuiTexture` | `textureAt(long nowMillis)` | 返回已缓存的当前帧，不允许在此方法扫描文件或解析 JSON |
| `client.render.GuiTexture` | `isAnimated()` | 静态实现返回 `false`，动画实现返回 `true` |
| `client.config.GuiTextureResolver` | `resolveStatic(path, legacyGuiAtlasId)` | 把配置路径解析为静态统一贴图；无隐式绝对路径回退 |
| `client.config.GuiTextureResolver` | `resolveAnimation(directory)` | 在重载阶段解析动画目录并返回统一贴图 |
| `client.gui.ClassMatchMode` | `EXACT` / `ASSIGNABLE`、`parse(value)` | 配置类目标的精确或父类/接口匹配；未知值抛出 `IllegalArgumentException` |
| `client.render.AnimationPlaybackCondition` | `shouldPlay()` | 动画选帧前判断是否播放；内置配置映射为 `always/never` |

`GuiConfigLoader.load(source, reader, resolver)` 按单个 JSON 文件原子返回不可变 `GuiDefinition` 列表。

文档最后同步日期：2026-08-11。
