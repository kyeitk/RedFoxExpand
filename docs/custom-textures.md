# `sprites` 与自定义贴图参考

Minecraft 26.2 使用 `sprites` 作为推荐贴图接口。

## 纹理类型

```json
{"type":"resource_location","location":"minecraft:textures/gui/container/inventory.png"}
{"type":"gui_sprite","location":"minecraft:container/inventory/effect_background"}
{"type":"pack_resource","location":"textures/gui/panel.png"}
```

- `resource_location`：原样使用带 namespace 的纹理 ID；
- `gui_sprite`：原样使用 GUI atlas sprite ID，只允许完整纹理；
- `pack_resource`：相对路径映射到 `kyeitk:redfoxexpand/<path>`。

## Sprite 字段

| 字段 | 默认 | 说明 |
|---|---:|---|
| `texture` | 必填 | 显式 Texture object |
| `x/y/z/u/v` | `0` | `z` 只用于同层稳定排序 |
| `width/height` | `16` | 目标逻辑尺寸 |
| `full_texture` | `true` | `false` 使用区域 UV |
| `color` | `#FFFFFFFF` | ARGB 乘色和 Alpha |
| `layer` | `background` | 三种渲染层之一 |
| `anchor` | `gui` | 三种锚点之一 |

需要与 Slot 保持相对位置的破限纹理应使用 `gui`；独立屏幕装饰可用 `screen_center` 或 `screen`。
非法路径、缺图、解码失败、尺寸或像素超预算会隔离所属 definition 并记录来源。
