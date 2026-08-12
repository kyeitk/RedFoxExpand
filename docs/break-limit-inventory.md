# 破限背包实现逻辑

本文记录 RedFoxExpand 1.8.9 对“超出原版背包贴图边界”的实现说明，供开发者和资源包作者使用。

## 1. 功能边界

这里的“破限”指突破原版 `GuiContainer` 默认 `176×166` 的逻辑和绘制区域。它不是解除 OpenGL
最大纹理尺寸限制，也不会阻止窗口视口裁切。

实现必须同时完成两件事：

1. 通过 `width_offset`、`height_offset` 扩大 `xSize`、`ySize`，让新增区域进入 GUI 布局和鼠标边界。
2. 通过 `sprites` 独立绘制大背景。精灵是单独的纹理四边形，不受原版背包背景的 `176×166` 取样范围约束。

只替换 `minecraft:textures/gui/container/inventory.png` 不足以扩大 GUI 的逻辑边界。

## 2. 加载逻辑闭环

```text
资源包重载
  -> KyeitkResourceScanner 扫描 assets/Kyeitk/ 并建立最高优先级路径索引
  -> GuiConfigLoader + GuiDefinition 解析目标、尺寸、槽位和贴图
  -> ResourcePathResolver 在重载阶段解码并缓存引用的 PNG/动画帧
  -> GuiModifierManager.resolve 按 screen_class/container_class/screen_title 匹配
  -> GuiContainer 初始化完成后缓存并应用 ResolvedGuiModifier
  -> 复位基础尺寸和槽位坐标，再应用本次配置
  -> 原版背景 -> 自定义 background 精灵 -> 槽位/物品 -> foreground 精灵/文字
```

相关入口：

- `client/resource/KyeitkResourceScanner.java`：固定目录、文件夹/ZIP/Mod JAR 和优先级枚举。
- `client/resource/ResourcePathResolver.java`：安全相对路径、静态/动画资源解析。
- `client/config/GuiConfigLoader.java`：严格 JSON 解析。
- `client/gui/GuiModifierManager.java`：快照替换、目标匹配和当前界面刷新。
- `mixin/MixinGuiContainer.java`：尺寸、原点和渲染顺序。
- `client/gui/SpriteOverlay.java`：已解析的坐标、UV 与层配置。
- `client/render/GuiTextureRenderer.java`、`AlphaBlendState.java`：任意尺寸四边形与完整状态恢复。
- `mixin/MixinContainer.java`、`mixin/MixinSlot.java`：槽位首次加入及幂等复位。

## 3. GUI 几何公式

RedFoxExpand 在保存原始尺寸和原点后应用：

```text
newWidth   = baseWidth  + width_offset
newHeight  = baseHeight + height_offset
newGuiLeft = baseGuiLeft + x_offset - width_offset / 2
newGuiTop  = baseGuiTop  + y_offset - height_offset / 2
```

例如，将原版玩家背包从 `176×166` 扩大为 `204×190`：

```text
width_offset  = 204 - 176 = 28
height_offset = 190 - 166 = 24
```

若希望原版内容绝对位置不变，只向右、向下增加空间：

```text
x_offset = width_offset / 2  = 14
y_offset = height_offset / 2 = 12
```

此时 `newGuiLeft == baseGuiLeft`、`newGuiTop == baseGuiTop`，无需整体移动原版槽位。

`newWidth/newHeight` 只表示扩展后的逻辑布局尺寸，不能直接作为原版背景纹理的绘制宽高。调用
`drawGuiContainerBackgroundLayer` 时，RedFoxExpand 会临时恢复 `baseWidth/baseHeight`，并用
`temporaryScreenWidth = 2 * newGuiLeft + baseWidth`（高度同理）让会自行居中的原版界面仍落在
`newGuiLeft/newGuiTop`。调用结束后再恢复扩展尺寸。这样既保留破限布局，又避免 1.8.9 固定 256 UV
在 `xSize > 256` 时发生纹理环绕、右侧重复背包或瞬时位置跳变。

玩家背包还有一条 1.8.9 专属链路：`GuiInventory` 的父类 `InventoryEffectRenderer` 会在每次
`updateScreen` 中调用 `updateActivePotionEffects`，无论当前是否有药水效果，都会再次按当前
`xSize` 计算 `guiLeft`，有可渲染药水时还会使用另一条约右移 60 像素的公式。若不拦截，它会把背景、
槽位、玩家模型、文字和 GUI 锚点贴图一起移出配置位置。

RedFoxExpand 在初始化期间先把原点恢复为 `(screenWidth - xSize) / 2`，因此捕获的基础原点与药水状态
无关；应用尺寸和偏移后保存最终 `guiLeft`，以后每次 `updateActivePotionEffects` 返回时直接恢复该值。
有/无药水效果因此使用同一套配置位置。原版药水列表本身不再依赖 `guiLeft - 124`，而改为：

```text
potionX = max(0, min(guiLeft + xSize + 4, screenWidth - 140 - 4))
```

结果是列表优先紧邻 GUI 右侧；窗口较窄时保持 4 像素右边距并允许与 GUI 局部重叠，而不是移动 GUI。

若 `x_offset = 0`、`y_offset = 0`，新增空间将围绕屏幕中心对称展开，GUI 左上角会分别移动
`-14`、`-12`。

## 4. 精灵坐标与纹理尺寸

精灵坐标和绘制尺寸使用浮点数，因此支持参考包中的 `307.5` 这类半像素逻辑尺寸。精灵支持两种坐标原点：

- `"anchor": "gui"`：默认值，`x/y` 相对于修改后 GUI 左上角。
- `"anchor": "screen_center"`：`x/y` 相对于实际屏幕中心，对应现代 Polytone/旧 Slotify
  常见的中心坐标配置方式。

