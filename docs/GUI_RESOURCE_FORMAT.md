# GUI 资源格式

本页主要说明 legacy v1 `assets/Kyeitk/` 架构中的静态 GUI、目录动画和 Alpha 图片要求。0.2.0 native
v2/v3 改用小写 manifest、`{type, location}` texture object 与可选 inline `animation`；其完整字段见
[`resourcepack-api.md`](resourcepack-api.md)，Reactive Property Animation 见 [`SCHEMA_V3.md`](SCHEMA_V3.md)。

纹理帧 animation 与 Schema v3 Property Animation 是两套正交功能：前者选择 PNG，后者只合成
visible/alpha/translate/scale/rotation，禁止用纹理帧修改基础布局。

## 1. 静态 GUI

文件：

```text
assets/Kyeitk/config/inventory.json
assets/Kyeitk/textures/gui/inventory.png
```

配置：

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "anchor": "screen_center",
      "x": -245,
      "y": -109,
      "width": 150,
      "height": 218,
      "layer": "underlay"
    }
  ]
}
```

静态 `texture_type`：

| 值 | 用途 | 必要字段 |
|---|---|---|
| `full` | 取完整 PNG 并缩放 | `texture` |
| `region` | 从 PNG 取一块区域再缩放 | `texture`、`texture_width`、`texture_height` |

`texture` 不带 namespace 时相对于 `assets/Kyeitk/`。`Kyeitk:textures/...` 也接受，但推荐简短的
相对写法。`minecraft:textures/...` 等其他 namespace 仍按普通 Minecraft 资源读取。

`custom_textures.resource_type` 默认为 `resource_location`，不会按扩展名或目录外观改写合法
ResourceLocation；`gui_sprite` 会将 `namespace:id` 映射到
`namespace:textures/gui/sprites/id.png`，`auto` 显式保留旧启发式。旧 `sprites` 默认仍为 `auto`。
动画目录不支持 `gui_sprite`。

## 2. 动画 GUI

目录：

```text
assets/Kyeitk/textures/gui/inventory.png
assets/Kyeitk/textures/gui/inventory/
├─ frame_0.png
├─ frame_1.png
├─ frame_2.png
└─ animation.json
```

GUI 配置使用动画目录，不写 `animation.json` 文件名：

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "custom_textures": [
    {
      "texture_type": "animation",
      "texture": "textures/gui/inventory",
      "anchor": "gui",
      "x": 0,
      "y": 0,
      "width": 176,
      "height": 166,
      "layer": "background"
    }
  ]
}
```

`animation.json`：

```json
{
  "frames": [
    "frame_0.png",
    { "texture": "frame_1.png", "duration_ms": 150 },
    "frame_2.png"
  ],
  "frame_duration_ms": 100,
  "loop": true,
  "condition": "always",
  "default_texture": "textures/gui/inventory.png",
  "missing_frame": "use_default"
}
```

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `frames` | 数组 | 无 | 必需，1–4096 项；字符串或含 `texture/duration_ms` 的对象 |
| `frame_duration_ms` | 整数 | `100` | 字符串帧和未单独设置时的持续时间，范围 1–600000 ms |
| `loop` | 布尔 | `true` | `false` 时序列播完后显示默认静态图片 |
| `condition` | 字符串 | `always` | 当前支持 `always` 和 `never`；`never` 始终显示默认图 |
| `default_texture` | 字符串 | 动画目录同名 `.png` | 静态回退图片 |
| `missing_frame` | 字符串 | `use_default` | `use_default`、`skip` 或 `disable` |

帧只写文件名时相对于动画目录；以 `textures/`、`compatibility/` 或 namespace 开头时按完整资源路径
处理。全部动画 JSON 和 PNG 在加载/F3+T 时解析并缓存，逐帧只根据时间选择已注册纹理。

缺帧行为：

- `use_default`：用默认图占据缺失帧原本的持续时间；
- `skip`：从缓存序列移除缺失帧；
- `disable`：任一帧缺失就把本动画降级为默认静态图；
- 默认图本身缺失、PNG 损坏或动画 JSON 非法：包含它的 GUI 配置文件整体跳过。

## 3. RGBA 与 Alpha 制作要求

- 导出带 8 位 Alpha 通道的 RGBA PNG；不要只用黑白遮罩替代 Alpha。
- 局部半透明像素的 Alpha 应处于 `1..254`，完全透明为 `0`，完全不透明为 `255`。
- 透明边缘仍应保留与主体接近的 RGB，以减少缩放采样产生的黑边/白边。
- 同层图片按配置数组顺序绘制，后面的半透明像素会用标准
  `SRC_ALPHA / ONE_MINUS_SRC_ALPHA` 覆盖前面的颜色。
- `underlay` 适合外框和立绘，`background` 位于原版底图与物品之间，`foreground` 可能遮挡物品和
  文字。

渲染器绘制前会保存当前颜色 RGBA、Alpha Test 函数/阈值、blend 开关与四个混合因子、纹理绑定、
texture/depth/scissor 开关；绘制时关闭 Alpha Test 并启用标准混合，随后在 `finally` 中逐项恢复。
这避免半透明像素被 1.8.9 的 Alpha Test 阈值裁掉，也避免后续物品、文字或其他 Mod GUI 被染色。

## 4. 兼容 Mod 资源

```text
assets/Kyeitk/compatibility/examplemod/config/inventory.json
assets/Kyeitk/compatibility/examplemod/textures/gui/inventory.png
```

兼容配置中的图片写：

```json
"texture": "compatibility/examplemod/textures/gui/inventory.png"
```

只有 `examplemod` 已加载时，上述 `config/` 才被扫描。普通 `config/` 不做 Mod 存在性检查。

## 5. 错误与回退

以下错误会记录 `Invalid Kyeitk GUI config` 并跳过整份配置文件：

- JSON 语法、根节点或字段类型错误；
- 缺少 `target`、`texture` 或必要的动画字段；
- 未知目标、类匹配模式、资源类型、锚点、层、纹理类型、条件或缺帧策略；
- 非正显示/取样尺寸、非法颜色或帧时长；
- 绝对路径、路径穿越、缺失/损坏 PNG；
- `region` 未提供有效纹理尺寸。

若没有其他有效配置命中当前 GUI，界面保持原版。单文件错误按 config/Definition 隔离；reload 顶层失败
不会发布半成品，而是保留上一个 immutable generation。成功 reload 后已删除或禁用的定义不会残留。
