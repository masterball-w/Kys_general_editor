package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le16
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import java.nio.ByteBuffer

class WarRecord(var data: IntArray, val layout: WarLayout = WarLayout.PROMISE) {
    init {
        val w = layout.words
        if (data.size < w) {
            val padded = IntArray(w)
            System.arraycopy(data, 0, padded, 0, data.size)
            data = padded
        } else if (data.size > w) {
            data = data.copyOf(w)
        }
    }

    private fun get(i: Int, default: Int = 0): Int {
        return if (i in 0 until data.size) data[i] else default
    }

    private fun set(i: Int, v: Int) {
        if (i in 0 until data.size) data[i] = v.toInt()
    }

    fun getWord(i: Int): Int = get(i)
    fun setWord(i: Int, v: Int) = set(i, v)

    var battleNum: Int
        get() = get(0)
        set(v) = set(0, v)

    var name: String
        get() {
            val raw = ByteArray(10)
            for (i in 0 until 5) {
                val w = get(1 + i)
                raw[i * 2] = (w and 0xFF).toByte()
                raw[i * 2 + 1] = ((w shr 8) and 0xFF).toByte()
            }
            var end = raw.size
            while (end > 0) {
                val b = raw[end - 1]
                if (b == 0.toByte() || b == 0xFF.toByte() || b == 0x20.toByte()) end--
                else break
            }
            return if (end == 0) "" else decodeBytes(raw.copyOfRange(0, end))
        }
        set(text) {
            val bytes = encodeText(text)
            val padded = ByteArray(10)
            System.arraycopy(bytes, 0, padded, 0, minOf(bytes.size, 9))
            for (i in 0 until 5) {
                val w = (padded[i * 2].toInt() and 0xFF) or ((padded[i * 2 + 1].toInt() and 0xFF) shl 8)
                set(1 + i, w.toShort().toInt())
            }
        }

    var battleMap: Int
        get() = get(6)
        set(v) = set(6, v)

    var exp: Int
        get() = get(7)
        set(v) = set(7, v)

    var music: Int
        get() = get(8)
        set(v) = set(8, v)

    fun mate(i: Int): Int {
        val lay = layout
        return if (i in 0 until lay.mateCount) get(lay.mateOff + i, -1) else -1
    }

    fun setMate(i: Int, v: Int) {
        val lay = layout
        if (i in 0 until lay.mateCount) set(lay.mateOff + i, v)
    }

    fun autoMate(i: Int): Int {
        val lay = layout
        return if (lay.autoMateOff >= 0 && i in 0 until lay.autoMateCount) get(lay.autoMateOff + i, -1) else -1
    }

    fun setAutoMate(i: Int, v: Int) {
        val lay = layout
        if (lay.autoMateOff >= 0 && i in 0 until lay.autoMateCount) set(lay.autoMateOff + i, v)
    }

    fun mateX(i: Int): Int = get(layout.mateXOff + i)

    fun setMateX(i: Int, v: Int) {
        if (i in 0 until layout.mateCount) set(layout.mateXOff + i, v)
    }

    fun mateY(i: Int): Int = get(layout.mateYOff + i)

    fun setMateY(i: Int, v: Int) {
        if (i in 0 until layout.mateCount) set(layout.mateYOff + i, v)
    }

    fun enemy(i: Int): Int {
        val lay = layout
        return if (i in 0 until lay.enemyCount) get(lay.enemyOff + i, -1) else -1
    }

    fun setEnemy(i: Int, v: Int) {
        val lay = layout
        if (i in 0 until lay.enemyCount) set(lay.enemyOff + i, v)
    }

    fun enemyX(i: Int): Int = get(layout.enemyXOff + i)

    fun setEnemyX(i: Int, v: Int) {
        if (i in 0 until layout.enemyCount) set(layout.enemyXOff + i, v)
    }

    fun enemyY(i: Int): Int = get(layout.enemyYOff + i)

    fun setEnemyY(i: Int, v: Int) {
        if (i in 0 until layout.enemyCount) set(layout.enemyYOff + i, v)
    }

    var boutEvent: Int
        get() {
            val off = layout.boutEventOff
            return if (off >= 0) get(off) else 0
        }
        set(v) {
            if (layout.boutEventOff >= 0) set(layout.boutEventOff, v)
        }

    var operationEvent: Int
        get() {
            val off = layout.operationEventOff
            return if (off >= 0) get(off) else 0
        }
        set(v) {
            if (layout.operationEventOff >= 0) set(layout.operationEventOff, v)
        }

