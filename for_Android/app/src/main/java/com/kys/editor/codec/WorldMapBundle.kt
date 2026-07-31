package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le16
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import com.kys.editor.util.putLe32

const val WORLD_MAP_SIZE = 480
const val WORLD_MAP_BYTES = WORLD_MAP_SIZE * WORLD_MAP_SIZE * 2
const val WORLD_LAYER_COUNT = 5

val WORLD_LAYER_FILES = arrayOf(
    "earth" to arrayOf("earth.002", "Earth.002"),
    "surface" to arrayOf("surface.002", "Surface.002"),
    "building" to arrayOf("building.002", "Building.002"),
    "buildx" to arrayOf("buildx.002", "Buildx.002"),
    "buildy" to arrayOf("buildy.002", "Buildy.002")
)

class WorldLayerGrid {
    var path: VfsNode? = null
    var size: Int = WORLD_MAP_SIZE
    var grid: Array<IntArray> = Array(WORLD_MAP_SIZE) { IntArray(WORLD_MAP_SIZE) }

    fun load(node: VfsNode) {
        path = node
        val raw = node.readBytes()
        if (raw.size < WORLD_MAP_BYTES) {
            error("${node.name} size ${raw.size} < $WORLD_MAP_BYTES")
        }
        val buf = raw.leBuffer()
        grid = Array(WORLD_MAP_SIZE) { x ->
            IntArray(WORLD_MAP_SIZE) { y ->
                val off = (x * WORLD_MAP_SIZE + y) * 2
                buf.le16(off).toShort().toInt()
            }
        }
    }

    fun get(x: Int, y: Int): Int = grid[x][y]

    fun set(x: Int, y: Int, value: Int) {
        grid[x][y] = value.toInt()
    }

    fun toBytes(): ByteArray {
        val out = ByteArray(WORLD_MAP_BYTES)
        val buf = out.leBuffer()
        for (x in 0 until WORLD_MAP_SIZE) {
            for (y in 0 until WORLD_MAP_SIZE) {
                buf.putLe16((x * WORLD_MAP_SIZE + y) * 2, grid[x][y])
            }
        }
        return out
    }

    fun save() {
        val node = path ?: error("not loaded")
        node.writeBytes(toBytes())
    }

    fun copyRect(x0: Int, y0: Int, x1: Int, y1: Int): Array<IntArray> {
        val xa = minOf(x0, x1)
        val xb = maxOf(x0, x1)
        val ya = minOf(y0, y1)
        val yb = maxOf(y0, y1)
        return Array(xb - xa + 1) { dx ->
            IntArray(yb - ya + 1) { dy ->
                val x = xa + dx
                val y = ya + dy
                if (x in 0 until size && y in 0 until size) get(x, y) else -1
            }
        }
    }

    fun pasteRect(x0: Int, y0: Int, data: Array<IntArray>, skipNegative: Boolean = false): Int {
        var written = 0
        for (dx in data.indices) {
            val col = data[dx]
            for (dy in col.indices) {
                val v = col[dy]
                if (skipNegative && v < 0) continue
                val x = x0 + dx
                val y = y0 + dy
                if (x in 0 until size && y in 0 until size) {
                    set(x, y, v)
                    written++
                }
            }
        }
        return written
    }

    fun fillRect(x0: Int, y0: Int, x1: Int, y1: Int, value: Int): Int {
        val xa = minOf(x0, x1)
        val xb = maxOf(x0, x1)
        val ya = minOf(y0, y1)
        val yb = maxOf(y0, y1)
        var n = 0
        for (x in xa..xb) {
            for (y in ya..yb) {
                if (x in 0 until size && y in 0 until size) {
                    set(x, y, value)
                    n++
                }
            }
        }
        return n
    }
}

data class SceneEntrance(
    val sceneId: Int,
    val name: String,
    val which: Int,
    val x: Int,
    val y: Int
) {
    val label: String
        get() {
            val tag = if (which > 1) "#$which" else ""
            return "$sceneId:$name$tag ($x,$y)"
        }
}

class WorldMapBundle {
    var layers: MutableMap<String, WorldLayerGrid> = mutableMapOf()
    var idxPath: VfsNode? = null
    var grpPath: VfsNode? = null

    val hasEarth: Boolean get() = "earth" in layers

