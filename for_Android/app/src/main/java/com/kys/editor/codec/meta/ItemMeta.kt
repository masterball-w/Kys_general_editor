package com.kys.editor.codec.meta

import com.kys.editor.codec.EditorCompat

object ItemMeta {
    val ITEM_TYPES: Map<Int, String> = mapOf(
        0 to "剧情物品",
        1 to "神兵宝甲(装备)",
        2 to "武功秘笈",
        3 to "灵丹妙药",
        4 to "伤人暗器"
    )

    val EQUIP_TYPES: Map<Int, String> = mapOf(
        -1 to "(非装备/不限)",
        0 to "武器",
        1 to "身披",
        2 to "头戴",
        3 to "脚踩",
        4 to "第五装备位"
    )

    val NEED_SEX: Map<Int, String> = mapOf(
        -1 to "不限",
        0 to "仅男",
        1 to "仅女"
    )

    val NEED_MP_TYPES: Map<Int, String> = mapOf(
        -1 to "不限",
        0 to "需阴内",
        1 to "需阳内",
        2 to "需调和/不限"
    )

    val CHANGE_MP_TYPES: Map<Int, String> = mapOf(
        -1 to "不变",
        0 to "改为阴",
        1 to "改为阳",
        2 to "改为调和"
    )

    val BATTLE_STATES: Map<Int, String> get() = MagicMeta.BATTLE_STATES

    fun itemTypeDisplay(v: Int): String = ITEM_TYPES[v] ?: "未知类型($v)"
    fun equipTypeDisplay(v: Int): String = EQUIP_TYPES[v] ?: "部位($v)"
    fun needSexDisplay(v: Int): String = NEED_SEX[v] ?: "未知($v)"
    fun needMpTypeDisplay(v: Int): String = NEED_MP_TYPES[v] ?: "未知($v)"
    fun changeMpTypeDisplay(v: Int): String = CHANGE_MP_TYPES[v] ?: "未知($v)"

    fun equipTypesForCompat(compat: EditorCompat?): Map<Int, String> {
        if (compat == null || compat.itemHatShoesEquip) {
            return EQUIP_TYPES
        }
        return EQUIP_TYPES.filterKeys { it in setOf(-1, 0, 1) }
    }

    fun itemSummary(rec: IntArray, name: String = ""): String {
        val t = if (rec.size > 41) rec[41] else 0
        return "$name  [${itemTypeDisplay(t)}]"
    }
}
