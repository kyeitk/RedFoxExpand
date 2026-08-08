# 扩展容器、Slot 与配方书跟随

Minecraft 26.2 的 `imageWidth/imageHeight` 是 final。RedFoxExpand 不修改这两个字段，而是维护独立逻辑
geometry，并只调整真实 `leftPos/topPos`、Slot 坐标和自定义内容。

每轮 reconcile：

1. 从当前容器原点减去 RedFoxExpand 上一次施加的 delta；
2. 从每个当前 Slot 坐标减去自身上一次 delta；
3. 使用外部最新布局构造新的 GUI 上下文；
4. 重新匹配、合并并施加当前 generation 的 geometry 与 Slot 规则。

这样可以避免 F3+T、窗口 resize、配方书或其他 Mod 修改后累计漂移。

## 配方书跟随

配方书展开/收起会让原版重新计算容器 `leftPos`。`gui` 锚点每帧读取最终实时原点，因此扩展背景、前景
贴图、文本和 Slot 使用同一个基准同步移动。`screen_center` 与 `screen` 保持固定屏幕语义，不跟随容器。

## Slot 规则

Slot selector 可组合索引/范围、基础坐标和完整或简单类名。多个命中规则的 offset 相加；首条包含颜色的
规则负责悬停渐变。简单类名必须显式选择，完整类名不会自动退化匹配。
