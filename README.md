# RedFoxExpand

[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-25-red.svg)](https://www.oracle.com/java/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Client Side](https://img.shields.io/badge/Side-Client--Only-purple.svg)](#)
[![Status](https://img.shields.io/badge/Status-Development-yellow.svg)](#)

RedFoxExpand 是由 **RedFox团队** 开发的 Minecraft **26.2 Fabric** 客户端 Mod。它允许材质包作者通过
原生小写资源域、Schema v2 JSON 配置和现代渲染管线修改容器 GUI，而不必修改或重新编译 Mod。

- 当前版本：`0.1.0`（Git Tag：`v0.1.0-mc26.2`）
- Minecraft：`26.2`
- Fabric Loader：`0.19.3`
- Fabric API：`0.156.0+26.2`
- 运行端：仅客户端
- Java：25

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
- F3+T 采用不可变 generation 原子切换，成功后统一释放上一代引用；
- 全部 GUI 绘制使用 `GuiGraphicsExtractor` 与 `RenderPipelines`，不直接调用 OpenGL/LWJGL。

### Planned

- 表达式、鼠标悬停等复杂动画播放条件；
- 动态坐标和动态尺寸；
- 按钮/widget 修改；
- 玩家 3D 模型独立偏移；
- 更完整的 OpenGL/Vulkan、第三方 Screen 和生产实例兼容矩阵。

完整状态和边界见 [`docs/FEATURES.md`](docs/FEATURES.md)。

## 安装

1. 安装 Minecraft 26.2、Fabric Loader `0.19.3` 和 Fabric API `0.156.0+26.2`；
2. 从 [GitHub Releases](https://github.com/kyeitk/RedFoxExpand/releases) 下载
   `RedFoxExpand-26.2-0.1.0.jar`；
3. 将 JAR 放入 Minecraft 实例的 `mods/` 目录；
4. 启动游戏并启用符合 Schema v2 的 RedFoxExpand 材质包。

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
build/libs/RedFoxExpand-26.2-0.1.0.jar
```

## 文档

- [功能状态与实现边界](docs/FEATURES.md)
- [材质包开发者 API](docs/resourcepack-api.md)
- [GUI 静态、动画与 Alpha 格式](docs/GUI_RESOURCE_FORMAT.md)
- [材质包目录与迁移规范](docs/RESOURCE_PACK_STRUCTURE.md)
- [任意位置自定义贴图](docs/custom-textures.md)
- [破限背包与配方书跟随](docs/break-limit-inventory.md)
- [更新日志](CHANGELOG.md)

## 当前限制

- 仅处理 Minecraft 26.2 Fabric 客户端的容器 GUI；
- 不扫描旧 `assets/Kyeitk/`，旧版配置必须迁移至原生小写 Schema v2；
- `imageWidth/imageHeight` 保持原版 final 值，扩展尺寸使用独立逻辑 geometry；
- 动画条件仅有 `always` 和 `never`；
- 不支持按钮/widget 或玩家 3D 模型独立偏移；
- OpenGL/Vulkan 已确认启动与资源 reload，但完整 GUI/交互矩阵仍需逐项验证。

## 许可

当前仓库遵循 `CC BY-NC-SA 4.0` 协议；除非权利人另行授权，源代码默认保留所有权利。仓库不包含
第三方材质包、测试资源或受限制美术素材。
