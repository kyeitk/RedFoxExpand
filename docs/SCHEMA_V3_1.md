# RedFoxExpand Schema v3.1 完整协议

本文是 RedFoxExpand 1.8.9 `0.2.1` 所实现的 Schema v3.1 独立协议文档。它完整描述资源发现、Definition、匹配器、几何与 Slot、Scene Graph、纹理与文字、Runtime Context、表达式、Binding、Event、Behavior、Action、属性动画、限制、错误与回退；阅读本文不需要先阅读 v2 或 v3.0 文档。

Schema v3.1 已在 Minecraft 1.8.9 项目的 `0.2.1` 中实现；本文只描述该目标的真实能力和限制。

本文使用以下规范词：

- “必须/禁止”：违反即不是有效 v3.1 配置，通常在资源重载阶段拒绝该 config。
- “应当/不应”：当前可能接受，但不保证兼容或效果明确。
- “默认值”：字段省略时的实际值。
- “Definition-local”：名称和引用只在当前 Definition 内有效。
- “实现行为”：来自当前解析器、运行时与平台适配层。
- “实机表现”：可能受 Minecraft 客户端、GUI Scale 与其他渲染 Mod 影响。

## 1. 协议对象总览

一个 v3.1 材质包由下列对象组成：

```text
Resource Pack
├─ pack.mcmeta
└─ assets/kyeitk/redfoxexpand/index.json          Manifest
   └─ configs[]                                   Config 路径
      └─ ConfigRoot
         └─ definitions[]                         Definition
            ├─ match                              Matcher
            ├─ geometry                           Geometry
            ├─ slot_modifiers[]                   SlotModifier
            ├─ elements[]
            │  ├─ Group
            │  └─ Sprite
            │     ├─ TextureSpec
            │     └─ animation?                   纹理帧动画
            ├─ texts[]                            TextOverlay
            ├─ text_rules[]                       TextRule
            ├─ constants{}                        常量
            ├─ values{}                           派生值
            ├─ bindings[]                         Binding
            ├─ animations[]                       PropertyAnimation
            │  └─ tracks[] -> keyframes[]
            └─ behaviors[]                        Behavior
               ├─ on                              EventTrigger
               └─ actions[]                       Action
```

协议不向 JSON 暴露 Java 类，也不允许调用任意 Java 方法。“可调用”能力只有：

- 表达式中的内置函数；
- Behavior 中列出的 Action；
- 由平台提供的只读 Runtime Context 变量。

任何未在本文列出的对象字段、函数、事件、Action、Property 或枚举值都不是 v3.1 公共协议。

## 2. 资源包入口与发现

### 2.1 `pack.mcmeta`

Minecraft 1.8.9 示例必须声明资源包格式 `1`：

```json
{
  "pack": {
    "description": "My RedFoxExpand Schema v3.1 Pack",
    "pack_format": 1
  }
}
```

这属于 Minecraft 自身的资源包元数据，不属于 RedFoxExpand Schema，但缺少或版本不合适会阻止 Minecraft 正常启用材质包。

### 2.2 Manifest

唯一入口路径为：

```text
assets/kyeitk/redfoxexpand/index.json
```

格式：

```json
{
  "api_version": 3.1,
  "configs": [
    "redfoxexpand/config/inventory.json"
  ]
}
```

字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `api_version` | JSON number | 是 | 必须写数值 `3.1`，不能写字符串 `"3.1"` 或内部编号 `31` |
| `configs` | string[] | 是 | 同一命名空间 `kyeitk` 下、相对 `redfoxexpand/` 目录的 config 资源路径 |

`configs` 最多 256 项。重复路径只加载一次并产生诊断；路径必须是安全相对路径，禁止绝对路径、盘符、空段、`.`、`..`，长度最多 512 字符。

### 2.3 ConfigRoot

每个 config 的根格式为：

```json
{
  "api_version": 3.1,
  "definitions": []
}
```

只允许 `api_version` 和 `definitions`。每个 config 最多 256 个 Definition。

Manifest 和其引用的所有 config 必须使用完全相同的 `api_version`。1.8.9 `0.2.1` 在加载 config 时执行精确版本校验；`3` 与 `3.1` 不相等，混用会拒绝该 config。

### 2.4 资源堆栈与来源隔离

RedFoxExpand 使用 Minecraft 原生资源堆栈发现每个已启用资源包的 `index.json`。每个 Manifest 只能引用本资源包来源中同名的 config，不能借由高优先级包隐式替换另一包的 config。Definition 的跨包优先级仍服从 Minecraft 资源包顺序。

## 3. 通用数据类型与词法规则

### 3.1 严格对象

协议对象是严格对象：未知字段会使所属 config 校验失败，不会静默忽略。数组、对象、字符串、布尔和数值也必须符合字段要求的 JSON 类型。

### 3.2 有限数

所有协议 number 与表达式运行结果都必须是有限数，禁止 `NaN`、`Infinity` 和 `-Infinity`。JSON 本身通常也不能表示这些值。

静态 GUI 坐标或尺寸通常受绝对值 `65536` 限制；具体例外和范围见“完整数值索引”。

### 3.3 整数

标为 integer 的字段必须无小数部分并处于 Java `int` 可表示范围；采用 GUI 整数校验的字段还必须处于 `[-65536, 65536]`。

### 3.4 Definition ID

格式：

```text
[a-z0-9_.-]+:[a-z0-9_./-]+
```

示例：

```text
my_pack:inventory/main
```

最长 512 字符。它用于跨资源包的 `replace`/`disable` 覆盖。

### 3.5 Element、Animation 等稳定 ID

Definition-local 稳定 ID 格式：

```text
[a-z0-9_.-]+
```

最长 512 字符。Element ID 在 Group 与 Sprite 之间共享同一命名空间。PropertyAnimation ID 使用同一格式，但属于动画命名空间。

### 3.6 符号名称

`constants` 和 `values` 的名称格式：

```text
[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)*
```

允许 `foo`、`eye.radius`，禁止连字符。最长 512 字符。以下前缀保留：

```text
player. screen. gui. mouse. event. self. parent.
```

符号不能覆盖 Runtime Context、同 Definition 的其他常量或派生值。

### 3.7 颜色

颜色采用 ARGB：

```text
0xAARRGGBB
```

接受：

- JSON integer；
- `"#RRGGBB"`，自动补 `FF` Alpha；
- `"#AARRGGBB"`；
- `"0xRRGGBB"` 或 `"0xAARRGGBB"`；
- 十六进制字符串内的 `_` 会被移除。

示例：

```json
"color": "#80FF4000"
```

## 4. Definition

完整字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | string | 是 | — | 全局 Definition ID |
| `operation` | string enum | 否 | `append` | `append`、`replace`、`disable` |
| `priority` | integer | 否 | `0` | Definition 内部排序优先级，范围 `[-65536,65536]` |
| `match` | Matcher | 是 | — | 屏幕/菜单匹配条件 |
| `geometry` | Geometry | 否 | 全 0 | GUI 原点与逻辑尺寸修正 |
| `slot_modifiers` | SlotModifier[] | 否 | `[]` | Slot 偏移与悬停高亮 |
| `elements` | Element[] | 否 | `[]` | v3.1 Group/Sprite 场景图，最多 256 项 |
| `texts` | TextOverlay[] | 否 | `[]` | 静态文字覆盖，最多 128 项 |
| `text_rules` | TextRule[] | 否 | `[]` | 原版文字修改，最多 64 项 |
| `constants` | object | 否 | `{}` | Definition-local primitive 常量，最多 128 项 |
| `values` | object | 否 | `{}` | 有序派生表达式，最多 128 项 |
| `bindings` | Binding[] | 否 | `[]` | 属性绑定，最多 128 项 |
| `animations` | PropertyAnimation[] | 否 | `[]` | 属性动画定义，最多 64 项 |
| `behaviors` | Behavior[] | 否 | `[]` | 事件行为，最多 128 项 |

