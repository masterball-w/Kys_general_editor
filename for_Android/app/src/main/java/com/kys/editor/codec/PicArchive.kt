package com.kys.editor.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kys.editor.fs.VfsNode
import com.kys.editor.util.BitmapCache
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe32
import java.io.ByteArrayOutputStream

private data class PicFrameEntry(
    var x: Int = 0,
    var y: Int = 0,
    var black: Int = 0,
    var pngStart: Int = 0,
    var pngEnd: Int = 0
)

data class PicFrame(
    var x: Int = 0,
    var y: Int = 0,
    var black: Int = 0,
    var pngBytes: ByteArray = ByteArray(0)
) {
    fun toBitmap(): Bitmap? {
        if (pngBytes.isEmpty()) return null
        return BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PicFrame) return false
        return x == other.x && y == other.y && black == other.black && pngBytes.contentEquals(other.pngBytes)
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + black
        result = 31 * result + pngBytes.contentHashCode()
        return result
    }
}

class PicArchive {
    var node: VfsNode? = null
    private var rawData: ByteArray = ByteArray(0)
    private var entries: MutableList<PicFrameEntry> = mutableListOf()
    var cacheKeyPrefix: String = ""

    val count: Int
        get() = entries.size

    fun load(vnode: VfsNode) {
        node = vnode
        cacheKeyPrefix = "pic:${vnode.name}:${vnode.javaClass.simpleName}:"
        val raw = vnode.readBytes()
        rawData = raw
        entries.clear()
        if (raw.size < 4) return
        val buf = raw.leBuffer()
        val cnt = buf.le32(0)
        if (cnt <= 0) return
        if (raw.size < 4 + cnt * 4) {
            throw IllegalArgumentException("pic header truncated: need ${4 + cnt * 4}, have ${raw.size}")
        }
        val offsets = IntArray(cnt) { i -> buf.le32(4 + i * 4) }
        val headerSize = (cnt + 1) * 4
        for (i in 0 until cnt) {
            val start = if (i == 0) headerSize else offsets[i - 1]
            val end = offsets[i]
            if (start < 0 || end > raw.size || end < start + 12) {
                entries.add(PicFrameEntry())
                continue
            }
            val x = buf.le32(start)
            val y = buf.le32(start + 4)
            val black = buf.le32(start + 8)
            entries.add(PicFrameEntry(x, y, black, start + 12, end))
        }
    }

    private fun pngBytesAt(index: Int): ByteArray {
        if (index < 0 || index >= entries.size) return ByteArray(0)
        val e = entries[index]
        if (e.pngEnd <= e.pngStart) return ByteArray(0)
        return rawData.copyOfRange(e.pngStart, e.pngEnd)
    }

    fun getFrame(index: Int): PicFrame {
        if (index < 0 || index >= entries.size) return PicFrame()
        val e = entries[index]
        return PicFrame(e.x, e.y, e.black, pngBytesAt(index))
    }

    fun toBytes(): ByteArray {
        val cnt = entries.size
        val headerSize = (cnt + 1) * 4
        val chunks = mutableListOf<ByteArray>()
        val offsets = mutableListOf<Int>()
        var cursor = headerSize
        for (i in 0 until cnt) {
            val e = entries[i]
            val baos = ByteArrayOutputStream()
            val hdr = leBuffer(12)
            hdr.putLe32(0, e.x)
            hdr.putLe32(4, e.y)
            hdr.putLe32(8, e.black)
            baos.write(hdr.array())
            val png = if (e.pngEnd > e.pngStart && rawData.size >= e.pngEnd) {
                rawData.copyOfRange(e.pngStart, e.pngEnd)
            } else ByteArray(0)
            baos.write(png)
            val chunk = baos.toByteArray()
            cursor += chunk.size
            offsets.add(cursor)
            chunks.add(chunk)
        }
        val out = ByteArrayOutputStream()
        val cntBuf = leBuffer(4)
        cntBuf.putLe32(0, cnt)
        out.write(cntBuf.array())
        if (cnt > 0) {
            val offBuf = leBuffer(cnt * 4)
            for (i in 0 until cnt) {
                offBuf.putLe32(i * 4, offsets[i])
            }
            out.write(offBuf.array())
        }
        for (c in chunks) out.write(c)
        return out.toByteArray()
    }

