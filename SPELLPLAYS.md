# HexGuide —— 演示与 Iota 内嵌文档（备用 README）
# HexGuide — Demo & Iota Inline Documentation (Supplementary README)

本文档介绍 HexGuide 的新功能：**手册内嵌施法演示**（动画配置）与 **Iota 文本内嵌**（Inline 渲染）。主 README 见根目录 `README.md`。

This document covers HexGuide's new features: the **in-book spellcasting demo** (animation configs) and **Iota text inlining** (Inline rendering). See the main `README.md` for the rest of the mod.

---

## 目录 / Contents

- [1. 手册内嵌施法演示（Spellplay 动画配置）](#1-手册内嵌施法演示spellplay-动画配置)
- [2. Iota 文本内嵌（Inline 渲染）](#2-iota-文本内嵌inline-渲染)
- [3. 语言文件 / 界面补充](#3-语言文件--界面补充)

---

## 1. 手册内嵌施法演示（Spellplay 动画配置）

### 1.1 是什么 / What it is

《咒术笔记》的 **guide** 分类中新增「施法演示」条目：书页内嵌真实的法阵绘制界面（`hexguide:spellcast_demo` 页面类型），自动播放一串预定义的步骤，演示 **图案绘制** 与 **栈变化**，全程带音效。

The HexBook's **guide** category now has a "Spellcasting Demo" entry: a real spellcasting grid embedded in the page (page type `hexguide:spellcast_demo`) that automatically plays a sequence of predefined steps, demonstrating **pattern drawing** and **stack changes**, complete with sounds.

### 1.2 配置文件 / Config files

演示配置存放在**数据包目录**，通过页面 JSON 引用：

Configs live in the **datapack directory**, referenced from the page:

```
data/<命名空间 ns>/spellplays/<名称 name>.json
```

页面引用 / Page reference:

```json
{
  "type": "hexguide:spellcast_demo",
  "demo": "hexguide:text",   // ns:name（默认 ns 为 hexguide）
  "hex_size": 8              // 网格点间距（可选）
}
```

客户端向服务端请求配置（服务端读取数据包），因此**多人服务器可自行添加演示**，客户端无需安装任何文件。

The client requests the config from the server (which reads the datapack), so **server admins can add their own demos** — clients need nothing installed.

### 1.3 配置格式 / Config format

```json
{
  "title": "咒术演示 Demo",    // 全局大标题（未播放时显示）
  "interval": 50,           // 全局每步间隔（tick），可被每步覆盖
  "clear_before": true,     // 每一步之前是否清空画布（网格，不清栈），默认 true
  "start_dir": "NORTH_EAST",// 全局图案起始朝向
  "steps": [
    { "type": "execute", "action": "hexcasting:get_caster" },
    { "type": "push", "pattern": "aqw", "push": "double:3.14", "title": "压入数字" },
    { "type": "clear", "title": "清空画布" },
    { "type": "peek", "index": 0 }
  ]
}
```

> 顶部标题为**动态**的：播放中显示**当前步骤标题**，未播放（暂停/未开始/播完）显示配置文件的**全局大标题**（`title`）。每步可用 `title` 自定义标题；未配置时默认取该步 `action` 的本地化名称（`hexcasting.action.<id>`），若翻译键不存在或没有 action 则不显示标题。

> The top title is **dynamic**: while playing it shows the **current step title**; when not playing (paused/not started/finished) it shows the config's **global title** (`title`). Each step can set a custom `title`; by default it uses the step's `action` localized name (`hexcasting.action.<id>`), and is hidden if no such translation key exists or the step has no `action`.

### 1.4 步骤类型 / Step types

| 类型 type | 作用 / Behavior |
|---|---|
| `execute`（默认） | 绘制图案（默认**执行蓝**）→ 把本地 CastingImage **上传服务端**运行（新 VM，**不碰玩家法杖栈**）→ 结果传回 → 本地栈 = 结果。失败（ERRORED）时图案染红 |
| `push` | 绘制图案 → 把**配置的自定义 Iota** 压入本地栈（纯本地） |
| `clear` | **清空画布（只清网格，不清栈）**，可作为序列中间的一步 |
| `peek` | **移除本地栈指定位置的 Iota**（栈顶为下标 0），支持单个或多个，越界忽略 |

### 1.5 每步字段 / Per-step fields

| 字段 | 说明 / Description |
|---|---|
| `type` | 步骤类型（execute/push/clear/peek） |
| `pattern` | 图案**笔顺**（角度字符，如 `"aqw"`）——**网格显示**优先 |
| `action` | 已注册图案 id（如 `"hexcasting:get_caster"`）——**服务端执行**优先 |
| `start_dir` | 该步的起始朝向（如 `"EAST"`），默认取全局 |
| `origin` / `q`,`r` | 图案起始**网格坐标**（默认 `[-1, 2]`；`origin` 为 `[q, r]` 数组） |
| `color` | 图案颜色（`"#rrggbb"` / `"0xrrggbb"` / 十进制），默认执行蓝 |
| `interval` | 该步播放间隔（tick），默认取全局 |
| `push` | push 步骤的 Iota（见下文 §2） |
| `index` / `indices` | peek 步骤：移除的下标（`index`: 单个 / `indices`: `[a,b,c]` 多个） |

> **pattern 与 action 同时指定时**：网格绘制 `pattern` 的图案，服务端执行 `action` 的图案（显示与执行分离）。

> **When both `pattern` and `action` are given**: the grid draws `pattern`, while the server executes `action` (display and execution are decoupled).

---

## 2. Iota 文本内嵌（Inline 渲染）

HexGuide 集成了 [Inline](https://modrinth.com/mod/inline) 的字体钩子：聊天、书本中书写 `iota:...` 文本会**直接渲染为对应的 Iota 图形**（可悬停查看、点击复制引用）。

HexGuide hooks into [Inline](https://modrinth.com/mod/inline)'s font renderer: typing `iota:...` text in chat or books renders the corresponding **Iota graphic** inline (hover to inspect, click to copy the reference).

### 2.1 文本格式 / Text formats

| 形式 / Form | 示例 / Example | 结果 / Result |
|---|---|---|
| `iota:<a85>` | `iota:<压缩编码>` | 内联编码（NBT→deflate→Ascii85），短 |
| `iota:<ns>:<name>.json` | `iota:hexguide:vec0.json` | 从 `assets/<ns>/iotas/` 或游戏目录加载 |
| `iota:<name>.json` | `iota:vec0.json` | 短引用（默认 ns = hexguide） |
| `iota:[<元素>,...]` | `iota:[double:1, vec:{2,3,4}]` | ListIota（便携输入，可嵌套） |
| `double:<数字>` | `double:3.14` | DoubleIota（便携输入） |
| `vec:{a,b,c}` | `vec:{1,2,3}` | Vec3Iota（便携输入） |
| `pattern:<action id>` | `pattern:hexcasting:empty_list` | 已注册图案 → PatternIota |
| `pattern{<朝向>,<笔顺>}` | `pattern{NORTH_EAST,qqaeaae}` | 直接解析 → PatternIota（花括号：避免在 `iota:[...]` 列表内与外层 `[` 冲突） |
| `null` | `null` | NullIota |
| `""` | `""` | **不入栈任何东西** |

> 上述格式同样可用于演示配置的 `push` 字段（字符串形式）。`iota:[...]` 的元素可递归使用本表任意形式（含嵌套 `iota:[...]`）；逗号分割按括号深度进行，`vec:{...}` / `pattern{...}` 内部的逗号不会误分割。

> These formats also apply to the demo config's `push` field (string form). Elements of `iota:[...]` may recursively use any form above (including nested `iota:[...]`); splitting is bracket-depth aware, so commas inside `vec:{...}` / `pattern{...}` are safe.

### 2.2 自动保存 / Auto-save (OpTextCopy)

`OpTextCopy`（图案「复制」）施法后，会把 Iota **自动保存**为 JSON 文件：

```json
{ "nbt": "<SNBT 字符串>" }
```

保存位置 / Location: `<游戏目录 gameDir>/hexguide/iotas/<hash>.json`

同时聊天栏提示引用 `iota:<hash>.json`（点击复制）。可放进书本页面直接内嵌显示。

The game also prints a reference `iota:<hash>.json` (click to copy), usable directly in book pages.

### 2.3 iota 资源文件格式 / Iota resource file format

`assets/<ns>/iotas/<name>.json`（或游戏目录回退）两种写法均可：

```json
{ "nbt": "{iota_type:\"hexcasting:pattern\", data:{startDir:0, angles:[0,1,2]}}" }
```

或直接 SNBT JSON / or direct SNBT JSON:

```json
{ "iota_type": "hexcasting:double", "data": 3.14 }
```

---

## 3. 语言文件 / 界面补充

- 创造模式标签页：全部已注册图案的石板 / 卷轴（排除逐世界图案与卓越图案）
- 手册新增条目：搜索、施法网格（交互）、施法演示（动画）
- 探测透镜（Scry Sight）下查看石板/卷轴显示图案提示，按键打开咒术笔记对应页

- Creative tab: slates / scrolls for all registered non-per-world, non-great patterns
- New book entries: search, interactive spellcasting grid, animated demo
- Under Scry Sight, slates/scrolls show pattern hints; a key opens the matching book page

---

*本文档为备用说明，随功能迭代更新。 / This is a supplementary document; it follows feature development.*
