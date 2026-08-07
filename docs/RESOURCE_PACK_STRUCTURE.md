# RedFoxExpand 材质包目录规范

本文定义 RedFoxExpand 0.1.0 的固定资源目录、资源包优先级和旧格式迁移方式。GUI 字段见
[`GUI_RESOURCE_FORMAT.md`](GUI_RESOURCE_FORMAT.md) 和 [`resourcepack-api.md`](resourcepack-api.md)。

## 1. 标准目录

```text
<resource-pack>/
├─ pack.mcmeta
└─ assets/
   └─ Kyeitk/
      ├─ config/
      │  ├─ inventory.json
      │  └─ <subdirectory>/*.json
      ├─ textures/
      │  └─ gui/
      │     ├─ inventory.png
      │     └─ inventory/
      │        ├─ frame_0.png
      │        ├─ frame_1.png
      │        └─ animation.json
      └─ compatibility/
         └─ <modid>/
            ├─ config/
            │  └─ *.json
            └─ textures/
               └─ gui/
                  └─ *.png
```

- `Kyeitk` 是物理目录名，规范大小写如上；`kyeitk` 仅作为已有包的兼容别名接受。
- `config/` 可递归建立子目录，全部 `.json` 都作为 GUI 定义读取。
- `textures/gui/` 存放本 Mod GUI 图片；配置写相对于 `assets/Kyeitk/` 的路径，例如
  `textures/gui/inventory.png`。
- `compatibility/<modid>/config/` 只在 Forge 报告 `<modid>` 已加载时参与配置快照。
- 兼容图片应放在同一 Mod 目录，例如
  `compatibility/examplemod/textures/gui/inventory.png`，不要放回其他 namespace。

## 2. 为什么大写目录仍能工作

已通过 Minecraft 1.8.9 字节码确认：`ResourceLocation` 会把 domain 转为小写，原版文件夹和 ZIP
资源包还会忽略非小写 namespace。因此 `assets/Kyeitk/` 不能依赖原生
`IResourceManager#getResource`。

RedFoxExpand 在每次资源加载时按下列流程处理：

1. 枚举默认/Mod 资源包、已启用资源包和服务端资源包的后备文件；
2. 直接扫描文件夹或 ZIP 中的 `assets/Kyeitk/`；
3. 按 Minecraft 的低到高资源包顺序建立相对路径索引；
4. 解析最高优先级 JSON，并把引用的 PNG 解码为带 Alpha 的运行时纹理；
5. 一次性替换配置和纹理快照，刷新当前打开的容器 GUI；
6. 删除上一个快照拥有的运行时纹理。

没有文件扫描或 JSON 解析发生在逐帧渲染中。

## 3. 资源包优先级与合并

- 多个已启用资源包包含相同 Kyeitk 相对路径时，资源包列表中优先级最高的文件胜出。
- 不同配置路径按完整 Kyeitk 相对路径字典序解析；命中同一目标时沿用 GUI API 的累加/覆盖规则。
- 同一个配置引用的 PNG 也从全局 Kyeitk 路径索引选择最高优先级版本，因此只替换 PNG 后 F3+T
  即可刷新。
- 一旦发现至少一个适用于当前环境的 `config/` 或 `compatibility/<modid>/config/` 配置，本次快照
  不再加载旧 Polytone 目录，防止迁移前后相同规则重复叠加。

## 4. 旧目录迁移

| 旧位置 | 新位置 | 配置中的新写法 |
|---|---|---|
| `assets/<ns>/polytone/gui_modifiers/inventory.json` | `assets/Kyeitk/config/inventory.json` | 不适用 |
| `assets/<ns>/textures/gui/overlays/rw.png` | `assets/Kyeitk/textures/gui/inventory.png` | `textures/gui/inventory.png` |
| `assets/<ns>/textures/gui/container/*.png` | `assets/Kyeitk/textures/gui/*.png` | `textures/gui/<name>.png` |
| 散落的第三方 Mod 配置 | `assets/Kyeitk/compatibility/<modid>/config/*.json` | 不适用 |
| 散落的第三方 Mod 图片 | `assets/Kyeitk/compatibility/<modid>/textures/gui/*.png` | `compatibility/<modid>/textures/gui/<name>.png` |

迁移步骤：

1. 复制而不是同时启用新旧配置，先建立完整 `assets/Kyeitk/` 树。
2. 把 `sprites` / `custom_textures` 中属于本包的完整 namespace 路径改为 Kyeitk 相对路径。
3. 保留 `minecraft:...` 或其他 Mod 的外部原版资源位置；它们仍由原生资源管理器处理。
4. 用严格 JSON 校验所有文件，启用包并观察日志。
5. 验证背包、F3+T、禁用包回退后，再删除旧目录。

旧 `assets/<namespace>/polytone/gui_modifiers/**/*.json` 仍能在没有 Kyeitk 配置时加载，但它只用于
过渡，不支持 Kyeitk 动画目录。

## 5. 路径安全与命名

- 配置不得包含 `C:\...`、`/home/...`、UNC 路径、前导 `/`、`.` 或 `..` 段。
- Kyeitk 路径统一使用 `/`；运行时会拒绝路径穿越和绝对路径。
- PNG、JSON 和子目录建议只使用小写 ASCII、数字、下划线与短横线。
- 静态/动画 GUI 图片必须是 `.png`；缺失或无法解码时，该配置文件被安全跳过。
- 无法枚举后备文件的自定义 `IResourcePack` 无法提供大写 Kyeitk 资源，会记录日志而不会崩溃。
- IDE 缓存、Gradle 缓存、日志、crash report 和本地游戏运行目录。
 