v3.1 禁止顶层 `sprites`。所有 Sprite 必须放入 `elements` 并显式写 `"type":"sprite"`。

### 4.1 Operation

Definition 候选先按下列稳定键排序：

```text
资源包来源优先级 -> priority -> config 资源路径 -> Definition 数组索引
```

随后依次执行：

| 值 | 行为 |
|---|---|
| `append` | 保留已有同 ID Definition，并追加当前 Definition |
| `replace` | 移除此前所有同 ID 活动 Definition，再加入当前 Definition |
| `disable` | 移除此前所有同 ID活动 Definition，不加入自身 |

屏幕运行时，所有匹配的活动 Definition 会合并：Geometry 偏移相加；SlotModifier、Sprite、Text 与 TextRule 依稳定顺序追加；Sprite 再按绘制键排序。

## 5. Matcher

每个 Matcher 对象必须且只能含一个运算符。

> 1.8.9 平台可用 matcher 为 `all/any/not`、screen/menu 完整类名或简单类名的
> exact/assignable 形式，以及 `screen_title_key/screen_title_text`。由于没有现代注册表和 GUI
> 资源捕获，`menu_type`、matcher `resource_location`、`mod_namespace` 会在 reload 明确拒绝；
> 它们保留在跨版本协议词汇中，但不是本平台可调用 matcher。

### 5.1 逻辑匹配器

| 运算符 | 值 | 说明 |
|---|---|---|
| `all` | Matcher[] | 1–256 项，全部为真 |
| `any` | Matcher[] | 1–256 项，任一为真 |
| `not` | Matcher | 对单个 Matcher 取反 |

示例：

```json
{
  "all": [
    {"menu_type": "minecraft:player"},
    {"not": {"screen_title_text": "Creative*"}}
  ]
}
```

### 5.2 屏幕类匹配器

| 运算符 | 值 | 说明 |
|---|---|---|
| `exact_screen_class` | 完整类名 | 屏幕运行时类完全相同 |
| `assignable_screen_class` | 完整类名 | 屏幕类继承层级中包含该类 |
| `exact_screen_simple_class` | 简单类名 | 简单类名完全相同 |
| `assignable_screen_simple_class` | 简单类名 | 简单类名继承层级中包含该名称 |

完整类名必须包含 `.`；简单类名禁止包含 `.`。

### 5.3 Menu 类匹配器

| 运算符 | 值 | 说明 |
|---|---|---|
| `exact_menu_class` | 完整类名 | Menu 类完全相同 |
| `assignable_menu_class` | 完整类名 | Menu 类继承层级包含该类 |
| `exact_menu_simple_class` | 简单类名 | Menu 简单类名完全相同 |
| `assignable_menu_simple_class` | 简单类名 | Menu 简单类名继承层级包含该名称 |

### 5.4 标题与资源匹配器

| 运算符 | 值 | 说明 |
|---|---|---|
| `screen_title_key` | 非空 string | 精确匹配可翻译标题 key |
| `screen_title_text` | 非空 string | 匹配最终标题文本；仅 `*` 是通配符，整体锚定 |
| `menu_type` | namespaced ID | 精确匹配 Menu 注册 ID |
| `resource_location` | namespaced ID | 匹配捕获到的 GUI 背景资源 |
| `mod_namespace` | namespace | 匹配捕获资源的命名空间 |

`menu_type` 与 ResourceLocation 必须为小写 namespaced ID，例如 `minecraft:player`。在 1.8.9 中，这三个现代 matcher 不可用并在 reload 拒绝，不会静默返回 false。纹理对象的 `resource_location` 类型仍然可用，与同名 matcher 无关。

## 6. Geometry

格式：

```json
"geometry": {
  "x_offset": 0,
  "y_offset": 0,
  "width_offset": 0,
  "height_offset": 0
}
```

全部字段都是可省略 integer，默认 `0`，范围 `[-65536,65536]`。

语义：

- `width_offset`、`height_offset` 修改 RedFoxExpand 使用的逻辑 GUI 宽高；
- GUI 保持围绕原中心修正，实际 origin 包含 `x_offset - width_offset / 2`、`y_offset - height_offset / 2` 的对齐效果；
- `gui.x/y/width/height`、GUI Anchor、Slot 与背景渲染使用修正后的实时布局；
- 窗口 Resize 或第三方 GUI 改变原版容器位置时，实时 GUI origin 会继续更新；1.8.9 原版没有 Recipe Book。

多个匹配 Definition 的 Geometry 四项分别相加。极端负尺寸虽然字段可通过单项范围检查，但没有实用意义，作者应确保合并后的逻辑宽高为正。

## 7. SlotModifier

字段：

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `slots` | number/string/array | 全部 Slot | 按索引筛选 |
| `target_x` | integer | 不筛选 | 按原始 Slot x 精确筛选 |
| `target_y` | integer | 不筛选 | 按原始 Slot y 精确筛选 |
| `target_class_name` | string | 不筛选 | Slot 类名筛选 |
| `target_class_match` | enum | `exact` | `exact` 或 `assignable` |
| `target_class_name_type` | enum | `full` | `full` 或 `simple` |
| `x_offset` | integer | `0` | 匹配 Slot 的横向增量 |
| `y_offset` | integer | `0` | 匹配 Slot 的纵向增量 |
| `highlight_color` | ARGB | 无 | 悬停高亮起始色 |
| `highlight_color_2` | ARGB | 无 | 悬停高亮结束色 |

所有已配置筛选条件按 AND 组合。筛选始终比较 Slot 的原始索引、原始 x/y 和类层级，而不是前一条规则修改后的坐标。

`slots` 写法：

```json
"slots": 5
"slots": "0-8"
"slots": [0, "1-8", "12"]
```

- 索引必须非负；
- 字符串单值和范围端点是十进制整数；
- `"8-0"` 也合法且包含两端；
- 单个范围最多展开 4096 个索引；
- 重复索引去重；
- 省略或空集合表示全部 Slot。

多条匹配规则的 `x_offset/y_offset` 相加。若任一匹配规则定义高亮，鼠标位于该 16×16 Slot 时绘制渐变；只有 `highlight_color_2` 时起始色回退为 `0x80FFFFFF`，只有第一色时第二色等于第一色。

## 8. Scene Graph 与 Element

`elements` 是 Definition-local 场景图声明数组，允许 Group 和 Sprite。最多 256 个 Element，其中 Group 最多 128 个。

共同规则：

- 每项必须有唯一稳定 `id`；
- 每项必须显式声明 `type`；
- 每个 Element 最多一个父 Group；
- Group 子引用必须存在；
- 禁止重复 child、多父级和环；
- 每个 Group 最多 256 个 child；
- 最大深度 32，根节点计为第 1 层；
- 根按 `elements` 声明顺序遍历，子节点按 `children` 顺序遍历。

