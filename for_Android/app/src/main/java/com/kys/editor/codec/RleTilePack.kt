package com.kys.editor.codec

import android.graphics.Bitmap
import android.graphics.Color
import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe32
import com.kys.editor.util.s_le16

data class TileHotspot(val x: Int, val y: Int)

class RleTilePack {
    var idxNode: VfsNode? = null
    var grpNode: VfsNode? = null
    var offsets: IntArray = intArrayOf()
    var tiles: MutableList<ByteArray> = mutableListOf()

    private val colorCache = HashMap<Int, Int>()
    private val imgCache = HashMap<Int, Bitmap>()

    val count: Int
        get() = tiles.size

    fun load(idx: VfsNode, grp: VfsNode) {
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

        tiles = mutableListOf()
        colorCache.clear()
        imgCache.clear()

        var prev = 0
        for (end in offsets) {
            if (end <= 0 || end < prev || end > grpData.size) {
                tiles.add(ByteArray(0))
                continue
            }
            tiles.add(grpData.copyOfRange(prev, end))
            prev = end
        }
    }

    fun decodeTile(index: Int, palette: IntArray, useCache: Boolean = true): Bitmap? {
        if (useCache) {
            imgCache[index]?.let { return it }
        }
        if (index < 0 || index >= tiles.size) return null
        val block = tiles[index]
        if (block.size < 8) return null

        val buf = block.leBuffer()
        val w = buf.s_le16(0).toInt()
        val h = buf.s_le16(2).toInt()
        if (w <= 0 || h <= 0 || w > 512 || h > 512) return null

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        decodeRleRows(block, w, h, palette, bitmap)

        if (useCache) {
            imgCache[index] = bitmap
        }
        return bitmap
    }

    fun getHotspot(index: Int): TileHotspot {
        if (index < 0 || index >= tiles.size) return TileHotspot(0, 0)
        val block = tiles[index]
        if (block.size < 8) return TileHotspot(0, 0)
        val buf = block.leBuffer()
        val xs = buf.s_le16(4).toInt()
        val ys = buf.s_le16(6).toInt()
        return TileHotspot(xs, ys)
    }

    fun averageColor(index: Int, palette: IntArray): Int {
        colorCache[index]?.let { return it }
        if (index < 0 || index >= tiles.size) {
            val c = Color.rgb(40, 40, 40)
            colorCache[index] = c
            return c
        }
        val block = tiles[index]
        if (block.size < 8) {
            val c = Color.rgb(40, 40, 40)
            colorCache[index] = c
            return c
        }
        val buf = block.leBuffer()
        val w = buf.s_le16(0).toInt()
        val h = buf.s_le16(2).toInt()
        if (w <= 0 || h <= 0 || w > 512 || h > 512) {
            val c = Color.rgb(40, 40, 40)
            colorCache[index] = c
            return c
        }

        var rs = 0L
        var gs = 0L
        var bs = 0L
        var n = 0L
        forEachRlePixel(block, w, h) { _, _, palIdx ->
            if (palIdx in 0 until palette.size) {
                val color = palette[palIdx]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                if (r + g + b > 12) {
                    rs += r
                    gs += g
                    bs += b
                    n++
                }
            }
        }
        val c = if (n <= 0) {
            Color.rgb(55, 55, 55)
        } else {
            Color.rgb((rs / n).toInt(), (gs / n).toInt(), (bs / n).toInt())
        }
        colorCache[index] = c
        return c
    }

    fun replaceRaw(index: Int, block: ByteArray) {
        if (index < 0 || index >= tiles.size) throw IndexOutOfBoundsException("$index")
        tiles[index] = block
        colorCache.remove(index)
        imgCache.remove(index)
    }

    fun appendRaw(block: ByteArray): Int {
        tiles.add(block)
        return tiles.size - 1
    }

    fun toBytesPair(): Pair<ByteArray, ByteArray> {
        val ends = mutableListOf<Int>()
        val grp = java.io.ByteArrayOutputStream()
        for (t in tiles) {
            if (t.isEmpty()) {
                ends.add(if (grp.size() > 0 || ends.isNotEmpty()) grp.size() else 0)
            } else {
                grp.write(t)
                ends.add(grp.size())
            }
        }
        val idx = if (ends.isNotEmpty()) {
            val buf = leBuffer(ends.size * 4)
            for (i in ends.indices) {
                buf.putLe32(i * 4, ends[i])
            }
            buf.array()
        } else ByteArray(0)
        return idx to grp.toByteArray()
    }

