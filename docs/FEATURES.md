# 功能状态（Minecraft 26.2）

本文只描述 `RedFoxExpand-26.2` 0.2.0 当前已经实现的公开能力与明确边界。

## Schema v2：Strict Definition Protocol

- 使用 Fabric Resource Loader 和同 pack 的原生 manifest/config discovery；
- 严格 matcher、priority、`append` / `replace` / `disable`；
- geometry、Slot、Sprite、Text 与 TextRule；
- `underlay`、`background`、`foreground` 三层渲染；
- 整图/区域纹理、ARGB/Alpha 与 Sprite 纹理帧动画；
- `gui` 锚点、geometry 与 Slot 跟随配方书和 resize；
- immutable reload generation、资源预算与错误隔离。

Schema v3 没有改变这些语义；不需要响应式功能的 v2 材质包无需迁移，v2 Sprite 也不需要元素 ID。

## Schema v3：Reactive UI Protocol

- Definition 内唯一的稳定 Sprite Element ID；
- Runtime Context：health/max health、burning、sneaking、sprinting、armor、food、air、level、experience、
  screen/gui、鼠标绝对/GUI 相对坐标，以及左右键持续状态；
- reload 时预编译并类型检查的表达式，支持比较、布尔、算术、括号和
  `min/max/clamp/abs/lerp/hypot`；
- `visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z` Binding，数值属性可选指数平滑；
- health decreased/increased、started/stopped burning 和一次性的 `screen.opened` Event；
- ON + IF + ACTIONS Behavior，以及 health Event 的 `every` + `coalesce`；
- play/stop animation、set visible/alpha Action；
- 平移、透明度、缩放、旋转 Property Animation，支持 `linear` / `smoothstep` 与 restart/ignore；
- Base → Binding → Layout → Animation → Runtime Override 属性管线，不回写基础坐标和尺寸；
- capability、表达式/行为/动画/实例预算与限频运行诊断；
- F3+T generation、per-screen runtime 和玩家/世界生命周期隔离；
- 与 Minecraft 无关的 Reactive Core 保持 Java 8 源兼容。

完整字段、默认值、示例和错误语义见 [Schema v3 规范](SCHEMA_V3.md)。

## 安全与回退

Schema v3 是声明式、不可信输入协议，不支持 Java、反射、eval、脚本、命令、网络、文件系统、任意函数、
无界循环或递归。配置错误按文件隔离，纹理错误按 Definition 隔离；顶层 reload 失败时保留上一 generation。
热路径不进行文件/ZIP/JSON/PNG/表达式解析。

## 尚未实现

- HUD API、Widget 与通用 Components；
- Semantic Slot，以及 text/Slot reactive target；
- Texture State；
- width、height、color reactive binding；
- 自定义旋转枢轴、玩家 3D 模型独立偏移与 3D/网格形变；
- custom variable/event、Timer、User Function、loop/recursion；
- `every.mode=repeat`；
- 面向材质包作者的 Inspector；
- 1.8.9/1.7.10 Schema v3 回迁；
- 完整 OpenGL/Vulkan、第三方 Screen 与生产实例交互矩阵。

这些项目是路线图，不应从接口预留推断为已经实现。
