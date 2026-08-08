# 功能说明

本文只描述 `RedFoxExpand-26.2` 当前已经实现的公开能力与边界。

## 资源发现与合并

- 使用 Fabric Resource Loader v1 和原生小写 `assets/kyeitk/redfoxexpand/index.json`；
- manifest 只读取同一个 `sourcePackId` 中列出的配置；
- 候选按 pack priority、definition priority、来源路径和数组序号稳定排序；
- 支持 `append`、`replace`、`disable`，成功 reload 后原子安装新 generation；
- 严格校验 Schema、路径、数量、PNG、动画和 reload 预算。

## GUI 能力

- screen/menu 完整类名与显式简单类名精确/继承匹配；
- title key/text、menu type、resource location、Mod namespace、`all/any/not`；
- 独立逻辑 geometry，不修改 final `imageWidth/imageHeight`；
- Slot 索引、范围、基础坐标和类选择，实际坐标位移与双色高亮；
- `underlay`、`background`、`foreground` 三层绘制；
- `gui`、`screen_center`、`screen` 三种锚点；
- 整图、区域 UV、Alpha、ARGB 乘色和显式时间动画；
- 独立 `texts` 和语义 `title` / `player_inventory` 文本规则；
- `gui` 锚点、geometry 与 Slot 以实时 `leftPos/topPos` 为共同基准，跟随配方书和 resize。

## 安全与回退

配置文件错误按文件隔离，纹理或动画错误按 definition 隔离。成功 prepare 后一次性切换 generation；若顶层
reload 无法形成候选，则保留上一代。热路径不进行文件 I/O、ZIP 扫描、反射、JSON 或图片解码。

渲染仅调用 Minecraft 26.2 的提取器与渲染管线。第三方 Screen 完全跳过原版标签或背景流程时，相应语义
文本或 underlay 可能降级；完整兼容性需在具体 Mod、渲染后端和窗口布局组合中验证。
