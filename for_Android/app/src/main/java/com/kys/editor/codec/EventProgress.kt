package com.kys.editor.codec

private val LAYOUT_WORDS = setOf(9, 10)

fun eventRowEqual(a: IntArray, b: IntArray): Boolean {
    val n = minOf(a.size, b.size, EVENT_WORDS)
    for (i in 0 until n) {
        if (a[i] != b[i]) return false
    }
    return true
}

fun eventRuntimeChanged(
    templateEv: IntArray,
    currentEv: IntArray,
    ignoreLayout: Boolean = true
): Boolean {
    for (w in 0 until EVENT_WORDS) {
        if (ignoreLayout && w in LAYOUT_WORDS) continue
        if (w >= templateEv.size || w >= currentEv.size) return true
        if (templateEv[w] != currentEv[w]) return true
    }
    return false
}

fun eventProgressFlag(
    template: SceneEventData?,
    current: SceneEventData?,
    scene: Int,
    eventId: Int
): Int {
    if (template == null || current == null) return -1
    if (scene >= template.sceneCount || scene >= current.sceneCount) return -1
    if (eventId < 0 || eventId >= EVENTS_PER_SCENE) return -1
    val tpl = template.scenes[scene][eventId]
    val cur = current.scenes[scene][eventId]
    return if (eventRuntimeChanged(tpl, cur)) 1 else 0
}

fun formatConditionHint(condition: Int): String {
    val c = condition
    return when (c) {
        0 -> "条件=0（可自动执行：踩上脚本[4]>0 时引擎会跑）"
        1 -> "条件=1（常见初始/挂接态，非「未发生」专用位）"
        else -> "条件=$c"
    }
}

fun progressFileLabels(slot: Int): Pair<String, String> {
    return if (slot <= 0) {
        "alldef.grp" to "allsin.grp"
    } else {
        "D$slot.grp" to "S$slot.grp"
    }
}
