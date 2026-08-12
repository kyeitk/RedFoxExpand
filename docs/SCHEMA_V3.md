# RedFoxExpand Schema v3：Reactive UI Protocol

本文是 RedFoxExpand Schema v3 公开规范。26.2、1.8.9 与 1.7.10 均已提供各自的平台 adapter；
Minecraft/Fabric/Forge 类名只属于平台实现，不属于协议语义。Schema v3.1 的场景图扩展目前仅适用于 26.2。

## 1. 设计原则

Schema v3 是严格、声明式、不可信输入协议：

```text
v3 = v2 Strict Definition + Stable Element ID + Reactive Runtime
```

资源包描述 WHEN/IF/DO 和 BIND/TO；Mod 提供状态、事件、表达式、安全预算、生命周期与渲染桥。协议不允许
Java/反射/eval/脚本/文件/网络/命令，也不为某个 element ID、材质路径或资源包名称赋予硬编码含义。

## 2. Version

- v1：Legacy Compatibility Protocol；
- v2：Strict Definition / Manifest Protocol；
- v3：Reactive UI Protocol。

26.2 同时接受 v2 与 v3。一个 manifest 的 `api_version` 决定它引用的全部 config 版本；manifest v3
引用 v2 config（或反之）会作为该 config 的明确校验错误，不做自动猜测。

## 3. Manifest

沿用唯一发现入口：

```text
assets/kyeitk/redfoxexpand/index.json
```

```json
{
  "api_version": 3,
  "configs": ["redfoxexpand/config/reactive_inventory.json"]
}
```

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `api_version` | integer | 必填 | v3 必须为 `3` |
| `configs` | string[] | 必填 | 0..256；同 pack、固定目录、小写安全路径 |

不提供 `reactive-index.json`、`behavior-index.json` 等第二发现机制。

## 4. Config Root

```json
{
  "api_version": 3,
  "definitions": []
}
```

根只允许 `api_version` 与 `definitions`。单文件最多 256 个 Definition；未知字段或错误类型拒绝整个
config，其他 config 继续加载。

## 5. Definition

v3 完整保留 v2 字段及语义：

| 字段 | 默认 | 说明 |
|---|---:|---|
| `id` | 必填 | 小写 namespaced Definition ID |
| `operation` | `append` | `append` / `replace` / `disable` |
| `priority` | `0` | Definition priority |
| `match` | 必填 | 与 v2 完全相同的严格 matcher |
| `geometry` | 全 0 | 与 v2 相同 |
| `slot_modifiers` | `[]` | 与 v2 相同 |
| `sprites` | `[]` | v2 Sprite 字段 + v3 `id` |
| `texts` | `[]` | 与 v2 相同；MVP 不作为 reactive target |
| `text_rules` | `[]` | 与 v2 相同 |
| `bindings` | `[]` | 连续状态绑定 |
| `animations` | `[]` | Property Animation，不是纹理帧 animation |
| `behaviors` | `[]` | WHEN + IF + ACTIONS |

候选顺序和 `append/replace/disable` 仍是
`pack priority -> definition priority -> source path -> array index`。Reactive 数据与基础 Definition 一同进入
immutable generation，不建立旁路 registry。

## 6. Element ID

v3 的每个 Sprite 必须有：

```json
{"id":"character", "texture": {"type":"pack_resource", "location":"textures/gui/character.png"}}
```

- 格式：`[a-z0-9_.-]+`，最长 512 字符；
- 在单个 Definition 中唯一；
- Binding/Action 的 `target` 只能引用同一 Definition 的 Sprite ID；
- 两个同时匹配的 Definition 可以各自使用 `character`，运行时作用域互不相干；
- v2 Sprite 无 ID，继续正常绘制，但不能成为 Reactive target。

MVP 不允许 text、Slot、Widget 作为 target。

## 7. Runtime Context

26.2 MVP 每 client tick 提供：

