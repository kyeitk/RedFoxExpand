# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。Minecraft 26.2 版本使用
`vX.Y.Z-mc26.2` Git Tag。

## [0.1.0-mc26.2] - 2026-08-09

### Added

- 新增 Minecraft 26.2 / Fabric / JDK 25 独立构建；
- 新增原生小写 ResourceManager 协议、严格 Schema v2 和同来源 pack manifest/config 关联；
- 新增 pack priority、definition priority 以及 `append/replace/disable` 稳定合并；
- 新增 screen/menu、标题、menu type、GUI resource、Mod namespace 和组合 matcher；
- 新增独立逻辑 geometry、Slot 合作位移、渐变高亮、三层贴图和语义文本；
- 新增显式 `resource_location`、`gui_sprite`、`pack_resource` 纹理类型；
- 新增时间动画、资源预算、错误隔离和 F3+T 原子 generation 切换；
- 新增基于 `GuiGraphicsExtractor` / `RenderPipelines` 的现代渲染后端。

### Fixed

- 修复 underlay 被原版全屏灰色遮罩压暗的问题；
- 修复扩展 GUI 与 Slot 在 reload、resize 或其他布局变化后累计漂移的问题；
- 修复配方书展开/收起时破限纹理与 Slot 不同步的问题，`gui` 锚点改用实时容器原点；
- 修复动画目标尺寸比例错误导致正方形帧被压扁的问题。

### Release

- 公开 `26.2` 分支仅包含主源码、客户端源码、构建脚本、公开文档和发布 JAR；
- 排除自动验证代码、示例/验证材质包、迁移审计、本地运行目录和受限制第三方素材。
