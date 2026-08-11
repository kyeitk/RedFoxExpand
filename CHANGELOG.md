# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。Minecraft 1.7.10 版本使用
`vX.Y.Z-mc1.7.10` Git Tag。

## [0.2.0-mc1.7.10] - 2026-08-11

### Added

- 移植与 1.8.9/26.2 同协议的 Java 8 Reactive Core：Runtime Context、Expression、Binding、Event、
  Behavior、Action、Property Animation 与无漂移 Property Pipeline；
- native manifest 扩展为 strict Schema v2/v3，并加入稳定 Sprite ID、严格字段/引用/capability/预算校验；
- 新增 1.7.10 玩家、GUI、screen 与 LWJGL mouse snapshot，Forge legacy FML `ClientTickEvent.END`
  每 tick 推进一次，每个 `GuiContainer` 独占 runtime；
- 新增 visible/alpha/translate/scale/rotation、numeric smoothing、smoothstep、health/burning/screen 事件、
  `every + coalesce` 和 play/stop/set action；
- 新增 native TextureSpec、inline 帧动画、PNG/IHDR/像素预算与完整 Schema v3 公开文档；
- 版本号更新为 `0.2.0`。

### Fixed

- 1.7.10 `IResource` 无 pack name 时，由 ConfigRef 保留实际 active `IResourcePack`，确保 config 与
  声明它的 manifest 从同一资源包读取；
- 修复 v2/v3 切包后 TextureManager 重载上一包旧 PNG：仅移除由 RedFoxExpand 首次创建的
  `SimpleTexture`，不清理原版或其他 Mod 的共享纹理；
- reload 顶层失败改为保留上一 immutable generation；成功替换后清理旧 per-screen Reactive 状态；
- 动画停止、GUI 关闭或 generation 替换后恢复基础 transform，不累计位移；
- 保留既有 1.7.10 背景矩阵、Slot delta、`font_rules`、药水右侧布局和 legacy/v1/v2/v3 共存架构。

### Release

- 公开 `1.7.10` 分支仅包含主源码、构建脚本、公开文档和发布 JAR；
- 排除内部测试/验证代码、示例资源包、移植审计资料、构建缓存和受限制第三方素材。

## [0.1.0-mc1.7.10] - 2026-08-08

### Added

- 新增 Minecraft 1.7.10 / Forge `10.13.4.1614` 独立构建；
- 移植 GUI 配置、槽位、三层贴图、动画、文本、尺寸、药水布局和 F3+T 生命周期；
- 新增 `platform/forge1710` API、类名、标题和背景适配；
- 新增 native lowercase Kyeitk v2 manifest，同时保留跨版本 v1 与旧 Polytone 格式；
- legacy、v1、v2 同时生成候选，并按稳定 `id`、`priority` 和 `append/replace/disable` 合并；
- 新增显式 `font_rules`、现代 `*Menu` 名称映射和资源预算；
- 新增 ForgeGradle 1.2 失效下载地址到 Mojang 官方 content-addressed URL 的构建适配；
- 新增面向材质包作者和开发者的 1.7.10 文档。

### Fixed

- 修复 Slot 重载时覆盖其他 Mod 修改的问题，只撤销 RedFoxExpand 自身记录的偏移；
- 修复扩展 GUI 背景绘制时伪造屏幕宽高的问题；
- 修复 Mixin AP 生成的 Shadow SRG 未接入 ForgeGradle `reobf`，导致生产客户端 Mixin 失效；
- 修复可选 Slot/GUI Mixin 缺失时因接口强制转换而崩溃的问题；
- 修复药水效果出现时玩家背包左移，效果列表改为显示在 GUI 右侧可见区域。

