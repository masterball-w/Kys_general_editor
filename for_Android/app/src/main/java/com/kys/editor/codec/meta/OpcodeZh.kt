package com.kys.editor.codec.meta

data class ArgSpec(
    val name: String,
    val kind: String = "int"
)

interface OpcodeContext {
    fun talkGetText(id: Int): String?
    fun nameGetText(id: Int): String?
    fun itemName(id: Int): String?
    fun roleName(id: Int): String?
    fun magicName(id: Int): String?
    fun sceneName(id: Int): String?
    fun battleName(id: Int): String?
}

object OpcodeZh {
    val OPCODE_ZH: Map<Int, String> = mapOf(
        -1 to "结束",
        0 to "重绘场景",
        1 to "显示对话",
        2 to "获得/失去物品",
        3 to "修改场景事件(DData)",
        4 to "判断是否使用某物品?",
        5 to "询问是否战斗?",
        6 to "进入战斗",
        7 to "中断脚本",
        8 to "更换音乐/表情",
        9 to "询问是否加入?",
        10 to "加入队伍",
        11 to "询问是否休息?",
        12 to "休息",
        13 to "淡入",
        14 to "淡出",
        15 to "游戏失败",
        16 to "判断是否在队?",
        17 to "设置场景贴图",
        18 to "判断物品数量?",
        19 to "瞬移坐标",
        20 to "判断队伍是否已满?",
        21 to "离队",
        22 to "内力清零",
        23 to "角色中毒",
        24 to "空指令",
        25 to "镜头平移",
        26 to "累加事件参数",
        27 to "播放事件动画",
        28 to "判断道德?",
        29 to "判断攻击力?",
        30 to "角色行走",
        31 to "判断金钱?",
        32 to "静默增减物品",
        33 to "学会武功",
        34 to "增加资质",
        35 to "设置武功栏",
        36 to "判断性别?",
        37 to "增加道德",
        38 to "更换场景贴图",
        39 to "开启场景",
        40 to "设置面向",
        41 to "设置角色携带物品",
        42 to "判断队中有女性?",
        43 to "有某物品则跳转?",
        44 to "双人动画",
        45 to "增加轻功",
        46 to "增加内力上限",
        47 to "增加攻击",
        48 to "增加生命上限",
        49 to "设置内力属性",
        50 to "扩展指令50e",
        51 to "软星对话",
        52 to "显示道德",
        53 to "显示声望",
        54 to "华山论剑",
        55 to "判断事件贴图?",
        56 to "增加声望",
        57 to "空",
        58 to "全员离队",
        59 to "屏幕震动",
        60 to "判断事件是否存在?",
        61 to "跳转脚本",
        62 to "游戏结束",
        63 to "设置性别",
        64 to "打开商店",
        65 to "空",
        66 to "播放音乐",
        67 to "播放音效",
        68 to "新对话(NewTalk)",
        69 to "重设名称",
        70 to "显示标题",
        71 to "跳转场景",
        83 to "扩展空操作(83)"
    )

    val OPCODE_ARGC: Map<Int, Int> = mapOf(
        0 to 0, 1 to 3, 2 to 2, 3 to 13, 4 to 3, 5 to 2, 6 to 4, 7 to 0, 8 to 1, 9 to 2,
        10 to 1, 11 to 2, 12 to 0, 13 to 0, 14 to 0, 15 to 0, 16 to 3, 17 to 5, 18 to 3, 19 to 2,
        20 to 2, 21 to 1, 22 to 0, 23 to 2, 24 to 0, 25 to 4, 26 to 5, 27 to 3, 28 to 5, 29 to 5,
        30 to 4, 31 to 3, 32 to 2, 33 to 3, 34 to 2, 35 to 4, 36 to 3, 37 to 1, 38 to 4, 39 to 1,
        40 to 1, 41 to 3, 42 to 2, 43 to 3, 44 to 6, 45 to 2, 46 to 2, 47 to 2, 48 to 2, 49 to 2,
        50 to 7, 51 to 0, 52 to 0, 53 to 0, 54 to 0, 55 to 4, 56 to 1, 57 to 0, 58 to 0, 59 to 0,
        60 to 5, 61 to 2, 62 to 0, 63 to 2, 64 to 0, 65 to 0, 66 to 1, 67 to 1, 68 to 7, 69 to 3,
        70 to 2, 71 to 3, 83 to 0
    )

