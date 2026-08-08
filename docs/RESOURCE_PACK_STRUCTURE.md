# RedFoxExpand 材质包目录规范

Minecraft 26.2 分支只使用原生小写 namespace 和 Schema v2。

```text
<resource-pack>/
├─ pack.mcmeta
└─ assets/kyeitk/redfoxexpand/
   ├─ index.json
   ├─ config/
   │  └─ inventory.json
   └─ textures/gui/
      └─ inventory.png
```

`pack.mcmeta` 的 `pack_format`、`min_format` 和 `max_format` 均为 `88`。`index.json` 使用
`api_version: 2`，并列出最多 256 个位于 `redfoxexpand/config/` 下的 JSON。

## 资源优先级

Mod 使用 `ResourceManager.getResourceStack` 获取所有 manifest，并从同一来源包读取配置和纹理。文件夹、
ZIP、Mod 内置和服务器资源包共享同一原生流程。合并顺序由资源包优先级、definition priority、来源路径
和数组序号共同决定。

## 从旧版迁移

1. 将 namespace 改为 `assets/kyeitk/redfoxexpand/`；
2. 新增 `index.json`；
3. 配置包裹为 `api_version: 2` 与 `definitions`；
4. `target_type/target/class_match` 改为单个 `match`；
5. 几何字段移入 `geometry`；
6. `custom_textures` 改为 `sprites`，纹理改为显式对象；
7. 标题/标签字段改为 `text_rules`；
8. 动画写入 Sprite 的 `animation`；
9. 为 definition 指定 namespaced `id` 和 `operation`。

26.2 不扫描大写 `assets/Kyeitk/`，也不会猜测路径或模拟旧 flat v1 配置。
