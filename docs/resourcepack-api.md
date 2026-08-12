# RedFoxExpand Resource-Pack API（Minecraft 26.2 Fabric）

本文是 26.2 项目的资源发现入口与 Schema v2 基础 Definition 规范。v2/v3/v3.1 均严格校验：未知字段、错误
类型、非有限数字、越界值或路径穿越都会产生带来源的错误，不会静默回退到拼写相近的字段。

## 0. Protocol 关系

```text
v1 = Legacy Compatibility Protocol（26.2 不作为主协议读取）
v2 = Strict Definition / Manifest Protocol
v3 = v2 + Stable Element ID + Reactive UI Runtime
v3.1 = v3 + Scene Graph + Authoring/Composition primitives
```

不需要响应式行为的资源包继续使用 v2；需要 RuntimeContext、Expression、Binding、Event、Behavior、Action
与 Property Animation 时使用 v3。v3 完整规范、默认值、预算、错误/回退和示例见
[SCHEMA_V3.md](SCHEMA_V3.md)。
需要 Group/Parent 局部坐标、九点 Anchor、Pivot、Constants、Derived Values、`self/parent` 几何变量或显式
动画合成时使用 v3.1；完整正式规范见 [SCHEMA_V3_1.md](SCHEMA_V3_1.md)。

### Schema v3 鼠标状态入口

26.2 当前公开以下严格变量：

```text
mouse.x / mouse.y                 number，GUI-scaled 屏幕绝对坐标
mouse.gui_x / mouse.gui_y         number，相对实时 gui.x/gui.y
mouse.left_down / mouse.right_down boolean，当前 tick 持续按下状态
```

示例：

```json
{
  "bindings": [
    {"target":"cursor_hint","property":"visible","value":"mouse.left_down || mouse.right_down"},
    {"target":"cursor_hint","property":"translate_x","value":"mouse.gui_x"},
    {"target":"cursor_hint","property":"translate_y","value":"mouse.gui_y"}
  ]
}
```

