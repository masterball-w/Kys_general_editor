package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le16
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import java.nio.ByteBuffer
import java.nio.charset.Charset

class RangerArchive(var layout: RangerLayout = RangerLayout()) {
    var textEncoding: Charset = Charsets.UTF_8
    var roleOffset: Int = 0
    var itemOffset: Int = 0
    var sceneOffset: Int = 0
    var magicOffset: Int = 0
    var shopOffset: Int = 0
    var totalLen: Int = 0
    var header = RangerHeader()
    var roles = RecordTable(layout.roleWords)
    var items = RecordTable(layout.itemWords)
    var scenes = RecordTable(layout.sceneWords)
    var magics = RecordTable(layout.magicWords)
    var shops = RecordTable(layout.shopWords)
    private var raw: ByteArray = ByteArray(0)
    private var headerPad: ByteArray = ByteArray(0)

    fun load(saveDir: VfsNode, slot: Int = 0) {
        val idxNode = findIdx(saveDir)
        val grpNode = resolveGrp(saveDir, slot)
        val idx = idxNode.readBytes()
        if (idx.size < 24) error("ranger.idx too small")
        val idxBuf = idx.leBuffer()
        roleOffset = idxBuf.le32(0)
        itemOffset = idxBuf.le32(4)
        sceneOffset = idxBuf.le32(8)
        magicOffset = idxBuf.le32(12)
        shopOffset = idxBuf.le32(16)
        totalLen = idxBuf.le32(20)
        raw = grpNode.readBytes()
        if (raw.size < totalLen) error("grp size ${raw.size} < TotalLen $totalLen")
        parseHeader()
        val lay = layout
        roles = RecordTable.parse(raw.leBuffer(), roleOffset, itemOffset, lay.roleWords, textEncoding)
        items = RecordTable.parse(raw.leBuffer(), itemOffset, sceneOffset, lay.itemWords, textEncoding)
        scenes = RecordTable.parse(raw.leBuffer(), sceneOffset, magicOffset, lay.sceneWords, textEncoding)
        magics = RecordTable.parse(raw.leBuffer(), magicOffset, shopOffset, lay.magicWords, textEncoding)
        shops = RecordTable.parse(raw.leBuffer(), shopOffset, totalLen, lay.shopWords, textEncoding)
    }

    private fun parseHeader() {
        val buf = raw.leBuffer()
        val h = RangerHeader()
        h.inShip = buf.le16(0).toShort().toInt()
        h.where = buf.le16(2).toShort().toInt()
        h.my = buf.le16(4).toShort().toInt()
        h.mx = buf.le16(6).toShort().toInt()
        h.sy = buf.le16(8).toShort().toInt()
        h.sx = buf.le16(10).toShort().toInt()
        h.mface = buf.le16(12).toShort().toInt()
        h.shipX = buf.le16(14).toShort().toInt()
        h.shipY = buf.le16(16).toShort().toInt()
        h.time = buf.le16(18).toShort().toInt()
        h.timeEvent = buf.le16(20).toShort().toInt()
        h.randomEvent = buf.le16(22).toShort().toInt()
        h.sface = buf.le16(24).toShort().toInt()
        h.shipFace = buf.le16(26).toShort().toInt()
        h.gameTime = buf.le16(28).toShort().toInt()
        h.team = MutableList(layout.teamCount) { i ->
            buf.le16(layout.teamOffset + i * 2).toShort().toInt()
        }
        val teamEnd = layout.teamOffset + layout.teamCount * 2
        val padEnd = if (layout.moneyOffset >= 0) layout.moneyOffset else layout.inventoryBase
        headerPad = if (padEnd > teamEnd) {
            raw.copyOfRange(teamEnd, padEnd)
        } else ByteArray(0)
        h.money = if (layout.moneyOffset >= 0) buf.le16(layout.moneyOffset).toShort().toInt() else 0
        val invBase = layout.inventoryBase
        val invBytes = maxOf(0, roleOffset - invBase)
        val slots = invBytes / 4
        h.inventory = MutableList(slots) { i ->
            val off = invBase + i * 4
            InventorySlot(buf.le16(off).toShort().toInt(), buf.le16(off + 2).toShort().toInt())
        }
        while (h.inventory.size < layout.inventorySlots) {
            h.inventory.add(InventorySlot(-1, 0))
        }
        header = h
    }