    fun getKongfu(i: Int): Int {
        val lay = layout
        return if (lay.getKongfuOff >= 0 && i in 0 until lay.getKongfuCount) get(lay.getKongfuOff + i, -1) else -1
    }

    fun setKongfu(i: Int, v: Int) {
        val lay = layout
        if (lay.getKongfuOff >= 0 && i in 0 until lay.getKongfuCount) set(lay.getKongfuOff + i, v)
    }

    fun getItems(i: Int): Int {
        val lay = layout
        return if (lay.getItemsOff >= 0 && i in 0 until lay.getItemsCount) get(lay.getItemsOff + i, -1) else -1
    }

    fun setItems(i: Int, v: Int) {
        val lay = layout
        if (lay.getItemsOff >= 0 && i in 0 until lay.getItemsCount) set(lay.getItemsOff + i, v)
    }

    var getMoney: Int
        get() {
            val off = layout.getMoneyOff
            return if (off >= 0) get(off) else 0
        }
        set(v) {
            if (layout.getMoneyOff >= 0) set(layout.getMoneyOff, v)
        }

    fun enemyCount(): Int {
        return (0 until layout.enemyCount).count { enemy(it) >= 0 }
    }

    fun mateCount(): Int {
        var n = (0 until layout.mateCount).count { mate(it) >= 0 }
        if (layout.autoMateOff >= 0) {
            n += (0 until layout.autoMateCount).count { autoMate(it) >= 0 }
        }
        return n
    }

    fun clear() {
        val w = layout.words
        for (i in 0 until w) data[i] = -1
        data[0] = 0
        for (i in 1..5) data[i] = 0
        data[6] = 0
        data[7] = 0
        data[8] = 0
    }

    companion object {
        fun createEmpty(layout: WarLayout = WarLayout.PROMISE): WarRecord {
            return WarRecord(IntArray(layout.words), layout)
        }
    }
}

class WarArchive(var layout: WarLayout = WarLayout.PROMISE) {
    var path: VfsNode? = null
    var records: MutableList<WarRecord> = mutableListOf()

    val count: Int get() = records.size
    val warWords: Int get() = layout.words
    val warBytes: Int get() = layout.words * 2

    fun load(resourceDir: VfsNode) {
        var p: VfsNode? = null
        for (name in listOf("War.sta", "war.sta")) {
            val child = resourceDir.child(name)
            if (child.exists()) {
                p = child
                break
            }
        }
        val warNode = p ?: error("War.sta not found")
        path = warNode
        val raw = warNode.readBytes()
        val wb = warBytes
        val ww = warWords
        if (raw.size % wb != 0) {
            error("War.sta size ${raw.size} not multiple of $wb (words=$ww)")
        }
        records = mutableListOf()
        val buf = raw.leBuffer()
        val numRecords = raw.size / wb
        for (i in 0 until numRecords) {
            val words = IntArray(ww)
            val baseOff = i * wb
            for (w in 0 until ww) {
                words[w] = buf.le16(baseOff + w * 2).toShort().toInt()
            }
            records.add(WarRecord(words, layout))
        }
    }

    fun findByNum(battleNum: Int): WarRecord? {
        for (r in records) {
            if (r.battleNum == battleNum) return r
        }
        if (battleNum in records.indices) return records[battleNum]
        return null
    }

    fun appendCopy(srcIndex: Int = 0): WarRecord {
        val src = if (records.isNotEmpty()) records[srcIndex] else WarRecord.createEmpty(layout)
        val rec = WarRecord(src.data.copyOf(), layout)
        val maxNum = records.maxOfOrNull { it.battleNum } ?: 0
        rec.battleNum = maxNum + 1
        records.add(rec)
        return rec
    }

    fun toBytes(): ByteArray {
        val ww = warWords
        val total = records.size * ww * 2
        val out = ByteArray(total)
        val buf = out.leBuffer()
        for (i in records.indices) {
            val r = records[i]
            val baseOff = i * ww * 2
            for (w in 0 until ww) {
                buf.putLe16(baseOff + w * 2, if (w < r.data.size) r.data[w] else 0)
            }
        }
        return out
    }

    fun save() {
        val node = path ?: error("not loaded")
        node.writeBytes(toBytes())
    }

    companion object {
        fun fromProfile(profile: GameProfile): WarArchive {
            return WarArchive(profile.war)
        }
    }
}
