# RedFoxExpand 1.8.9 功能说明

本文记录 `0.2.1` 的公开能力、运行流程、兼容边界与后续方向。

## v1 与静态 GUI

- 读取 `assets/Kyeitk/config/**/*.json` 历史配置；
- 在没有 Kyeitk 配置时保留旧 Polytone GUI modifier 回退路径；
- 支持 GUI 位置与尺寸、Slot 位移和渐变高亮；
- 支持静态贴图、区域 UV、旧目录帧动画、文字和三层渲染；
- 支持文件夹、ZIP 资源包和可枚举 Mod JAR 中的资源；
- F3+T 后重新扫描资源并刷新已打开的容器界面。

## Schema v2

- 原生入口为 `assets/kyeitk/redfoxexpand/index.json`；
- 支持同一资源包来源内的 config 引用；
- 支持严格字段、类型、路径、ID、PNG 与资源预算校验；
- 支持 `append`、`replace`、`disable` 和 Definition priority；
- 支持 class/title matcher 与 `all/any/not` 组合；
- 支持 Geometry、SlotModifier、Sprite、Text 与 TextRule；
- 支持静态、区域和内联帧动画纹理。

## Schema v3

- 在 v2 上增加稳定 Sprite ID；
- 提供 Java 8 Expression Engine 与每 tick Runtime Context；
- 支持玩家、屏幕、GUI、鼠标和按键状态；
- 支持健康变化、燃烧边沿与 `screen.opened` 事件；
- 支持 Binding、Behavior、Action 与 Property Animation；
- 响应式属性包括 `visible`、`alpha`、`translate_x/y`、`scale_x/y`、`rotation_z`；
- 支持 numeric smoothing、`linear` / `smoothstep`、loop、restart 与 coalesce；
- v2 与 v3 保持独立，v2 Sprite 不会自动成为响应式 target。

## Schema v3.1

- manifest/config 使用严格数值 `api_version: 3.1`；
- 使用统一 `elements` 声明 Sprite 与 Group；
- 支持 Parent/Child 场景树、单父关系、环检查与最大深度限制；
- 子 Element 使用父级局部坐标，并继承平移、缩放、旋转、显隐和透明度；
- 根 Element 支持 GUI/Screen 九点 Anchor，Element 支持自定义 Pivot；
- 绘制顺序稳定使用 `layer → z → scene order`；
- 支持 Definition-local `constants` 与按声明顺序求值的 `values`；
- Binding 可读取 `self.*` / `parent.*` 基础布局几何；
- Property Animation 支持 `replace`、`add`、`multiply` 合成；
- Group 可作为 Binding、Animation 与 Action target，其结果由子树继承；
- v3.0 JSON 保持原语义，不会被自动升级到 v3.1。

## 1.8.9 平台能力

- 使用 Forge `11.15.1.2318`、Java 8 与 ForgeGradle 2.1；
- 每个 `GuiContainer` 拥有独立运行时；
- Forge `ClientTickEvent.END` 负责推进状态；
- 关闭 GUI、玩家实例变化、重新布局与 generation 更换会清理旧瞬时状态；
- 1.8.9 OpenGL 渲染桥支持 root-to-leaf 场景矩阵，并恢复全部相关绘制状态；
- 资源切换时仅移除 RedFoxExpand 创建且属于上一 generation 的原生纹理缓存；
- `InventoryMenu` 可作为玩家背包的兼容别名映射至 `ContainerPlayer`；
- 1.8.9 没有 Recipe Book，因此相关联动不适用于此分支。

## 安全与回退

- 表达式不允许脚本、反射、命令、网络、文件访问或任意 Java 调用；
- 未知字段、错误类型、非法路径和非法引用会在资源重载阶段被拒绝；
- 单个 config 或 Definition 的错误按来源隔离；
- 运行期求值错误回退到安全的基础属性；
- 新 generation 完成加载前继续保留上一可用 generation；
- 没有有效配置时保持原版 GUI 行为。

## 尚未实现

- HUD API 与 Widget；
- Semantic Slot，以及 Text/Slot 的响应式 target；
- Layout Container、Component、Clip/Scroll；
- Inspector、离线 Validator 与可视化编辑器；
- Texture State 与 width/height/color Binding；
- 自定义变量、事件、Timer、User Function、递归和 `every.mode=repeat`；
- 鼠标点击事件、音频 Action 与更丰富的安全动作；
- 现代 `menu_type`、`resource_location`、`mod_namespace` matcher 在 1.8.9 平台不可用。