### 8.1 Group

Group 不直接绘图，只提供局部坐标、尺寸、Pivot、场景变换、可见性、Alpha 和父子关系。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | stable ID | 是 | — | Element ID |
| `type` | string | 是 | — | 必须是 `group` |
| `children` | stable ID[] | 否 | `[]` | 子 Element，顺序稳定 |
| `x` | finite number | 否 | `0` | 父局部坐标；根相对 Anchor |
| `y` | finite number | 否 | `0` | 父局部坐标；根相对 Anchor |
| `width` | finite number | 否 | `0` | 非负，只用于 Pivot/几何变量，不绘制 |
| `height` | finite number | 否 | `0` | 非负，只用于 Pivot/几何变量，不绘制 |
| `anchor` | Anchor | 否 | 根 `gui`，子 `parent` | 根定位点或父局部坐标系 |
| `pivot` | Pivot | 否 | `center` | 旋转/缩放原点 |

`x/y/width/height` 的绝对值上限为 65536，宽高不得为负。

### 8.2 Sprite

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | stable ID | 是 | — | Element ID |
| `type` | string | 是 | — | 必须是 `sprite` |
| `texture` | TextureSpec | 是 | — | 基础纹理 |
| `animation` | TextureFrameAnimation | 否 | 无 | 纹理帧切换动画 |
| `x` | finite number | 否 | `0` | 局部 x |
| `y` | finite number | 否 | `0` | 局部 y |
| `z` | finite number | 否 | `0` | 同 Layer 内排序 |
| `u` | finite number | 否 | `0` | 采样区域左上 u |
| `v` | finite number | 否 | `0` | 采样区域左上 v |
| `width` | positive finite number | 否 | `16` | 目标绘制宽 |
| `height` | positive finite number | 否 | `16` | 目标绘制高 |
| `source_width` | positive finite number | 否 | `width` | 源采样宽 |
| `source_height` | positive finite number | 否 | `height` | 源采样高 |
| `texture_width` | positive finite number | 否 | `256` | 源纹理逻辑宽 |
| `texture_height` | positive finite number | 否 | `256` | 源纹理逻辑高 |
| `full_texture` | boolean | 否 | `true` | 是否缩放绘制整张纹理 |
| `color` | ARGB | 否 | `#FFFFFFFF` | 顶点色/Alpha 调制 |
| `layer` | Layer | 否 | `background` | 绘制阶段 |
| `anchor` | Anchor | 否 | 根 `gui`，子 `parent` | 定位点 |
| `pivot` | Pivot | 否 | `center` | 旋转/缩放原点 |

除 Pivot 对象坐标外，上表静态数值绝对值上限为 65536；尺寸必须大于 0。

`full_texture:true` 时整张纹理缩放到 `width × height`，区域采样字段不参与绘制。`full_texture:false` 时从 `texture_width × texture_height` 逻辑纹理的 `(u,v,source_width,source_height)` 区域采样，并缩放到目标尺寸。

## 9. Anchor

根 Element 支持：

```text
gui
screen
screen_center

gui_top_left
gui_top_center
gui_top_right
gui_center_left
gui_center
gui_center_right
gui_bottom_left
gui_bottom_center
gui_bottom_right

screen_top_left
screen_top_center
screen_top_right
screen_center_left
screen_center_right
screen_bottom_left
screen_bottom_center
screen_bottom_right
```

别名与含义：

| 值 | 原点 |
|---|---|
| `gui` | 等同 `gui_top_left` |
| `screen` | 等同 `screen_top_left` |
| `screen_center` | 屏幕正中心 |
| `*_top_*` | 对应矩形上边 |
| `*_center_left/right` | 对应矩形垂直中线左/右边 |
| `*_bottom_*` | 对应矩形下边 |

不存在 `screen_center_center`；屏幕中心使用 `screen_center`。

有父级的 Element 只允许：

```text
parent
```

根禁止 `parent`，子禁止任何非 `parent` Anchor。Anchor 只确定局部 `(0,0)` 的世界参考点，不会依据 Element 自身宽高自动做对齐。例如要让宽 160 的根元素在 `screen_bottom_center` 水平居中，通常写 `x:-80`。

GUI Anchor 基于 Geometry 应用后的实时 GUI origin 与逻辑宽高；Screen Anchor 基于 GUI-scaled 屏幕宽高。

## 10. Pivot

Pivot 可为命名值：

```text
parent
top_left top_center top_right
center_left center center_right
bottom_left bottom_center bottom_right
```

也可为对象：

```json
"pivot": {"x": 80, "y": 32}
```

命名值由当前 Element `width/height` 计算，默认 `center`。`parent` 是兼容别名，实际等同 `top_left`，即 `(0,0)`；它不会把 Pivot 放到父元素中心。

对象形式只允许 `x` 与 `y`，两项必填且必须为有限数。当前解析器未对自定义 Pivot 坐标施加 ±65536 GUI 幅度限制，但作者不应依赖极端数值。

Pivot 只改变旋转和缩放原点，不改变基础 `x/y`，也不负责 Anchor 对齐。

## 11. 场景变换、继承与绘制顺序

根先应用 Anchor；从根到 Sprite 的每个节点按下列顺序应用：

```text
T(local x/y + resolved translate_x/y)
T(pivot)
R(rotation_z)
S(scale_x, scale_y)
T(-pivot)
```

等价矩阵：

```text
World(child) = World(parent)
             × T(local + reactiveTranslate)
             × T(pivot) × R(rotation_z) × S(scale) × T(-pivot)
```

正角度单位为度；在屏幕 y 轴向下的 GUI 坐标系中，视觉上为顺时针旋转。

继承规则：

- 子节点继承父级平移、旋转和缩放；
- 任一祖先 `visible:false`，整个后代分支不绘制；
- Sprite 最终 Alpha 是根到自身所有 resolved alpha 的乘积，再与 Sprite `color` 的源 Alpha 相乘；
- Group 动画只产生一个动画实例，后代通过矩阵继承，不为每个 Sprite 复制实例。

Property 管线：

```text
Base -> Binding -> Animation contributions -> Runtime Override -> Final Clamp -> Render
```

绘制排序键：

```text
layer -> z -> scene traversal order
```

Group 自身不绘制。父子关系只负责变换与继承，不会越过 Sprite 自己的 `layer/z` 强行改变合成顺序；遮挡关键项应明确设置 `layer` 和 `z`。

### 11.1 Layer

```text
underlay -> background -> foreground
```

| 值 | 绘制阶段 |
|---|---|
| `underlay` | 屏幕暗化/模糊之后、容器原版背景之前 |
| `background` | 原版容器背景之后、Slot 与标签之前 |
| `foreground` | Slot/标签之后、延迟 Tooltip 之前 |

## 12. TextureSpec

格式：

```json
{
  "type": "pack_resource",
  "location": "textures/gui/character.png"
}
```

只允许 `type` 和 `location`。

| `type` | `location` 示例 | 解析方式 |
|---|---|---|
| `pack_resource` | `textures/gui/panel.png` | 相对本包映射为 `kyeitk:redfoxexpand/<location>` |
| `resource_location` | `minecraft:textures/gui/container/inventory.png` | 原样作为 namespaced 纹理资源 ID |
| `gui_sprite` | `minecraft:container/slot` | 原样作为 Minecraft GUI Sprite atlas ID |