    fun load(resourceDir: VfsNode) {
        layers = mutableMapOf()
        val idxNode = findFile(resourceDir, listOf("MMAP.idx", "mmap.idx", "Mmap.idx"))
        val grpNode = findFile(resourceDir, listOf("MMAP.grp", "mmap.grp", "Mmap.grp"))
        if (idxNode != null && grpNode != null) {
            loadIdxGrp(idxNode, grpNode)
        } else {
            loadSeparateLayers(resourceDir)
        }
    }

    private fun findFile(dir: VfsNode, names: List<String>): VfsNode? {
        for (name in names) {
            val child = dir.child(name)
            if (child.exists()) return child
        }
        return null
    }

    private fun loadIdxGrp(idxNode: VfsNode, grpNode: VfsNode) {
        idxPath = idxNode
        grpPath = grpNode
        val idxData = idxNode.readBytes()
        val grpData = grpNode.readBytes()
        val idxBuf = idxData.leBuffer()
        val layerKeys = arrayOf("earth", "surface", "building", "buildx", "buildy")
        var prev = 0
        val numEntries = minOf(idxData.size / 4, WORLD_LAYER_COUNT)
        for (i in 0 until numEntries) {
            val end = idxBuf.le32(i * 4)
            if (end <= prev || end > grpData.size) break
            val layerSize = end - prev
            if (layerSize >= WORLD_MAP_BYTES) {
                val grid = WorldLayerGrid()
                grid.size = WORLD_MAP_SIZE
                val layerBuf = grpData.copyOfRange(prev, end).leBuffer()
                grid.grid = Array(WORLD_MAP_SIZE) { x ->
                    IntArray(WORLD_MAP_SIZE) { y ->
                        layerBuf.le16((x * WORLD_MAP_SIZE + y) * 2).toShort().toInt()
                    }
                }
                layers[layerKeys[i]] = grid
            }
            prev = end
        }
    }

    private fun loadSeparateLayers(resourceDir: VfsNode) {
        for ((key, names) in WORLD_LAYER_FILES) {
            val node = findFile(resourceDir, names.toList())
            if (node != null) {
                val grid = WorldLayerGrid()
                grid.load(node)
                layers[key] = grid
            }
        }
    }

    fun getLayer(key: String): WorldLayerGrid? = layers[key]

    fun toBytes(): Pair<ByteArray, ByteArray> {
        val grpBuilder = mutableListOf<Byte>()
        val ends = mutableListOf<Int>()
        val layerKeys = arrayOf("earth", "surface", "building", "buildx", "buildy")
        for (key in layerKeys) {
            val grid = layers[key]
            if (grid != null) {
                for (x in 0 until WORLD_MAP_SIZE) {
                    for (y in 0 until WORLD_MAP_SIZE) {
                        val v = grid.get(x, y)
                        grpBuilder.add((v and 0xFF).toByte())
                        grpBuilder.add(((v shr 8) and 0xFF).toByte())
                    }
                }
            } else {
                repeat(WORLD_MAP_BYTES) {
                    grpBuilder.add(0)
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
        val idxNode = idxPath
        val grpNode = grpPath
        if (idxNode != null && grpNode != null) {
            val (idx, grp) = toBytes()
            idxNode.writeBytes(idx)
            grpNode.writeBytes(grp)
        } else {
            for ((key, grid) in layers) {
                grid.save()
            }
        }
    }

    companion object {
        fun collectSceneEntrances(ranger: RangerArchive, mapSize: Int = WORLD_MAP_SIZE): List<SceneEntrance> {
            val out = mutableListOf<SceneEntrance>()
            if (ranger.scenes.count == 0) return out
            val mainEntranceY1 = 10
            val mainEntranceX1 = 11
            val mainEntranceY2 = 12
            val mainEntranceX2 = 13
            for (i in 0 until ranger.scenes.count) {
                val rec = ranger.scenes.records[i]
                if (rec.size <= mainEntranceX2) continue
                val name = ranger.sceneName(i).trim().ifEmpty { "场景$i" }
                val pairs = arrayOf(
                    Triple(1, rec[mainEntranceX1], rec[mainEntranceY1]),
                    Triple(2, rec[mainEntranceX2], rec[mainEntranceY2])
                )
                for ((which, x, y) in pairs) {
                    val xi = x.toInt()
                    val yi = y.toInt()
                    if (xi !in 0 until mapSize || yi !in 0 until mapSize) continue
                    if (which == 2 && out.isNotEmpty()) {
                        val last = out.last()
                        if (last.sceneId == i && last.x == xi && last.y == yi) continue
                    }
                    out.add(SceneEntrance(i, name, which, xi, yi))
                }
            }
            return out
        }
    }
}
