package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le16
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import com.kys.editor.util.putLe32

class WarFieldArchive {
    var idxPath: VfsNode? = null
    var grpPath: VfsNode? = null
    var offsets: MutableList<Int> = mutableListOf()
    var layerCounts: MutableList<Int> = mutableListOf()
    var fields: MutableList<MutableList<Array<IntArray>>> = mutableListOf()

    val count: Int get() = fields.size

    fun load(resourceDir: VfsNode) {
        var idxNode: VfsNode? = null
        var grpNode: VfsNode? = null
        for (name in listOf("warfld.idx", "Warfld.idx")) {
            val child = resourceDir.child(name)
            if (child.exists()) {
                idxNode = child
                break
            }
        }
        for (name in listOf("warfld.grp", "Warfld.grp")) {
            val child = resourceDir.child(name)
            if (child.exists()) {
                grpNode = child
                break
            }
        }
        if (idxNode == null || grpNode == null) {
            error("warfld.idx/grp not found")
        }
        idxPath = idxNode
        grpPath = grpNode
        val idxData = idxNode.readBytes()
        val grpData = grpNode.readBytes()
        val idxBuf = idxData.leBuffer()
        val numEntries = idxData.size / 4
        val ends = IntArray(numEntries) { i -> idxBuf.le32(i * 4) }
        offsets = mutableListOf()
        layerCounts = mutableListOf()
        fields = mutableListOf()
        var prev = 0
        val layerStride = FIELD_SIZE * FIELD_SIZE * 2
        var corrupt = false
        for (end in ends) {
            if (end <= prev || end > grpData.size) {
                corrupt = true
                break
            }
            val size = end - prev
            val layers = maxOf(1, size / layerStride)
            offsets.add(prev)
            layerCounts.add(layers)
            appendField(grpData, prev, layers)
            prev = end
        }
        if (corrupt || fields.isEmpty()) {
            fields.clear()
            offsets.clear()
            layerCounts.clear()
            if (grpData.size >= FIELD_BYTES) {
                val nFields = grpData.size / FIELD_BYTES
                prev = 0
                for (i in 0 until nFields) {
                    offsets.add(prev)
                    layerCounts.add(FIELD_LAYERS)
                    appendField(grpData, prev, FIELD_LAYERS)
                    prev += FIELD_BYTES
                }
            }
        }
    }

    private fun appendField(grpData: ByteArray, off: Int, layers: Int) {
        val layerList = mutableListOf<Array<IntArray>>()
        val grpBuf = grpData.leBuffer()
        for (layer in 0 until layers) {
            val grid = Array(FIELD_SIZE) { IntArray(FIELD_SIZE) }
            val layerOff = off + layer * FIELD_SIZE * FIELD_SIZE * 2
            for (x in 0 until FIELD_SIZE) {
                for (y in 0 until FIELD_SIZE) {
                    val o = layerOff + (x * FIELD_SIZE + y) * 2
                    if (o + 2 <= grpData.size) {
                        grid[x][y] = grpBuf.le16(o).toShort().toInt()
                    }
                }
            }
            layerList.add(grid)
        }
        fields.add(layerList)
    }

    fun get(field: Int, layer: Int, x: Int, y: Int): Int {
        return fields[field][layer][x][y]
    }

    fun set(field: Int, layer: Int, x: Int, y: Int, value: Int) {
        fields[field][layer][x][y] = value.toInt()
    }

    fun toBytes(): Pair<ByteArray, ByteArray> {
        val grpBuilder = mutableListOf<Byte>()
        val ends = mutableListOf<Int>()
        for (layers in fields) {
            for (grid in layers) {
                for (x in 0 until FIELD_SIZE) {
                    for (y in 0 until FIELD_SIZE) {
                        val v = grid[x][y]
                        grpBuilder.add((v and 0xFF).toByte())
                        grpBuilder.add(((v shr 8) and 0xFF).toByte())
                    }
                }
            }
            ends.add(grpBuilder.size)
        }
        val grp = ByteArray(grpBuilder.size)
        for (i in grpBuilder.indices) grp[i] = grpBuilder[i]
        val idx = ByteArray(ends.size * 4)
        val idxBuf = idx.leBuffer()
        for (i in ends.indices) {
            idxBuf.putLe32(i * 4, ends[i])
        }
        return Pair(idx, grp)
    }

    fun save() {
        val idxNode = idxPath ?: error("not loaded")
        val grpNode = grpPath ?: error("not loaded")
        val (idx, grp) = toBytes()
        idxNode.writeBytes(idx)
        grpNode.writeBytes(grp)
    }

    companion object {
        const val FIELD_SIZE = 64
        const val FIELD_LAYERS = 2
        const val FIELD_BYTES = FIELD_LAYERS * FIELD_SIZE * FIELD_SIZE * 2
    }
}