    val OPCODE_NAMES: Map<Int, String> = mapOf(
        0 to "Redraw", 1 to "Dialogue", 2 to "AddItem", 3 to "ModifyEvent",
        4 to "HaveItem?", 5 to "AskBattle", 6 to "Battle", 7 to "Break",
        8 to "ChangeFace", 9 to "AskJoin?", 10 to "Join", 11 to "AskRest?",
        12 to "Rest", 13 to "FadeIn", 14 to "FadeOut", 15 to "GameFail",
        16 to "InTeam?", 17 to "SetSceneTile", 18 to "HaveItemAmt?",
        19 to "Teleport", 20 to "TeamFull?", 21 to "Leave", 22 to "ZeroMP",
        23 to "UsePoi", 25 to "PanCamera", 26 to "AddEventParam",
        27 to "Animate", 28 to "Morality?", 29 to "Attack?", 30 to "Walk",
        31 to "Money?", 32 to "AddItemSilent", 33 to "LearnMagic",
        34 to "AddAptitude", 35 to "SetMagicSlot", 36 to "Sexual?",
        37 to "AddMorality", 38 to "ChangePic", 39 to "OpenScene",
        40 to "SetFace", 41 to "TakingItem", 42 to "FemaleInTeam?",
        43 to "SubFunc", 44 to "DualAnimate", 45 to "AddSpeed",
        46 to "AddMP", 47 to "AddAttack", 48 to "AddHP", 49 to "SetMPType",
        50 to "50e", 51 to "SoftStarTalk", 52 to "ShowMorality",
        53 to "ShowFame", 54 to "Huashan", 55 to "EventPic?", 56 to "AddFame",
        58 to "AllLeave", 59 to "Shake", 60 to "EventExist?", 61 to "Jump",
        62 to "GameEnd", 63 to "SetSexual", 64 to "Shop", 66 to "PlayMusic",
        67 to "PlaySound", 68 to "NewTalk", 69 to "ReSetName",
        70 to "ShowTitle", 71 to "JmpScene", 83 to "Nop83"
    )

