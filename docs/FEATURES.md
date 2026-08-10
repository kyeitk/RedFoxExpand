# 功能状态（Minecraft 1.8.9）

本文只描述 `RedFoxExpand-1.8.9` 0.2.0 当前已经实现的公开能力与明确边界。

## v1：Legacy Compatibility Protocol

- 继续扫描大写 `assets/Kyeitk/` 文件夹、ZIP 材质包和可枚举 Mod JAR；
- 支持 GUI geometry、Slot 位移/高亮、静态/区域贴图、旧目录帧动画、文字和三层渲染；
- 标题、容器和界面规则按低到高优先级合并，类目标支持 `exact` / `assignable`；
- 玩家背包在有/无药水效果时保持相同原点，药水列表显示在 GUI 右侧；
- 没有可用 v1 配置时保留旧 Polytone GUI modifier 回退。

## Schema v2：Strict Definition Protocol

- 使用小写 `assets/kyeitk/redfoxexpand/index.json`，只读取声明 manifest 的同一资源包 config；
- 严格 matcher、Definition ID、priority、`append` / `replace` / `disable`；
- geometry、Slot、Sprite、Text 与 TextRule；
- 整图/区域纹理、ARGB/Alpha、三种锚点、三层渲染与内联纹理帧动画；
- 路径、JSON、PNG、像素与 reload generation 预算；
- F3+T 原子切换 immutable generation，失败时保留上一代；
- 切换 native v2/v3 材质包时，仅清理由本 Mod 首次创建的旧 `SimpleTexture` 缓存。

## Schema v3：Reactive UI Protocol

- Definition 内唯一的稳定 Sprite ID；
- Runtime Context：health/max health、burning、sneaking、sprinting、armor、food、air、level、experience、
  screen/gui、鼠标绝对/GUI 相对坐标及左右键持续状态；
- Java 8 预编译表达式，支持比较、布尔、算术、括号和 `min/max/clamp/abs/lerp/hypot`；
- `visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z` Binding，数值属性可选平滑；
- health decreased/increased、started/stopped burning 和一次性的 `screen.opened` Event；
- ON + IF + ACTIONS Behavior，以及 health Event 的 `every` + `coalesce`；
- play/stop animation、set visible/alpha Action；
- 平移、透明度、缩放、旋转 Property Animation，支持 `linear` / `smoothstep` 与 restart/ignore；
- Base → Binding → Layout → Animation → Runtime Override 属性管线，不回写基础坐标和尺寸；
- capability、表达式/行为/动画/实例预算与每 GUI 限频诊断；
- 每个 `GuiContainer` 独占运行时；Forge END client tick 推进状态，关闭、玩家变化、resize/init 与 F3+T
  清理旧 animation、smoother、override 和 `every` 累加器。

完整字段、示例和错误语义见 [Schema v3 规范](SCHEMA_V3.md)。

## 1.8.9 平台差异

- 1.8.9 没有现代注册表化菜单/GUI 标识，`menu_type`、`resource_location`、`mod_namespace` matcher
  会被明确拒绝；应使用 screen/menu class、simple class 或 title matcher；
- `net.minecraft.world.inventory.InventoryMenu` 仅作为兼容别名映射到
  `net.minecraft.inventory.ContainerPlayer`，其他现代类名不会被猜测；
- 原版没有 Recipe Book，因此配方书跟随不适用于此版本；
- 渲染继续使用 1.8.9 OpenGL 桥，并在局部绘制后恢复颜色、混合、Alpha、深度、纹理、scissor 和矩阵状态。

## 安全与回退

Schema v3 是声明式、不可信输入协议，不支持 Java、反射、eval、脚本、命令、网络、文件系统、任意函数、
无界循环或递归。配置错误按 config 隔离，纹理错误按 Definition 隔离；顶层 reload 失败时保留上一
generation。热路径不进行文件/ZIP/JSON/PNG/表达式解析。

## 尚未实现

- HUD API、Widget 与通用 Components；
- Semantic Slot，以及 text/Slot reactive target；
- Texture State；
- width、height、color reactive binding；
- 自定义旋转枢轴、玩家 3D 模型独立偏移与 3D/网格形变；
- custom variable/event、Timer、User Function、loop/recursion；
- `every.mode=repeat`；
- 面向材质包作者的 Inspector；
- 完整第三方 Mod、不同 GUI Scale、F3+T 视觉结果和实际 OpenGL 交互矩阵。

这些项目是路线图，不应从接口预留推断为已经实现。
