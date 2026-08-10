# RedFoxExpand 1.8.9 材质包目录规范

本文定义 `0.2.0` 的 v1/v2/v3 目录、资源优先级和迁移边界。字段见
[`resourcepack-api.md`](resourcepack-api.md) 与 [`SCHEMA_V3.md`](SCHEMA_V3.md)。

## Native v2/v3 标准目录

```text
<resource-pack>/
├─ pack.mcmeta                       # 1.8.9: pack_format 1
└─ assets/
   └─ kyeitk/
      └─ redfoxexpand/
         ├─ index.json               # api_version 2 或 3
         ├─ config/
         │  └─ *.json
         └─ textures/
            └─ gui/
               └─ *.png
```

- namespace 必须是原生小写 `kyeitk`；唯一发现入口是
  `kyeitk:redfoxexpand/index.json`。
- manifest config 必须位于 `kyeitk:redfoxexpand/config/`，最多 256 项，并从 manifest 的同一 pack 读取。
- `pack_resource` 无 namespace 路径以 `kyeitk:redfoxexpand/` 为根；带 namespace 时按给出的
  ResourceLocation 读取。
- 普通纹理遵循 Minecraft 资源覆盖栈，因此较高资源包可以只覆盖 PNG；config 不允许被另一 pack
  冒名顶替。

## Legacy v1 目录

```text
assets/Kyeitk/
├─ config/**/*.json
├─ textures/gui/**/*.png
└─ compatibility/<modid>/
   ├─ config/*.json
   └─ textures/gui/*.png
```

大写 `Kyeitk` 是 0.1.0 历史物理目录。Minecraft 1.8.9 会把 ResourceLocation domain 小写化且原版资源包
拒绝大写 namespace，因此 RedFoxExpand 仍通过文件夹/ZIP 后备文件直接扫描这棵树。逐帧渲染不会扫描文件。

没有适用的大写 v1 配置时，旧 `assets/<namespace>/polytone/gui_modifiers/**/*.json` 回退仍可加载；
一旦存在大写 v1 配置，旧 Polytone 路径按 0.1.0 规则停止加载，避免重复。小写 v2/v3 与这一选择无关，
可和大写 v1 同时存在。

## 资源优先级

v1：相同大写 Kyeitk 相对路径由资源包列表中最高优先级文件胜出；不同路径按字典序解析。

v2/v3：候选按以下顺序应用：

```text
resource-pack priority
-> definition priority
-> config resource path
-> definition array index
```

`append` 保留同 ID 定义；`replace` 移除之前同 ID 候选并加入自己；`disable` 移除之前同 ID 候选且不加入。
reload 在临时对象中完成扫描、解析与纹理验证；成功后一次替换 generation，失败保留旧 generation。

## 路径安全与预算

- 拒绝绝对路径、盘符、前导 `/`、反斜杠穿越、空段、`.` 与 `..`。
- config 最大 1 MiB，JSON 最大嵌套 32；Definition/Sprite/Slot/Text/动画帧均有固定计数预算。
- native PNG 最大 32 MiB、单边 4096、单图 16M pixels、单动画 64M pixels、generation 128M pixels。
- 纹理存在性和 PNG IHDR 在 reload 验证；render/tick 不执行文件、ZIP、JSON、PNG 或表达式解析。
- v1 动态纹理由 generation 的 `KyeitkTextureRegistry` 持有；成功替换后释放旧 registry。

## 迁移建议

保留原 v1 包时无需迁移。要采用严格协议：

1. 新建小写 `assets/kyeitk/redfoxexpand/index.json`；
2. 把 config 改成 v2 root（`api_version` + `definitions`），为每条 Definition 提供稳定 namespaced `id`；
3. 把旧 target 转为明确的 `match` operator；
4. 把纹理改成 `{type, location}` 对象；
5. 需要 Reactive UI 时再升级 manifest/config 到 v3，并为每个 Sprite 增加 Definition-local `id`；
6. F3+T 后检查日志、Slot 点击区、透明度/层级、GUI Scale 与禁用包回退。

不要在同一 manifest 中混用 v2/v3 config。1.8.9 不支持 `menu_type/resource_location/mod_namespace`
matcher；使用 class/title matcher。

## 建议验证流程

从最小目录开始，只加入一条 Definition 和一张自制 PNG；确认可加载后再逐步增加 Binding、Animation 与
Behavior。每次修改后执行 F3+T，检查日志、Slot 点击区、透明度/层级和至少两种 GUI Scale。公开仓库不
分发测试材质包、第三方图片或其 ZIP。
