# GUI 渲染与资源格式

本文说明 Minecraft 1.7.10 渲染语义；完整字段表见 [resourcepack-api.md](resourcepack-api.md)。

## 坐标模型

`x_offset/y_offset` 移动 GUI 原点，`width_offset/height_offset` 扩展或缩小 GUI 尺寸。最终原点仍按修改后的
尺寸居中。原版背景绘制时保留真实屏幕宽高，仅用矩阵把原版背景移动到配置原点，避免影响依赖屏幕尺寸的
其他 Mod。

贴图锚点：

| `anchor` | 原点 |
|---|---|
| `gui` | 修改后的 GUI 左上角 |
| `screen_center` | 真实屏幕中心 |
| `screen` | 真实屏幕左上角 |

## 渲染层

| `layer` | 时机 |
|---|---|
| `underlay` | 原版 `drawScreen` 之前 |
| `background` | 原版容器背景之后、Slot/物品之前 |
| `foreground` | 原版前景文字之后 |

未写 `layer` 时，负 `z` 为 `underlay`，其余为 `background`。每个批次按 `z` 和定义顺序稳定绘制。

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "width_offset": 120,
  "custom_textures": [
    {
      "texture_type": "full",
      "texture": "textures/gui/inventory.png",
      "anchor": "screen_center",
      "x": -210,
      "y": -88,
      "width": 307.5,
      "height": 180,
      "layer": "underlay"
    }
  ]
}
```

## 整图、区域与动画

`texture_type: full` 把整个 PNG 映射到目标矩形。`region` 使用 `image_x/image_y/image_width/image_height`
和 `texture_width/texture_height` 截取 UV。`animation` 的 `texture` 指向包含 `animation.json` 的目录。

```json
{
  "frame_duration_ms": 100,
  "frames": [
    "frame_0.png",
    { "texture": "frame_1.png", "duration_ms": 160 }
  ],
  "loop": true,
  "condition": "always",
  "default_texture": "frame_0.png",
  "missing_frame": "use_default"
}
```

`condition` 当前只支持 `always` 和 `never`。时长范围为 1..600000 ms，帧数为 1..4096。

## Slot 与文字

Slot 规则匹配修改前的基础坐标。reload 或再次应用时只撤销 RedFoxExpand 自己记录的偏移，不覆盖其他 Mod
随后施加的变化。高亮绘制在实际 Slot 位置，使用 16×16 双色渐变。

新配置使用 `font_rules` 的文字、翻译键、坐标或调用序号显式选择原版文字；旧 v1 的
`title_x_offset/title_y_offset/title_color` 和 label 字段继续按兼容调用序号工作。`texts` 是附加文字，坐标
相对 GUI 原点。

## 药水效果与 OpenGL

`InventoryEffectRenderer.initGui` 完成后恢复居中的 `guiLeft`，药水列表绘制 X 改到 GUI 右侧并夹紧到屏幕
可见区域。Slot、背景和材质坐标因此不随药水效果出现而移动。

自定义贴图使用普通 Alpha 混合，并在批次结束恢复颜色、纹理、Blend、Alpha Test、Depth、Blend Func 与
当前绑定纹理。源码和 JVM 几何测试不能代替显卡驱动上的真实状态验证。
