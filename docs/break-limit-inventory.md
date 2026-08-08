# 扩展容器与 Slot 布局

RedFoxExpand 通过修改 `GuiContainer` 的 `xSize/ySize/guiLeft/guiTop` 扩展容器，不裁切到原版 256×256 GUI
贴图尺寸。原版背景绘制仍获得其居中所需的基础尺寸，再由 1.7.10 平台矩阵适配移动到配置原点；真实
`width/height` 不会被伪造。

## Slot 规则

```json
{
  "slot_modifiers": [
    {
      "slots": [0, "1-4"],
      "target_x": 124,
      "target_y": 35,
      "target_class_name": "net.minecraft.inventory.SlotCrafting",
      "target_class_match": "exact",
      "x_offset": 2,
      "y_offset": -1,
      "highlight_color": "#80FFFFFF",
      "color_2": "#4000FFFF"
    }
  ]
}
```

选择器之间是 AND 关系；未填写的选择器不限制。`slots` 接受整数、字符串整数和正向/反向范围，范围最多
4096 项。`target_x/target_y` 匹配 RedFoxExpand 修改前的基础位置。`target_class_match` 默认为 `exact`，
`assignable` 显式允许子类或接口实现。

多个匹配规则的偏移累加。刷新时只撤销 RedFoxExpand 上一次记录的 delta，再重新计算；不会把 Slot 恢复到
构造时硬编码位置，也不会主动撤销其他 Mod 的修改。

## 高亮

`highlight_color`（或别名 `color`）和 `color_2` 是 ARGB。只写一个颜色时两端相同。高亮使用容器当前
`theSlot` 和实际 Slot 坐标绘制，不靠鼠标反推伪槽位。

## 药水效果

1.7.10 原版会在 `InventoryEffectRenderer.initGui` 根据药水效果把玩家背包左移。本项目在该方法返回后恢复
修改尺寸对应的居中原点，并把药水效果列表 X 放到 GUI 右侧、夹紧在屏幕内。因此药水效果出现或消失不再
改变 Slot、背景和自定义材质位置。

该行为已通过 1.7.10 Forge 字节码和纯几何测试确认；不同 GUI Scale、窗口宽度及第三方药水列表改写仍属于
实机待验证项。
