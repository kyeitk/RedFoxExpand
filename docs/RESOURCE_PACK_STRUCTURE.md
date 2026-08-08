# 材质包目录结构

## Kyeitk v1：跨 1.7.10 / 1.8.9

需要同一份包兼容两个项目时使用此结构：

```text
MyPack/
├─ pack.mcmeta
└─ assets/
   └─ Kyeitk/
      ├─ config/
      │  └─ inventory.json
      ├─ textures/gui/
      │  ├─ inventory.png
      │  └─ inventory_anim/
      │     ├─ animation.json
      │     └─ frame_0.png
      └─ compatibility/<modid>/config/
         └─ backpack.json
```

v1 中没有 namespace 的贴图和动画路径相对 `assets/Kyeitk/`。`compatibility/<modid>/config/`
仅在相应 Mod 已加载时参与扫描。大小写 `Kyeitk` 是 v1 兼容协议的一部分；文件夹、ZIP/JAR 必须实际保留。

## Native v2：1.7.10 原生资源域

不可枚举或没有后备文件的资源包应使用 v2：

```text
assets/kyeitk/
└─ redfoxexpand/
   ├─ index.json
   ├─ config/
   │  └─ inventory.json
   └─ textures/gui/
      └─ inventory.png
```

`index.json`：

```json
{
  "api_version": 2,
  "configs": [
    "redfoxexpand/config/inventory.json"
  ]
}
```

约束：

- manifest 固定为 `kyeitk:redfoxexpand/index.json`；
- `api_version` 必须为 `2`；
- 配置必须位于 `redfoxexpand/config/` 且以 `.json` 结尾；
- v2 的相对资源从小写 `kyeitk` domain 读取；
- 当前 1.8.9 项目不读取 v2，跨版本包应保留 v1。

## Legacy Polytone

```text
assets/<namespace>/polytone/gui_modifiers/<name>.json
```

该入口只保证文档列出的兼容字段。新包优先使用 Kyeitk v1 或 v2。

## 优先级与失败隔离

同一路径使用 Minecraft 资源管理器给出的最高优先级内容。不同路径产生的 definitions 按发现顺序进入
registry；`priority` 只控制最终应用顺序，不替代资源包优先级。显式相同 `id` 才能跨文件
`replace` 或 `disable`。

坏 JSON、非法路径、缺失纹理或超预算内容只跳过对应来源。一个坏 v1/v2 文件不会全局关闭 legacy 或其他
有效配置。