| Variable | Type | 语义 |
|---|---|---|
| `player.health` | number | Minecraft Health Point |
| `player.max_health` | number | 最大 Health Point |
| `player.is_burning` | boolean | 当前是否着火 |
| `player.is_sneaking` | boolean | 当前是否潜行 |
| `player.is_sprinting` | boolean | 当前是否疾跑 |
| `player.armor` | number | armor value |
| `player.food` | number | food level |
| `player.air` | number | air supply |
| `player.level` | number | experience level |
| `player.experience` | number | 当前等级进度，平台原始 0..1 值 |
| `screen.width/height` | number | GUI-scaled 屏幕尺寸 |
| `gui.x/y` | number | 应用 geometry 后的 live GUI origin |
| `gui.width/height` | number | 原版 image 尺寸 + geometry offset |
| `mouse.x/y` | number | GUI-scaled 屏幕绝对鼠标坐标 |
| `mouse.gui_x/gui_y` | number | 相对应用 geometry 后实时 GUI origin 的鼠标坐标 |
| `mouse.left_down` | boolean | 当前 client tick 左键是否持续按下 |
| `mouse.right_down` | boolean | 当前 client tick 右键是否持续按下 |

`gui.x/y` 随 resize 与配方书 leftPos/topPos 更新；`mouse.gui_x = mouse.x - gui.x`，`mouse.gui_y = mouse.y - gui.y`。
GUI 相对坐标不 clamp，鼠标位于 GUI 左侧/上方时为负数，位于 GUI 范围外时可以大于 `gui.width/height`。
左右键可同时为 `true`；它们是每 client tick 采样的持续 State，不是按下沿/释放沿，也不会生成鼠标 Event。
若 26.2 的 native cursor/window 缩放 API 在窗口瞬态状态返回 `NaN` 或 Infinity，平台 adapter 会把对应
`mouse.x` 或 `mouse.y` 回退为 `0`，对应 GUI 相对坐标随后仍按上述公式计算；Core 仍只接受有限 double，
其他 Runtime Context 数值不会因此放宽。

## 8. Data Type

Core 只有三种数据类型：

- `number`：有限 double；NaN/Infinity 永远非法；
- `boolean`：`true` / `false`；
- `string`：受控字符串值与双引号 literal，MVP 没有公开 string 状态变量。

不进行 number/boolean/string 隐式转换。

## 9. Expression Grammar

按低到高优先级：

```text
or             := and ("||" and)*
and            := equality ("&&" equality)*
equality       := comparison (("==" | "!=") comparison)*
comparison     := additive (("<" | "<=" | ">" | ">=") additive)*
additive       := multiply (("+" | "-") multiply)*
multiply       := unary (("*" | "/") unary)*
unary          := ("!" | "+" | "-") unary | primary
primary        := number | boolean | string | variable | function | "(" or ")"
```

- 比较与算术只接受 number；`!` 只接受 boolean；
- `&&` / `||` 短路求值；
- `==` / `!=` 要求两侧类型相同；
- 未知变量、未知函数、非法 token、括号/参数/类型错误在 reload 拒绝；
- 表达式在 reload 预编译，tick/render 只 evaluate，不重新 tokenize/parse。

## 10. Functions

白名单固定为：

```text
min(a, b)
max(a, b)
clamp(value, min, max)
abs(value)
lerp(a, b, t)
hypot(a, b)
```

参数数量和类型严格；`hypot(a,b)` 返回稳定的二维欧氏距离；`clamp` 的 min 大于 max、除零或任何非有限运行结果使用安全 fallback，并由平台限频
记录 warning。不存在任意函数调用。

## 11. State

State 表示“当前是什么”：

```text
player.is_burning
player.health <= 15
clamp((15 - player.health) / 15, 0, 1)
mouse.left_down || mouse.right_down
mouse.gui_x >= 0 && mouse.gui_x < gui.width
```

持续属性应使用 Binding。不得通过每 tick Action 模拟持续状态。

## 12. Event

MVP Event：

```text
player.health.decreased
player.health.increased
player.started_burning
player.stopped_burning
screen.opened
```

Event 由同一 Screen runtime 的相邻 client-tick RuntimeSnapshot 推导。第一份 snapshot 只建立基线；同一 tick
内多次 render 不会产生或重放 Event。唯一例外是 `screen.opened`：它在 per-screen runtime 创建并完成第一轮
Binding 求值后合成一次，用于启动循环动画；resize/Recipe Book 的同候选 reconcile 不会重复触发，F3+T、
候选集合变化、LocalPlayer 更换或关闭后重开导致 runtime 重建时会再次触发。

## 13. Event Payload

health Event 提供：

| Variable | 语义 |
|---|---|
| `event.old` | 变化前 health |
| `event.new` | 变化后 health |
| `event.delta` | 始终为正的绝对变化量 |

burning 与 `screen.opened` Event 没有上述 payload；在这些 behavior 中引用 `event.delta` 会在 reload 被当作
未知变量拒绝。

## 14. Binding

