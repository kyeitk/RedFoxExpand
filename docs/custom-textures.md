# 任意位置自定义贴图

`custom_textures` 是 RedFoxExpand 面向 Minecraft 1.8.9 材质包开发者提供的显式贴图方法。
它与兼容 Polytone 的 `sprites` 并存；新包使用固定 Kyeitk 目录，旧目录仅在没有 Kyeitk 配置时
作为回退读取。

## 1. 最小结构

配置文件放置于：

```text
assets/Kyeitk/config/<name>.json
```

完整图片放置于材质包的普通纹理目录，例如：

```text
assets/Kyeitk/textures/gui/inventory.png
```

对应配置：

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

一个配置可以包含多个 `custom_textures`，按数组顺序绘制。

## 2. 字段参考

### 通用字段

| 字段 | 必需 | 默认值 | 说明 |
|---|---:|---|---|
| `texture_type` | 否 | `full` | `full` 整图、`region` 局部取图或 `animation` 动画目录 |
| `texture` | 是 | — | Kyeitk 相对路径；外部原版资源可写 `namespace:textures/...` |
| `resource_type` | 否 | `resource_location` | `resource_location` 原样解析，`gui_sprite` 映射 GUI sprite ID，`auto` 启用旧启发式 |
| `anchor` | 否 | `gui` | 坐标原点：`gui`、`screen_center` 或 `screen` |
| `x` / `y` | 否 | `0` | 相对坐标原点的显示位置，支持小数和负数 |
| `width` / `height` | 否 | `16` | 游戏界面中的最终显示尺寸，支持小数 |
| `layer` | 否 | `background` | `underlay`、`background` 或 `foreground` |

### 坐标原点

- `gui`：修改后容器 GUI 的左上角。适合绑定槽位或面板装饰。
- `screen_center`：当前游戏界面的正中心。适合左右立绘、超限外框和居中装饰。
- `screen`：游戏界面左上角 `(0,0)`。适合固定屏幕装饰。

位置单位是 Minecraft GUI 逻辑像素，会随 GUI 缩放设置一起缩放，不是显示器物理像素。

`custom_textures` 默认不会根据路径外观猜测资源类型。例如 `example:gui/icon` 会保持该
ResourceLocation；只有 `"resource_type": "gui_sprite"` 才会把 `example:inventory` 映射为
`example:textures/gui/sprites/inventory.png`。`animation` 不接受 `gui_sprite`，动画目录应使用默认的
`resource_location`；`auto` 仅用于明确兼容旧启发式的配置。

### 渲染层

```text
underlay
  -> 原版容器背景
  -> background
  -> 槽位与物品
  -> 原版文字
  -> foreground
```

- `underlay`：人物立绘、超限边框、需要被原版背包遮挡的图片。
- `background`：原版底图上方、物品下方的装饰。
- `foreground`：物品和文字上方的提示或遮罩。

## 3. 整图模式

`texture_type: "full"` 总是采样整张 PNG。原图尺寸和显示尺寸完全解耦：

```json
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
```

示例 `RW.png` 的物理尺寸是 `600×872`，配置为 `150×218` 等于按 25% 比例显示。

## 4. 局部取图模式

`texture_type: "region"` 使用原图像素坐标截取区域，再缩放到指定显示尺寸：

```json
{
  "texture_type": "region",
  "texture": "textures/gui/inventory.png",
  "anchor": "gui",
  "x": 180,
  "y": 10,
  "width": 72,
  "height": 81,
  "image_x": 17,
  "image_y": 67,
  "image_width": 288,
  "image_height": 324,
  "texture_width": 600,
  "texture_height": 872,
  "layer": "background"
}
```

字段含义：

| 字段 | 说明 |
|---|---|
| `image_x` / `image_y` | 原图中截取区域的左上角像素 |
| `image_width` / `image_height` | 原图中截取区域的宽高 |
| `texture_width` / `texture_height` | 整张 PNG 的实际物理尺寸 |
| `width` / `height` | 截取结果在游戏内的最终显示尺寸 |

`region` 模式必须提供正确的 `texture_width` 和 `texture_height`，否则该配置会被拒绝并写入日志。

## 5. 动画模式

`texture_type: "animation"` 的 `texture` 指向 Kyeitk 动画目录，例如
`textures/gui/inventory`；目录内必须有 `animation.json`。动画帧、时长、循环、播放条件、默认图和
缺帧策略见 [`GUI_RESOURCE_FORMAT.md`](GUI_RESOURCE_FORMAT.md)。动画只支持完整帧取样，所有资源在
加载/F3+T 阶段缓存。

## 6. 任意位置调整

以屏幕中心为原点时：

```text
x < 0：中心左侧
x = 0：从中心开始
x > 0：中心右侧

y < 0：中心上方
y = 0：从中心开始
y > 0：中心下方
```

例如一张宽 `150` 的图片要完全居中：

```text
x = -150 / 2 = -75
```

要放在原版玩家背包左侧，可从 `x=-245,y=-109` 开始，然后结合实际素材透明边距逐步微调。

## 7. 错误处理与限制

- 缺少 `texture`、尺寸非正数、未知 `texture_type/resource_type`、未知 `anchor/layer`，或动画使用
  `gui_sprite`，会拒绝该 modifier 并记录日志。
- 绘制时会临时解除 GUI scissor/depth/Alpha Test 限制，因此可以显示在原版容器边界之外并保留
  低 Alpha 像素；结束时恢复调用前的 RGBA、Alpha 函数、纹理绑定、混合因子和相关开关，避免污染
  后续原版或其他 Mod GUI。
- 超出实际游戏窗口的部分仍会被 OpenGL 视口裁切。
- 图片尺寸越大，占用的显存和资源重载时间越高；建议按实际需求控制分辨率。
- `foreground` 可能遮挡物品和提示文字，人物立绘通常使用 `underlay`。

上述游戏内视觉功能仍需您主动对材质包进行适配操作。某些数值可能需要反复调整，大部分调整工作已由项目自动适配(例如锚点换算、UV 换算、 非法区域配置)
