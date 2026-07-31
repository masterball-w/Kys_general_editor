package com.kys.editor.codec

data class RangerLayout(
    val roleWords: Int = ROLE_WORDS,
    val itemWords: Int = ITEM_WORDS,
    val sceneWords: Int = SCENE_WORDS,
    val magicWords: Int = MAGIC_WORDS,
    val shopWords: Int = SHOP_WORDS,
    val inventorySlots: Int = INVENTORY_SLOTS,
    val teamOffset: Int = PROMISE_TEAM_OFFSET,
    val teamCount: Int = MAX_TEAM_SLOTS,
    val moneyOffset: Int = -1,
    val inventoryBase: Int = 42
) {
    val hasMoneyWord: Boolean get() = moneyOffset >= 0

    companion object {
        const val ROLE_WORDS = 91
        const val ITEM_WORDS = 95
        const val SCENE_WORDS = 26
        const val MAGIC_WORDS = 111
        const val SHOP_WORDS = 18
        const val INVENTORY_SLOTS = 400

        const val PROMISE_TEAM_OFFSET = 30
        const val CLASSIC_TEAM_OFFSET = 24
        const val STANDARD_TEAM_OFFSET = PROMISE_TEAM_OFFSET
        const val MAX_TEAM_SLOTS = 6

        fun fromProfile(p: GameProfile) = RangerLayout(
            roleWords = p.roleWords,
            itemWords = p.itemWords,
            sceneWords = p.sceneWords,
            magicWords = p.magicWords,
            shopWords = p.shopWords,
            inventorySlots = p.inventorySlots,
            teamOffset = p.rangerTeamOffset,
            teamCount = p.rangerTeamCount,
            moneyOffset = p.rangerMoneyOffset,
            inventoryBase = p.rangerInventoryBase
        )
    }
}

data class RangerHeaderLayout(
    val teamOffset: Int = RangerLayout.STANDARD_TEAM_OFFSET,
    val teamCount: Int = RangerLayout.MAX_TEAM_SLOTS,
    val moneyOffset: Int = -1,
    val inventoryBase: Int = 42
) {
    val hasMoneyWord: Boolean get() = moneyOffset >= 0
}

private fun scoreTeamBlock(header: ByteArray, teamOff: Int, count: Int, roleCount: Int): Int {
    var score = 0
    for (i in 0 until count) {
        val off = teamOff + i * 2
        if (off + 2 > header.size) return -999
        val lo = header[off].toInt() and 0xFF
        val hi = header[off + 1].toInt() and 0xFF
        val v = (lo or (hi shl 8)).let { if (it >= 0x8000) it - 0x10000 else it }
        when {
            v == -1 -> score += 4
            v == 0 -> score += 1
            v in 1 until roleCount -> score += 5
            v > 0 -> score -= 3
        }
    }
    return score
}

private fun teamCountBeforeInventory(invBase: Int, moneyOffset: Int): Int {
    return RangerLayout.MAX_TEAM_SLOTS
}

private fun teamOffsetCandidates(roleOffset: Int, magicWords: Int): IntArray {
    if (roleOffset == 836 && magicWords == 68) {
        return intArrayOf(RangerLayout.CLASSIC_TEAM_OFFSET, RangerLayout.PROMISE_TEAM_OFFSET)
    }
    if (roleOffset >= 1600 && magicWords in intArrayOf(111, 93)) {
        return intArrayOf(RangerLayout.PROMISE_TEAM_OFFSET)
    }
    return intArrayOf(RangerLayout.CLASSIC_TEAM_OFFSET, RangerLayout.PROMISE_TEAM_OFFSET)
}

fun probeRangerHeaderLayout(
    roleOffset: Int,
    magicWords: Int,
    header: ByteArray,
    roleCount: Int = 300
): RangerHeaderLayout {
    val hdr = header.copyOf(maxOf(64, minOf(header.size, roleOffset)))
    var bestScore = -1000000000
    var best = RangerHeaderLayout()

    val invOptions = mutableListOf<Pair<Int, Int>>()
    for (invBase in 32 until 52 step 2) {
        val nbytes = roleOffset - invBase
        if (nbytes < 0 || nbytes % 4 != 0) continue
        invOptions.add(invBase to nbytes / 4)
    }

    for ((invBase, slots) in invOptions) {
        var slotScore = 0
        if (slots == 200 && magicWords == 68) slotScore += 25
        if (slots in intArrayOf(400, 401) && magicWords in intArrayOf(111, 93)) slotScore += 25
        if (slots in intArrayOf(198, 199, 200, 201, 400, 401)) slotScore += 8

        val moneyCandidates: IntArray = when (invBase) {
            44 -> intArrayOf(42, -1)
            46 -> intArrayOf(44, -1)
            else -> intArrayOf(-1)
        }

        for (moneyOff in moneyCandidates) {
            if (moneyOff >= 0 && moneyOff + 2 > invBase) continue
            val teamCnt = teamCountBeforeInventory(invBase, moneyOff)
            if (teamCnt <= 0) continue
            for (teamOff in teamOffsetCandidates(roleOffset, magicWords)) {
                val ts = scoreTeamBlock(hdr, teamOff, teamCnt, roleCount)
                var total = slotScore + ts
                if (invBase == 42 && slots in intArrayOf(400, 401) && teamCnt == 6) total += 12
                if (invBase == 44 && slots in intArrayOf(198, 199, 200) && moneyOff == 42 &&
                    teamCnt == 6 && teamOff == RangerLayout.CLASSIC_TEAM_OFFSET) total += 25
                if (roleOffset == 836 && invBase == 44 && moneyOff == 42) total += 30
                if (roleOffset == 836 && invBase == 36) total -= 60
                if (invBase == 44 && slots in intArrayOf(198, 199, 200) && moneyOff == 42 &&
                    teamCnt == 6 && teamOff == RangerLayout.PROMISE_TEAM_OFFSET) total += 5
                if (invBase == 36 && teamOff == RangerLayout.PROMISE_TEAM_OFFSET) total -= 40
                if (roleOffset == 836 && teamOff == RangerLayout.PROMISE_TEAM_OFFSET) total -= 30
                if (total > bestScore) {
                    bestScore = total
                    best = RangerHeaderLayout(
                        teamOffset = teamOff,
                        teamCount = teamCnt,
                        moneyOffset = moneyOff,
                        inventoryBase = invBase
                    )
                }
            }
        }
    }
    return best
}

fun probeRangerInventoryBase(roleOffset: Int, magicWords: Int): Int {
    val layout = probeRangerHeaderLayout(roleOffset, magicWords, ByteArray(64) { 0xFF.toByte() })
    return layout.inventoryBase
}