```json
{
  "target": "character_fire",
  "property": "visible",
  "value": "player.is_burning"
}
```

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `target` | string | 必填 | 同 Definition element ID |
| `property` | enum | 必填 | 见下一节 |
| `value` | string expression | 必填 | reload 预编译，类型必须与 property 一致 |
| `smoothing_ms` | integer | `0` | 0..600000；仅 number property，作为 render-time 指数平滑的时间常数 |

同一 target/property 只能有一个 Binding。运行求值失败时只回退该属性的 base 值，不影响其他元素；日志
按 generation/screen scope 限频。`smoothing_ms:0` 保持逐 tick 立即更新；非零值在第一次成功求值时立即建立
初值，后续目标变化从当前采样值平滑过渡，不会把旧 generation/runtime 的状态带入 F3+T 或关闭重开。
boolean Binding 声明 `smoothing_ms > 0` 会在 reload 拒绝。

## 15. Property

| Property | 类型 | Base | Binding / Animation 语义 |
|---|---|---:|---|
| `visible` | boolean | `true` | Binding 或 `set_visible` 最终覆盖 |
| `alpha` | number | `1` | 结果 clamp 到 0..1，再乘 Sprite 原 ARGB Alpha |
| `translate_x` | number | `0` | 相对 base/anchor 的瞬时横向偏移 |
| `translate_y` | number | `0` | 相对 base/anchor 的瞬时纵向偏移 |
| `scale_x` | number | `1` | 以 Sprite 中心为原点的瞬时横向缩放，最终 clamp 到 0..8 |
| `scale_y` | number | `1` | 以 Sprite 中心为原点的瞬时纵向缩放，最终 clamp 到 0..8 |
| `rotation_z` | number | `0` | 以 Sprite 中心为枢轴的顺时针二维旋转角度（度），最终 clamp 到 -360..360 |

width、height、color 和自定义旋转枢轴不属于 26.2 MVP。`scale_x/scale_y/rotation_z` 不修改 Sprite 的基础 width/height、锚点或 x/y；
`0` 表示该轴不绘制，负数和大于 `8` 的动画关键帧在 reload 拒绝。Binding 运行产生非有限值时按现有运行
失败规则回退 Base，最终属性管线还会把 scale 限制在 0..8、rotation 限制在 -360..360。

## 16. Property Animation

Property Animation 与 Sprite 内现有纹理帧 `animation` 是两套正交能力：前者改变 translate/alpha/scale/rotation，后者选择
texture frame。

```json
{
  "id": "damage_shake",
  "duration_ms": 240,
  "loop": false,
  "tracks": [{
    "property": "translate_x",
    "interpolation": "linear",
    "keyframes": [
      {"time_ms":0,"value":0},
      {"time_ms":40,"value":-5},
      {"time_ms":80,"value":5},
      {"time_ms":240,"value":0}
    ]
  }]
}
```

规则：

- `id` 必填且在 Definition 内唯一；
- `duration_ms` 必填，1..600000；
- `loop` 默认 `false`；
- `tracks` 必填 1..16，property 仅 `translate_x`、`translate_y`、`alpha`、`scale_x`、`scale_y`、`rotation_z`；
- `interpolation` 默认 `linear`，接受 `linear` 或分段内零端点速度的 `smoothstep`；
- `keyframes` 必填 1..128，第一帧必须在 0 ms，时间严格递增且不超过 duration，value 必须有限；
- 非循环动画在 duration 时采样结束值，之后移除并返回 base/binding；
- 同时运行的 translate/rotation track 相加；scale track 相乘；多个 alpha track 由最后启动的活动实例决定；
- animation 永远不修改 Sprite base x/y，因此不会累计漂移。

## 17. Behavior

```json
{
  "on": {
    "event": "player.health.decreased",
    "every": 2,
    "mode": "coalesce"
  },
  "if": "event.delta > 0",
  "actions": []
}
```

| 字段 | 默认 | 说明 |
|---|---:|---|
| `on.event` | 必填 | MVP Event ID |
| `on.every` | 无 | 正有限 number；仅 health change Event |
| `on.mode` | `coalesce` | MVP 只接受 `coalesce` |
| `if` | `"true"` | boolean expression；health Event 可读 payload |
| `actions` | 必填 | 1..32，按数组顺序执行 |

`every:2` 会累计 health delta。1+1 触发一次；单次下降 6 在 `coalesce` 下只触发一次并保留除以 2 的余数，
不会同 tick 创建三份动画实例。

## 18. Action

### play_animation

