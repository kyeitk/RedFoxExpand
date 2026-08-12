# RedFoxExpand

[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-25-red.svg)](https://www.oracle.com/java/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Client Side](https://img.shields.io/badge/Side-Client--Only-purple.svg)](#)
[![Status](https://img.shields.io/badge/Status-Development-yellow.svg)](#)

RedFoxExpand 是由 **RedFox团队** 开发的 Minecraft **26.2 Fabric** 客户端 Mod。它允许材质包作者通过
原生小写资源域、Schema v2/v3/v3.1 JSON 配置和现代渲染管线修改容器 GUI，而不必修改或重新编译 Mod。

- 当前版本：`0.2.1`（Git Tag：`v0.2.1-mc26.2`）
- Minecraft：`26.2`
- Fabric Loader：`0.19.3`
- Fabric API：`0.156.0+26.2`
- 运行端：仅客户端
- Java：25

## 项目实例演示

![RedFoxExpand 响应式角色界面演示](docs/assets/redfoxexpand-demo.gif)

## 功能状态

### 已实现

- 通过 Fabric Resource Loader 读取 `assets/kyeitk/redfoxexpand/index.json` 原生 manifest；
- 严格校验 Schema v2、路径、JSON、PNG、动画与数量预算，按来源隔离错误；
- 支持 `append`、`replace`、`disable`、definition priority 和稳定资源包优先级；
- 支持 screen/menu 类、标题 key/text、menu type、GUI resource、Mod namespace 与 `all/any/not` 匹配；
- 支持 GUI 逻辑位置/尺寸、Slot 索引/范围/基础坐标/类选择、实际位移和渐变高亮；
- 支持整图、区域 UV、三种显式资源类型、三种锚点和三种渲染层；
- `gui` 锚点、破限纹理和 Slot 会跟随配方书展开/收起及窗口重新布局同步移动；
- 支持 PNG Alpha、显式文本、语义标题/玩家背包标签规则及时间动画；
- Schema v3 与 v2 并存：不需要响应式能力的旧 v2 材质包无需迁移；
- Schema v3 提供稳定 Sprite ID、严格表达式引擎和每 tick Runtime Context；
- 支持玩家、屏幕、GUI、鼠标与按键状态，以及 health/burning/screen 生命周期事件；
- 支持 `visible`、`alpha`、平移、缩放、旋转绑定和数值平滑；
- 支持属性动画、`linear` / `smoothstep` 插值、行为条件与安全动作；
- Schema v3.1 新增统一 `elements`、Group/Parent 场景树、局部坐标和父级变换/显隐/透明度继承；
- 支持 GUI/Screen 九点 Anchor、元素 Pivot，以及 `layer → z → scene order` 稳定绘制顺序；
- 支持 Definition-local Constants、按声明顺序求值的 Derived Values，以及 Binding 中的
  `self.*` / `parent.*` 基础布局几何；
- 属性动画支持显式 `replace` / `add` / `multiply` 合成，Group 动画可由整个子树继承；
- Reactive Core 独立保持 Java 8 源兼容，平台层仍使用 Minecraft 26.2 所需的 Java 25；
- F3+T 采用不可变 generation 原子切换，成功后统一释放上一代引用；
- 全部 GUI 绘制使用 `GuiGraphicsExtractor` 与 `RenderPipelines`，不直接调用 OpenGL/LWJGL。

### 下一步

- 更清晰的结构化诊断与离线 Validator；
- 面向材质包作者的 Inspector 与属性管线视图；
- Layout Container、Clip/Scroll 与 Component 复用；
- Semantic Slot，以及 Text/Slot 的响应式 target；
- HUD API 与 Widget；
- Texture State、width/height/color Binding 与更多安全事件/动作；
- 可读取并导出正式 Schema JSON 的可视化编辑器。

完整状态和边界见 [`docs/FEATURES.md`](docs/FEATURES.md)。

## 项目方向

RedFoxExpand 的核心设计哲学是：

> **让资源包负责表现，让 Mod 负责能力。**

资源包使用 Texture、Layout、Animation、Color、Visibility、Style 与 Theme 描述界面；Mod 负责 State、
Rendering、Events、Input、Compatibility、Security 与 Runtime。目标不是简单支持更多 PNG，而是让材质包作者
只使用 PNG、JSON、Animation、Expression 和 Components，也能制作动态背包、动态 HUD、RPG/PvP/科幻界面、
角色面板、响应式 GUI 与第三方 Mod UI。

```text
Resource Pack API ─┐
                   ├─> Expression Engine -> UI Definition -> HUD / GUI / Components
Runtime Context  ──┘                                      -> Layout Engine
                                                          -> Animation Engine
                                                          -> Render Pipeline -> Minecraft
```

项目将从“材质包辅助 Mod”继续发展为 **Minecraft Resource Pack UI Framework**，并最终形成可复用的
**Minecraft Resource Pack UI Runtime**。Schema v3.1 已让复杂界面从平铺 Sprite 进入 Scene Graph；下一阶段
优先改善诊断、Inspector、布局与复用能力，再在同一核心上扩展 Semantic Slot、HUD 和 Widget。

## 安装

1. 安装 Minecraft 26.2、Fabric Loader `0.19.3` 和 Fabric API `0.156.0+26.2`；
2. 从 [GitHub Releases](https://github.com/kyeitk/RedFoxExpand/releases) 下载
   `RedFoxExpand-26.2-0.2.1.jar`；
3. 将 JAR 放入 Minecraft 实例的 `mods/` 目录；
4. 启动游戏并启用符合 Schema v2、v3 或 v3.1 的 RedFoxExpand 材质包。

Mod 不要求服务端安装。建议在新增或修改配置后执行 F3+T。

## 制作第一个 Kyeitk 材质包

### 1. 建立目录

```text
MyKyeitkPack/
├─ pack.mcmeta
└─ assets/
   └─ kyeitk/
      └─ redfoxexpand/
         ├─ index.json
         ├─ config/
         │  └─ inventory.json
         └─ textures/
            └─ gui/
               └─ inventory.png
```

Minecraft 26.2 的 `pack.mcmeta` 必须同时声明格式与范围：

```json
{
  "pack": {
    "pack_format": 88,
    "min_format": 88,
    "max_format": 88,
    "description": "My first RedFoxExpand GUI pack"
  }
}
```

### 2. 放置 GUI 图片

将带 Alpha 通道的 PNG 放到：

```text
assets/kyeitk/redfoxexpand/textures/gui/inventory.png
```

26.2 只接受小写原生 namespace。图片路径不会进行 `.png`、`textures/` 或 namespace 猜测。

### 3. 写入配置

`assets/kyeitk/redfoxexpand/index.json`：

```json
{
  "api_version": 2,
  "configs": ["redfoxexpand/config/inventory.json"]
}
```

`assets/kyeitk/redfoxexpand/config/inventory.json`：

```json
{
  "api_version": 2,
  "definitions": [{
    "id": "example:inventory",
    "operation": "replace",
    "priority": 100,
    "match": {
      "exact_menu_class": "net.minecraft.world.inventory.InventoryMenu"
    },
    "sprites": [{
      "texture": {
        "type": "pack_resource",
        "location": "textures/gui/inventory.png"
      },
      "anchor": "gui",
      "x": 0,
      "y": 0,
      "width": 176,
      "height": 166,
      "layer": "background"
    }]
  }]
}
```

启用材质包并打开玩家背包即可查看。修改资源后按 F3+T，成功 reload 后新 generation 一次性生效。

## Schema v3 响应式界面

需要根据玩家或界面状态动态改变 Sprite 时，将 manifest 与 config 的 `api_version` 都设为 `3`，并为每个
Sprite 增加 Definition 内唯一的 `id`。下面的配置会在玩家着火时显示火焰，并让图标平滑跟随鼠标横坐标：

```json
{
  "api_version": 3,
  "definitions": [{
    "id": "example:reactive_inventory",
    "match": {"exact_menu_class":"net.minecraft.world.inventory.InventoryMenu"},
    "sprites": [{
      "id": "fire",
      "texture": {"type":"pack_resource","location":"textures/gui/fire.png"},
      "anchor": "gui", "x": 80, "y": 20, "width": 32, "height": 32
    }],
    "bindings": [
      {"target":"fire","property":"visible","value":"player.is_burning"},
      {"target":"fire","property":"translate_x","value":"mouse.gui_x - 80","smoothing_ms":120}
    ]
  }]
}
```

v3 表达式在 reload 时预编译并严格校验，不支持脚本、反射、文件、网络或任意函数调用。完整 Runtime
Context、表达式语法、属性、事件、动画、行为、预算及迁移说明见
[`docs/SCHEMA_V3.md`](docs/SCHEMA_V3.md)。

## Schema v3.1 场景图

Schema v3.1 面向由多个图层组成的角色、面板和动态界面。将 manifest 与 config 的 `api_version` 都设为
数值 `3.1`，使用统一 `elements` 数组，并通过 Group 组织可共同移动、缩放、旋转、显隐或改变透明度的子树：

```json
{
  "api_version": 3.1,
  "definitions": [{
    "id": "example:character",
    "match": {"menu_type":"minecraft:player"},
    "constants": {"turn_limit": 4},
    "values": {
      "head_turn": "clamp((mouse.x - screen.width / 2) * 0.01, -turn_limit, turn_limit)"
    },
    "elements": [
      {
        "id":"character_root", "type":"group",
        "anchor":"screen_bottom_center", "x":-80, "y":-160,
        "width":160, "height":160,
        "children":["body", "head_group"]
      },
      {
        "id":"body", "type":"sprite",
        "texture":{"type":"pack_resource","location":"textures/gui/body.png"},
        "width":160, "height":160, "layer":"foreground"
      },
      {
        "id":"head_group", "type":"group", "width":160, "height":160,
        "children":["head"]
      },
      {
        "id":"head", "type":"sprite",
        "texture":{"type":"pack_resource","location":"textures/gui/head.png"},
        "width":160, "height":160, "layer":"foreground"
      }
    ],
    "bindings": [{
      "target":"head_group", "property":"rotation_z",
      "value":"head_turn", "smoothing_ms":180
    }]
  }]
}
```

根 Element 可使用 GUI/Screen 九点 Anchor，子 Element 使用父级局部坐标；Pivot 决定旋转和缩放中心。
Constants 消除重复数字，Derived Values 复用表达式，`self.*` / `parent.*` 为鼠标跟随等效果提供稳定基础
几何。完整字段、默认值、场景继承、动画合成和 v3.0 迁移步骤见
[`docs/SCHEMA_V3_1.md`](docs/SCHEMA_V3_1.md)。

## 完整配置示例

下面的示例扩展玩家背包、移动槽位并添加前景文字：

```json
{
  "api_version": 2,
  "definitions": [{
    "id": "example:expanded_inventory",
    "operation": "replace",
    "priority": 100,
    "match": {"exact_menu_class": "net.minecraft.world.inventory.InventoryMenu"},
    "geometry": {
      "width_offset": 132,
      "height_offset": 14,
      "x_offset": 66,
      "y_offset": 7
    },
    "slot_modifiers": [{
      "slots": ["9-35"],
      "x_offset": 2,
      "highlight_color": "#80FFFFFF",
      "highlight_color_2": "#20306080"
    }],
    "sprites": [{
      "texture": {"type": "pack_resource", "location": "textures/gui/inventory.png"},
      "anchor": "gui",
      "x": -122,
      "y": -5,
      "width": 307.5,
      "height": 180,
      "layer": "underlay"
    }],
    "texts": [{
      "text": "RedFoxExpand",
      "x": 92,
      "y": 6,
      "color": "#FFFFFFFF",
      "shadow": true,
      "anchor": "gui",
      "layer": "foreground"
    }]
  }]
}
```

常用字段：

- `match`：严格的单 operator matcher，也可用 `all`、`any`、`not` 组合；
- `geometry`：`x_offset`、`y_offset`、`width_offset`、`height_offset`；
- `texture.type`：`resource_location`、`gui_sprite` 或 `pack_resource`；
- `anchor`：`gui`、`screen_center` 或 `screen`；
- `layer`：`underlay`、`background` 或 `foreground`；
- `id` / `priority` / `operation`：控制跨资源包定义合并。

字段、默认值、合并顺序及错误回退见 [`docs/resourcepack-api.md`](docs/resourcepack-api.md)。

## 动画 GUI

26.2 将动画直接写入 Sprite：

```json
{
  "texture": {"type":"pack_resource","location":"textures/gui/default.png"},
  "animation": {
    "frame_duration_ms": 100,
    "loop": true,
    "condition": "always",
    "missing_frame": "use_default",
    "frames": [
      {"texture":{"type":"pack_resource","location":"textures/gui/frame_0.png"}},
      {"texture":{"type":"pack_resource","location":"textures/gui/frame_1.png"},"duration_ms":160}
    ]
  },
  "width": 176,
  "height": 166,
  "anchor": "gui",
  "layer": "background"
}
```

当前支持逐帧时长、循环、`always` / `never`、默认图片和 `use_default` / `skip` / `disable` 缺帧策略。
格式见 [`docs/GUI_RESOURCE_FORMAT.md`](docs/GUI_RESOURCE_FORMAT.md)。

## 第三方 Mod GUI

第三方 GUI 不再使用专用 compatibility 目录，可直接通过数据驱动 matcher 匹配：

```json
{
  "all": [
    {"assignable_screen_class": "example.mod.client.ExampleContainerScreen"},
    {"mod_namespace": "examplemod"}
  ]
}
```

完整类名不会回退为简单类名；确需简单类名时必须使用显式 `*_simple_class` operator。第三方 Screen 若
完全重写标签提取或跳过原版背景流程，相应语义文本或 underlay 能力可能降级。

## 开发者构建

要求 JDK 25。

```powershell
$env:JAVA_HOME = 'C:\Path\To\JDK25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon
```

构建使用 Minecraft `26.2`、Fabric Loader `0.19.3`、Fabric API `0.156.0+26.2`、Loom `1.17.19` 和
Gradle `9.5.1`。发布 JAR：

```text
build/libs/RedFoxExpand-26.2-0.2.1.jar
```

## 文档

- [功能状态与实现边界](docs/FEATURES.md)
- [材质包开发者 API](docs/resourcepack-api.md)
- [Schema v3 响应式 UI 协议](docs/SCHEMA_V3.md)
- [Schema v3.1 场景图与创作协议](docs/SCHEMA_V3_1.md)
- [GUI 静态、动画与 Alpha 格式](docs/GUI_RESOURCE_FORMAT.md)
- [材质包目录与迁移规范](docs/RESOURCE_PACK_STRUCTURE.md)
- [任意位置自定义贴图](docs/custom-textures.md)
- [破限背包与配方书跟随](docs/break-limit-inventory.md)
- [更新日志](CHANGELOG.md)

## 当前限制

- 当前仅处理 Minecraft 26.2 Fabric 客户端的容器 GUI，HUD 与 Widget 尚未实现；
- 不扫描旧 `assets/Kyeitk/`，旧版配置必须迁移至原生小写 Schema v2；
- `imageWidth/imageHeight` 保持原版 final 值，扩展尺寸使用独立逻辑 geometry；
- Sprite 纹理帧动画条件仍只有 `always` 和 `never`；动态条件请使用 Schema v3；
- Schema v3.1 Scene Graph 当前只作用于 Sprite/Group；Text 和 Slot 尚不能进入场景树或作为响应式 target；
- 不支持 HUD、Widget、Semantic Slot、Layout Container、Component、Clip/Scroll 或 Inspector；
- 尚无 Texture State、width/height/color Binding、自定义变量/事件、Timer、User Function、循环/递归或
  `every.mode=repeat`；
- `self.*` / `parent.*` 提供静态基础布局几何，不读取本帧 Binding/Animation 后的最终矩阵；
- Schema v3.1 当前仅适用于 26.2；1.8.9/1.7.10 仍使用 v2/v3.0；
- OpenGL/Vulkan、第三方 Screen 与不同 GUI 布局的表现可能因环境组合而异。

## 许可

当前仓库遵循 `CC BY-NC-SA 4.0` 协议；除非权利人另行授权，源代码默认保留所有权利。仓库不包含
第三方材质包或受限制美术素材。
