# KYS 编辑器 Android 版 (Kys Editor for Android)

将桌面版 KYS 编辑器移植到 Android 平台，使用 Kotlin + Jetpack Compose 实现，支持在手机上直接编辑金庸群侠传系列游戏存档。

## 功能说明

### 已实现功能

| 功能模块 | 说明 |
|---------|------|
| **存档编辑** | 支持 Ranger.grp 存档的读取/编辑/保存，包括角色、物品、武功、场景、商店数据 |
| **角色编辑** | 91 个字段的完整编辑（姓名、绰号、等级、生命、内力、攻防、技能、装备、武功栏等） |
| **物品编辑** | 95 个字段的完整编辑（名称、说明、类型、价格、属性加成、装备需求等） |
| **武功编辑** | 68~111 个字段的完整编辑（名称、类别、伤害、耗内、各级威力等） |
| **背包编辑** | 物品槽列表的增删改查，自动显示物品名称 |
| **总览页** | 队伍成员、银两、当前位置、时间等存档头信息 |
| **事件查看** | Kdef.grp 脚本反汇编，中文 opcode 名称，参数自动解析为角色/物品/场景名 |
| **对话编辑** | Talk 文件的 XOR 0xFF 编解码，支持对话文本的读取和修改 |
| **战斗数据** | War.sta 战斗记录的读取和编辑，战斗名称、队友/敌人列表、坐标、奖励 |
| **战斗地图** | Warfld.idx/grp 战场地图的 64×64 网格预览 |
| **大地图** | earth/surface/building/transport/building2 五层大地图加载和 Canvas 渲染 |
| **贴图查看** | Heads.Pic / Items.Pic 的 PNG 帧提取和网格预览 |
| **贴图导出** | 将任意贴图帧导出为 PNG 文件到用户选择的目录 |
| **贴图替换** | 从设备中选择 PNG 图片替换原有贴图帧 |
| **交叉引用** | 按 ID 搜索角色/物品/武功/场景的使用位置 |
| **自动探测** | 自动检测游戏类型（前传 / 经典 KYS），设置正确的字段偏移和编码 |

### 技术特性

- **语言**：Kotlin（AOT 原生编译，最高运行效率）
- **UI 框架**：Jetpack Compose + Material3
- **文件系统**：Android Storage Access Framework (SAF)，支持选择任意目录
- **二进制兼容**：与桌面版 Python 编辑器字节级兼容，所有 `struct.pack/unpack` 逻辑 1:1 移植
- **文本编码**：自动检测 GBK / Big5 编码（经典 KYS = Big5，前传 = GBK）
- **图片缓存**：LRU 内存缓存 + 缩略图采样，流畅滚动大量贴图
- **异步加载**：所有文件 IO 在 IO 线程执行，UI 不卡顿

## 环境要求

### 构建环境

| 工具 | 版本要求 |
|------|---------|
| JDK | 17 或 21（推荐 17，不支持 Java 25） |
| Android SDK | API 34 (Android 14)，最低 build-tools 34.0.0 |
| Gradle | 8.13（通过 gradlew 自动下载） |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.10 |
| Compose BOM | 2024.02.00 |

### 运行环境

- Android 7.0 (API 24) 及以上
- 需要存储权限（通过 SAF 授权目录访问）

## 构建步骤

### 方式一：Android Studio（推荐）

1. 打开 Android Studio
2. 选择 `File → Open`，定位到 `for_Android` 目录
3. 等待 Gradle Sync 完成
4. 点击 `Build → Generate Signed Bundle / APK`
5. 选择 APK，配置签名密钥
6. 选择 `release` 构建类型
7. 点击 `Finish`，等待构建完成

### 方式二：命令行

```bash
# 进入项目目录
cd for_Android

# 创建 local.properties 指向 Android SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Debug 构建
./gradlew assembleDebug

# Release 构建（需配置签名）
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/debug/` 或 `app/build/outputs/apk/release/`。

### 签名配置

如需 release 签名，在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = "your_password"
            keyAlias = "your_alias"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 使用方法

1. **选择目录**：启动应用后，点击"选择目录"按钮，选择包含 `save/` 和 `resource/` 子目录的游戏根目录
2. **选择存档**：点击顶部的"档0"按钮，选择要编辑的存档位（0=自动存档，1~3=手动存档）
3. **编辑数据**：在底部导航栏切换各个编辑页面：
   - **存档**：总览、角色、物品、武功、背包
   - **事件**：查看事件脚本和对话文本
   - **战斗**：查看战斗数据和战场地图
   - **大地图**：点击"加载大地图"按钮加载世界地图
   - **贴图**：点击图片可导出 PNG 或替换图片
   - **引用**：按 ID 搜索交叉引用
