package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import com.kys.editor.util.putLe32
import com.kys.editor.util.s_le16

val OPCODE_ARGC: Map<Int, Int> = mapOf(
    0 to 0, 1 to 3, 2 to 2, 3 to 13, 4 to 3, 5 to 2, 6 to 4, 7 to 0, 8 to 1, 9 to 2,
    10 to 1, 11 to 2, 12 to 0, 13 to 0, 14 to 0, 15 to 0, 16 to 3, 17 to 5, 18 to 3, 19 to 2,
    20 to 2, 21 to 1, 22 to 0, 23 to 2, 24 to 0, 25 to 4, 26 to 5, 27 to 3, 28 to 5, 29 to 5,
    30 to 4, 31 to 3, 32 to 2, 33 to 3, 34 to 2, 35 to 4, 36 to 3, 37 to 1, 38 to 4, 39 to 1,
    40 to 1, 41 to 3, 42 to 2, 43 to 3, 44 to 6, 45 to 2, 46 to 2, 47 to 2, 48 to 2, 49 to 2,
    50 to 7, 51 to 0, 52 to 0, 53 to 0, 54 to 0, 55 to 4, 56 to 1, 57 to 0, 58 to 0, 59 to 0,
    60 to 5, 61 to 2, 62 to 0, 63 to 2, 64 to 0, 65 to 0, 66 to 1, 67 to 1, 68 to 7, 69 to 3,
    70 to 2, 71 to 3,
    83 to 0,
)

val OPCODE_NAMES: Map<Int, String> = mapOf(
    0 to "Redraw",
    1 to "Dialogue",
    2 to "AddItem",
    3 to "ModifyEvent",
    4 to "HaveItem?",
    5 to "AskBattle",
    6 to "Battle",
    7 to "Break",
    8 to "ChangeFace",
    9 to "AskJoin?",
    10 to "Join",
    11 to "AskRest?",
    12 to "Rest",
    13 to "FadeIn",
    14 to "FadeOut",
    15 to "GameFail",
    16 to "InTeam?",
    17 to "SetSceneTile",
    18 to "HaveItemAmt?",
    19 to "Teleport",
    20 to "TeamFull?",
    21 to "Leave",
    22 to "ZeroMP",
    23 to "UsePoi",
    25 to "PanCamera",
    26 to "AddEventParam",
    27 to "Animate",
    28 to "Morality?",
    29 to "Attack?",
    30 to "Walk",
    31 to "Money?",
    32 to "AddItemSilent",
    33 to "LearnMagic",
    34 to "AddAptitude",
    35 to "SetMagicSlot",
    36 to "Sexual?",
    37 to "AddMorality",
    38 to "ChangePic",
    39 to "OpenScene",
    40 to "SetFace",
    41 to "TakingItem",
    42 to "FemaleInTeam?",
    43 to "SubFunc",
    44 to "DualAnimate",
    45 to "AddSpeed",
    46 to "AddMP",
    47 to "AddAttack",
    48 to "AddHP",
    49 to "SetMPType",
    50 to "50e",
    51 to "SoftStarTalk",
    52 to "ShowMorality",
    53 to "ShowFame",
    54 to "Huashan",
    55 to "EventPic?",
    56 to "AddFame",
    58 to "AllLeave",
    59 to "Shake",
    60 to "EventExist?",
    61 to "Jump",
    62 to "GameEnd",
    63 to "SetSexual",
    64 to "Shop",
    66 to "PlayMusic",
    67 to "PlaySound",
    68 to "NewTalk",
    69 to "ReSetName",
    70 to "ShowTitle",
    71 to "JmpScene",
    83 to "Nop83",
)

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
    83 to "扩展空操作(83)",
)

data class Instruction(
    val opcode: Int,
    val args: List<Int> = emptyList(),
    val pc: Int = 0
) {
    val name: String
        get() = if (opcode < 0) "END" else OPCODE_NAMES.getOrDefault(opcode, "Op$opcode")

    val nameZh: String
        get() = if (opcode < 0) "结束" else OPCODE_ZH.getOrDefault(opcode, "未知指令($opcode)")

    fun toWords(): List<Int> {
        return if (opcode < 0) listOf(opcode) else listOf(opcode) + args
    }
}

class Script(
    val scriptId: Int,
    var instructions: MutableList<Instruction> = mutableListOf(),
    var rawWords: MutableList<Int> = mutableListOf()
) {
    fun disassemble(): List<Instruction> {
        val words = rawWords
        var pc = 0
        val out: MutableList<Instruction> = mutableListOf()
        while (pc < words.size) {
            val op = words[pc]
            if (op < 0) {
                out.add(Instruction(op, emptyList(), pc))
                break
            }
            val argc = OPCODE_ARGC[op]
            if (argc == null) {
                out.add(Instruction(op, emptyList(), pc))
                pc += 1
                continue
            }
            val endIdx = minOf(pc + 1 + argc, words.size)
            val args = words.subList(pc + 1, endIdx).toMutableList()
            while (args.size < argc) args.add(0)
            out.add(Instruction(op, args.toList(), pc))
            pc += 1 + argc
        }
        instructions = out
        return out
    }

    fun assemble(): List<Int> {
        val words: MutableList<Int> = mutableListOf()
        for (ins in instructions) {
            if (ins.opcode < 0) {
                words.add(ins.opcode)
                break
            }
            val argc = OPCODE_ARGC.getOrDefault(ins.opcode, 0)
            val args = ins.args.take(argc).toMutableList()
            while (args.size < argc) args.add(0)
            words.add(ins.opcode)
            words.addAll(args.take(argc))
        }
        if (words.isEmpty() || words.last() >= 0) {
            words.add(-1)
        }
        rawWords = words
        return words
    }
}