    val OPCODE_ARGS: Map<Int, List<ArgSpec>> = mapOf(
        1 to listOf(
            ArgSpec("对话ID", "talk"),
            ArgSpec("头像ID", "head"),
            ArgSpec("显示模式(0左上/1右下/2无头像…)", "int")
        ),
        2 to listOf(ArgSpec("物品ID", "item"), ArgSpec("数量(负=失去)", "int")),
        3 to listOf(
            ArgSpec("场景号(-2=当前)", "scene"),
            ArgSpec("事件号(-2=当前)", "int"),
            ArgSpec("DData[0] 条件", "int"),
            ArgSpec("DData[1]", "int"),
            ArgSpec("DData[2] 手动脚本", "jump"),
            ArgSpec("DData[3] 物品脚本", "jump"),
            ArgSpec("DData[4] 踩上脚本", "jump"),
            ArgSpec("DData[5] 贴图当前(偶数代码, /2=smp)", "pic"),
            ArgSpec("DData[6] 贴图结束", "pic"),
            ArgSpec("DData[7] 贴图起始", "pic"),
            ArgSpec("DData[8]", "int"),
            ArgSpec("DData[9] 坐标Y", "int"),
            ArgSpec("DData[10] 坐标X", "int")
        ),
        4 to listOf(ArgSpec("物品ID", "item"), ArgSpec("是→跳转", "jump"), ArgSpec("否→跳转", "jump")),
        5 to listOf(ArgSpec("是→跳转", "jump"), ArgSpec("否→跳转", "jump")),
        6 to listOf(
            ArgSpec("战斗ID(War.sta)", "battle"),
            ArgSpec("胜→跳转", "jump"),
            ArgSpec("负→跳转", "jump"),
            ArgSpec("是否得经验", "flag")
        ),
        8 to listOf(ArgSpec("音乐/参数", "int")),
        9 to listOf(ArgSpec("是→跳转", "jump"), ArgSpec("否→跳转", "jump")),
        10 to listOf(ArgSpec("角色ID", "role")),
        11 to listOf(ArgSpec("是→跳转", "jump"), ArgSpec("否→跳转", "jump")),
        16 to listOf(ArgSpec("角色ID", "role"), ArgSpec("在队→跳转", "jump"), ArgSpec("不在→跳转", "jump")),
        17 to listOf(
            ArgSpec("场景号", "scene"),
            ArgSpec("图层", "int"),
            ArgSpec("Y", "int"),
            ArgSpec("X", "int"),
            ArgSpec("贴图值", "int")
        ),
        18 to listOf(ArgSpec("物品ID", "item"), ArgSpec("有→跳转", "jump"), ArgSpec("无→跳转", "jump")),
        19 to listOf(ArgSpec("X", "int"), ArgSpec("Y", "int")),
        20 to listOf(ArgSpec("满→跳转", "jump"), ArgSpec("未满→跳转", "jump")),
        21 to listOf(ArgSpec("角色ID", "role")),
        23 to listOf(ArgSpec("角色ID", "role"), ArgSpec("毒值", "int")),
        25 to listOf(ArgSpec("X1", "int"), ArgSpec("Y1", "int"), ArgSpec("X2", "int"), ArgSpec("Y2", "int")),
        26 to listOf(
            ArgSpec("场景号", "scene"),
            ArgSpec("事件号", "int"),
            ArgSpec("DData[2]增量", "int"),
            ArgSpec("DData[3]增量", "int"),
            ArgSpec("DData[4]增量", "int")
        ),
        27 to listOf(ArgSpec("事件号", "int"), ArgSpec("起始贴图", "int"), ArgSpec("结束贴图", "int")),
        28 to listOf(
            ArgSpec("角色ID", "role"),
            ArgSpec("下限", "int"),
            ArgSpec("上限", "int"),
            ArgSpec("在范围→跳转", "jump"),
            ArgSpec("否则→跳转", "jump")
        ),
        29 to listOf(
            ArgSpec("角色ID", "role"),
            ArgSpec("下限", "int"),
            ArgSpec("上限", "int"),
            ArgSpec("在范围→跳转", "jump"),
            ArgSpec("否则→跳转", "jump")
        ),
        30 to listOf(ArgSpec("X1", "int"), ArgSpec("Y1", "int"), ArgSpec("X2", "int"), ArgSpec("Y2", "int")),
        31 to listOf(ArgSpec("金钱阈值", "int"), ArgSpec("够→跳转", "jump"), ArgSpec("不够→跳转", "jump")),
        32 to listOf(ArgSpec("物品ID", "item"), ArgSpec("数量", "int")),
        33 to listOf(ArgSpec("角色ID", "role"), ArgSpec("武功ID", "magic"), ArgSpec("显示模式", "int")),
        34 to listOf(ArgSpec("角色ID", "role"), ArgSpec("资质增量", "int")),
        35 to listOf(
            ArgSpec("角色ID", "role"),
            ArgSpec("武功栏位", "int"),
            ArgSpec("武功ID", "magic"),
            ArgSpec("经验", "int")
        ),
        36 to listOf(ArgSpec("性别(0男1女)", "int"), ArgSpec("是→跳转", "jump"), ArgSpec("否→跳转", "jump")),
        37 to listOf(ArgSpec("道德增量", "int")),
        38 to listOf(
            ArgSpec("场景号", "scene"),
            ArgSpec("图层", "int"),
            ArgSpec("旧贴图", "int"),
            ArgSpec("新贴图", "int")
        ),
        39 to listOf(ArgSpec("场景号", "scene")),
        40 to listOf(ArgSpec("面向(0-3)", "face")),
        41 to listOf(ArgSpec("角色ID", "role"), ArgSpec("物品ID", "item"), ArgSpec("数量", "int")),
        42 to listOf(ArgSpec("有→跳转", "jump"), ArgSpec("无→跳转", "jump")),
        43 to listOf(ArgSpec("物品ID", "item"), ArgSpec("有→跳转", "jump"), ArgSpec("无→跳转", "jump")),
        44 to listOf(
            ArgSpec("事件1", "int"),
            ArgSpec("起始图1", "int"),
            ArgSpec("结束图1", "int"),
            ArgSpec("事件2", "int"),
            ArgSpec("起始图2", "int"),
            ArgSpec("结束图2", "int")
        ),
        45 to listOf(ArgSpec("角色ID", "role"), ArgSpec("轻功增量", "int")),
        46 to listOf(ArgSpec("角色ID", "role"), ArgSpec("内力增量", "int")),
        47 to listOf(ArgSpec("角色ID", "role"), ArgSpec("攻击增量", "int")),
        48 to listOf(ArgSpec("角色ID", "role"), ArgSpec("生命增量", "int")),
        49 to listOf(ArgSpec("角色ID", "role"), ArgSpec("内力属性(0阴1阳2调和)", "mp_type")),
        50 to listOf(
            ArgSpec("子码 code", "int"),
            ArgSpec("e1", "int"),
            ArgSpec("e2", "int"),
            ArgSpec("e3", "int"),
            ArgSpec("e4", "int"),
            ArgSpec("e5", "int"),
            ArgSpec("e6", "int")
        ),
        55 to listOf(
            ArgSpec("事件号", "int"),
            ArgSpec("贴图值", "int"),
            ArgSpec("相等→跳转", "jump"),
            ArgSpec("不等→跳转", "jump")
        ),
        56 to listOf(ArgSpec("声望增量", "int")),
        60 to listOf(
            ArgSpec("场景号", "scene"),
            ArgSpec("事件号", "int"),
            ArgSpec("贴图", "int"),
            ArgSpec("存在→跳转", "jump"),
            ArgSpec("不存在→跳转", "jump")
        ),
        61 to listOf(ArgSpec("目标脚本相对偏移?", "jump"), ArgSpec("备用", "int")),
        63 to listOf(ArgSpec("角色ID", "role"), ArgSpec("性别", "int")),
        66 to listOf(ArgSpec("音乐号", "int")),
        67 to listOf(ArgSpec("音效号", "int")),
        68 to listOf(
            ArgSpec("头像ID", "head"),
            ArgSpec("对话ID", "talk"),
            ArgSpec("姓名条目(-2=跟头像)", "name"),
            ArgSpec("位置(0左1右…)", "int"),
            ArgSpec("是否显示头像(0显示)", "flag"),
            ArgSpec("颜色/色板", "int"),
            ArgSpec("边框", "int")
        ),
        69 to listOf(ArgSpec("类型", "int"), ArgSpec("目标ID", "int"), ArgSpec("新名称条目", "name")),
        70 to listOf(ArgSpec("标题对话ID?", "talk"), ArgSpec("参数", "int")),
        71 to listOf(ArgSpec("场景号", "scene"), ArgSpec("X", "int"), ArgSpec("Y", "int")),
        83 to listOf()
    )

