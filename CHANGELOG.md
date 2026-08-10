# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。Minecraft 1.8.9 版本使用
`vX.Y.Z-mc1.8.9` Git Tag。

## [0.2.0-mc1.8.9] - 2026-08-10

### Added

- 移植与 26.2 逐文件一致的 Java 8 Reactive Core：Runtime Context、Expression、Binding、Event、
  Behavior、Action、Property Animation 与无漂移 Property Pipeline；
- 新增小写 native v2/v3 manifest、同 pack config 解析、严格 Schema parser 与
  `append/replace/disable` Definition Registry；
- 新增 health/burning/player/screen/gui/mouse 状态、`screen.opened`、scale、rotation、
  smoothing、smoothstep、`set_visible` 与 `set_alpha`；
- 新增 Forge 1.8.9 END client-tick adapter、每屏运行时所有权、玩家实例变化/关闭 GUI/F3+T 清理；
- 新增 native 静态/区域/inline 帧动画纹理、路径/JSON/PNG/像素预算与 Schema v3 公开规范；
- 版本号更新为 `0.2.0`。

### Fixed

- 完整类名不再退化为简单类名比较，避免不同包中同名 GUI/Slot 误命中；
- GUI 和槽位类目标新增显式 `exact` / `assignable` 匹配，默认保持 `exact`；
- 贴图新增 `resource_type`，新 `custom_textures` 默认保留原始 ResourceLocation，旧 `sprites` 保留自动图集兼容；
- 标题、容器和界面规则改为共同合并，固定按标题、容器、界面由低到高优先级应用；
- 玩家背包不再因药水效果改变 GUI、槽位、人物或 GUI 锚点贴图的水平原点；原版药水效果列表统一
  绘制在 GUI 右侧，窄屏时钳制到屏幕右边界内。
- reload 顶层失败不再清空正在使用的配置，而是保留上一 immutable generation；
- Reactive 动画停止、GUI 关闭或 generation 替换后恢复基础 transform，不累计位移；
- 修复在 native v2/v3 材质包之间切换时，`TextureManager` 持续重载上一材质包 `SimpleTexture` 而产生
  旧 PNG `FileNotFoundException`；现在只在 reload 前移除由 RedFoxExpand 首次创建的 native 纹理缓存，
  不清理原版或其他 Mod 已拥有的共享纹理。

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
- 新增面向材质包作者和开发者的完整文档。

### Fixed

- 修复扩展 GUI 尺寸被用于原版固定 UV 背景取样而产生的纹理环绕；
- 修复 `InventoryEffectRenderer` 在打开玩家背包后的下一 tick 重算原点导致整体左移；
- 修复资源重载后 GUI 和槽位偏移累计；
- 修复低 Alpha 像素被 Alpha Test 裁切以及绘制状态污染后续 GUI；
- 修复 LWJGL 2 `glGetFloat` 对缓冲区容量的要求导致的崩溃。
