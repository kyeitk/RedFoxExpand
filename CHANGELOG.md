# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。正式版本使用 `vX.Y.Z` Git Tag。

## [0.1.0] - 2026-08-08

### Added

- 新增固定 `assets/Kyeitk/` 资源目录扫描，支持文件夹、ZIP 材质包和可枚举 Mod JAR；
- 新增 `screen_class`、`container_class` / `menu_class`、`screen_title` GUI 目标匹配；
- 新增 GUI 位置/尺寸、槽位偏移/高亮、标题/标签、三层贴图和前景文字配置；
- 新增整图、局部 UV、三种锚点、浮点位置与缩放；
- 新增 RGBA / Alpha 半透明绘制及完整 OpenGL 状态恢复；
- 新增缓存式 GUI 动画、逐帧时长、循环、默认图和缺帧策略；
- 新增 `compatibility/<modid>/` 第三方 Mod 兼容目录；
- 新增 F3+T 不可变快照重载和当前 GUI 刷新；
- 新增旧 Polytone GUI modifier 格式的兼容回退；
- GUI 和槽位类目标新增显式 `exact` / `assignable` 匹配；
- 新增 `resource_type`，区分原始 ResourceLocation、GUI sprite 和旧启发式兼容；
- 标题、容器和界面规则按由低到高优先级共同合并；
- 新增面向材质包作者和开发者的完整文档。

### Fixed

- 修复扩展 GUI 尺寸被用于原版固定 UV 背景取样而产生的纹理环绕；
- 修复 `InventoryEffectRenderer` 在打开玩家背包后的下一 tick 重算原点导致整体左移；
- 修复资源重载后 GUI 和槽位偏移累计；
- 修复低 Alpha 像素被 Alpha Test 裁切以及绘制状态污染后续 GUI；
- 修复 LWJGL 2 `glGetFloat` 对缓冲区容量的要求导致的崩溃。
- 完整类名不再退化为简单类名比较，避免不同包中同名 GUI/Slot 误命中；
- 玩家背包不再因药水效果改变 GUI、槽位、人物或 GUI 锚点贴图的水平原点；
- 药水效果列表改为优先紧邻 GUI 右侧，窄屏时钳制在可见区域。

### Release

- 公开仓库排除本地运行目录、构建缓存、日志、测试材质包、参考底包和受限制第三方素材；
- 公开仓库不包含工作区内部验证代码和验证资源。