```json
{"type":"play_animation","target":"character","animation":"damage_shake","restart":true}
```

`restart` 默认 `true`；为 `false` 时，目标上的同 ID 动画仍在运行则忽略本次 play。

### stop_animation

```json
{"type":"stop_animation","target":"character","animation":"damage_shake"}
```

### set_visible

```json
{"type":"set_visible","target":"character_fire","value":false}
```

### set_alpha

```json
{"type":"set_alpha","target":"character_heat","value":0.5}
```

`set_alpha` value 必须是 0..1 有限 number。set action 是 runtime override，会保持到同 target 的后续 set 或
runtime dispose；MVP 不提供 clear action。未知 action、target 或 animation 在 reload 拒绝。

## 19. Precedence

单帧最终属性顺序：

```text
Sprite Base
  -> Binding
  -> live Anchor / Layout
  -> Property Animation
  -> Runtime Override (set_visible / set_alpha)
  -> Final Render Property
  -> existing GuiTextureRenderer
```

translate 是 offset；任何阶段都不执行 `sprite.x += offset`。数值 Binding 的平滑结果在 Animation 前采样；
scale Binding 与动画按乘法合成，rotation Binding 与动画按角度相加，并由 renderer 围绕 Sprite 中心改变
本帧目标矩形/pose，不回写 base width/height/x/y。Alpha 最终 clamp 后与 Sprite 原 ARGB Alpha
相乘，而不是丢弃资源包设置的基础透明度。

## 20. Lifecycle

```text
F3+T prepare
  -> parse manifest/config v2 or v3
  -> compile expression
  -> resolve element/animation/action references
  -> validate capability/budget/texture
  -> build immutable generation
  -> atomic install
  -> old per-screen runtime disposed on next reconcile
```

- Screen open：创建 runtime，以当前 snapshot 建立基线，不伪造 damage Event；首轮 Binding 后只触发一次 `screen.opened`；
- END_CLIENT_TICK：更新 snapshot、生成 Event、求 Binding、执行 Behavior；
- render：只采样 Property Animation 与最终属性；
- resize/recipe book：candidate 集合不变则保留动画和 accumulator，只更新 live GUI context；
- F3+T 或匹配 candidate 集合变化：新 runtime 整体替换；
- Screen close：删除 runtime，不保留 Screen/Player/World 强引用；
- player/world 离开或 LocalPlayer 实例改变：重建 runtime，清除 accumulator、override 与动画；
- 系统时钟调整不会使动画倒退；26.2 使用 monotonic clock。

## 21. Budget

在全部 v2 预算基础上增加：

| 项目 | 上限 | 超限行为 |
|---|---:|---|
| binding / Definition | 128 | 拒绝 config |
| behavior / Definition | 128 | 拒绝 config |
| property animation / Definition | 64 | 拒绝 config |
| track / animation | 16 | 拒绝 config |
| keyframe / track | 128 | 拒绝 config |
| action / behavior | 32 | 拒绝 config |
| expression chars | 1024 | 拒绝 config |
| expression tokens | 256 | 拒绝 config |
| expression AST depth | 32 | 拒绝 config |
| function arguments | 3 | 拒绝 config |
| active animation instances / Screen | 32 | 拒绝新实例并限频 warning |

循环动画仍受“每 Screen 32 实例”限制；Schema 没有创建无界 timer/instance 的能力。

## 22. Validation

reload 严格拒绝：

- 未知字段、enum、property、event、action、interpolation；
- 错误类型、NaN/Infinity、非整数 duration/time；
- 非法或重复 element/animation ID；
- 不存在的 target/animation；
- 未知/不支持变量与函数、语法/括号/类型错误；
- 非法 keyframe 次序、范围或预算；
- 当前平台不支持的 capability。

不把 `translateX`、`translate-x`、`translation_x` 猜成 `translate_x`。config 解析失败按文件隔离；通过解析后
某个 Definition 的纹理失败按 Definition 隔离；顶层 reload 失败保留上一 generation。

## 23. Security

Schema v3 不支持并禁止：

```text
Java/reflection/eval/JavaScript/Lua
shell/PowerShell/cmd
HTTP/filesystem
Minecraft command
for/while/recursion/user function/import
custom mutable variable/unbounded timer
```

热路径不进行文件/ZIP/JSON/PNG/Expression parse、反射或资源路径解析。26.2 renderer 不调用 raw OpenGL/
LWJGL，继续使用 `GuiGraphicsExtractor` 与 `RenderPipelines`。

