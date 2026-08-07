# RedFoxExpand 功能说明

本文记录 `0.1.0` 的实际功能、实现入口和验证边界。README 面向普通用户与材质包作者；本页面向维护者
和兼容开发者。

## 功能矩阵

| 功能 | 状态 | 说明 |
|---|---|---|
| 固定 Kyeitk 目录 | 已实现 | 扫描 `assets/Kyeitk/`，兼容小写 `kyeitk` 物理目录 |
| 文件夹/ZIP/Mod JAR | 已实现 | 按已启用资源包优先级建立最高优先级路径索引 |
| GUI 目标匹配 | 已实现 | 标题 → 容器 → 界面由低到高合并；类目标支持 `exact/assignable` |
| GUI 几何修改 | 已实现 | 位置、逻辑宽高、标题与标签偏移 |
| 槽位修改 | 已实现 | 索引/范围/坐标/类名筛选，类名支持 `exact/assignable`，并修改位置与高亮颜色 |
| 玩家背包药水布局 | 已实现 | 药水不改变 GUI 原点；列表位于 GUI 右侧并在窄屏钳制到可见区域 |
| 自定义贴图 | 已实现 | `full`、`region`、显式资源类型、三种锚点和三种渲染层 |
| RGBA / Alpha | 已实现 | 标准 Alpha 混合并恢复调用前 OpenGL 状态 |
| 动画贴图 | 已实现（基础） | 帧顺序、时长、循环、`always/never`、默认图、缺帧策略 |
| F3+T 热重载 | 已实现 | 重建不可变快照、释放旧纹理并刷新当前容器 GUI |
| 第三方 Mod 目录 | 已实现 | 仅在 `<modid>` 已加载时应用 compatibility 配置 |
| 旧 Polytone 配置 | 已实现（回退） | 仅在没有适用 Kyeitk 配置时加载 |
| 复杂动画条件 | Planned | 表达式、鼠标悬停、游戏状态条件尚未实现 |
| 动态坐标/尺寸 | Planned | 当前位置和尺寸在配置解析后固定 |
| 按钮/widget | Planned | 0.1.0 只处理容器 GUI、槽位、贴图和文字 |
| 玩家模型独立偏移 | Planned | 扩展 GUI 时需由材质设计适配模型窗口 |

## 加载与渲染流程

```text
资源加载 / F3+T
  -> 扫描 Kyeitk 物理目录
  -> 选择最高优先级 JSON/PNG
  -> 严格解析配置并缓存纹理/动画帧
  -> 原子替换 GUI modifier 快照
  -> 刷新当前 GuiContainer

每帧绘制
  -> underlay
  -> 原版背景
  -> background
  -> 槽位与物品
  -> 原版标题/标签
  -> foreground
  -> 自定义文字
```

逐帧绘制不执行文件扫描、JSON 解析或 PNG 解码。

## 核心模块

| 模块 | 职责 |
|---|---|
| `client/resource/KyeitkResourceScanner` | 物理目录、ZIP、资源优先级和安全索引 |
| `client/resource/ResourcePathResolver` | 路径校验、静态/动画资源解析 |
| `client/resource/KyeitkTextureRegistry` | PNG 解码、运行时纹理注册与释放 |
| `client/config/GuiConfigLoader` | 严格 JSON 解析与文件级原子失败 |
| `client/gui/GuiModifierManager` | 快照、目标匹配、合并和界面刷新 |
| `client/render/GuiTextureRenderer` | 分层四边形绘制 |
| `client/render/AnimatedGuiRenderer` | 只访问已缓存帧的时间选帧 |
| `client/render/AlphaBlendState` | Alpha 混合设置和 OpenGL 状态恢复 |
| `mixin/MixinGuiContainer` | GUI 几何和渲染阶段接入 |
| `mixin/MixinContainer` / `MixinSlot` | 槽位基础坐标捕获与幂等重应用 |

## 错误隔离

以下错误会记录来源并跳过整份配置，不会让游戏循环继续持有半有效状态：

- JSON 语法、字段类型、类匹配/资源类型枚举值或颜色非法；
- 绝对路径、路径穿越或不允许的资源位置；
- PNG 缺失、损坏或无法解码；
- 动画帧、时长、条件或缺帧策略非法。

若新快照没有命中当前 GUI，则恢复原版尺寸、槽位和渲染行为。

## 验证范围

发布前已检测：扫描路径、ZIP 路径、资源优先级、几何、UV、动画选帧、Alpha 状态模型、
玩家背包稳定原点及药水列表右侧定位验证，基础测试功能良好无异常。

以下需您自行手动验证，并完成材质包本体适配：

- 不同 GUI 缩放和窗口尺寸；
- 保持 GUI 打开时执行 F3+T；
- 有/无药水效果的玩家背包；
- 与目标第三方 Mod 同时加载；
- 不同显卡驱动下的实际 OpenGL 状态恢复。

文档最后同步日期：2026-08-08。