    fun save() {
        val n = node ?: throw IllegalStateException("not loaded")
        // Flush rawData so the next load reads the updated version
        rawData = toBytes()
        // Rebuild entries from new rawData
        entries.clear()
        val buf = rawData.leBuffer()
        val cnt = buf.le32(0)
        val offsets = IntArray(cnt) { i -> buf.le32(4 + i * 4) }
        val headerSize = (cnt + 1) * 4
        for (i in 0 until cnt) {
            val start = if (i == 0) headerSize else offsets[i - 1]
            val end = offsets[i]
            if (start < 0 || end > rawData.size || end < start + 12) {
                entries.add(PicFrameEntry())
                continue
            }
            entries.add(PicFrameEntry(
                buf.le32(start), buf.le32(start + 4), buf.le32(start + 8),
                start + 12, end
            ))
        }
        BitmapCache.clear()
        n.writeBytes(rawData)
    }

    /** Get cached bitmap, only decodes once. Call from any thread (decode happens on a background dispatcher via BitmapCache if needed). */
    fun getBitmap(index: Int, maxDim: Int = 0): Bitmap? {
        if (index < 0 || index >= entries.size) return null
        val key = "$cacheKeyPrefix$index:$maxDim"
        BitmapCache.get(key)?.let { return it }
        val png = pngBytesAt(index)
        if (png.isEmpty()) return null
        return BitmapCache.decodePng(key, png, maxDim)
    }

    /** Suspend version that guarantees decoding off main thread. */
    suspend fun getBitmapAsync(index: Int, maxDim: Int = 0): Bitmap? {
        if (index < 0 || index >= entries.size) return null
        val key = "$cacheKeyPrefix$index:$maxDim"
        return BitmapCache.getOrDecode(key, maxDim) { pngBytesAt(index) }
    }

    fun replaceFrame(index: Int, pngBytes: ByteArray, x: Int = 0, y: Int = 0) {
        if (index < 0 || index >= entries.size) throw IndexOutOfBoundsException("$index")
        val oldBlack = entries[index].black
        // Write new PNG into rawData by appending; simpler: rebuild rawData
        val newFrames = entries.mapIndexed { i, e ->
            if (i == index) PicFrame(x, y, oldBlack, pngBytes) else getFrame(i)
        }
        rebuildFromFrames(newFrames)
    }

    fun appendFrame(pngBytes: ByteArray, x: Int = 0, y: Int = 0): Int {
        val newFrames = (0 until entries.size).map { getFrame(it) } + PicFrame(x, y, 0, pngBytes)
        rebuildFromFrames(newFrames)
        return entries.size - 1
    }

    fun deleteFrame(index: Int) {
        if (index !in entries.indices) return
        val newFrames = (0 until entries.size).mapNotNull { i -> if (i == index) null else getFrame(i) }
        rebuildFromFrames(newFrames)
    }

    private fun rebuildFromFrames(frames: List<PicFrame>) {
        val cnt = frames.size
        val headerSize = (cnt + 1) * 4
        val out = ByteArrayOutputStream()
        val cntBuf = leBuffer(4)
        cntBuf.putLe32(0, cnt)
        out.write(cntBuf.array())
        // Reserve offset table
        val offBytes = ByteArray(cnt * 4)
        out.write(offBytes)
        val offsets = IntArray(cnt)
        var cursor = headerSize
        for ((i, fr) in frames.withIndex()) {
            val hdr = leBuffer(12)
            hdr.putLe32(0, fr.x)
            hdr.putLe32(4, fr.y)
            hdr.putLe32(8, fr.black)
            out.write(hdr.array())
            out.write(fr.pngBytes)
            cursor += 12 + fr.pngBytes.size
            offsets[i] = cursor
        }
        val bytes = out.toByteArray()
        val ob = bytes.leBuffer()
        for (i in 0 until cnt) {
            ob.putLe32(4 + i * 4, offsets[i])
        }
        rawData = bytes
        entries.clear()
        val buf = rawData.leBuffer()
        val newCnt = buf.le32(0)
        val newOffsets = IntArray(newCnt) { i -> buf.le32(4 + i * 4) }
        for (i in 0 until newCnt) {
            val start = if (i == 0) headerSize else newOffsets[i - 1]
            val end = newOffsets[i]
            entries.add(PicFrameEntry(
                buf.le32(start), buf.le32(start + 4), buf.le32(start + 8),
                start + 12, end
            ))
        }
    }

    companion object {
        fun loadOrNull(node: VfsNode): PicArchive? {
            return try {
                val pic = PicArchive()
                pic.load(node)
                pic
            } catch (_: Exception) {
                null
            }
        }
    }
}
