# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。Minecraft 1.7.10 版本使用
`vX.Y.Z-mc1.7.10` Git Tag。

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

### Release

- 公开 `1.7.10` 分支仅包含主源码、构建脚本、公开文档和发布 JAR；
- 排除内部验证代码、验证资源、构建缓存、本地运行目录和受限制第三方素材。
