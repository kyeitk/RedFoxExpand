# GUI 渲染与资源格式

本文说明 Minecraft 26.2 的渲染、锚点和动画语义；完整字段表见 [resourcepack-api.md](resourcepack-api.md)。

## 坐标与锚点

`geometry` 使用整数 `x_offset/y_offset/width_offset/height_offset`。最终原点增量：

```text
dx = x_offset - width_offset / 2
dy = y_offset - height_offset / 2
```

| `anchor` | 原点 |
|---|---|
| `gui` | 实时容器 `leftPos/topPos`；跟随配方书、resize 和原版重新布局 |
| `screen_center` | 屏幕中心；不跟随容器偏移 |
| `screen` | 屏幕左上角 |

## 渲染层

| `layer` | 时机 |
|---|---|
| `underlay` | 全屏 dim/blur 后、原版容器背景前 |
| `background` | 原版容器背景后、Slot 与标签前 |
| `foreground` | 容器内容后、tooltip 前 |

## 纹理与动画

`full_texture=true` 绘制完整纹理；`false` 使用 `u/v/source_width/source_height` 与
`texture_width/texture_height` 区域。`gui_sprite` 只允许完整纹理。

动画在 Sprite 内声明帧、逐帧时长、循环、`always/never` 条件、默认纹理和
`use_default/skip/disable` 缺帧策略。完整纹理会缩放到目标尺寸，不自动保持源图比例。

## 文本与渲染后端

`texts` 是显式 overlay；`text_rules` 只选择语义 `title` 或 `player_inventory`。所有绘制通过
`GuiGraphicsExtractor` 与 `RenderPipelines`，源码不直接操作 OpenGL、Vulkan 或 LWJGL 状态。