`location` 最长 512，必须使用 `/`，禁止绝对路径、盘符、空段、`.`、`..`。`resource_location` 和 `gui_sprite` 必须是合法小写 namespaced ID。Minecraft 1.8.9 没有现代 GUI atlas，因此 `gui_sprite` 兼容映射为 `textures/gui/sprites/<path>.png`。

`gui_sprite` 当前只支持 `full_texture:true`；区域采样会在加载时拒绝。

## 13. 纹理帧动画

Sprite 的 `animation` 是“切换 TextureSpec”的帧动画，不是修改位置/旋转/缩放的 PropertyAnimation。

格式：

```json
"animation": {
  "frames": [
    {"texture":{"type":"pack_resource","location":"textures/gui/f0.png"}},
    {"texture":{"type":"pack_resource","location":"textures/gui/f1.png"},"duration_ms":150}
  ],
  "frame_duration_ms": 100,
  "loop": true,
  "condition": "always",
  "default_texture": {
    "type":"pack_resource",
    "location":"textures/gui/fallback.png"
  },
  "missing_frame": "use_default"
}
```

字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `frames` | Frame[] | 是 | — | 1–512 帧 |
| `frame_duration_ms` | integer | 否 | `100` | 每帧默认时长，1–600000 |
| `loop` | boolean | 否 | `true` | 是否循环 |
| `condition` | enum | 否 | `always` | 仅 `always`、`never` |
| `default_texture` | TextureSpec | 否 | Sprite `texture` | 禁用、结束或回退纹理 |
| `missing_frame` | enum | 否 | `use_default` | `use_default`、`skip`、`disable` |

Frame 字段：

| 字段 | 类型 | 必填 | 默认值 |
|---|---|---:|---|
| `texture` | TextureSpec | 是 | — |
| `duration_ms` | integer | 否 | `frame_duration_ms` |

行为：

- `condition:never`：始终绘制 default texture；该字段不是表达式；
- `use_default`：缺失帧保留时长并绘制 default texture；
- `skip`：缺失帧从时间轴移除；
- `disable`：任一帧缺失即禁用整段动画并绘制 default texture；
- 非循环动画到达总时长后回到 default texture，不停留在最后一帧；
- 循环动画在总时长边界回到第一帧；
- 时间起点是当前资源 snapshot 的发布时间，因此 F3+T 会重新开始帧动画。

## 14. TextOverlay 与 TextRule

### 14.1 TextOverlay

`texts` 是静态文字覆盖，不是 Element，不属于 Group，不支持 Binding、Action、Pivot 或场景继承。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `text` | string | 是 | — | 字面量或翻译 key |
| `translate` | boolean | 否 | `false` | true 使用 Minecraft translatable component |
| `x` | integer | 否 | `0` | Anchor 相对 x |
| `y` | integer | 否 | `0` | Anchor 相对 y |
| `color` | ARGB | 否 | `#FFFFFFFF` | 文字颜色 |
| `shadow` | boolean | 否 | `false` | 原版字体阴影 |
| `layer` | Layer | 否 | `foreground` | 绘制阶段 |
| `anchor` | Anchor | 否 | `gui` | 定位点 |

`x/y` 范围 `[-65536,65536]`。有意义的 Text Anchor 是全部根级 GUI/Screen Anchor。解析器技术上也接受 `parent`，但 Text 没有父节点，此时 origin 为 `(0,0)`；不应使用该兼容行为。

### 14.2 TextRule

`text_rules` 修改已知原版文字的位置、颜色或可见性。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `selector` | enum | 是 | — | `title` 或 `player_inventory` |
| `x_offset` | integer | 否 | `0` | 原版位置增量 |
| `y_offset` | integer | 否 | `0` | 原版位置增量 |
| `color` | ARGB | 否 | 保持原色 | 替换颜色 |
| `hidden` | boolean | 否 | `false` | 隐藏文字 |

偏移范围 `[-65536,65536]`。多条匹配规则按合并顺序应用；隐藏为最终隐藏意图。

## 15. Constants 与 Derived Values

### 15.1 `constants`

允许的值只有：

- finite number；
- boolean；
- string。

禁止 object、array、null。

```json
"constants": {
  "eye.radius": 4.4,
  "enabled": true,
  "mode": "compact"
}
```

常量可用于 `values`、Binding 的 `value`、Behavior 的 `if`。

### 15.2 `values`

`values` 是保持 JSON 声明顺序的表达式对象：

```json
"values": {
  "mouse_dx": "mouse.x - screen.width / 2",
  "head_turn": "clamp(mouse_dx * 0.01, -4, 4)"
}
```

规则：

- 每个值必须是表达式 string；
- 先在 reload 阶段编译并推断静态类型；
- 后项可以引用前项；
- 禁止前向引用，因此循环也会作为 unknown variable 拒绝；
- 求值时先注入 constants，再按声明顺序求值 values；
- 可读取 Runtime Context；
- 不能读取 `self.*`/`parent.*`，因为 values 没有 Element target。

## 16. Runtime Context

### 16.1 完整变量表

| 变量 | 类型 | 单位/说明 |
|---|---|---|
| `player.health` | number | 当前生命值，1 颗心 = 2 health points |
| `player.max_health` | number | 最大生命值 |
| `player.armor` | number | 护甲值 |
| `player.food` | number | 饥饿值 |
| `player.air` | number | 当前空气值 |
| `player.level` | number | 经验等级 |
| `player.experience` | number | 当前等级经验进度，原版通常为 0–1 |
| `player.is_burning` | boolean | 玩家当前是否着火 |
| `player.is_sneaking` | boolean | 玩家当前是否潜行 |
| `player.is_sprinting` | boolean | 玩家当前是否疾跑 |
| `screen.width` | number | GUI-scaled 屏幕宽 |
| `screen.height` | number | GUI-scaled 屏幕高 |
| `gui.x` | number | Geometry 应用后的实时 GUI 左上 x |
| `gui.y` | number | Geometry 应用后的实时 GUI 左上 y |
| `gui.width` | number | Geometry 应用后的逻辑 GUI 宽 |
| `gui.height` | number | Geometry 应用后的逻辑 GUI 高 |
| `mouse.x` | number | GUI-scaled 屏幕鼠标 x |
| `mouse.y` | number | GUI-scaled 屏幕鼠标 y |
| `mouse.gui_x` | number | `mouse.x - gui.x` |
| `mouse.gui_y` | number | `mouse.y - gui.y` |
| `mouse.left_down` | boolean | 鼠标左键当前按下 |
| `mouse.right_down` | boolean | 鼠标右键当前按下 |

Runtime Context 是只读快照。协议不保证生命、护甲、空气等一定处于原版默认上限；Modded 玩家属性可以超出，因此配置应主动 `clamp`。

### 16.2 Capability

每个 Binding 在编译后计算所需能力；平台缺少任一能力时，该 Binding 在加载阶段拒绝。`Capability` 是跨版本实现使用的 Java enum，不是 JSON 可配置字段。当前完整枚举如下：

