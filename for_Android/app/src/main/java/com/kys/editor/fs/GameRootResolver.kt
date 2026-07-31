package com.kys.editor.fs

import com.kys.editor.codec.GameProfile
import com.kys.editor.codec.WarLayout
import com.kys.editor.codec.AssetPaths
import com.kys.editor.codec.EditorCompat
import com.kys.editor.util.le16
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import java.nio.charset.Charset

object GameRootResolver {

    private fun readableNameScore(raw: ByteArray): Int {
        var r = raw
        val nullIdx = r.indexOf(0)
        if (nullIdx >= 0) r = r.copyOf(nullIdx)
        while (r.isNotEmpty() && (r.last() == 0xFF.toByte() || r.last() == 0x20.toByte())) {
            r = r.copyOf(r.size - 1)
        }
        if (r.size < 2) return 0
        var score = 0
        for (enc in listOf("GBK", "Big5")) {
            try {
                val text = String(r, Charset.forName(enc))
                val han = text.count { it in '\u4e00'..'\u9fff' }
                if (han > 0) score = maxOf(score, han * 3 + text.length)
            } catch (_: Exception) {}
        }
        return score
    }

    fun detectProfile(root: VfsNode): GameProfile {
        val save = root.child("save")
        val res = root.child("resource")

        var magicW = GameProfile.PROMISE.magicWords
        var shopW = GameProfile.PROMISE.shopWords
        var inv = GameProfile.PROMISE.inventorySlots
        var invBase = 42
        var teamOff = 30
        var teamCnt = 6
        var moneyOff = -1
        var warLayout = WarLayout.PROMISE
        var assets = AssetPaths()
        var encoding = "auto"
        var pid = "promise"
        var display = "KYS 自动探测"
        var compat = EditorCompat.PROMISE

        // Ranger idx+grp detection
        var idxNode: VfsNode? = null; var grpNode: VfsNode? = null
        for (n in listOf("ranger.idx", "Ranger.idx")) {
            val c = save.child(n); if (c.exists()) { idxNode = c; break }
        }
        for (n in listOf("Ranger.grp", "ranger.grp")) {
            val c = save.child(n); if (c.exists()) { grpNode = c; break }
        }

        if (idxNode != null && grpNode != null) {
            try {
                val idx = idxNode.readBytes()
                val grp = grpNode.readBytes()
                if (idx.size >= 24) {
                    val buf = idx.leBuffer()
                    val roleO = buf.le32(0); val itemO = buf.le32(4); val sceneO = buf.le32(8)
                    val magicO = buf.le32(12); val shopO = buf.le32(16); val total = buf.le32(20)

                    // probe magic words
                    for (cand in listOf(111, 68, 93)) {
                        val b = cand * 2
                        if ((shopO - magicO) % b == 0) {
                            val count = (shopO - magicO) / b
                            if (count > 10) { magicW = cand; break }
                        }
                    }
                    // probe shop words
                    for (cand in listOf(18, 15)) {
                        val b = cand * 2
                        if ((total - shopO) % b == 0 && (total - shopO) / b >= 1) { shopW = cand; break }
                    }

                    // probe header layout
                    val grpBuf = grp.leBuffer()
                    // Try classic (money@42, inv@44, team@24)
                    val isClassic = grpBuf.le16(42).let { money ->
                        money in 0..999999  // money is always non-negative small value
                    }
                    if (isClassic && (roleO - 44) % 4 == 0) {
                        invBase = 44; moneyOff = 42; teamOff = 24; teamCnt = 6
                    } else {
                        invBase = 42; moneyOff = -1; teamOff = 30; teamCnt = 6
                    }
                    inv = maxOf(1, (roleO - invBase) / 4)
                }
            } catch (_: Exception) {}
        }

        // War.sta detection
        var warNode: VfsNode? = null
        for (n in listOf("War.sta", "war.sta")) {
            val c = res.child(n); if (c.exists()) { warNode = c; break }
        }
        if (warNode != null) {
            try {
                val warBytes = warNode.readBytes()
                for (cand in listOf(93, 156)) {
                    if (warBytes.size % (cand * 2) == 0) {
                        val count = warBytes.size / (cand * 2)
                        if (count > 5) {
                            warLayout = if (cand == 93) WarLayout.CLASSIC else WarLayout.PROMISE
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Asset detection (simplified - check key files)
        val headsPic = res.child("Heads.Pic").exists() || res.child("heads.pic").exists()
        val itemsPic = res.child("Items.Pic").exists() || res.child("items.pic").exists()
        if (headsPic || itemsPic) {
            assets = AssetPaths(headsMode = if (headsPic) "pic" else "none",
                itemsMode = if (itemsPic) "pic" else "none")
        } else {
            assets = AssetPaths(headsMode = "png_dir", itemsMode = "png_dir",
                fightMode = "idx_grp", eftMode = "idx_grp")
        }

        if (magicW == 68 || warLayout.words == 93 || assets.headsMode == "png_dir") {
            encoding = "big5"; pid = "classic"; display = "经典 KYS (自动探测)"
            compat = EditorCompat.CLASSIC
            invBase = 44; moneyOff = 42; teamOff = 24; teamCnt = 6
            warLayout = WarLayout.CLASSIC
            assets = AssetPaths(headsMode = "png_dir", itemsMode = "png_dir", fightMode = "idx_grp", eftMode = "idx_grp")
        } else {
            pid = "promise"; display = "金庸群侠前传 (自动探测)"; compat = EditorCompat.PROMISE
        }

        return GameProfile(
            id = pid, displayName = display, roleWords = 91, itemWords = 95,
            sceneWords = 26, magicWords = magicW, shopWords = shopW,
            inventorySlots = maxOf(inv, 1), rangerTeamOffset = teamOff,
            rangerTeamCount = teamCnt, rangerMoneyOffset = moneyOff,
            rangerInventoryBase = invBase, war = warLayout,
            assets = assets, defaultTextEncoding = encoding, compat = compat
        )
    }
}
