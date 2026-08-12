# 功能状态（Minecraft 26.2）

本文描述 RedFoxExpand 26.2 `0.2.1` 当前公开能力与明确边界。

## Schema v2：Strict Definition Protocol

- 使用 Fabric Resource Loader 和同一资源包来源的原生 manifest/config；
- 严格 matcher、priority、`append` / `replace` / `disable`；
- geometry、Slot、Sprite、Text 与 TextRule；
- `underlay`、`background`、`foreground` 三层渲染；
- 整图/区域纹理、ARGB/Alpha 与 Sprite 纹理帧动画；
- `gui` 锚点、geometry 与 Slot 跟随配方书和 resize；
- immutable reload generation、资源预算与错误隔离。

不需要响应式能力的资源包可继续使用 v2；v2 Sprite 不需要 Element ID。

## Schema v3：Reactive UI Protocol

- Definition 内唯一的稳定 Sprite ID；
- Runtime Context：health/max health、burning、sneaking、sprinting、armor、food、air、level、experience、
  screen/gui、鼠标绝对/GUI 相对坐标，以及左右键持续状态；
- reload 时预编译并类型检查的表达式，支持比较、布尔、算术、括号和
  `min/max/clamp/abs/lerp/hypot`；
- `visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z` Binding，数值属性可选指数平滑；
- health decreased/increased、burning started/stopped 和一次性的 `screen.opened` Event；
- ON + IF + ACTIONS Behavior，以及 health Event 的 `every` + `coalesce`；
- play/stop animation、set visible/alpha Action；
- 平移、透明度、缩放、旋转 Property Animation，支持 `linear` / `smoothstep`；
- Base → Binding → Animation → Runtime Override 属性管线，不回写基础坐标和尺寸；
- capability、表达式/行为/动画/实例预算与限频运行诊断；
- F3+T generation、per-screen runtime 和玩家/世界生命周期隔离；
- 与 Minecraft 无关的 Reactive Core 保持 Java 8 源兼容。

完整字段与错误语义见 [Schema v3 协议](SCHEMA_V3.md)。

## Schema v3.1：Scene Graph / Authoring

- 统一 `elements` 数组和显式 `sprite` / 非渲染 `group` 类型；
- Definition-local Element ID、单父级 `children` 场景树与严格引用/深度/环校验；
- 子 Element 局部坐标，以及父级 translate/scale/rotation/visible/alpha 继承；
- GUI/Screen 九点 Anchor 与命名/数值 Pivot；
- `layer → z → scene traversal order` 稳定绘制顺序；
- Definition-local primitive Constants 与按声明顺序求值的 Derived Values；
- Binding target 可读取 `self.*` / `parent.*` 基础布局几何；
- Property Animation track 支持显式 `replace` / `add` / `multiply` 合成；
- Group 可成为 Binding、Animation 与 Action target，整棵子树共享父级效果；
- Element、Group、child、深度、Constants 与 Derived Values 均有明确预算；
- v2/v3.0 格式与既有语义保持不变。

完整字段、默认值、迁移方式和场景组合示例见 [Schema v3.1 协议](SCHEMA_V3_1.md)。

## 26.2 平台能力

- 支持 screen/menu 类、标题 key/text、menu type、GUI resource、Mod namespace 与组合 matcher；
- GUI Anchor 使用应用 geometry 后的实时容器位置，能够跟随 Recipe Book 与 resize；
- GUI 绘制通过 `GuiGraphicsExtractor` 与 `RenderPipelines`，不直接调用 raw OpenGL/LWJGL；
- 资源包只使用小写 `assets/kyeitk/redfoxexpand/` 原生入口，不扫描旧 `assets/Kyeitk/`。

## 安全与回退

Schema v3/v3.1 是声明式输入协议，不支持 Java、反射、eval、脚本、命令、网络、文件系统、任意函数、
无界循环或递归。配置错误按文件隔离，纹理错误按 Definition 隔离；顶层 reload 失败时保留上一 generation。
热路径不进行文件、JSON、PNG 或表达式解析。

## 尚未实现

- HUD API 与 Widget；
- Semantic Slot，以及 Text/Slot reactive target；
- Texture State；
- width、height、color reactive binding；
- Layout Container、Component 与 Clip/Scroll；
- 玩家 3D 模型独立偏移及 3D/网格形变；
- custom variable/event、Timer、User Function、loop/recursion；
- `every.mode=repeat`；
- 结构化诊断模型、离线 Validator、Inspector 与可视化编辑器；
- Schema v3.1 的 1.8.9/1.7.10 平台支持。