```text
PLAYER_HEALTH
PLAYER_MAX_HEALTH
PLAYER_BURNING
PLAYER_SNEAKING
PLAYER_SPRINTING
PLAYER_ARMOR
PLAYER_FOOD
PLAYER_AIR
PLAYER_LEVEL
PLAYER_EXPERIENCE
SCREEN_SIZE
GUI_POSITION
GUI_SIZE
MOUSE_POSITION
MOUSE_BUTTONS
PROPERTY_VISIBLE
PROPERTY_ALPHA
PROPERTY_TRANSLATE
PROPERTY_SCALE
PROPERTY_ROTATION
EVENT_HEALTH
EVENT_BURNING
EVENT_SCREEN_LIFECYCLE
ACTION_ANIMATION
ACTION_SET_VISIBLE
ACTION_SET_ALPHA
```

前 15 项对应 Runtime Context 数据；`PROPERTY_*` 对应可绑定/动画的属性能力；`EVENT_*` 与 `ACTION_*` 对应 Behavior 能力。当前 1.8.9 平台实现全部上列 Reactive 能力。材质包只能通过使用相应变量、Property、Event 或 Action 间接要求能力，不能直接声明 Capability。

### 16.3 Binding-only 几何变量

只有 Binding 表达式额外提供：

```text
self.local_x
self.local_y
self.world_x
self.world_y
self.world_center_x
self.world_center_y
self.width
self.height

parent.local_x
parent.local_y
parent.world_x
parent.world_y
parent.world_center_x
parent.world_center_y
parent.width
parent.height
```

全部是 number。

- `local_x/y`：Element 静态基础 `x/y`；
- `world_x/y`：静态局部坐标沿父链求和并加根 Anchor；
- `world_center_x/y = world_x/y + width/height / 2`；
- `width/height`：Element 静态尺寸；
- `parent.*` 指直接父 Group；根 Binding 使用 `parent.*` 会在 reload 阶段拒绝。

这些 world 几何故意不包含当前帧 Binding、PropertyAnimation 或 Override 变换，以避免自反馈和求值顺序依赖。它不是最终渲染矩阵回读 API。

### 16.4 Event payload

只在 `player.health.decreased` 和 `player.health.increased` 的 Behavior `if` 表达式中提供：

| 变量 | 类型 | 说明 |
|---|---|---|
| `event.old` | number | 变化前生命 |
| `event.new` | number | 变化后生命 |
| `event.delta` | number | 变化量绝对值，始终非负 |

其他事件没有 payload；在不支持的 Behavior 中引用这些变量会在编译阶段失败。

## 17. 表达式语言

### 17.1 值类型

只有三种：

```text
number
boolean
string
```

不支持 null、array、object、动态属性访问或用户自定义函数。

### 17.2 字面量

```text
12
-3.5
1.0e-3
true
false
"text"
```

字符串转义支持：

```text
\"  \\  \n  \r  \t
```

不支持十六进制数值字面量。

### 17.3 运算符与优先级

从低到高：

| 优先级 | 运算符 | 输入 | 输出 | 说明 |
|---:|---|---|---|---|
| 1 | `||` | boolean, boolean | boolean | 短路或 |
| 2 | `&&` | boolean, boolean | boolean | 短路与 |
| 3 | `==` `!=` | 同类型 | boolean | 精确相等/不等 |
| 4 | `<` `<=` `>` `>=` | number, number | boolean | 数值比较 |
| 5 | `+` `-` | number, number | number | 加减；`+` 不连接字符串 |
| 6 | `*` `/` | number, number | number | 乘除；除零失败 |
| 7 | `!` | boolean | boolean | 逻辑非 |
| 7 | unary `+` `-` | number | number | 正负号 |
| 8 | `( )` | — | — | 显式分组 |

数值相等使用 double 精确比较，不做 epsilon 近似。字符串区分大小写并精确比较。不同类型不能比较相等。

不支持：

```text
?:  %  **  位运算  赋值  ++/--  字符串拼接
```

### 17.4 可调用函数

| 函数 | 参数 | 返回 | 解释 |
|---|---:|---|---|
| `abs(x)` | 1 number | number | 绝对值 |
| `min(a,b)` | 2 number | number | 较小值 |
| `max(a,b)` | 2 number | number | 较大值 |
| `hypot(x,y)` | 2 number | number | `sqrt(x*x + y*y)` 的稳定计算 |
| `clamp(x,min,max)` | 3 number | number | 将 x 限制在闭区间；运行时要求 `min <= max` |
| `lerp(a,b,t)` | 3 number | number | `a + (b-a)*t`；不自动限制 t |

不存在其他函数。函数名区分大小写。

### 17.5 错误语义

下列情况在 reload 编译阶段拒绝：未知变量/函数、参数数量错误、静态类型错误、无效 token、过深表达式。

下列情况可能在运行阶段失败：除零、`clamp` 上下界反转、结果非有限数。运行失败会产生限频诊断，并让受影响的 Binding 使用该 Property 基础值；不会写回 JSON 或破坏场景结构。

### 17.6 表达式预算

| 项目 | 上限 |
|---|---:|
| 表达式字符 | 1024 |
| token | 256 |
| AST/解析深度 | 32 |
| 函数参数 | 3 |

## 18. Binding

格式：

```json
{
  "target": "head_control",
  "property": "rotation_z",
  "value": "clamp((mouse.x - self.world_center_x) * 0.01, -4, 4)",
  "smoothing_ms": 180
}
```

字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `target` | Element ID | 是 | — | Group 或 Sprite |
| `property` | Property enum | 是 | — | 见下表 |
| `value` | expression string | 是 | — | 结果类型必须符合 Property |
| `smoothing_ms` | integer | 否 | `0` | 数值平滑时间，0–600000 |

同一 Definition 内禁止重复 `(target, property)` Binding。

### 18.1 Property 完整表

| Property | 类型 | Base | Binding 后最终范围 | 动画默认 compose | 可动画 |
|---|---|---:|---|---|---:|
| `visible` | boolean | `true` | true/false | — | 否 |
| `alpha` | number | `1` | clamp 到 `[0,1]` | `replace` | 是 |
| `translate_x` | number | `0` | finite，无额外 clamp | `add` | 是 |
| `translate_y` | number | `0` | finite，无额外 clamp | `add` | 是 |
| `scale_x` | number | `1` | clamp 到 `[0,8]` | `multiply` | 是 |
| `scale_y` | number | `1` | clamp 到 `[0,8]` | `multiply` | 是 |
| `rotation_z` | number | `0` | clamp 到 `[-360,360]` 度 | `add` | 是 |

Base 是不可变协议基础值，不会被 Binding/Animation 写回 Element 的 `x/y/width/height`。

### 18.2 平滑

`smoothing_ms` 只适用于 number Property。它使用指数趋近而不是线性插值；目标变化时从当前平滑值继续，经过 `8 × smoothing_ms` 后强制精确落到目标，避免无限尾差。Boolean Binding 必须使用 `0` 或省略。

Binding 在逻辑 tick 求值，数值在渲染阶段使用最近状态；因此视觉平滑由 `smoothing_ms` 和渲染帧共同完成。

## 19. Event 与 Behavior

Behavior 格式：

```json
{
  "on": {
    "event": "player.health.decreased",
    "every": 2,
    "mode": "coalesce"
  },
  "if": "event.new <= 10",
  "actions": [
    {"type":"play_animation","target":"character","animation":"damage_shake","restart":true}
  ]
}
```

Behavior 字段：