class KdefArchive {
    var idxNode: VfsNode? = null
    var grpNode: VfsNode? = null
    var offsets: IntArray = intArrayOf()
    var words: ShortArray = shortArrayOf()

    val scriptCount: Int
        get() = offsets.size

    fun load(resourceDir: VfsNode) {
        var idx: VfsNode? = null
        var grp: VfsNode? = null
        for (n in listOf("Kdef.idx", "kdef.idx")) {
            val child = resourceDir.child(n)
            if (child.exists()) {
                idx = child
                break
            }
        }
        for (n in listOf("Kdef.grp", "kdef.grp")) {
            val child = resourceDir.child(n)
            if (child.exists()) {
                grp = child
                break
            }
        }
        if (idx == null || grp == null) {
            throw java.io.FileNotFoundException("Kdef.idx/grp not found")
        }
        idxNode = idx
        grpNode = grp

        val idxData = idx.readBytes()
        val grpData = grp.readBytes()

        val idxCount = idxData.size / 4
        offsets = IntArray(idxCount)
        val idxBuf = idxData.leBuffer()
        for (i in 0 until idxCount) {
            offsets[i] = idxBuf.le32(i * 4)
        }

        val wordCount = grpData.size / 2
        words = ShortArray(wordCount)
        val grpBuf = grpData.leBuffer()
        for (i in 0 until wordCount) {
            words[i] = grpBuf.s_le16(i * 2)
        }
    }

    fun getScript(scriptId: Int): Script {
        if (scriptId <= 0 || scriptId > offsets.size) {
            throw IndexOutOfBoundsException("scriptId: $scriptId")
        }
        val startByte = offsets[scriptId - 1]
        val endByte = if (scriptId < offsets.size) {
            offsets[scriptId]
        } else {
            words.size * 2
        }
        val start = startByte / 2
        val end = endByte / 2
        val raw = mutableListOf<Int>()
        for (i in start until minOf(end, words.size)) {
            raw.add(words[i].toInt())
        }
        val script = Script(scriptId, rawWords = raw)
        script.disassemble()
        return script
    }

    fun setScript(script: Script) {
        val scripts: MutableList<List<Int>> = mutableListOf()
        for (sid in 1..scriptCount) {
            if (sid == script.scriptId) {
                scripts.add(script.assemble())
            } else {
                scripts.add(getScript(sid).rawWords)
            }
        }
        rebuild(scripts)
    }

    fun appendScript(words: List<Int>): Int {
        val wordList = words.toMutableList()
        if (wordList.isEmpty() || wordList.last() >= 0) {
            wordList.add(-1)
        }
        val scripts = mutableListOf<List<Int>>()
        for (sid in 1..scriptCount) {
            scripts.add(getScript(sid).rawWords)
        }
        scripts.add(wordList)
        rebuild(scripts)
        return scripts.size
    }

    private fun rebuild(scripts: List<List<Int>>) {
        val newOffsets = mutableListOf<Int>()
        val allWords = mutableListOf<Short>()
        var cursor = if (offsets.isNotEmpty()) offsets[0] else 0
        if (cursor == 4 && allWords.isEmpty()) {
            if (words.isNotEmpty()) {
                allWords.add(words[0])
                allWords.add(words.getOrElse(1) { 0 })
            } else {
                allWords.add(0)
                allWords.add(0)
            }
            cursor = 4
        }
        for (body in scripts) {
            newOffsets.add(cursor)
            for (w in body) {
                allWords.add(w.toShort())
            }
            cursor += body.size * 2
        }
        offsets = newOffsets.toIntArray()
        words = allWords.toShortArray()
    }

    fun toIdxBytes(): ByteArray {
        val buf = leBuffer(offsets.size * 4)
        for (i in offsets.indices) {
            buf.putLe32(i * 4, offsets[i])
        }
        return buf.array()
    }

    fun toGrpBytes(): ByteArray {
        val buf = leBuffer(words.size * 2)
        for (i in words.indices) {
            buf.putLe16(i * 2, words[i])
        }
        return buf.array()
    }

    fun save(backup: Boolean = true) {
        val idx = idxNode ?: throw IllegalStateException("not loaded")
        val grp = grpNode ?: throw IllegalStateException("not loaded")
        idx.writeBytes(toIdxBytes())
        grp.writeBytes(toGrpBytes())
    }

    fun findBattleRefs(battleId: Int): List<Int> {
        val hits = mutableListOf<Int>()
        for (sid in 1..scriptCount) {
            val script = getScript(sid)
            for (ins in script.instructions) {
                if (ins.opcode == 6 && ins.args.isNotEmpty() && ins.args[0] == battleId) {
                    hits.add(sid)
                    break
                }
            }
        }
        return hits
    }

    companion object {
        fun findIdx(resourceDir: VfsNode): VfsNode {
            for (name in listOf("Kdef.idx", "kdef.idx")) {
                val child = resourceDir.child(name)
                if (child.exists()) return child
            }
            error("Kdef.idx not found")
        }

        fun findGrp(resourceDir: VfsNode): VfsNode {
            for (name in listOf("Kdef.grp", "kdef.grp")) {
                val child = resourceDir.child(name)
                if (child.exists()) return child
            }
            error("Kdef.grp not found")
        }
    }
}
