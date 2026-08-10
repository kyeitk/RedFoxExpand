# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。Minecraft 26.2 版本使用
`vX.Y.Z-mc26.2` Git Tag。

## [0.2.0-mc26.2] - 2026-08-10

### Added

- 新增与 Schema v2 并存的严格 Schema v3 Reactive UI Protocol；v2 材质包无需迁移；
- 新增 Definition-local Sprite ID、预编译表达式、类型检查和 `min/max/clamp/abs/lerp/hypot` 白名单函数；
- 新增玩家、屏幕、GUI、鼠标位置与左右键 Runtime Context；非有限鼠标坐标安全回退；
- 新增 health、burning 与 `screen.opened` 事件，以及带 `every` / `coalesce` 的 Behavior；
- 新增 `visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z` Binding 和数值平滑；
- 新增 Definition-level Property Animation、`linear` / `smoothstep` 插值，以及播放、停止、可见性和
  透明度 Action；
- 新增 Base → Binding → Layout → Animation → Runtime Override 的无漂移属性管线；
- 新增 capability、表达式/动画实例预算、限频诊断和按 Screen 隔离的响应式生命周期；
- 新增 Java 8 源兼容的独立 Reactive Core，以及 Schema v3 公开规范和使用示例；
- README 新增项目设计哲学、目标架构与优先路线图。

### Changed

- 配方书展开/收起与 resize 在候选不变时保留当前动画、事件累计器和响应式状态；
- F3+T、候选集合变化、玩家切换或 Screen 重开会创建全新 runtime，避免跨 generation 泄漏状态；
- 公开功能清单与未实现列表按 Schema v3 MVP 的实际边界更新。

### Release

- `26.2` 分支继续只发布主源码、客户端源码、构建脚本、公开文档与发布 JAR；
- 自动测试代码、测试资源、示例材质包、内部审计/实施文档和受限制第三方素材不进入公开上传版。

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