| 字段 | 类型 | 必填 | 默认值 |
|---|---|---:|---|
| `on` | EventTrigger | 是 | — |
| `if` | boolean expression | 否 | `true` |
| `actions` | Action[] | 是 | —；1–32 项 |

### 19.1 事件完整表

| 事件 | 触发时机 | payload | 支持 `every` |
|---|---|---|---:|
| `player.health.decreased` | 当前生命低于上次快照 | old/new/delta | 是 |
| `player.health.increased` | 当前生命高于上次快照 | old/new/delta | 是 |
| `player.started_burning` | `is_burning` false -> true | 无 | 否 |
| `player.stopped_burning` | `is_burning` true -> false | 无 | 否 |
| `screen.opened` | 新 GUI runtime 初始化 | 无 | 否 |

第一次快照用于建立基线，不把当前状态误判成健康变化或着火边沿；`screen.opened` 是显式打开事件。

### 19.2 EventTrigger

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `event` | event enum | 是 | — | 上表之一 |
| `every` | positive finite number | 否 | 无 | 只允许 health change，累计 delta 阈值 |
| `mode` | enum | 否 | `coalesce` | v3.1 只有 `coalesce` |

`coalesce` 为每条 Behavior 独立累计 `event.delta`。累计值不足 `every` 不执行；达到后执行一次，并保留 `accumulator % every` 的余数。一次掉 6 health、`every:2` 仍只在该 tick 合并触发一次，不同时叠加 3 次动画。`if` 只在阈值通过后求值。

同一 tick 若同时产生健康与着火事件，事件检测顺序为健康变化后着火边沿；每个事件内按 `behaviors` 声明顺序，单个 Behavior 内按 `actions` 顺序执行。

## 20. Action

### 20.1 `play_animation`

```json
{
  "type": "play_animation",
  "target": "character",
  "animation": "damage_shake",
  "restart": true
}
```

| 字段 | 类型 | 必填 | 默认值 |
|---|---|---:|---|
| `type` | string | 是 | `play_animation` |
| `target` | Element ID | 是 | — |
| `animation` | PropertyAnimation ID | 是 | — |
| `restart` | boolean | 否 | `true` |

活动实例键是 `(target, animation)`。`restart:true` 重新从 0ms 播放并把该实例作为最新启动贡献；`restart:false` 在同键实例仍活动时不重启。

### 20.2 `stop_animation`

```json
{"type":"stop_animation","target":"character","animation":"damage_shake"}
```

停止同键实例，移除其所有 Property 贡献；Property 立即回到剩余管线结果。

### 20.3 `set_visible`

```json
{"type":"set_visible","target":"fire","value":true}
```

设置运行时 `visible` Override。它位于 Binding 与 Animation 之后，并持续到另一个 set、runtime reset、关闭 GUI 或 F3+T generation 更换。

### 20.4 `set_alpha`

```json
{"type":"set_alpha","target":"flash","value":0.8}
```

`value` 是静态 number，范围 `[0,1]`。设置运行时 alpha Override，生命周期与 `set_visible` 相同。

Action 不接受表达式作为 `value`，也没有音频、点击回调、任意字段修改、创建/删除 Element 或 Java 调用。

## 21. PropertyAnimation

格式：

```json
{
  "id": "damage_shake",
  "duration_ms": 240,
  "loop": false,
  "tracks": [{
    "property": "translate_x",
    "interpolation": "linear",
    "compose": "add",
    "keyframes": [
      {"time_ms":0,"value":0},
      {"time_ms":40,"value":-5},
      {"time_ms":80,"value":5},
      {"time_ms":240,"value":0}
    ]
  }]
}
```

### 21.1 动画字段

| 字段 | 类型 | 必填 | 默认值 | 范围 |
|---|---|---:|---|---|
| `id` | stable ID | 是 | — | 最长 512 |
| `duration_ms` | integer | 是 | — | 1–600000 |
| `loop` | boolean | 否 | `false` | — |
| `tracks` | Track[] | 是 | — | 1–16 项 |

### 21.2 Track 字段

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---:|---|---|
| `property` | numeric Property | 是 | — | 除 `visible` 外的 Property |
| `interpolation` | enum | 否 | `linear` | `linear`、`smoothstep` |
| `compose` | enum | 否 | 按 Property | `replace`、`add`、`multiply` |
| `keyframes` | Keyframe[] | 是 | — | 1–128 项 |

同一动画内禁止两个 Track 修改同一 Property。

插值：

- `linear`：区间线性插值；
- `smoothstep`：先将区间 t 变换为 `t*t*(3-2*t)`，再插值。

### 21.3 Keyframe 字段与约束

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `time_ms` | integer | 是 | 0–`duration_ms` |
| `value` | finite number | 是 | 见 Property 范围 |

规则：

- 第一帧必须为 `0ms`；
- 时间必须严格递增；
- 最后一帧可以早于 `duration_ms`，之后保持最后值直到动画结束；
- `scale_x/scale_y` Keyframe 值必须在 `[0,8]`；
- `rotation_z` Keyframe 值必须在 `[-360,360]`；
- `alpha`、`translate_x/y` 只要求 finite，最终 Alpha 再 clamp。

非循环实例在 `elapsed > duration_ms` 时移除；恰好等于 duration 时仍采样结束值。循环实例使用 `elapsed % duration_ms`，所以恰好到周期边界会采样 0ms。

### 21.4 Compose

| 值 | 计算 |
|---|---|
| `replace` | `result = sampled` |
| `add` | `result = result + sampled` |
| `multiply` | `result = result * sampled` |

省略时：

| Property | 默认 compose |
|---|---|
| `scale_x`、`scale_y` | `multiply` |
| `alpha` | `replace` |
| `translate_x`、`translate_y`、`rotation_z` | `add` |

v3.1 对所有 numeric Property 接受三种 compose；作者应依据单位选择有意义的组合。

同一 target 的多个活动动画按确定的实例启动顺序贡献。后出现的 `replace` 覆盖此前结果；后续 `add/multiply` 再作用于新结果。每屏最多 32 个活动实例。

## 22. 生命周期、状态清理与回退

### 22.1 资源重载

F3+T 构建新的不可变 ResourceSnapshot。只有完整完成发现、解析、场景校验、表达式编译和纹理预算检查后才发布新 generation。

单个坏 config 被隔离并记录诊断；同一次 reload 的其他合法 config 可以继续加载。旧 snapshot 在新 snapshot 成功发布前保持可用，不会被半成品覆盖。

generation 更换会清理：

- Binding smoother；
- PropertyAnimation 活动实例；
- Behavior `every` 累加器；
- EventDetector 基线；
- `set_visible`/`set_alpha` Override；
- 纹理帧动画时间基点。

### 22.2 GUI 关闭与重开

每个 GUI Screen 使用独立 runtime。关闭后不把任何瞬时属性写回静态 Definition；重新打开会从 Base、首个 RuntimeSnapshot 和 `screen.opened` 开始。

### 22.3 缺失纹理

- 配置和资源路径在 reload 阶段检查；
- 纹理帧动画的缺帧按 `missing_frame` 明确回退；
- 不能安全解码、尺寸超限或预算超限的资源会使所属 config 失败；
- 协议不会程序化伪造缺失角色图。

### 22.4 OpenGL/渲染状态

每个 Scene Sprite 绘制使用成对矩阵 push/pop，变换不会有意泄漏到后续原版绘制。与其他渲染 Mod 组合时，最终表现可能受渲染链差异影响。