旧 `sprites` 在 `resource_type` 省略或为 `auto` 时，会把现代 GUI 图集 ID（例如
`minecraft:inventory`）自动解析为 1.8.9 可加载的
`minecraft:textures/gui/sprites/inventory.png`，且未写 `anchor` 时默认使用 `screen_center`。
`custom_textures` 默认使用 `resource_location`，不会凭路径外观改写；需要图集映射时应显式写
`"resource_type": "gui_sprite"`。显式原始纹理路径默认使用 `gui`。

`screen_x/screen_y` 与 `x/y` 是别名。建议新配置统一使用 `x/y` 配合显式 `anchor`。

没有提供 `u/v`、源区域或纹理尺寸时，渲染器默认采样完整纹理，`width/height` 只决定屏幕上的逻辑
绘制尺寸。因此 `1230×720` 高清 PNG 可以直接缩放绘制为 `307.5×180`，不会只截取左上角。

需要从纹理图集中取一个区域时，提供 `u/v` 和 `tex_width/tex_height`；`source_width/source_height`
决定取样区域，`width/height` 决定屏幕绘制尺寸，两者可以不同。`texture_width/texture_height` 是
`tex_width/tex_height` 的别名。也可用 `full_texture` 显式选择是否采样完整纹理。

`layer` 只能是：

- `underlay`：先绘制额外图片，再绘制原版 GUI 背景，适合人物立绘和超限外框。
- `background`：在原版背景后、槽位和物品前绘制，适合完整背包底图。
- `foreground`：在槽位、物品和原版文字后绘制，适合装饰或提示，不适合不透明底图。

没有显式 `layer` 时，负 `z` 自动映射为 `underlay`，其余值映射为 `background`。1.8.9 实现通过
明确的渲染阶段表达前后关系，不直接依赖大幅负 Z 值。绘制期间会暂时关闭 scissor 和深度检测，完成后
恢复调用前的 RGBA、Alpha 函数/阈值、纹理绑定、混合因子及
texture/blend/alpha/depth/scissor 开关，确保 GUI 逻辑矩形外和低 Alpha 像素不会被其他界面状态
裁掉，同时不影响后续原版或其他 Mod 的 GUI 绘制。

## 5. 玩家背包模板

资源包结构：

```text
assets/Kyeitk/config/inventory_expanded.json
assets/Kyeitk/textures/gui/inventory_expanded.png
```

以 `204×190`、只向右下扩展为例：

```json
{
  "target_type": "container_class",
  "target": "net.minecraft.inventory.ContainerPlayer",
  "width_offset": 28,
  "height_offset": 24,
  "x_offset": 14,
  "y_offset": 12,
  "sprites": [
    {
      "texture": "textures/gui/inventory_expanded.png",
      "anchor": "gui",
      "x": 0,
      "y": 0,
      "u": 0,
      "v": 0,
      "width": 204,
      "height": 190,
      "tex_width": 204,
      "tex_height": 190,
      "layer": "background"
    }
  ]
}
```

保持前述“只向右下扩展”的逻辑矩形，同时改用屏幕中心坐标时，精灵部分应写为：

```json
{
  "texture": "textures/gui/inventory_expanded.png",
  "anchor": "screen_center",
  "x": -88,
  "y": -83,
  "width": 204,
  "height": 190,
  "tex_width": 204,
  "tex_height": 190,
  "layer": "background"
}
```

这里的 `-88/-83` 是原版 `176×166` 背包左上角相对屏幕中心的位置，因此它与
`x_offset=14`、`y_offset=12` 后保持不变的 `guiLeft/guiTop` 对齐。

若要模仿旧 Slotify 示例，让 `204×190` 背景围绕屏幕中心对称展开，则应同时使用：

```json
{
  "x_offset": 0,
  "y_offset": 0,
  "width_offset": 28,
  "height_offset": 24,
  "sprites": [
    {
      "anchor": "screen_center",
      "x": -102,
      "y": -95,
      "width": 204,
      "height": 190
    }
  ]
}
```

这种方式会让 GUI 原点以及所有原版槽位整体向左 `14`、向上 `12`。若希望背景对称扩展但槽位
保留原绝对位置，需要再给槽位、标题和标签增加 `+14/+12` 偏移；当前玩家 3D 模型还不能通过
JSON 独立补偿。不要在一次配置中同时对同一坐标重复补偿。

## 6. 合并与重载

- 所有命中的规则都会参与合并，顺序为 `screen_title` -> `container_class` -> `screen_class`，即界面类
  最具体并最后应用。
- 同一匹配级别的多个配置保持资源路径顺序合并。
- 同一路径出现在多个资源包中时，由 Minecraft 资源包栈选择最高优先级版本。
- 重载时先在局部构建并检查新 immutable generation，成功后原子替换并刷新当前容器；顶层失败保留旧 generation。
- GUI 和槽位始终从捕获的基础值复位后重应用，避免每次 F3+T 后累计漂移。

## 7. 已知限制

- 当前没有表达式驱动的动态尺寸或坐标。
- 当前不执行现代 Polytone 的任意 `z` 深度表达式；负 `z` 用于自动选择 `underlay`，精确层级请使用
  `underlay/background/foreground`。
- 当前没有独立的玩家 3D 模型偏移。如果贴图改变了模型窗口位置，需要后续增加专用配置。
- 超出实际游戏窗口的部分仍会被视口裁切。
- 窗口不足以同时容纳 GUI 和 140 像素药水列表时，列表会贴住右边界并可能与 GUI 重叠。
- 大于 `256×256` 的纹理可以按实际尺寸取样，但仍受显卡最大纹理尺寸限制。
  