4. **保存**：点击顶部保存按钮将修改写入存档文件

### 支持的游戏版本

| 游戏 | 编码 | 自动探测标志 |
|------|------|-------------|
| 金庸群侠前传 (Kys Promise) | GBK | magicWords=111, 战斗=156字 |
| 经典 KYS / 天龙八部 (kys-awaken) | Big5 | magicWords=68, 战斗=93字 |

## 目录结构

```
for_Android/
├── app/
│   ├── build.gradle.kts              # 模块构建配置
│   ├── proguard-rules.pro            # 代码混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml       # 应用清单
│       ├── res/                       # 资源文件（图标/主题/字符串）
│       └── java/com/kys/editor/
│           ├── MainActivity.kt        # 入口 Activity
│           ├── KysEditorApp.kt        # Application 类
│           ├── codec/                 # 二进制格式编解码
│           │   ├── RangerArchive.kt   # 存档解析（含背包偏移修正）
│           │   ├── KdefArchive.kt     # 事件脚本反汇编
│           │   ├── WarArchive.kt      # 战斗数据
│           │   ├── WarFieldArchive.kt # 战场地图
│           │   ├── TalkArchive.kt     # 对话文本 (XOR 0xFF)
│           │   ├── PicArchive.kt      # PNG-in-Pic 帧归档
│           │   ├── RleTilePack.kt     # RLE8 瓦片解码
│           │   ├── WorldMapBundle.kt  # 大地图 (480×480×5层)
│           │   ├── ImageBank.kt       # 图片资源加载
│           │   ├── GameProfile.kt     # MOD 配置档
│           │   ├── TextEncoding.kt    # GBK/Big5 编解码
│           │   └── meta/              # 物品/武功/角色/opcode 中文映射
│           ├── fs/                    # 文件系统层
│           │   ├── SafHelper.kt       # SAF 目录选择
│           │   ├── VirtualFileSystem.kt # VfsNode 抽象
│           │   └── GameRootResolver.kt  # MOD 类型探测
│           ├── ui/
│           │   ├── components/        # 通用组件
│           │   ├── context/           # 全局状态
│           │   ├── screens/           # 6 个 Tab 页面
│           │   └── theme/             # Material3 主题
│           └── util/
│               ├── BinaryReadWrite.kt # 小端 ByteBuffer 扩展
│               └── BitmapCache.kt     # 图片 LRU 缓存
├── build.gradle.kts                  # 根构建配置
├── settings.gradle.kts               # 项目设置
├── gradle.properties                 # Gradle 属性
├── gradlew / gradlew.bat            # Gradle Wrapper
└── gradle/wrapper/                   # Wrapper JAR 和配置
```

## 注意事项

1. **编码问题**：经典 KYS（天龙八部）使用 Big5 编码，前传使用 GBK 编码。应用会自动探测，如遇乱码请检查探测结果
2. **备份存档**：编辑前建议备份原始存档文件，应用内置备份机制但建议额外备份
3. **大文件加载**：大地图和贴图包可能较大，首次加载需要几秒钟，加载后缓存在内存中
4. **SAF 限制**：Android SAF 不支持随机写入，每次保存会重写整个文件
5. **Java 版本**：构建时使用 JDK 17 或 21，不要使用 Java 25（AGP 8.2.2 不兼容）
6. **字段对齐**：所有二进制偏移和字段顺序与桌面版 Python 编辑器完全一致

## 与桌面版的对应关系

| 桌面版 (Python/PySide6) | Android 版 (Kotlin/Compose) |
|------------------------|----------------------------|
| `kys_formats/ranger.py` | `codec/RangerArchive.kt` |
| `kys_formats/kdef.py` | `codec/KdefArchive.kt` |
| `kys_formats/war.py` | `codec/WarArchive.kt` |
| `kys_formats/talk.py` | `codec/TalkArchive.kt` |
| `kys_formats/pic.py` | `codec/PicArchive.kt` |
| `kys_formats/rle_tile.py` | `codec/RleTilePack.kt` |
| `kys_formats/profile.py` | `codec/GameProfile.kt` |
| `kys_formats/encoding.py` | `codec/TextEncoding.kt` |
| `ui/save_editor.py` | `ui/screens/SaveEditorScreen.kt` |
| `ui/event_editor.py` | `ui/screens/EventEditorScreen.kt` |
| `ui/battle_editor.py` | `ui/screens/BattleEditorScreen.kt` |
| `ui/asset_editor.py` | `ui/screens/AssetViewerScreen.kt` |

## 许可证

与主项目相同。