## 24. Schema v2 -> v3 Migration

1. manifest/config `api_version` 从 2 改为 3；
2. 为 v3 config 中每个 Sprite 添加 Definition-local `id`；
3. 保留原 matcher/operation/priority/geometry/slot/texture/sprite/text 字段；
4. 持续状态加入 `bindings`；
5. 瞬时变换加入 `animations`；
6. Event 响应加入 `behaviors`；
7. 不把现有 Sprite 纹理帧 `animation` 改成 Definition-level `animations`。

不需要 reactive 能力的旧包应继续保持 v2，无需为了版本号迁移而补 ID。

## 25. Cross-Version Capability

只维护一个 Schema v3。平台通过 capability 明确差异；使用不支持能力会产生 reload validation error，不能
静默 false/0。

26.2 MVP capability：

```text
PLAYER_HEALTH / PLAYER_MAX_HEALTH / PLAYER_BURNING
PLAYER_SNEAKING / PLAYER_SPRINTING
PLAYER_ARMOR / PLAYER_FOOD / PLAYER_AIR / PLAYER_LEVEL / PLAYER_EXPERIENCE
SCREEN_SIZE / GUI_POSITION / GUI_SIZE / MOUSE_POSITION / MOUSE_BUTTONS
PROPERTY_VISIBLE / PROPERTY_ALPHA / PROPERTY_TRANSLATE / PROPERTY_SCALE
PROPERTY_ROTATION
EVENT_HEALTH / EVENT_BURNING / EVENT_SCREEN_LIFECYCLE
ACTION_ANIMATION / ACTION_SET_VISIBLE / ACTION_SET_ALPHA
```

三版本回迁最低共同集合是 health/max_health/burning、screen/gui、visible/alpha/translate、
health.decreased 与 play_animation。旧版本尚未实现前不得宣称已跨版本运行确认。

## 26. Full Example

下面是一份完整核心 config。将对应 PNG 放入配置引用的位置，并按本文 Manifest 章节建立发现入口即可使用：

```json
{
  "api_version": 3,
  "definitions": [{
    "id": "example:reactive_inventory",
    "operation": "append",
    "priority": 100,
    "match": {"exact_menu_class":"net.minecraft.world.inventory.InventoryMenu"},
    "geometry": {},
    "slot_modifiers": [],
    "sprites": [
      {
        "id":"character",
        "texture":{"type":"pack_resource","location":"textures/gui/reactive/character.png"},
        "anchor":"gui","x":180,"y":-20,"width":120,"height":120,"layer":"underlay"
      },
      {
        "id":"character_fire",
        "texture":{"type":"pack_resource","location":"textures/gui/reactive/fire.png"},
        "anchor":"gui","x":190,"y":100,"width":96,"height":64,
        "color":"#C0FF6020","layer":"underlay"
      },
      {
        "id":"character_heat",
        "texture":{"type":"pack_resource","location":"textures/gui/reactive/head_heat.png"},
        "anchor":"gui","x":210,"y":0,"width":52,"height":48,
        "color":"#A0FF3030","layer":"background"
      }
    ],
    "texts": [],
    "text_rules": [],
    "bindings": [
      {"target":"character_fire","property":"visible","value":"player.is_burning"},
      {"target":"character_heat","property":"alpha",
       "value":"clamp((15 - player.health) / 15, 0, 1)"}
    ],
    "animations": [{
      "id":"damage_shake","duration_ms":240,"loop":false,
      "tracks":[{
        "property":"translate_x","interpolation":"linear",
        "keyframes":[
          {"time_ms":0,"value":0},{"time_ms":40,"value":-5},
          {"time_ms":80,"value":5},{"time_ms":120,"value":-4},
          {"time_ms":160,"value":3},{"time_ms":200,"value":-1},
          {"time_ms":240,"value":0}
        ]
      }]
    }],
    "behaviors": [{
      "on":{"event":"player.health.decreased","every":2,"mode":"coalesce"},
      "if":"event.delta > 0",
      "actions":[{
        "type":"play_animation","target":"character",
        "animation":"damage_shake","restart":true
      }]
    }]
  }]
}
```

当前限制：HUD、Widget、Semantic Slot、Texture State、自定义旋转枢轴/3D 网格形变、Custom Variable/Event、
Timer、User Function、Loop/递归、`every.mode=repeat`、Inspector，以及 text/Slot reactive target 均未实现；
1.8.9 与 1.7.10 尚未完成 v3 回迁。
