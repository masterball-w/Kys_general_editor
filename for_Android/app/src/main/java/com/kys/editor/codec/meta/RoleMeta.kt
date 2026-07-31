package com.kys.editor.codec.meta

object RoleMeta {
    val SEXUAL: Map<Int, String> = mapOf(
        0 to "男",
        1 to "女"
    )

    val MP_TYPES: Map<Int, String> = mapOf(
        0 to "阴",
        1 to "阳",
        2 to "调和"
    )

    val EQUIP_SLOTS: Map<Int, String> = mapOf(
        0 to "武器",
        1 to "身披",
        2 to "头戴",
        3 to "脚踩",
        4 to "第五装备位"
    )

    fun asU16(signedWord: Int): Int = signedWord and 0xFFFF

    fun toI16FromU16(value: Int): Short {
        val v = value and 0xFFFF
        return if (v >= 0x8000) (v - 0x10000).toShort() else v.toShort()
    }

    fun sexualDisplay(v: Int): String = SEXUAL[v] ?: "未知($v)"
    fun mpTypeDisplay(v: Int): String = MP_TYPES[v] ?: "未知($v)"
    fun equipSlotDisplay(v: Int): String = EQUIP_SLOTS[v] ?: "未知($v)"

    fun roleSummary(rec: IntArray, name: String = ""): String {
        val lv = if (rec.size > 15) rec[15] else 0
        val sex = sexualDisplay(if (rec.size > 14) rec[14] else 0)
        return "$name  Lv$lv $sex"
    }
}