    fun opcodeDisplayName(opcode: Int): String {
        if (opcode < 0) return "结束(END)"
        return OPCODE_ZH[opcode] ?: "未知指令($opcode)"
    }

    fun formatOpcodeChoice(opcode: Int): String {
        return "$opcode — ${opcodeDisplayName(opcode)}"
    }

    fun parseOpcodeChoice(text: String): Int {
        val trimmed = text.trim()
        val match = Regex("^(-?\\d+)").find(trimmed)
            ?: throw IllegalArgumentException("无法解析 opcode: $trimmed")
        return match.groupValues[1].toInt()
    }

    fun knownOpcodes(): List<Int> {
        val ops = (OPCODE_ZH.keys + OPCODE_ARGC.keys + setOf(-1)).toSortedSet()
        return ops.toList()
    }

    fun defaultArgsForOpcode(opcode: Int): IntArray {
        val presets = mapOf(
            1 to intArrayOf(1, 0, 0),
            2 to intArrayOf(0, 1),
            4 to intArrayOf(0, 1, 1),
            5 to intArrayOf(1, 1),
            6 to intArrayOf(0, 0, 0, 1),
            9 to intArrayOf(1, 1),
            11 to intArrayOf(1, 1),
            16 to intArrayOf(0, 1, 1),
            18 to intArrayOf(0, 1, 1),
            20 to intArrayOf(1, 1),
            68 to intArrayOf(0, 1, -2, 0, 0, 28515, 0)
        )
        presets[opcode]?.let { return it.copyOf() }
        if (opcode < 0) return intArrayOf()
        val argc = OPCODE_ARGC[opcode] ?: 0
        return IntArray(argc)
    }

    fun argSpecs(opcode: Int): List<ArgSpec> {
        return OPCODE_ARGS[opcode] ?: emptyList()
    }

    private fun clip(text: String, n: Int = 80): String {
        val t = text.replace("\n", " ").replace("\r", " ").trim()
        return if (t.length <= n) t else t.substring(0, n - 1) + "…"
    }

    private fun formatPicCode(value: Int): String {
        if (value == -2) return "保持原值(-2)"
        if (value == 0) return "清除贴图(0)"
        val smp = value / 2
        return if (value % 2 == 0) "贴图帧 #$smp(偶数)" else "贴图帧 #$smp(奇数/翻转?)"
    }

