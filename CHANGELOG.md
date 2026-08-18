# Changelog

本项目使用语义化版本号，Minecraft 目标版本通过 Git Tag 后缀区分。

## [Unreleased]

## [0.2.2-mc1.8.9] - 2026-08-18

### Removed

- 移除材质包像素预算上限（`MAX_ANIMATION_PIXELS`、`MAX_RELOAD_PIXELS`、`MAX_IMAGE_PIXELS`），
  高分辨率材质包不再因像素总量被拒绝；
- 添加 `MixinAbstractResourcePack` 拦截 Minecraft 原版 64MB 文件大小限制，
  允许超大材质文件正常加载。

### Changed

- 版本号更新为 `0.2.2`。

## [0.2.1-mc1.8.9] - 2026-08-12

### Added

- 新增 Schema v3.1：严格数值 `api_version: 3.1`、统一 `elements`、Sprite/Group 与 Parent/Child 场景树；
- 新增 GUI/Screen 九点 Anchor、Element Pivot、局部坐标和父级平移、缩放、旋转、显隐、透明度继承；
- 新增稳定的 `layer → z → scene order` 绘制顺序；
- 新增 Definition-local `constants`、按声明顺序求值的 `values`；
- 新增 Binding 可用的 `self.*` / `parent.*` 基础布局几何；
- 新增 `replace`、`add`、`multiply` 属性动画合成；
- Group 可作为 Binding、Animation 与 Action target；
- 1.8.9 OpenGL 渲染桥新增 root-to-leaf 场景矩阵；
- 新增完整 Schema v3.1 创作协议与 1.8.9 使用示例。

### Changed

- manifest/config 对 `2`、`3`、`3.1` 使用精确版本读取，不再截断或猜测未来小版本；
- 文档导航、未实现列表和材质包 API 同步至 `0.2.1`；
- v1、v2 与 v3.0 入口保持原有兼容行为。

### Fixed

- 修复 native v2/v3 材质包切换时，Minecraft 1.8.9 `TextureManager` 继续请求上一资源包 PNG 的问题；
- 纹理清理仅作用于 RedFoxExpand 创建的原生纹理缓存，不影响原版或其他 Mod 的共享纹理。

## [0.2.0-mc1.8.9] - 2026-08-10

### Added

- 移植 Java 8 Reactive Core：Runtime Context、Expression、Binding、Event、Behavior、Action、
  Property Animation 与无漂移 Property Pipeline；
- 新增小写 native v2/v3 manifest、同资源包 config 读取与严格 Schema parser；
- 新增 `append`、`replace`、`disable` Definition Registry；
- 新增 health、burning、player、screen、gui、mouse 状态与 `screen.opened`；
- 新增 translate、scale、rotation、smoothing、smoothstep、`set_visible` 与 `set_alpha`；
- 新增 Forge 1.8.9 END client-tick adapter、每屏运行时所有权和生命周期清理；
- 新增 native 静态、区域与内联帧动画纹理。

### Fixed

- 完整类名不再退化为简单类名比较；
- GUI 和槽位类目标新增显式 `exact` / `assignable` 匹配；
- 标题、容器和界面规则按稳定顺序共同合并；
- 玩家背包不再因药水效果改变 GUI、槽位、人物或 GUI 锚点贴图的水平原点；
- 资源重载失败时保留上一可用 generation；
- 动画停止、GUI 关闭或 generation 替换后恢复基础 transform，不累计位移。

## [0.1.0-mc1.8.9] - 2026-08-08

### Added

- 新增 `assets/Kyeitk/` 资源目录扫描；
- 新增 screen/container/title GUI 目标匹配；
- 新增 GUI geometry、槽位偏移与高亮、标题、标签、三层贴图和前景文字；
- 新增整图、局部 UV、锚点、浮点位置、缩放与 Alpha 半透明；
- 新增帧动画、逐帧时长、循环、默认图和缺帧策略；
- 新增第三方 Mod 兼容目录、F3+T 不可变快照重载和旧 Polytone 格式回退。

### Fixed

- 修复扩展 GUI 尺寸导致的原版固定 UV 纹理环绕；
- 修复玩家背包打开后下一 tick 重新计算原点导致的整体左移；
- 修复资源重载后 GUI 和槽位偏移累计；
- 修复低 Alpha 像素裁切与 OpenGL 状态污染；
- 修复 LWJGL 2 `glGetFloat` 缓冲区容量问题。
