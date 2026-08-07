# Hex Guide

[![powered by hexdoc](https://img.shields.io/endpoint?url=https://hexxy.media/api/v0/badge/hexdoc?label=1)](https://github.com/hexdoc-dev/hexdoc)

Hex Guide addon for Hex Casting

## 功能 / Features

### 手册增强 / TheHexBook enhancements

- **搜索页 / Search page**：在咒术笔记中加入自然语言搜索页，可按名称/关键词/描述搜索条目并点击跳转。
  A natural-language search page for TheHexBook — search entries by name, keyword, or description and click to jump.

- **内嵌施法网格页 / Embedded spellcasting grid**：在手册中练习绘制图案（可配置点间距、坐标偏移），支持 Cast/Write 切换与本地栈显示。
  Practice drawing patterns right in the book (configurable hex size and offset), with Cast/Write toggle and a live stack.

- **施法演示页 / Spellcasting demo**：按配置文件自动演示图案绘制与栈变化（`data/<ns>/spellplays/<name>.json`），支持 execute/push/peek/clear 步骤、转义（内省/考察）模拟、可配置颜色与标题。
  Auto-plays pattern drawing and stack changes from a config file, with execute/push/peek/clear steps, escape (introspection/consideration) simulation, configurable colors and titles.

- **指南教程章节 / Guide tutorials**：新增「栈、图案、元运行、逻辑分支」等教程条目，配合内联 Iota 展示。
  Tutorial entries on the stack, patterns, meta-execution, and logic branching, with inline Iota examples.

### Inline 集成 / Inline integration

- **Iota 内联渲染 / Inline Iota rendering**：书写 `iota:...` 即可在聊天/书中渲染为 Iota 图形（Ascii85 压缩编码、`iota:<ns>:<name>.json` 资源文件、或游戏目录自动保存），支持悬停查看与点击复制引用。
  Type `iota:...` to render Iota graphics in chat and books (Ascii85-compressed, `iota:<ns>:<name>.json` resource files, or game-dir auto-save), hover to inspect, click to copy.

- **颜色与换行后缀 / Color & wrap suffixes**：`*b`/`*w`/`*RRGGBB` 强制颜色，`**n` 强制换行（ListIota 按元素换行）。
  `*b`/`*w`/`*RRGGBB` force colors; `**n` forces line-wrapping (ListIota wraps per element).

### 自定义图案 / Custom patterns

- **`hexguide:copy`（复制 Iota / Copy Iota）**：把栈顶 Iota 复制到剪贴板并自动保存为 JSON（`<gameDir>/hexguide/iotas/`）。
  Copies the top Iota to the clipboard and auto-saves it as JSON.

- **`hexguide:demo`（保存演示 / Save Demo）**：把栈顶 ListIota 生成为演示配置文件（`<gameDir>/hexguide/spellplays/`）。
  Generates a demo config file from the top ListIota.

### 其他 / Other

- **探知 / Scrying**：佩戴探知透镜（SCRY_SIGHT > 0）时查看石板/卷轴上的图案提示，按快捷键（默认 R）打开咒术笔记对应页。
  With a scrying lens (SCRY_SIGHT > 0), shows pattern hints for slates and wall scrolls; press the hotkey (default R) to open TheHexBook at the matching page.

- **创造模式标签页 / Creative tab**：包含所有已注册图案的石板与大型卷轴（排除卓越法术与每世界图案），按图案本地化名命名（「XX之石板 / XX之卷轴」）。
  A creative tab with slates and large scrolls for every registered pattern (excluding great spells and per-world patterns), named by localized pattern names.

- **互联 Tag 修复 / Interop Tag Fix**：Fabric 与 Forge 的 `hexcasting:action` tag 数据包路径不同（Fabric 为 `data/<ns>/tags/action/`，Forge 为 `data/<ns>/tags/hexcasting/action/`）。在 Connector 互联环境下（Forge 运行 Fabric 版附属），Forge 的 TagLoader 只读 Forge 路径，导致 Fabric 附属的 tag（如 `requires_enlightenment`、`per_world_pattern`）全部丢失。本模组在**服务器启动时**自动扫描所有数据包（含 mods 目录 jar），把 Fabric 路径的 tag 复制进 `hexcasting:action` 注册表，恢复这些 tag。可在配置中关闭（`server.fixTags`，默认开启）。

  Fabric and Forge use different datapack paths for `hexcasting:action` tags (`data/<ns>/tags/action/` vs `data/<ns>/tags/hexcasting/action/`). Under Connector (Fabric addons on Forge), Forge's TagLoader only reads the Forge path, so Fabric addon tags (e.g. `requires_enlightenment`, `per_world_pattern`) are lost. This mod scans all datapacks (including mods-dir jars) at **server start** and copies Fabric-path tags into the `hexcasting:action` registry. Disablable via `server.fixTags` (default on).

## 翻译状态 / Translation status

> **注意：缺少英文翻译支持 / Note: English translation support is incomplete.**

本项目以中文为主要语言，部分新增内容（手册教程文本、演示配置标题等）**缺少完整的英文翻译**，英文（en_us）显示时可能回落为中文或键名。欢迎提交翻译贡献。

The mod is primarily authored in Chinese; some content (book tutorial texts, demo titles, etc.) **lacks full English translation** and may fall back to Chinese or raw keys in en_us. Translations are welcome.

## 相关文档 / Docs

- [SPELLPLAYS.md](SPELLPLAYS.md)：施法演示配置格式 / demo config format
