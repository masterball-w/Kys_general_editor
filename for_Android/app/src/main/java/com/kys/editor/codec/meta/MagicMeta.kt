package com.kys.editor.codec.meta

import kotlin.math.pow
import kotlin.math.roundToInt

data class ModulusField(
    val wordIndex: Int,
    val label: String,
    val engName: String,
    val description: String
)

object MagicMeta {
    val MAGIC_TYPES: Map<Int, String> = mapOf(
        0 to "未分类",
        1 to "外功·拳掌",
        2 to "外功·剑术",
        3 to "外功·刀法",
        4 to "外功·奇门",
        5 to "内功"
    )

    val HURT_TYPES: Map<Int, String> = mapOf(
        0 to "伤气血",
        1 to "伤内力(吸星)"
    )

    val ATT_AREA_TYPES: Map<Int, String> = mapOf(
        0 to "点/菱形(目标系)",
        1 to "方向直线",
        2 to "十字/米字(原地系)",
        3 to "方形·面(目标/原地)",
        4 to "方向菱形",
        5 to "方向方形",
        6 to "远程点",
        7 to "无定向直线"
    )

    val MODULUS_FIELDS: List<ModulusField> = listOf(
        ModulusField(21, "攻击型", "AttackModulus", "伤害与攻击力差挂钩 (权重×6)"),
        ModulusField(22, "内力型", "MPModulus", "伤害与当前内力差挂钩 (权重×1)"),
        ModulusField(23, "轻功型", "SpeedModulus", "伤害与轻功差挂钩 (权重×2)"),
        ModulusField(24, "兵器型", "WeaponModulus", "伤害与当前兵器(拳/剑/刀/奇)差挂钩 (权重×2)")
    )

    val BATTLE_STATES: Map<Int, String> = mapOf(
        0 to "(无)",
        1 to "体力不减",
        2 to "女性武功威力加成",
        3 to "饮酒功效加倍",
        4 to "随机伤害转移",
        5 to "随机伤害反噬",
        6 to "内伤免疫",
        7 to "杀伤体力",
        8 to "增加闪躲几率",
        9 to "攻击力随等级循环增减",
        10 to "内力消耗减少",
        11 to "每回合恢复生命",
        12 to "负面状态免疫",
        13 to "全部武功威力加成",
        14 to "随机二次攻击",
        15 to "拳掌武功威力加成",
        16 to "剑术武功威力加成",
        17 to "刀法武功威力加成",
        18 to "奇门武功威力加成",
        19 to "增加内伤几率",
        20 to "增加封穴几率",
        21 to "攻击微量吸血",
        22 to "攻击距离增加",
        23 to "每回合恢复内力",
        24 to "使用暗器距离增加",
        25 to "附加杀伤吸收内力",
        26 to "每回合提高攻击",
        27 to "令附近敌人中毒",
        28 to "大幅提升医疗和解毒的效果"
    )

    val CLASSIC_HURT_LEVEL_WORDS: IntArray = intArrayOf(18, 19, 20, 21, 22, 23, 24, 25, 26, 27)

    const val GROWTH_CURVE_HELP: String =
        "成长曲线 HurtModulus → 引擎 CalNewHurtValue（战斗实际用等级-1 作为 lv）\n" +
        "\n" +
        "  p = HurtModulus / 1000\n" +
        "  n = (MaxHurt − MinHurt)^(1/p) / 9\n" +
        "  威力(等级) = round( ((等级−1) × n)^p ) + MinHurt\n" +
        "\n" +
        "其中等级取 1…10；一级时 lv=0 → 结果恒为 MinHurt。\n" +
        "HurtModulus=0 时按 100 处理。\n" +
        "HurtModulus 越大后期越陡；=1000 时接近线性增长。\n" +
        "最终战斗伤害还会再乘武学常识等修正，此处仅为招式基础威力。"

    fun calNewHurtValue(lv: Int, minVal: Int, maxVal: Int, proportion: Int): Int {
        var prop = proportion
        if (prop == 0) prop = 100
        val p = prop / 1000.0
        if (maxVal <= minVal) return minVal
        val n = ((maxVal - minVal).toDouble().pow(1.0 / p)) / 9.0
        return ((lv * n).pow(p)).roundToInt() + minVal
    }

    fun hurtTable(minVal: Int, maxVal: Int, proportion: Int): List<Pair<Int, Int>> {
        return (0 until 10).map { lv ->
            (lv + 1) to calNewHurtValue(lv, minVal, maxVal, proportion)
        }
    }

    fun dominantModulus(att: Int, mp: Int, spd: Int, wpn: Int): String {
        val weighted = listOf(
            "攻击型" to (att * 6),
            "内力型" to mp,
            "轻功型" to (spd * 2),
            "兵器型" to (wpn * 2)
        )
        val total = weighted.sumOf { it.second }
        if (total <= 0) return "无加成权重"
        val parts = weighted.filter { it.second > 0 }.map { "${it.first}${it.second}/$total" }
        val top = weighted.maxByOrNull { it.second }!!
        return "主导:${top.first}  (${parts.joinToString(", ")})"
    }

    fun modulusSummary(rec: IntArray): String {
        return dominantModulus(
            if (rec.size > 21) rec[21] else 0,
            if (rec.size > 22) rec[22] else 0,
            if (rec.size > 23) rec[23] else 0,
            if (rec.size > 24) rec[24] else 0
        )
    }

    fun powerAtLevel(rec: IntArray, level1Based: Int): Pair<Int, Int> {
        val lv = maxOf(0, minOf(9, level1Based - 1))
        val minH = if (rec.size > 18) rec[18] else 0
        val maxH = if (rec.size > 19) rec[19] else 0
        val mod = if (rec.size > 20) rec[20] else 0
        return calNewHurtValue(lv, minH, maxH, mod) to lv
    }

    fun labelMagicType(v: Int): String = MAGIC_TYPES[v] ?: "未知($v)"
    fun labelHurtType(v: Int): String = HURT_TYPES[v] ?: "未知($v)"
    fun labelAttArea(v: Int): String = ATT_AREA_TYPES[v] ?: "未知($v)"
    fun labelBattleState(v: Int): String = BATTLE_STATES[v] ?: "自定义($v)"

    fun categoryDisplay(rec: IntArray): String {
        val mt = if (rec.size > 12) rec[12] else 0
        val ht = if (rec.size > 14) rec[14] else 0
        val base = labelMagicType(mt)
        if (ht == 1 && mt != 5) return "$base · 吸星(伤内力)"
        return base
    }
}