## 23. 错误分类

### 23.1 Reload 校验错误

包括但不限于：

- 非数值或不支持的 `api_version`；
- 未知字段、错误 JSON 类型、非法枚举；
- v3.1 使用顶层 `sprites`；
- 非法 ID、路径或 namespaced ID；
- 重复 Element、重复 Binding、重复 Track Property；
- child 缺失、重复、多父、场景环或超深；
- 根使用 `parent` Anchor、子使用非 `parent` Anchor；
- 未知变量、函数、target、Property、Animation 或 Event；
- 表达式静态类型错误；
- `values` 前向引用；
- root Binding 引用 `parent.*`；
- 预算、纹理大小或 PNG 解码限制超出。

### 23.2 运行时错误

包括除零、非有限结果、运行时 `clamp(min>max)`、平台状态短暂不可用、活动动画预算到达等。运行时错误使用限频诊断，尽量回到基础 Property 或保留安全 snapshot，而不是崩溃客户端。

## 24. 完整数值索引

此节集中列出全部可定义数值、默认值、单位和限制。

### 24.1 布局与绘制数值

| 路径 | 默认 | 单位 | 限制/解释 |
|---|---:|---|---|
| `Definition.priority` | 0 | 排序值 | integer，`[-65536,65536]` |
| `geometry.x_offset/y_offset` | 0 | GUI px | integer，`[-65536,65536]` |
| `geometry.width_offset/height_offset` | 0 | GUI px | integer，`[-65536,65536]` |
| `slot_modifiers.target_x/target_y` | 无 | 原始 Slot px | optional integer，`[-65536,65536]` |
| `slot_modifiers.x_offset/y_offset` | 0 | GUI px | integer，`[-65536,65536]` |
| `slots` 数字索引 | 全部 | Slot index | 非负 integer；字符串端点可到 Java int 上限，范围展开最多 4096 |
| `Group.x/y` | 0 | GUI px | finite，`[-65536,65536]` |
| `Group.width/height` | 0 | GUI px | finite，`[0,65536]` |
| `Sprite.x/y/z/u/v` | 0 | GUI/纹理 px | finite，`[-65536,65536]` |
| `Sprite.width/height` | 16 | GUI px | finite，`(0,65536]` |
| `Sprite.source_width/source_height` | 目标尺寸 | 源 px | finite，`(0,65536]` |
| `Sprite.texture_width/texture_height` | 256 | 源 px | finite，`(0,65536]` |
| `Pivot.x/y` | width/2,height/2 | 本地 px | finite；当前无 ±65536 限制 |
| `Text.x/y` | 0 | GUI px | integer，`[-65536,65536]` |
| `TextRule.x_offset/y_offset` | 0 | GUI px | integer，`[-65536,65536]` |
| ARGB color | 场景默认白 | 32-bit ARGB | integer 或 6/8 位十六进制字符串 |

GUI px 指 Minecraft GUI scale 后的逻辑像素，不是窗口物理像素。

### 24.2 时间与动画数值

| 路径 | 默认 | 单位 | 限制 |
|---|---:|---|---|
| Texture `frame_duration_ms` | 100 | ms | integer，1–600000 |
| Texture Frame `duration_ms` | 默认帧时长 | ms | integer，1–600000 |
| Binding `smoothing_ms` | 0 | ms | integer，0–600000 |
| PropertyAnimation `duration_ms` | 必填 | ms | integer，1–600000 |
| Keyframe `time_ms` | 必填 | ms | integer，0–duration，严格递增且首项 0 |
| EventTrigger `every` | 无 | event.delta | positive finite number |

### 24.3 Property 与表达式数值

| 值 | Base/默认 | 输入限制 | 最终限制 |
|---|---:|---|---|
| `alpha` | 1 | Binding/常量 finite；Keyframe finite；set_alpha 0–1 | `[0,1]` |
| `translate_x/y` | 0 | finite | 仅 finite，无幅度 clamp |
| `scale_x/y` | 1 | Binding finite；Keyframe 0–8 | `[0,8]` |
| `rotation_z` | 0 | Binding finite；Keyframe -360–360 | `[-360,360]` 度 |
| numeric constant | 无 | finite | 无额外范围 |
| derived/expression number | 无 | 运行结果 finite | 由消费 Property 再限制 |

### 24.4 Runtime 数值

Runtime 变量由 Minecraft/平台提供，材质包不可写入。除屏幕/GUI/鼠标几何使用 GUI px 外，生命与状态采用 Minecraft 当前运行值。协议不对 Modded 值强制原版范围。

## 25. 全部预算与安全限制

### 25.1 资源与结构

| 项目 | 上限 |
|---|---:|
| 单个 JSON 字节数 | 1 MiB |
| 单个 PNG 字节数 | 32 MiB |
| Manifest configs | 256 |
| 每包 config | 256 |
| 每 config Definition | 256 |
| 每 Definition Element | 256 |
| 每 Definition Group | 128 |
| 每 Group child | 256 |
| Scene depth（含根） | 32 |
| 每 Definition SlotModifier | 256 |
| 每 Definition TextOverlay | 128 |
| 每 Definition TextRule | 64 |
| 单个纹理帧动画 Frame | 512 |
| 单个字符串 Slot 范围展开 | 4096 |
| 路径/ID 最大长度 | 512 |
| JSON 嵌套深度 | 32 |
| 单张图宽或高 | 4096 px |
| 单张图像素 | 16,777,216 |
| 单个纹理帧动画累计像素 | 67,108,864 |
| 单次 reload 累计像素 | 134,217,728 |
| 常规静态 GUI 数值绝对值 | 65536 |

### 25.2 Reactive

| 项目 | 上限 |
|---|---:|
| Constants / Definition | 128 |
| Derived Values / Definition | 128 |
| Bindings / Definition | 128 |
| Behaviors / Definition | 128 |
| PropertyAnimations / Definition | 64 |
| Tracks / Animation | 16 |
| Keyframes / Track | 128 |
| Actions / Behavior | 32 |
| 活动动画实例 / Screen | 32 |
| 平滑或动画时长 | 600000 ms |
| 表达式字符 | 1024 |
| 表达式 token | 256 |
| 表达式深度 | 32 |
| 函数参数 | 3 |

预算在 generation 发布前尽可能完成校验；活动实例预算属于运行时。

## 26. 完整最小示例

### 26.1 Manifest

```json
{
  "api_version": 3.1,
  "configs": ["redfoxexpand/config/inventory.json"]
}
```

### 1.8.9 Config

此例在玩家背包屏幕底部中央绘制角色，平滑朝鼠标轻微转动，低血量降低 Alpha，并在每累计损失 2 health points 时抖动一次。

