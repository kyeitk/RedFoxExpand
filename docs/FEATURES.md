# 功能说明

本文只描述 `RedFoxExpand-1.7.10` 当前已经实现的公开能力与边界。

## 资源发现与合并

- 兼容 `assets/Kyeitk/` v1 文件夹、ZIP/JAR、Mod 来源与服务器资源包；
- 支持原生小写域 `assets/kyeitk/redfoxexpand/index.json` v2 manifest；
- 兼容 `assets/<namespace>/polytone/gui_modifiers/**/*.json`；
- legacy、v1、v2 同时生成候选，不使用“发现新格式就全局关闭旧格式”的开关；
- 按资源包优先级选择同路径内容，再由稳定 `id` 和 `append/replace/disable` 合并；
- F3+T 时重建不可变配置快照并释放上一代动态纹理。

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

配置、列表、路径、PNG 和动态纹理代际有明确预算。单个文件失败会记录来源并隔离；顶层 reload
失败会清空旧快照，避免继续使用失效纹理。动画缺帧支持 `use_default`、`skip`、`disable`。

Mixin 注入点按 1.7.10 Forge 10.13.4.1614 字节码实现，并配置为可选注入：其他 coremod 改写同一方法时，
局部渲染能力可能降级。真实兼容性仍需在具体的 Minecraft、Forge、OptiFine 和第三方 coremod 组合中验证。
