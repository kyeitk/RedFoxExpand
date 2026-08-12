# Changelog

本项目使用语义化版本号，Minecraft 目标版本通过 Git Tag 后缀区分。

## [Unreleased]

## [0.2.1-mc1.7.10] - 2026-08-12

### Added

- 新增 Schema v3.1：严格数值 `api_version: 3.1`、统一 `elements`、Sprite/Group 与 Parent/Child 场景树；
- 新增 GUI/Screen 九点 Anchor、Element Pivot、局部坐标和父级平移、缩放、旋转、显隐、透明度继承；
- 新增稳定的 `layer → z → scene order` 绘制顺序；
- 新增 Definition-local `constants`、按声明顺序求值的 `values`；
- 新增 Binding 可用的 `self.*` / `parent.*` 基础布局几何；
- 新增 `replace`、`add`、`multiply` 属性动画合成；
- Group 可作为 Binding、Animation 与 Action target；
- 1.7.10 `GL11/GL14 + Tessellator` 渲染桥新增 root-to-leaf 场景矩阵与继承式状态组合；
- 新增完整 Schema v3.1 创作协议与 1.7.10 使用示例。

### Changed

- manifest/config 对 `2`、`3`、`3.1` 使用精确版本读取，不再截断或猜测未来小版本；
- 功能边界、未实现列表和材质包 API 同步至 `0.2.1`；
- v1、v2 与 v3.0 入口保持原有兼容行为；
- `font_rules`、药水布局、同资源包 config 来源隔离和旧纹理清理保持兼容。

## [0.2.0-mc1.7.10] - 2026-08-11

### Added

- 移植 Java 8 Reactive Core：Runtime Context、Expression、Binding、Event、Behavior、Action、
  Property Animation 与无漂移 Property Pipeline；
- 原生 manifest 扩展为严格 Schema v2/v3，并加入稳定 Sprite ID、字段、引用、能力与预算检查；
- 新增 1.7.10 玩家、GUI、Screen 与 LWJGL mouse snapshot；
- 新增 Forge `ClientTickEvent.END` 状态推进和每个 `GuiContainer` 独立运行时；
- 新增 visible、alpha、translate、scale、rotation、numeric smoothing 与 smoothstep；
- 新增 health、burning、screen 事件、`every + coalesce` 和 play/stop/set Action；
- 新增原生 TextureSpec、内联帧动画和 PNG/IHDR/像素预算。

### Fixed

- `IResource` 无资源包名称时，由 ConfigRef 持有实际活动 `IResourcePack` 并从同包读取 config；
- 修复 v2/v3 切包后 TextureManager 继续读取上一资源包 PNG 的问题；
- 仅移除 RedFoxExpand 创建的 `SimpleTexture`，不影响原版或其他 Mod 共享纹理；
- 资源重载失败时保留上一可用 generation；
- generation 替换后释放旧 per-screen 响应式状态；
- 保留既有背景矩阵、Slot delta、`font_rules`、药水右侧布局和多版本入口共存架构。

## [0.1.0-mc1.7.10] - 2026-08-08

### Added

- 建立 Minecraft 1.7.10 / Forge `10.13.4.1614` 独立项目；
- 移植 GUI 配置、槽位、三层贴图、动画、文本、尺寸、药水布局和 F3+T 生命周期；
- 新增 `platform/forge1710` API、类名、标题与背景适配；
- 新增小写原生 v2 manifest，同时保留 v1 与旧 Polytone 入口；
- 新增 Definition `id`、`priority`、`operation` 与显式 `font_rules`；
- 新增配置、列表、图片和动态纹理资源预算。

### Fixed

- Slot 只撤销 RedFoxExpand 自身 delta；
- 背景不再伪造 Screen 宽高；
- ForgeGradle 1.2 的旧 Mojang 地址改为官方 content-addressed URL；
- 修复 Mixin AP Shadow SRG 未接入 ForgeGradle `reobf` 的问题；
- 可选 Slot/GUI Mixin 缺失时安全降级。