    fun toBytes(): ByteArray {
        val roleBytes = itemOffset - roleOffset
        val itemBytes = sceneOffset - itemOffset
        val sceneBytes = magicOffset - sceneOffset
        val magicBytes = shopOffset - magicOffset
        val shopBytes = totalLen - shopOffset

        val headerArr = ByteArray(roleOffset)
        val hbuf = headerArr.leBuffer()
        val h = header
        hbuf.putLe16(0, h.inShip); hbuf.putLe16(2, h.where)
        hbuf.putLe16(4, h.my); hbuf.putLe16(6, h.mx)
        hbuf.putLe16(8, h.sy); hbuf.putLe16(10, h.sx)
        hbuf.putLe16(12, h.mface); hbuf.putLe16(14, h.shipX)
        hbuf.putLe16(16, h.shipY); hbuf.putLe16(18, h.time)
        hbuf.putLe16(20, h.timeEvent); hbuf.putLe16(22, h.randomEvent)
        hbuf.putLe16(24, h.sface); hbuf.putLe16(26, h.shipFace)
        hbuf.putLe16(28, h.gameTime)
        for (i in 0 until layout.teamCount) {
            hbuf.putLe16(layout.teamOffset + i * 2, if (i < h.team.size) h.team[i] else -1)
        }
        if (headerPad.isNotEmpty()) {
            val teamEnd = layout.teamOffset + layout.teamCount * 2
            System.arraycopy(headerPad, 0, headerArr, teamEnd, minOf(headerPad.size, headerArr.size - teamEnd))
        }
        if (layout.moneyOffset >= 0) hbuf.putLe16(layout.moneyOffset, h.money)
        val invSlots = (roleOffset - layout.inventoryBase) / 4
        for (i in 0 until invSlots) {
            val slot = if (i < h.inventory.size) h.inventory[i] else InventorySlot(-1, 0)
            hbuf.putLe16(layout.inventoryBase + i * 4, slot.number)
            hbuf.putLe16(layout.inventoryBase + i * 4 + 2, slot.amount)
        }

        val out = ByteArray(totalLen)
        System.arraycopy(headerArr, 0, out, 0, minOf(headerArr.size, roleOffset))
        System.arraycopy(RecordTable.pack(roles, roleBytes), 0, out, roleOffset, roleBytes)
        System.arraycopy(RecordTable.pack(items, itemBytes), 0, out, itemOffset, itemBytes)
        System.arraycopy(RecordTable.pack(scenes, sceneBytes), 0, out, sceneOffset, sceneBytes)
        System.arraycopy(RecordTable.pack(magics, magicBytes), 0, out, magicOffset, magicBytes)
        System.arraycopy(RecordTable.pack(shops, shopBytes), 0, out, shopOffset, shopBytes)
        return out
    }

    fun roleName(index: Int): String = roles.getName(index, 4, 5, textEncoding)
    fun itemName(index: Int): String = items.getName(index, 1, 10, textEncoding)
    fun magicName(index: Int): String = magics.getName(index, 1, 5, textEncoding)
    fun sceneName(index: Int): String = scenes.getName(index, 1, 5, textEncoding)

    companion object {
        fun findIdx(saveDir: VfsNode): VfsNode {
            for (name in listOf("ranger.idx", "Ranger.idx")) {
                val child = saveDir.child(name)
                if (child.exists()) return child
            }
            error("ranger.idx not found")
        }

        fun resolveGrp(saveDir: VfsNode, slot: Int): VfsNode {
            val names = if (slot <= 0) listOf("Ranger.grp", "ranger.grp")
            else listOf("R$slot.grp", "r$slot.grp")
            for (name in names) {
                val child = saveDir.child(name)
                if (child.exists()) return child
            }
            error("${names.first()} not found")
        }
    }
}
