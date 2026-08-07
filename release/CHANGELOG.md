# RedFoxExpand 0.1.0

发布日期：2026-08-08

Git Tag：`v0.1.0`

## 主要功能

- 通过 `assets/Kyeitk/` 和 JSON 修改 Minecraft 1.8.9 Forge 容器 GUI；
- 支持 GUI/槽位/文字、整图/局部贴图、三种锚点和三种渲染层；
- 支持 `exact/assignable` 类匹配、标题/容器/界面规则合并和显式 `resource_type`；
- 支持 RGBA / Alpha 半透明和基础时间序列动画；
- 玩家背包在有/无药水时保持同一原点，药水列表位于 GUI 右侧；
- 支持 F3+T 重载、第三方 Mod compatibility 目录和旧 Polytone 配置回退；
- 配置或纹理错误安全跳过，没有有效配置时回退原版 GUI。

## 修复

- 修复扩展 GUI 导致的原版背景纹理环绕；
- 修复玩家背包下一 tick 整体左移；
- 修复 F3+T 后 GUI/槽位偏移累计；
- 修复低 Alpha 像素裁切、OpenGL 状态污染和 LWJGL 缓冲区崩溃。
- 修复完整类名退化为简单类名导致的跨 Mod 误命中。

## 验证

- Temurin JDK 8；
- `gradlew clean build` 成功；
- Shadow JAR 和重混淆成功。

完整更新日志见仓库根目录 `CHANGELOG.md`。