    fun resolveArgValue(ctx: OpcodeContext?, kind: String, value: Int): String {
        if (value == -2 && kind in setOf("scene", "int", "jump", "name")) {
            return "特殊值 -2（常=当前场景/事件/自动）"
        }
        if (value == -1 && kind in setOf("item", "role", "magic", "battle", "talk")) {
            return "无(-1)"
        }

        try {
            when (kind) {
                "talk" -> if (ctx != null && value >= 0) {
                    val t = ctx.talkGetText(value)
                    if (t != null) return "对话「${clip(t)}」"
                }
                "name" -> if (ctx != null && value > 0) {
                    val n = ctx.nameGetText(value)
                    if (n != null) return "姓名「${clip(n, 40)}」"
                }
                "item" -> if (ctx != null && value >= 0) {
                    val n = ctx.itemName(value)
                    return if (n != null) "物品「$n」(#$value)" else "物品#$value"
                }
                "role" -> if (ctx != null && value >= 0) {
                    val n = ctx.roleName(value)
                    return if (n != null) "角色「$n」(#$value)" else "角色#$value"
                }
                "magic" -> if (ctx != null && value >= 0) {
                    val n = ctx.magicName(value)
                    return if (n != null) "武功「$n」(#$value)" else "武功#$value"
                }
                "scene" -> if (ctx != null && value >= 0) {
                    val n = ctx.sceneName(value)
                    return if (n != null) "场景「$n」(#$value)" else "场景#$value"
                }
                "battle" -> if (ctx != null && value >= 0) {
                    val n = ctx.battleName(value)
                    return if (n != null) "战斗「$n」(BattleNum=$value)" else "战斗#$value"
                }
                "head" -> return "头像帧 #$value（Heads.Pic）"
                "jump" -> return "相对跳过 $value 个「字」(int16)，从本指令结束后的下一条起始处再偏移（不是地图坐标，也不是跳过 N 条指令）"
                "face" -> {
                    val faces = mapOf(0 to "左", 1 to "上", 2 to "右", 3 to "下")
                    return "面向 ${faces[value] ?: value}"
                }
                "mp_type" -> return mapOf(0 to "阴性", 1 to "阳性", 2 to "调和")[value] ?: value.toString()
                "flag" -> return if (value != 0) "是/开" else "否/关"
                "pic" -> return formatPicCode(value)
            }
        } catch (e: Exception) {
            return "(解析失败: ${e.message})"
        }
        return value.toString()
    }

    fun formatArgsTooltip(ctx: OpcodeContext?, opcode: Int, args: IntArray): String {
        val specs = argSpecs(opcode)
        val lines = mutableListOf("指令 $opcode: ${opcodeDisplayName(opcode)}", "")
        if (opcode < 0) {
            lines.add("脚本结束标记（opcode < 0）")
            return lines.joinToString("\n")
        }
        if (specs.isEmpty()) {
            if (args.isEmpty()) {
                lines.add("无参数")
            } else {
                lines.add("参数（未登记释义，按原始数值）:")
                args.forEachIndexed { i, a -> lines.add("  [$i] = $a") }
            }
            return lines.joinToString("\n")
        }

        lines.add("参数释义:")
        specs.forEachIndexed { i, spec ->
            val argVal = if (i < args.size) args[i] else null
            if (argVal != null) {
                val resolved = resolveArgValue(ctx, spec.kind, argVal)
                lines.add("  [$i] ${spec.name} = $argVal")
                if (resolved != argVal.toString()) {
                    lines.add("       → $resolved")
                }
            } else {
                lines.add("  [$i] ${spec.name} = (缺省)")
            }
        }
        if (args.size > specs.size) {
            lines.add("多余参数: " + args.drop(specs.size).joinToString(","))
        }
        if (opcode == 6 && args.size >= 3) {
            val win = args[1]
            val lose = args[2]
            lines.add("")
            lines.add("跳转落点（相对本指令之后）:")
            lines.add("  胜利: 再跳过 $win 字 → 常用于跳过紧随其后的「游戏失败」")
            lines.add("  失败: 再跳过 $lose 字 → 0 表示直接执行下一条（多为游戏失败）")
            lines.add("  典型写法: 战斗(…, 3, 0, …) / 游戏失败 / …胜利剧情…")
        }
        return lines.joinToString("\n")
    }

    fun formatNameTooltip(opcode: Int): String {
        val eng = OPCODE_NAMES[opcode] ?: ""
        val zh = opcodeDisplayName(opcode)
        val specs = argSpecs(opcode)
        val lines = mutableListOf(zh)
        if (eng.isNotEmpty()) lines.add("英文标识: $eng")
        lines.add("Opcode: $opcode")
        if (specs.isNotEmpty()) {
            lines.add("参数: " + specs.joinToString(", ") { it.name })
        } else if (opcode >= 0) {
            lines.add("无参数")
        }
        return lines.joinToString("\n")
    }
}