```json
{
  "api_version": 3.1,
  "definitions": [{
    "id": "example:reactive_character",
    "match": {"exact_menu_simple_class": "ContainerPlayer"},
    "constants": {
      "turn_strength": 0.01,
      "turn_limit": 4
    },
    "values": {
      "turn": "clamp((mouse.x - screen.width / 2) * turn_strength, -turn_limit, turn_limit)",
      "health_alpha": "clamp(player.health / max(player.max_health, 1), 0.35, 1)"
    },
    "elements": [
      {
        "id": "character_control",
        "type": "group",
        "anchor": "screen_bottom_center",
        "x": -80,
        "y": -160,
        "width": 160,
        "height": 160,
        "children": ["character"]
      },
      {
        "id": "character",
        "type": "sprite",
        "width": 160,
        "height": 160,
        "texture": {
          "type": "pack_resource",
          "location": "textures/gui/character.png"
        },
        "layer": "foreground"
      }
    ],
    "bindings": [
      {
        "target": "character_control",
        "property": "rotation_z",
        "value": "turn",
        "smoothing_ms": 180
      },
      {
        "target": "character_control",
        "property": "alpha",
        "value": "health_alpha",
        "smoothing_ms": 120
      }
    ],
    "animations": [{
      "id": "damage_shake",
      "duration_ms": 240,
      "loop": false,
      "tracks": [{
        "property": "translate_x",
        "compose": "add",
        "interpolation": "linear",
        "keyframes": [
          {"time_ms":0,"value":0},
          {"time_ms":40,"value":-5},
          {"time_ms":80,"value":5},
          {"time_ms":120,"value":-4},
          {"time_ms":160,"value":3},
          {"time_ms":200,"value":-1},
          {"time_ms":240,"value":0}
        ]
      }]
    }],
    "behaviors": [{
      "on": {
        "event": "player.health.decreased",
        "every": 2,
        "mode": "coalesce"
      },
      "actions": [{
        "type": "play_animation",
        "target": "character_control",
        "animation": "damage_shake",
        "restart": true
      }]
    }]
  }]
}
```

## 27. 场景组合摘要

### 27.1 父 Group 统一呼吸，眼睛单独跟随

推荐结构：

```text
character_root                screen_bottom_center
└─ head_control              循环 scale/translate 呼吸
   ├─ face_base              头部底图
   ├─ left_eye_control       Binding translate_x/y
   │  └─ left_eye
   ├─ right_eye_control      Binding translate_x/y
   │  └─ right_eye
   └─ hands                  更高 z，遮挡眼睛
```

眼球跟随应以 `self.world_center_*` 或父级几何计算方向，用 `clamp` 限制幅度并配置 `smoothing_ms`。遮挡关系由 `layer/z` 明确控制，不依赖父子声明顺序猜测。

### 27.2 着火 + 低血 + 受伤

可组合方式：

- `character_heat.alpha` Binding 到低血表达式；
- `character_fire.visible` Binding 到 `player.is_burning`；
- `burning_tremble` 是循环 add 动画；由 burning started/stopped Action 播放/停止；
- `damage_shake` 是一次性 add 动画；由 health decreased + coalesce 播放；
- 两个 translate 动画使用 `compose:add`，热度只修改 alpha，因此互不覆盖。

无需且禁止在 Java 中对角色 ID、火焰或低血效果写专用分支。

### 27.3 Slot 与 GUI 一同位移

Geometry 用于整体逻辑 GUI 扩展；SlotModifier 用于指定 Slot 细调；Element 使用 GUI Anchor。三者都会以实时 GUI origin 为基础，因此 Resize 或第三方 GUI 重定位后仍可重新定位。若 Element 使用 Screen Anchor，则它有意独立于 GUI Slot 移动。1.8.9 原版没有 Recipe Book。

## 28. 与 v3.0/v2 的兼容和迁移

### 28.1 v3.0 -> v3.1

1. Manifest 与 config 同时改为数值 `3.1`；
2. 顶层 `sprites` 改为 `elements`；
3. 每个旧 Sprite 增加 `"type":"sprite"`；
4. 将共享变换的 Sprite 放到 Group，并用 `children` 关联；
5. 子 Element 坐标改为父局部坐标，Anchor 使用 `parent` 或省略；
6. 重复数字抽到 `constants`，重复表达式抽到按顺序声明的 `values`；
7. 将重复 Binding/Action 移到 Group；
8. 对覆盖语义敏感的 Track 显式写 `compose`；
9. F3+T 后检查关闭重开、Resize、Tooltip、Slot 与变换复位；Recipe Book 在 1.8.9 不适用。

v3.0 继续使用 `api_version:3`，不需要为了兼容而迁移。v3.1 不会对 v3.0 JSON 自动猜测或隐式转换。

### 28.2 v2

v2 没有 Runtime Context、Expression、Binding、Event、Behavior、Action、PropertyAnimation 或 Scene Graph。v3.1 仍复用 v2 的 Definition operation、Matcher、Geometry、Slot、Texture frame animation、Text 与 TextRule 语义；增加 v3.1 Demo 不会改变合法 v2 配置的解析协议。

## 29. 当前明确限制

- 本文协议已在 1.8.9 `0.2.1` 实现；跨版本资源包仍须使用目标版本支持的 matcher 与 `pack_format`；
- 表达式没有三元运算、取模、幂、数组、对象或自定义函数；
- Event 只有健康、着火边沿和 screen.opened；
- Action 只有 play/stop animation、set_visible、set_alpha；
- 没有鼠标点击事件，只有鼠标位置和按键当前状态变量；
- 没有音频 Action；
- Text 不是 Element，不能进入 Group 或做 Reactive Property；
- 纹理帧 `condition` 只有静态 `always/never`，动态显隐应使用 Sprite `visible` Binding；
- `self.world_*`/`parent.world_*` 是静态场景几何，不是最终矩阵回读；
- Manifest/config 的 3 与 3.1 在 reload 执行精确相等校验，混用时拒绝对应 config；
- 真实 Minecraft GUI、F3+T、Resize、Tooltip 遮挡及其他 Mod 兼容会受客户端环境影响；Recipe Book 在 1.8.9 不适用。

## 30. 枚举速查

```text
Definition.operation:
  append replace disable

Matcher:
  all any not
  exact_screen_class assignable_screen_class
  exact_screen_simple_class assignable_screen_simple_class
  exact_menu_class assignable_menu_class
  exact_menu_simple_class assignable_menu_simple_class
  screen_title_key screen_title_text
  menu_type resource_location mod_namespace

Element.type:
  group sprite

Layer:
  underlay background foreground

Root Anchor:
  gui screen screen_center
  gui_top_left gui_top_center gui_top_right
  gui_center_left gui_center gui_center_right
  gui_bottom_left gui_bottom_center gui_bottom_right
  screen_top_left screen_top_center screen_top_right
  screen_center_left screen_center_right
  screen_bottom_left screen_bottom_center screen_bottom_right

Child Anchor:
  parent

Pivot:
  parent top_left top_center top_right
  center_left center center_right
  bottom_left bottom_center bottom_right
  or {x,y}

Texture.type:
  resource_location gui_sprite pack_resource

Texture animation.condition:
  always never

Texture animation.missing_frame:
  use_default skip disable

Text.selector:
  title player_inventory

Property:
  visible alpha translate_x translate_y scale_x scale_y rotation_z

Interpolation:
  linear smoothstep

Compose:
  replace add multiply

Event:
  player.health.decreased
  player.health.increased
  player.started_burning
  player.stopped_burning
  screen.opened

Event.mode:
  coalesce

Action.type:
  play_animation stop_animation set_visible set_alpha

Expression functions:
  abs min max hypot clamp lerp
```

相关文档：

- `docs/resourcepack-api.md`：资源包 API 总览与版本选择；
- `docs/SCHEMA_V3.md`：Schema v3.0 独立协议；
- `docs/FEATURES.md`：当前实现功能矩阵。
