# KYS 通用制作器 (Kys General Editor)

与 C++ 引擎完全解耦的 **金庸群侠传系（KYS）** 数据编辑器：只读写游戏数据目录，通过 **GameProfile（配置档）** 适配不同游戏的字宽、目录与贴图布局。

截图示例基于本机「天龙八部 / GodsDevils」数据根（`save/` + `resource/` + 散图目录）。

> **Android 版现已上线！** 请前往 [`feature/android-editor`](https://github.com/masterball-w/Kys_general_editor/tree/feature/android-editor/for_Android) 分支查看 Kotlin + Jetpack Compose 实现的移动端编辑器，支持在手机上直接编辑存档。APK 下载见 [Releases](https://github.com/masterball-w/Kys_general_editor/releases)。

## 安装与启动

```bash
cd Kys_general_editor   # 或本仓库根目录
pip install -r requirements.txt
python main.py
```

启动后选择含 `save/` 与 `resource/` 的数据根目录；也可由工具栏「配置档」手动指定前传 / 经典 KYS。写回前会自动生成 `.bak`。

### 重要：`alldef.grp` / `allsin.grp` 是新游戏模板

- 场景 0 开场权威数据（稳定版）：事件 **0 = 8284**（躺床）、事件 **1 = 8268** 在卧室旁 `(Y=40,X=37)`。
- 制作器对这两份文件 **roundtrip 字节级无损**；若开场贴图异常，优先怀疑引擎/存档把**剧情推进后的 D/S**写回了模板，而不是制作器编解码错误。
- 引擎自 2026-07-28 起：`SaveGame(0)` **不再覆盖** `alldef`/`allsin`（只写 `ranger`）。进度请存 **1–5 槽**（`D1`/`S1` …）。
- 回归说明与单测：`cpp_reborn/doc/REGRESSION_SCRIPT_101.md`、`tests/test_formats.py::test_alldef_scene0_opening_pics`。

重新生成界面截图（需本地游戏数据）：

```bash
python scripts/capture_screenshots.py
```

## 界面一览

### 总览

启动后主窗口：工具栏可选数据根、配置档、文本编码；下方为各功能页签。

![主界面](docs/screenshots/00_main_overview.png)

### 1. 存档数据

编辑 `Ranger.grp` / `Rn.grp`：人物、物品（含合成材料）、武功、背包、商店、场景元数据。

![存档数据](docs/screenshots/01_save_data.png)

| 子页 | 说明 | 截图 |
|------|------|------|
| 人物 | 属性、装备、武功栏、头像预览；经典档隐藏前传「功体」字段；攻击/轻功/防御支持上千数值 | ![人物](docs/screenshots/01_save__roles.png) |
| 物品定义 | 类型/加成/需求/合成材料；经典档仅武器+身披，隐藏战斗特效/酒效应/套装 | ![物品](docs/screenshots/01_save__items.png) |
| 武功 | 前传：成长曲线 + 功体；经典：1～10 级逐级威力；特效预览 | ![武功](docs/screenshots/01_save__magic.png) |
| 背包 | 物品栏槽位（按配置档 ~200 / ~400） | ![背包](docs/screenshots/01_save__inventory.png) |
| 商店 | 按配置档字宽（15/18）动态列 | ![商店](docs/screenshots/01_save__shops.png) |
| 场景元数据 | 场景名、入场条件、地图号等 | ![场景](docs/screenshots/01_save__scenes.png) |

存档总览会按 **GameProfile** 显示队伍槽位数与字节偏移（经典 836 字节头：队伍 @24、银两 @42、背包 @44，共 6 格）。

### 2. 事件

Kdef 脚本、对话库、场景事件挂接（DData）、SData 事件层俯视图。按存档槽加载 **Dn/Sn** 剧情进度，可与 `alldef` 模板对比进度并支持事件回滚；DData 支持主脚本列与跳转 kdef 编辑器。

![事件](docs/screenshots/02_events.png)

**事件脚本**：Opcode 中文名 + 参数释义；支持插入/删除并写回 `kdef`。下图为脚本 81（含「进入战斗」及胜利后半段）。

![事件脚本](docs/screenshots/02_events__script.png)

**对话库 / 场景事件挂接**：

![对话库](docs/screenshots/02_events__talk.png)

![DData 挂接](docs/screenshots/02_events__ddata.png)

**SData 事件层**：层 0 地面砖主色铺底，红色半透明为事件格；可进入调整模式笔刷编辑。俯视图 **显示轴与游戏内朝向一致**（横轴=引擎 Y、纵轴=引擎 X），读写磁盘仍为引擎坐标；支持左键笔刷、右键擦除/取色、调整模式说明与稳定滚动。

![SData 俯视图](docs/screenshots/02_events__sdata_map.png)

### 3. 战斗

`War.sta` 列表与字段编辑；战场地形 `warfld` 俯视图（wmp 砖主色）。

![战斗](docs/screenshots/03_battle.png)

![战斗列表](docs/screenshots/03_battle__list.png)

![编辑战斗](docs/screenshots/03_battle__edit.png)

![战场地形](docs/screenshots/03_battle__field_map.png)

### 4. 大地图

`earth/surface/building/buildx/buildy/*.002`（480×480）+ `mmap` 砖库主色俯视图。
- **编辑层**：可切换地面 / 地表 / 建筑等；「俯视图用当前层上色」便于改建筑。
- **批量编辑**：调整模式下画笔按住拖动连续绘制；框选后复制 / 粘贴 / 填充（Ctrl+C/V），只改 `.002` 贴图码，**不改入口**。
- **导入导出**：选区 JSON、整层 `.002`；mmap 砖号可一键写入笔刷（×2）。

![大地图](docs/screenshots/04_world_map.png)

### 5. 贴图

按配置档打开 `.Pic` 包，或提示经典散图 / `idx+grp` 路径。
**RLE 砖库**面板支持 `mmap`（大地图/建筑）、`smp/sdx`（场景砖）、`wmp`（战场）的单帧与批量 PNG 导入导出，以及保存 idx+grp。

![贴图](docs/screenshots/05_assets.png)

![常用贴图包](docs/screenshots/05_assets__common.png)

![场景砖](docs/screenshots/05_assets__tiles.png)

### 6. 交叉引用

按 BattleNum / 物品等反查脚本引用。

![交叉引用](docs/screenshots/06_crossref.png)

## 配置档差异（摘要）

| 项目 | 前传 (Promise) | 经典 / GodsDevils |
|------|----------------|-------------------|
| Magic 字宽 | 111 | 68 |
| 武功威力 | Min/Max/成长曲线 `CalNewHurtValue` | `Hurt[18..27]` 每级单独存储 |
| 装备部位 | 武器 / 身披 / 头戴 / 脚踩 等 | 仅武器、身披 |
| 物品扩展 | 战斗特效、酒效应、套装号 | 无（UI 隐藏且保存时不写） |
| 功体 | 武功记录 + 人物 Gongti 字段 | 无 |
| Shop 字宽 | 18 | 15 |
| War 字宽 | 156 | 93 |
| 背包槽 | ~400 | ~200 |
| Ranger 队伍 | 6 格 @30，背包 @42 | 6 格 @24（836 头），银两 @42，背包 @44 |
| 头像/物品图 | `resource/*.Pic` | `head/*.png` `item/*.png` |
| 战斗/特效 | `fight/NNN/MM.pic`、`eft/*.pic` | `fight/fightNNN.grp`、`resource/eft.idx` |

兼容语义由 `GameProfile.compat`（`EditorCompat`）驱动；工具栏可强制指定配置档，自动探测时按 Magic 字宽、War 字宽与贴图布局判断。

## 格式库（无 UI）

```python
from kys_formats import RangerArchive, RangerLayout, detect_profile, WarArchive

profile = detect_profile("../")  # 或 game_data
arc = RangerArchive(RangerLayout.from_profile(profile))
arc.load("path/to/save")
```

## 注意事项

- 背包槽按磁盘实际长度读写，并 pad 到配置档 `inventory_slots`。
- talk / name 为 XOR 0xFF + Big5/GBK；GodsDevils 存档名偏 Big5。
- 经典 `eft` / `fight` / `hdgrp` 为 RLE 调色板图，部分面板预览仍有限。
- 经典武功勿用前传「成长曲线」面板编辑，否则可能破坏 `Hurt[18..27]`。
- **不要**将游戏二进制资源（`save/`、`resource/`、整包 `game_data`）提交进本仓库。

## 更新记录

（由新到旧，对应 `main` 分支提交。）

### `bcb9a33` — 经典 KYS 兼容与编辑器增强

- 新增 `EditorCompat`：经典武功按 **1～10 级逐级威力** 编辑；前传保留 Min/Max/成长曲线。
- 经典物品仅 **武器/身披**；隐藏战斗特效、酒效应、套装，保存时不误写。
- 经典隐藏武功 **功体** 块与人物 **Gongti** 字段。
- 人物攻击/轻功/防御 SpinBox 上限提升至 9999（修复高攻击显示为 200）。
- 大地图：编辑层切换、框选复制粘贴填充、选区 JSON / 整层 `.002` 导入导出。
- 贴图：RLE 砖库（mmap / smp / wmp）单帧与批量 PNG、idx+grp 保存；懒加载缩略图列表。

### `ca149ca` — 俯视图 XY 与 SData 编辑体验

- 场景小地图 / 大地图 / SData 俯视图 **显示轴与游戏内一致**（读写仍为引擎坐标）。
- SData 调整模式说明、右键擦除/取色、滚动条常显与滚动稳定。

### `74492bd` — 经典队伍槽位偏移

- 836 字节 Ranger 头：队伍 **Team[0] 在字节 24**（kys-cpp 布局），非字节 30。
- 保留头部 padding，roundtrip 无损。

### `080078f` — 六格队伍与背包 @44

- 与 `cpp_reborn` 对齐：队伍 **6 格**；停止将 inv@36 误当背包（会污染 team[3..5]）。
- 经典：**银两 @42、背包 @44**；存档总览完整保留队伍列表。

### `8df4c64` — 探测 836 字节 Mod 头

- 按 `role_o` 与 inventory 基址探测队伍槽数与偏移。
- 存档总览显示各布局下的槽位数与字节位置。

### `cb144e0` — 存档绑定剧情进度

- 按存档槽加载 **Dn/Sn**；存档页与事件页槽位同步。
- DData 与 `alldef` 模板对比进度（0/1），支持 **ModifyEvent 图回滚**。
- `GameProfile` 支持 ranger 背包基址 42/44；GodsDevils 并入经典 KYS 配置档。
- DData 主脚本列、下拉与跳转 kdef 脚本编辑器。

### `e864eef` — 首次发布

- 与引擎解耦的数据编辑器；`GameProfile` 多游戏字宽与贴图布局。
- 地图俯视图、存档/事件/战斗/贴图/交叉引用等主界面与 README 截图。

## 测试

```bash
pytest tests/ -v
```

测试会依次尝试仓库旁的游戏根目录与 `game_data/`。

## 许可证

编辑器源码以仓库声明为准；游戏数据版权归原作者所有，本仓库不包含游戏资源。