    fun save() {
        val idx = idxNode ?: throw IllegalStateException("not loaded")
        val grp = grpNode ?: throw IllegalStateException("not loaded")
        val (idxBytes, grpBytes) = toBytesPair()
        idx.writeBytes(idxBytes)
        grp.writeBytes(grpBytes)
    }

    companion object {
        private fun decodeRleRows(
            block: ByteArray, w: Int, h: Int, palette: IntArray, bitmap: Bitmap
        ) {
            if (block.size <= 8) return
            val pixels = IntArray(w * h)
            var pos = 8
            for (y in 0 until h) {
                if (pos >= block.size) break
                val rowNbytes = block[pos].toInt() and 0xFF
                pos++
                val rowEnd = minOf(pos + rowNbytes, block.size)
                if (rowNbytes <= 0) continue
                var x = 0
                while (pos < rowEnd && x < w) {
                    val skip = block[pos].toInt() and 0xFF
                    pos++
                    x += skip
                    if (pos >= rowEnd || x >= w) break
                    val count = block[pos].toInt() and 0xFF
                    pos++
                    for (k in 0 until count) {
                        if (pos >= rowEnd || x >= w) break
                        val palIdx = block[pos].toInt() and 0xFF
                        pos++
                        if (palIdx in 0 until palette.size) {
                            pixels[y * w + x] = palette[palIdx]
                        }
                        x++
                    }
                }
                pos = rowEnd
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        }

        private inline fun forEachRlePixel(
            block: ByteArray, w: Int, h: Int,
            onPixel: (x: Int, y: Int, palIdx: Int) -> Unit
        ) {
            if (block.size <= 8) return
            var pos = 8
            for (y in 0 until h) {
                if (pos >= block.size) break
                val rowNbytes = block[pos].toInt() and 0xFF
                pos++
                val rowEnd = minOf(pos + rowNbytes, block.size)
                if (rowNbytes <= 0) continue
                var x = 0
                while (pos < rowEnd && x < w) {
                    val skip = block[pos].toInt() and 0xFF
                    pos++
                    x += skip
                    if (pos >= rowEnd || x >= w) break
                    val count = block[pos].toInt() and 0xFF
                    pos++
                    for (k in 0 until count) {
                        if (pos >= rowEnd || x >= w) break
                        val palIdx = block[pos].toInt() and 0xFF
                        pos++
                        onPixel(x, y, palIdx)
                        x++
                    }
                }
                pos = rowEnd
            }
        }

        fun loadPalette(node: VfsNode): IntArray {
            val raw = node.readBytes()
            if (raw.size < 768) throw IllegalArgumentException("palette too small: ${raw.size} bytes")
            val needExpand = (0 until 256).all { i ->
                (raw[i * 3].toInt() and 0xFF) <= 63 &&
                (raw[i * 3 + 1].toInt() and 0xFF) <= 63 &&
                (raw[i * 3 + 2].toInt() and 0xFF) <= 63
            }
            return IntArray(256) { i ->
                var r = raw[i * 3].toInt() and 0xFF
                var g = raw[i * 3 + 1].toInt() and 0xFF
                var b = raw[i * 3 + 2].toInt() and 0xFF
                if (needExpand) {
                    r *= 4; g *= 4; b *= 4
                }
                Color.rgb(r, g, b)
            }
        }

        fun findPalette(resourceDir: VfsNode): VfsNode? {
            for (name in listOf("mmap.col", "MMAP.COL", "pallet.col", "Pallet.col")) {
                val child = resourceDir.child(name)
                if (child.exists()) return child
            }
            return null
        }

        fun loadOrNull(idx: VfsNode, grp: VfsNode): RleTilePack? {
            return try {
                val pack = RleTilePack()
                pack.load(idx, grp)
                pack
            } catch (_: Exception) {
                null
            }
        }

        fun loadTilePackPair(resourceDir: VfsNode, idxNames: List<String>, grpNames: List<String>): RleTilePack? {
            var idx: VfsNode? = null
            var grp: VfsNode? = null
            for (n in idxNames) {
                val c = resourceDir.child(n)
                if (c.exists()) { idx = c; break }
            }
            for (n in grpNames) {
                val c = resourceDir.child(n)
                if (c.exists()) { grp = c; break }
            }
            if (idx == null || grp == null) return null
            return loadOrNull(idx, grp)
        }

        fun codeToTileIndex(code: Int): Int {
            if (code == 0) return -1
            if (code < 0) return (-code) / 2
            return code / 2
        }
    }
}
