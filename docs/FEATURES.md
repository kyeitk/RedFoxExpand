# 功能状态（Minecraft 1.7.10）

本文只描述 `RedFoxExpand-1.7.10` 0.2.0 当前已经实现的公开能力与明确边界。

## 资源发现与合并

- 兼容 `assets/Kyeitk/` v1 文件夹、ZIP/JAR、Mod 来源与服务器资源包；
- 支持原生小写域 `assets/kyeitk/redfoxexpand/index.json` strict v2/v3 manifest；
- 兼容 `assets/<namespace>/polytone/gui_modifiers/**/*.json`；
- legacy、v1、v2/v3 同时生成候选，不使用“发现新格式就全局关闭旧格式”的开关；
- 按资源包优先级选择同路径内容，再由稳定 `id` 和 `append/replace/disable` 合并；
- F3+T 时重建不可变配置快照并释放上一代动态纹理。
- native config 直接保留实际 `IResourcePack` 来源，避免 1.7.10 缺少 pack-name API 时跨包串读；
- v2/v3 上一包首次创建的 `SimpleTexture` 会在下一轮 TextureManager reload 遍历前定向清理。

## Schema v3 Reactive UI

- Minecraft 无关的 strict parser 与 Reactive Core：Expression、Binding、Event、Behavior、Action、
  Property Animation 和无漂移 Property Pipeline；
- Runtime Context：玩家状态、屏幕/GUI geometry、鼠标屏幕/GUI 相对坐标及左右键持续状态；
- Property：`visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z`；
- Event：health 增减、开始/停止燃烧、`screen.opened`，health `event.delta` 始终为正；
- Action：play/stop animation、set visible/alpha；支持 linear/smoothstep、loop、restart、coalesce 和
  numeric smoothing；
- Forge legacy FML `ClientTickEvent.END` 每 tick 推进一步；每个 `GuiContainer` 独占 runtime；
- 关闭 GUI、玩家实例变化、reload 或 GUI 重新初始化会清理 animation、smoother、override 与 accumulator；
- Reactive transform 仅在渲染时合成，不写回 Sprite 基础 x/y/width/height，也不移动真实 Slot。

## GUI 能力

- 目标：`screen_class`、`container_class/menu_class`、`screen_title`；
- 类匹配：`exact` 或 `assignable`，并支持常见现代 `*Menu` 名到 1.7.10 Container 的版本表映射；
- GUI 原点、宽高、标题、标签的位置与颜色调整；
- Slot 索引、范围、原始坐标、类选择器，累积偏移和双色悬停高亮；
- `underlay`、`background`、`foreground` 三层贴图；
- `gui`、`screen_center`、`screen` 三种锚点；
- 整图、区域 UV、Alpha 混合和时间动画；
- 独立 `texts`、显式 `font_rules`，并保留 v1 标题/标签兼容字段；
- 玩家背包始终居中，药水效果列表固定到 GUI 右侧可见区域；
- 每一层批次统一保存和恢复 OpenGL 混合、颜色、Alpha Test、深度与绑定纹理状态。

## 安全与回退

配置、列表、路径、PNG、Expression、动画实例和动态纹理代际有明确预算。单个文件失败会记录来源并隔离；
顶层 reload 失败会保留上一份有效 immutable generation。动画缺帧支持 `use_default`、`skip`、`disable`。

Mixin 注入点按 1.7.10 Forge 10.13.4.1614 字节码实现，并配置为可选注入：其他 coremod 改写同一方法时，
局部渲染能力可能降级，真实兼容性仍需在具体客户端组合中验证。

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
