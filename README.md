# RedFoxExpand

[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-8-red.svg)](https://www.oracle.com/java/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Forge-blue.svg)](https://files.minecraftforge.net/)
[![Client Side](https://img.shields.io/badge/Side-Client--Only-purple.svg)](#)
[![Status](https://img.shields.io/badge/Status-Development-yellow.svg)](#)

RedFoxExpand 是由 **RedFox团队** 开发的 Minecraft **1.7.10 Forge** 客户端 Mod。它允许材质包作者通过
兼容的 v1 目录或严格的 Schema v2/v3 JSON 配置修改容器 GUI，而不必修改或重新编译 Mod。

- 当前版本：`0.2.0`（Git Tag：`v0.2.0-mc1.7.10`）
- Minecraft：`1.7.10`
- Forge：`10.13.4.1614`
- 运行端：仅客户端
- Java：8

## 功能状态

### 已实现

- 扫描文件夹、ZIP/JAR、Mod 来源和服务器资源包中的 `assets/Kyeitk/` v1 内容；
- 支持原生小写域 `assets/kyeitk/redfoxexpand/index.json` strict v2/v3 manifest；
- 同时生成 legacy、Kyeitk v1 和 native v2/v3 候选，单个坏配置不会关闭其他格式；
- 通过界面类、容器类或标题匹配原版及第三方 Mod 容器 GUI，类目标支持精确或继承匹配；
- 支持常见现代 `*Menu` 名称到 Minecraft 1.7.10 Container 类的版本映射；
- 修改 GUI 位置/尺寸、槽位位置/高亮、标题/标签颜色，并绘制三层贴图和前景文字；
- 支持整图、局部 UV、三种锚点、浮点坐标、缩放、RGBA Alpha 和时间动画；
- 支持显式 `font_rules`、独立 `texts` 以及 `id`、`priority`、`append/replace/disable` 合并；
- 玩家背包在有/无药水效果时保持相同居中布局，药水效果列表显示在 GUI 右侧；
- F3+T 时重建不可变配置快照，并按代释放动态纹理；
- 配置、路径、列表、PNG 和动态纹理均有资源预算与安全回退；
- 在没有 Kyeitk 配置时兼容旧 `assets/<namespace>/polytone/gui_modifiers/` 配置；
- Schema v3 在 v2 上加入稳定 Sprite ID、Java 8 Expression Engine 和每 tick Runtime Context；
- 支持玩家、屏幕、GUI、鼠标坐标及左右键状态，以及 health、burning、`screen.opened` 事件；
- 支持 `visible`、`alpha`、平移、缩放、旋转 Binding 和数值平滑；
- 支持 Property Animation、`linear` / `smoothstep`、Behavior 与安全 Action；
- 每个 `GuiContainer` 独占响应式运行时，关闭、玩家变化、resize/init 与 F3+T 会清理旧状态；
- native v2/v3 切换材质包时会清理由本 Mod 创建的旧纹理缓存，不影响原版或其他 Mod 共享纹理。

### 下一步

- HUD API 与 Widget/Component；
- Semantic Slot，以及 text/Slot 的响应式 target；
- width、height、color、Texture State 和自定义旋转枢轴；
- 自定义变量/事件、Timer、User Function 与 `every.mode=repeat`；
- 面向材质包作者的 Inspector；
- 更广泛的第三方 Mod 游戏内兼容验证。

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
**Minecraft Resource Pack UI Runtime**。当前优先级依次为完善 Expression Engine、建设 HUD API、实现
Semantic Slot，以及为材质包作者提供 Inspector；已完成和未实现边界以本文及功能文档为准。

## 安装

1. 安装 Minecraft 1.7.10 和 Forge `10.13.4.1614`；
2. 从 [GitHub Releases](https://github.com/kyeitk/RedFoxExpand/releases) 下载
   `RedFoxExpand-1.7.10-0.2.0.jar`；
3. 将 JAR 放入 Minecraft 实例的 `mods/` 目录；
4. 启动游戏，在“选项 → 资源包”中启用包含 Kyeitk 配置的材质包。

Mod 不要求服务端安装。建议在新增或修改配置后执行 F3+T。

## 制作第一个 Kyeitk 材质包

### 1. 建立目录

与 1.8.9 分支共用的 v1 结构：

```text
MyKyeitkPack/
├─ pack.mcmeta
└─ assets/
   └─ Kyeitk/
      ├─ config/
      │  └─ inventory.json
      └─ textures/
         └─ gui/
            └─ inventory.png
```

`Kyeitk` 是 v1 规范物理目录名，请保持此大小写；文件和子目录建议使用小写 ASCII。

Minecraft 1.7.10 的最小 `pack.mcmeta`：

```json
{
  "pack": {
    "pack_format": 1,
    "description": "My first RedFoxExpand GUI pack"
  }
}
```

1.7.10 分支还支持原生小写域 v2/v3：

```text
assets/kyeitk/
└─ redfoxexpand/
   ├─ index.json
   ├─ config/inventory.json
   └─ textures/gui/inventory.png
```

`assets/kyeitk/redfoxexpand/index.json`：

```json
{
  "api_version": 3,
  "configs": ["redfoxexpand/config/inventory.json"]
}
```

### 2. 放置 GUI 图片

v1 材质包将带 Alpha 通道的 PNG 放到：

```text
assets/Kyeitk/textures/gui/inventory.png
```

图片可以大于原版玩家背包的 `176×166`，显示尺寸由 JSON 的 `width` / `height` 决定。透明、半透明和
不透明像素均受支持；建议导出 8 位 RGBA PNG。

### 3. 写入配置

`assets/Kyeitk/config/inventory.json`：

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "class_match": "exact",
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "anchor": "screen_center",
      "x": -88,
      "y": -83,
      "width": 176,
      "height": 166,
      "layer": "background"
    }
  ]
}
```

启用材质包并打开玩家背包即可查看。修改 PNG 或 JSON 后按 F3+T，当前 GUI 会从基础尺寸和槽位坐标
重新应用配置，不会因重复重载累计偏移。

## Schema v2/v3 原生入口

v2 提供严格 Definition 协议，v3 再加入响应式运行时。manifest 与 config 的 `api_version` 必须一致；
v3 Sprite 必须有 Definition 内唯一 ID，才能被 Binding 或 Action 引用。

最小 Schema v3 config：

```json
{
  "api_version": 3,
  "definitions": [{
    "id": "example:reactive_inventory",
    "match": {"exact_menu_class":"net.minecraft.inventory.ContainerPlayer"},
    "sprites": [{
      "id": "character",
      "texture": {"type":"pack_resource","location":"textures/gui/character.png"},
      "anchor": "gui", "x": 180, "y": 0, "width": 120, "height": 120,
      "layer": "underlay"
    }],
    "bindings": [{
      "target":"character", "property":"alpha",
      "value":"clamp(player.health / player.max_health, 0, 1)",
      "smoothing_ms":120
    }],
    "animations": [{
      "id":"damage_shake", "duration_ms":240,
      "tracks":[{"property":"translate_x","keyframes":[
        {"time_ms":0,"value":0},{"time_ms":40,"value":-5},
        {"time_ms":80,"value":5},{"time_ms":240,"value":0}
      ]}]
    }],
    "behaviors": [{
      "on":{"event":"player.health.decreased","every":2,"mode":"coalesce"},
      "actions":[{"type":"play_animation","target":"character","animation":"damage_shake"}]
    }]
  }]
}
```

1.7.10 提供玩家、屏幕、GUI 与鼠标 Runtime Context；支持 `visible`、`alpha`、平移、缩放、旋转、
数值平滑、health/burning/screen 事件及安全 Action。原版没有 Recipe Book，也没有现代注册表化菜单身份；
应使用 screen/menu class、simple class 或 title matcher。完整变量、表达式、事件、属性、动画、行为、
安全预算与迁移说明见 [`docs/SCHEMA_V3.md`](docs/SCHEMA_V3.md)。

## 完整配置示例

下面的示例扩大玩家背包逻辑区域、绘制背景、移动部分槽位并添加文字：

```json
{
  "id": "mytheme:player_inventory",
  "operation": "append",
  "priority": 0,
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "class_match": "exact",
  "x_offset": 14,
  "y_offset": 12,
  "width_offset": 28,
  "height_offset": 24,
  "slot_modifiers": [
    {
      "slots": "0-8",
      "x_offset": 2,
      "y_offset": 0,
      "highlight_color": "#80FFFFFF",
      "color_2": "#20306080"
    }
  ],
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "resource_type": "resource_location",
      "anchor": "gui",
      "x": 0,
      "y": 0,
      "width": 204,
      "height": 190,
      "layer": "background"
    }
  ],
  "texts": [
    {
      "text": "My Kyeitk Pack",
      "x": 92,
      "y": 6,
      "color": "#FFFFFFFF",
      "shadow": true,
      "translate": false
    }
  ]
}
```

常用字段：

- `target_type`：`screen_class`、`container_class` / `menu_class` 或 `screen_title`；
- `class_match`：类目标使用 `exact`（默认）或显式 `assignable`；
- `anchor`：`gui`、`screen_center` 或 `screen`；
- `layer`：`underlay`、`background` 或 `foreground`；
- `texture_type`：`full`、`region` 或 `animation`；
- `resource_type`：`resource_location`、`gui_sprite` 或 `auto`；
- `id` / `priority` / `operation`：控制跨格式和跨资源包定义的稳定合并。

字段、默认值、合并顺序及错误回退见 [`docs/resourcepack-api.md`](docs/resourcepack-api.md)。

## 动画 GUI

动画目录示例：

```text
assets/Kyeitk/textures/gui/inventory.png
assets/Kyeitk/textures/gui/inventory/
├─ animation.json
├─ frame_0.png
└─ frame_1.png
```

在 GUI 配置中使用 `"texture_type": "animation"`，并让 `texture` 指向
`textures/gui/inventory`。当前已实现帧顺序、逐帧时长、循环、`always` / `never` 条件、默认图片和
缺帧策略；需要游戏状态、鼠标或事件驱动效果时应使用 Schema v3 Binding/Behavior。格式见
[`docs/GUI_RESOURCE_FORMAT.md`](docs/GUI_RESOURCE_FORMAT.md)。

## 第三方 Mod GUI

仅在目标 Mod 已加载时启用的 v1 配置应放在：

```text
assets/Kyeitk/compatibility/<modid>/config/inventory.json
assets/Kyeitk/compatibility/<modid>/textures/gui/inventory.png
```

配置可用 `screen_class` 匹配第三方界面类，或用 `container_class` 匹配容器类。优先使用实际的
Minecraft 1.7.10 类名；常见现代菜单别名会由平台映射表转换。需要让基类或接口规则覆盖子类时，
显式写 `"class_match": "assignable"`。第三方 Mod 的实机兼容性仍需按具体组合验证。

## 开发者构建

要求 64 位 JDK 8。ForgeGradle 1.2 不支持使用 JDK 17/21 构建。

```powershell
$env:JAVA_HOME = 'C:\Path\To\JDK8'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build
```

构建脚本使用 Forge `1.7.10-10.13.4.1614-1.7.10` 和 MCP `stable_12`，并将已失效的旧 Mojang S3
下载地址定向到官方 content-addressed URL。首次构建仍需网络。

构建会输出重混淆 Shadow JAR：

```text
build/libs/RedFoxExpand-1.7.10-0.2.0.jar
```

## 文档

- [功能状态与实现边界](docs/FEATURES.md)
- [材质包开发者 API](docs/resourcepack-api.md)
- [Schema v3 响应式 UI 协议](docs/SCHEMA_V3.md)
- [GUI 静态、动画与 Alpha 格式](docs/GUI_RESOURCE_FORMAT.md)
- [材质包目录与迁移规范](docs/RESOURCE_PACK_STRUCTURE.md)
- [任意位置自定义贴图](docs/custom-textures.md)
- [破限背包实现逻辑](docs/break-limit-inventory.md)
- [更新日志](CHANGELOG.md)

## 当前限制

- 仅支持 Minecraft 1.7.10 Forge 客户端容器 GUI，HUD 与 Widget 尚未实现；
- 1.7.10 没有现代注册表化 GUI 标识，`menu_type`、`resource_location`、`mod_namespace` matcher 不可用；
- v1 目录帧动画条件仍只有 `always` 和 `never`；动态效果请使用 Schema v3；
- Schema v3 尚不支持 width/height/color 绑定、Texture State、自定义旋转枢轴、text/Slot target、
  自定义变量/事件、Timer、User Function、循环/递归或 `every.mode=repeat`；
- 不支持按钮/widget、Semantic Slot、Inspector 或玩家 3D 模型独立偏移；
- Minecraft 1.7.10 原版没有 Recipe Book，因此不存在配方书跟随能力；
- 旧标题/标签字段保留调用序号兼容语义，新包应使用显式 `font_rules`；
- v1 大写目录依赖可枚举的文件夹/ZIP/JAR；自定义内存资源包应使用 native v2/v3；
- 可选 Mixin Hook 被其他 coremod 改写时可能降级相应图层、高亮或字体功能；
- 实际画面、F3+T、不同 GUI Scale、第三方 Mod 与 OpenGL 状态仍需客户端实机回归。

## 许可

当前仓库遵循 `CC BY-NC-SA 4.0` 协议；除非权利人另行授权，源代码默认保留所有权利。仓库不包含
第三方材质包或受限制美术素材。