坐标不做 GUI 边界 clamp；按键未按住时值为 `false`，且不是 click Event。26.2 native 坐标为非有限值时绝对坐标回退为 `0`，
GUI 相对坐标再以 `0 - gui.x/y` 计算。缺少 `MOUSE_POSITION` 或 `MOUSE_BUTTONS` capability 的旧平台会在
reload 拒绝引用对应变量的 v3 config；不会静默模拟。完整类型和兼容性见
[Schema v3 Runtime Context](SCHEMA_V3.md#7-runtime-context)。

### Schema v3 变换、平滑与 Screen 生命周期

26.2 还支持数值属性 `scale_x`、`scale_y`（Base `1`，最终范围 0..8）、`rotation_z`（Base `0`，单位为度，
最终范围 -360..360）以及无 payload 的 `screen.opened` Event。缩放与旋转均围绕 Sprite 中心完成，不修改
配置中的 `x/y/width/height`。数值 Binding 可声明 `smoothing_ms`（默认 `0`，0..600000）；非零时以该值
作为时间常数做 render-time 指数平滑，初次值立即建立，之后的 tick 目标变化不会逐帧跳变。boolean Binding
禁止声明该字段。表达式另提供 `hypot(x,y)`，动画轨道可选 `linear` 或 `smoothstep`：

```json
{
  "animations": [{
    "id": "bounce",
    "duration_ms": 800,
    "loop": true,
    "tracks": [
      {"property":"scale_x","interpolation":"smoothstep","keyframes":[{"time_ms":0,"value":1},{"time_ms":400,"value":0.98},{"time_ms":800,"value":1}]},
      {"property":"scale_y","interpolation":"smoothstep","keyframes":[{"time_ms":0,"value":1},{"time_ms":400,"value":1.03},{"time_ms":800,"value":1}]}
    ]
  }],
  "bindings": [{
    "target":"character", "property":"rotation_z",
    "value":"clamp((mouse.x - screen.width / 2) * 0.01, -4, 4)",
    "smoothing_ms":180
  }],
  "behaviors": [{
    "on":{"event":"screen.opened"},
    "actions":[{"type":"play_animation","target":"character","animation":"bounce"}]
  }]
}
```

`screen.opened` 在该 Screen 的 reactive runtime 创建时触发一次；普通 resize/Recipe Book 布局更新不会重复
触发，F3+T 或关闭重开造成 runtime 重建时会重新触发。缺少 `PROPERTY_SCALE`、`PROPERTY_ROTATION` 或
`EVENT_SCREEN_LIFECYCLE` capability 的平台会在 reload 明确拒绝相关配置。完整合成、错误和回退语义见
[Schema v3 Property](SCHEMA_V3.md#15-property) 与 [Lifecycle](SCHEMA_V3.md#20-lifecycle)。

## 1. 发现协议

Minecraft 26.2 的 `pack.mcmeta` 必须声明完整的资源包格式范围：

```json
{
  "pack": {
    "pack_format": 88,
    "min_format": 88,
    "max_format": 88,
    "description": "RedFoxExpand 26.2 resource pack"
  }
}
```

缺失 `min_format` 或 `max_format` 时，Minecraft 会将格式高于 64 的包判为元数据错误；这发生在
RedFoxExpand 读取 manifest 之前，Mod 不会绕过原版资源包兼容性判断。

每个启用包可提供：

```text
assets/kyeitk/redfoxexpand/index.json
```

```json
{
  "api_version": 2,
  "configs": [
    "redfoxexpand/config/inventory.json"
  ]
}
```

字段：

| 字段 | 类型 | 默认 | 说明 |
|---|---|---:|---|
| `api_version` | number | 必填 | `2`、`3` 或 `3.1`；它引用的 config 必须使用相同版本 |
| `configs` | string[] | 必填 | 最多 256 个；必须位于 `kyeitk:redfoxexpand/config/` 且以 `.json` 结尾 |

Mod 使用 `ResourceManager.getResourceStack` 读取所有 manifest，并只从同一个 `sourcePackId` 取得该
manifest 指向的配置。文件夹包、ZIP 包、Mod 内置资源与服务端资源包因此共用同一条原生路径，不依赖
物理文件。

## 2. Schema v2 配置根与 Definition

```json
{
  "api_version": 2,
  "definitions": [{
    "id": "example:inventory",
    "operation": "replace",
    "priority": 100,
    "match": {"exact_menu_class": "net.minecraft.world.inventory.InventoryMenu"},
    "geometry": {},
    "slot_modifiers": [],
    "sprites": [],
    "texts": [],
    "text_rules": []
  }]
}
```

| 字段 | 类型 | 默认 | 说明 |
|---|---|---:|---|
| `id` | namespaced string | 必填 | 小写稳定 ID；不能用文件名隐式代替 |
| `operation` | enum | `append` | `append`、`replace`、`disable` |
| `priority` | integer | `0` | 同一 pack priority 内的显式次序 |
| `match` | matcher | 必填 | 一个且仅一个 matcher operator |
| `geometry` | object | 全 0 | GUI 逻辑几何 |
| `slot_modifiers` | array | `[]` | 最多 256 条 |
| `sprites` | array | `[]` | 最多 256 条 |
| `texts` | array | `[]` | 最多 128 条 |
| `text_rules` | array | `[]` | 最多 64 条 |

候选排序固定为 `pack priority -> definition priority -> source path -> array index`。`replace` 先删除所有
同 ID 的较早活动项再加入自身；`disable` 删除但不加入；`append` 保留同 ID 的较早项并追加。

## 3. Matcher

每个 matcher object 只能包含一个 operator。最低支持集与附加显式 simple-name operator：

| Operator | 值 | 语义 |
|---|---|---|
| `exact_screen_class` | FQN | Screen 实际类完全相等 |
| `assignable_screen_class` | FQN | Screen 类/父类/接口集合包含 FQN |
| `exact_menu_class` | FQN | Menu 实际类完全相等 |
| `assignable_menu_class` | FQN | Menu 类/父类/接口集合包含 FQN |
| `exact_screen_simple_class` | simple name | 显式 simple-name 精确匹配 |
| `assignable_screen_simple_class` | simple name | 显式 simple-name 层级匹配 |
| `exact_menu_simple_class` | simple name | 显式 simple-name 精确匹配 |
| `assignable_menu_simple_class` | simple name | 显式 simple-name 层级匹配 |
| `screen_title_key` | string | `TranslatableContents#getKey` 完全相等 |
| `screen_title_text` | string | 最终显示文本；`*` 是 glob 通配符 |
| `menu_type` | identifier | `BuiltInRegistries.MENU` ID |
| `resource_location` | identifier | 背景阶段捕获的原始 GUI texture ID |
| `mod_namespace` | namespace | 优先取 GUI resource，其次 menu type，再取 vanilla/package namespace |
| `all` | matcher[] | 全部成立；至少一项 |
| `any` | matcher[] | 任一成立；至少一项 |
| `not` | matcher | 逻辑非 |

FQN operator 不会回退到 simple name。需要 simple name 时必须写对应显式 operator。

```json
{
  "all": [
    {"assignable_screen_class": "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen"},
    {"not": {"menu_type": "minecraft:creative_mode_inventory"}}
  ]
}
```

## 4. Geometry 与 GuiContext

`geometry` 字段均为 integer，默认 `0`：`x_offset`、`y_offset`、`width_offset`、`height_offset`。
实际 origin delta：

```text
dx = x_offset - width_offset / 2
dy = y_offset - height_offset / 2
```

26.2 平台上下文固定提供 screen/menu class 与层级、menu type、title/key、screen width/height、
`leftPos/topPos`、原版 `imageWidth/imageHeight`、Slot 列表、当前 GUI resource 与 mod namespace。
逻辑扩展不修改 final `imageWidth/imageHeight`，避免原版 UV 扩展和伪屏幕尺寸副作用。

## 5. Slot 规则

```json
{
  "slots": [0, "9-35"],
  "target_x": 8,
  "target_y": 18,
  "target_class_name": "net.minecraft.world.inventory.Slot",
  "target_class_match": "assignable",
  "target_class_name_type": "full",
  "x_offset": 2,
  "y_offset": 0,
  "highlight_color": "#80FF4040",
  "highlight_color_2": "#20302020"
}
```

所有 selector 可组合；缺省即不限制。`slots` 支持 integer、字符串 integer、升/降序范围，单范围最多
4096 项。class mode 为 `exact`/`assignable`，name type 为 `full`/`simple`；simple 必须显式声明。

Slot 位移修改实际 `Slot.x/y`。每次刷新先从当前坐标减去 RedFoxExpand 上一次施加的 delta，再按当前
规则重算，避免 F3+T、resize、recipe book 或其他 Mod 修改后累计漂移。多个命中规则的 offset 相加；
首条带颜色的命中规则负责悬停渐变。

## 6. Texture 与 Sprite

Texture 永远是显式对象：

```json
{"type":"resource_location","location":"minecraft:textures/gui/container/inventory.png"}
{"type":"gui_sprite","location":"minecraft:container/inventory/effect_background"}
{"type":"pack_resource","location":"textures/gui/panel.png"}
```

| 类型 | 解析 |
|---|---|
| `resource_location` | 原样作为 raw texture ID；必须带 namespace |
| `gui_sprite` | 原样作为 GUI atlas sprite ID；校验对应 `textures/gui/sprites/<path>.png` |
| `pack_resource` | 有 namespace 时原样；相对路径映射到 `kyeitk:redfoxexpand/<path>` |

不存在 `.png`/`textures/` 自动猜测。Sprite 字段：

| 字段 | 默认 | 说明 |
|---|---:|---|
| `texture` | 必填 | Texture object |
| `animation` | 无 | 动画对象 |
| `x`,`y`,`z`,`u`,`v` | `0` | `z` 用作同 layer 内排序，不调用深度 API |
| `width`,`height` | `16` | 目标尺寸，允许有限小数，提取时四舍五入 |
| `source_width`,`source_height` | 目标尺寸 | 区域源尺寸 |
| `texture_width`,`texture_height` | `256` | 区域纹理总尺寸 |
| `full_texture` | `true` | `false` 时使用 UV region；`gui_sprite` 只允许 `true` |
| `color` | `#FFFFFFFF` | ARGB 乘色/Alpha |
| `layer` | `background` | `underlay`/`background`/`foreground` |
| `anchor` | `gui` | `gui`/`screen_center`/`screen` |

锚点语义：

- `gui`：相对容器实时 `leftPos/topPos`；随配方书展开/收起、窗口 resize 和其他原版容器重新布局一起移动，
  需要与 Slot 保持相对位置的破限纹理应使用此值；
- `screen_center`：固定相对屏幕中心，不跟随配方书造成的容器偏移，适合独立屏幕装饰；
- `screen`：固定相对屏幕左上角。

层级对应：Screen 全屏 dim/blur 完成后且容器背景开始前的 underlay Mixin、`afterBackground`、最终 tooltip
提取之前的 foreground Mixin。underlay 因此不会被原版灰色遮罩覆盖，同时仍位于原版容器背景下。所有绘制
只调用 `GuiGraphicsExtractor` 与 `RenderPipelines.GUI_TEXTURED`。

## 7. Animation

```json
{
  "frame_duration_ms": 100,
  "loop": true,
  "condition": "always",
  "default_texture": {"type":"pack_resource","location":"textures/gui/default.png"},
  "missing_frame": "use_default",
  "frames": [{
    "texture": {"type":"pack_resource","location":"textures/gui/frame_0.png"},
    "duration_ms": 150
  }]
}
```

- `frames`：必填 1..512；每帧 texture 必填，`duration_ms` 缺省继承 100 ms；
- `loop` 默认 `true`；非循环结束后显示 default；
- `condition`：当前 `always`/`never`；默认 `always`；
- `default_texture` 缺省使用 Sprite 顶层 texture；必须有效；
- `missing_frame`：`use_default` 替帧、`skip` 跳帧、`disable` 整段退回 default。

`full_texture=true` 会把每一帧完整缩放到 Sprite 的目标 `width×height`，不会自动保持源图比例。动画各帧
应使用一致画布比例，目标宽高也应保持该比例；例如 `1348×1348` 帧应使用正方形目标尺寸。

JSON、资源存在性与 PNG 在 reload 阶段处理；运行阶段只按时间选择已校验 Identifier，不访问文件系统。

## 8. Text

显式 overlay：

```json
{
  "text": "container.inventory",
  "translate": true,
  "x": 8,
  "y": 6,
  "color": "#FFFFFFFF",
  "shadow": false,
  "layer": "foreground",
  "anchor": "gui"
}
```

`text` 必填；其余默认值如例。语义原版文字规则：

```json
{"selector":"title","x_offset":4,"y_offset":0,"color":"#FFB03030","hidden":false}
```

selector 仅为 `title` 或 `player_inventory`，不再把“全局前两次字体调用”隐式解释为标题/标签。多个规则
offset 相加，后出现的非空 color 覆盖，任一 `hidden=true` 隐藏该语义文本。

## 9. 预算、错误与回退

| 项目 | 默认上限 | 超限行为 |
|---|---:|---|
| 单 JSON | 1 MiB | 拒绝 manifest/config |
| 单 PNG 压缩字节 | 32 MiB | 拒绝相关 definition |
| manifest config / 单 pack config | 256 / 256 | 拒绝 manifest |
| definition / file | 256 | 拒绝 config |
| sprite/slot/text/text_rule | 256/256/128/64 | 拒绝 config |
| animation frame | 512 | 拒绝 config |
| path / JSON nesting | 512 chars / 32 | 拒绝输入 |
| PNG dimension / pixels | 4096 / 16,777,216 | 拒绝相关 definition |
| animation / reload pixels | 67,108,864 / 134,217,728 | 拒绝 definition 或后续纹理 |
| GUI 数值绝对值 | 65,536 | 拒绝 config |

Config 语法错误按文件隔离；Definition 的纹理/动画错误按 definition 隔离；缺帧按显式 policy；顶层
reload 未能形成候选 generation 时保留上一代。成功 prepare 后一次原子切换 N，再释放 N-1 引用。26.2
使用原生资源纹理，因此 Mod 不创建/持有动态 GPU texture。

Schema v3 在这些 v2 预算之上增加 binding/behavior/property-animation/expression/active-instance 预算；精确
数值和超限行为见 [Schema v3 Budget](SCHEMA_V3.md#21-budget)。
Schema v3.1 另增加 elements/groups/children/depth/constants/derived 预算，并在发布 generation 前校验场景
引用、单父级、无环、Anchor/Pivot 和动画 compose；见 [Schema v3.1 预算](SCHEMA_V3_1.md#25-全部预算与安全限制)。

## 10. 兼容性与 v1 迁移

26.2 不扫描 `assets/Kyeitk/`，也不接受旧 flat v1 JSON 作为主协议。迁移步骤：

1. namespace 改为 `assets/kyeitk/redfoxexpand/`；
2. 新增 `index.json`；
3. 配置包裹为 `api_version:2` + `definitions`；
4. `target_type/target/class_match` 改为单个 `match` object；
5. flat geometry 移入 `geometry`；
6. `custom_textures` 改为 `sprites`，texture 改为显式对象；
7. `title_*`/`label_*` 改为显式 `text_rules`；
8. 动画 metadata 改为 Sprite 内显式 animation；
9. 使用 namespaced definition ID 与 operation。

旧 1.8.9/1.7.10 项目继续维护各自 v1/v2 兼容协议，并已在 `0.2.0` 回迁同源 Schema v3 Reactive
语义；26.2 不会用反射、ZIP 扫描或猜测式路径模拟旧版资源发现。平台渲染、F3+T 与其他 Mod 组合的
表现可能因运行环境而异。

从 v2 升级 v3 时保留所有基础字段，只需同步 manifest/config version、为 v3 Sprite 添加稳定 ID，并按需
增加 `bindings`、Definition-level `animations` 与 `behaviors`。不要把 Sprite 内纹理帧 `animation` 改名。

26.2 `0.2.1` 已正式实现 Schema v3.1 的 Scene Graph、Group、Parent/Child、Pivot、Constants、Derived
Values、`self.*`/`parent.*` 与动画合成；迁移规则和完整错误/回退见 [SCHEMA_V3_1.md](SCHEMA_V3_1.md)。
Inspector、Visual Editor、HUD/Widget、Component、Clip/Scroll 尚不属于当前 API。

Schema v3.1 当前只在 26.2 `0.2.1` 接受。1.8.9/1.7.10 `0.2.0` 仍只接受 v2/v3.0；材质包必须按目标
版本选择协议，不能依赖旧项目自动降级 v3.1。
