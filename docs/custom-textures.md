# `custom_textures` 参考

`custom_textures` 是推荐贴图接口，一个 definition 可按数组顺序放置多张贴图。

## 字段

| 字段 | 默认值 | 说明 |
|---|---|---|
| `texture` | 无 | 必需；v1 相对 `assets/Kyeitk/`，v2 相对 `kyeitk` domain |
| `texture_type` | `full` | `full`、`region` 或 `animation` |
| `resource_type` | `resource_location` | `resource_location`、`gui_sprite`、`auto`；动画不能用 `gui_sprite` |
| `x/y` | `0` | 目标左上角；`screen_x/screen_y` 是别名 |
| `width/height` | `16` | 目标尺寸，必须大于 0 |
| `anchor` | `gui` | `gui`、`screen_center`、`screen` |
| `layer` | 由 `z` 推导 | `underlay`、`background`、`foreground` |
| `z` | `0` | 同层稳定排序；负值未显式写层时选 underlay |

区域模式还使用 `image_x/image_y/image_width/image_height` 和必需的
`texture_width/texture_height`。底层 `sprites` 对应字段为 `u/v/source_width/source_height/tex_width/tex_height`。

## 示例

```json
{
  "custom_textures": [
    {
      "texture_type": "region",
      "texture": "textures/gui/atlas.png",
      "image_x": 32,
      "image_y": 16,
      "image_width": 64,
      "image_height": 32,
      "texture_width": 256,
      "texture_height": 256,
      "x": 8,
      "y": 8,
      "width": 128,
      "height": 64,
      "layer": "background"
    }
  ]
}
```

## 路径与回退

`resource_location` 保留明确的 namespace 语义；不要依赖扩展名猜测。v1 无 namespace 相对 Kyeitk 根目录
解析；v2 资源必须能由当前 `IResourceManager` 读取。非法路径、缺图、解码失败或超预算会使所属配置文件
被隔离并记录日志。

动画缺帧行为：`use_default` 使用 `default_texture`，`skip` 忽略缺帧，`disable` 停用该动画。没有可用帧时
不会生成动态纹理。
