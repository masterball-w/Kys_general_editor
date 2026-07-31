package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import com.kys.editor.util.s_le16

const val MAP_LAYERS = 6
const val MAP_SIZE = 64
const val SCENE_MAP_BYTES = MAP_LAYERS * MAP_SIZE * MAP_SIZE * 2

class SceneMapData {
    var pathNode: VfsNode? = null
    var maps: Array<Array<Array<ShortArray>>> = emptyArray()

    val sceneCount: Int
        get() = maps.size

    fun load(path: VfsNode) {
        pathNode = path
        val raw = path.readBytes()
        if (raw.size % SCENE_MAP_BYTES != 0) {
            throw IllegalArgumentException("allsin size ${raw.size} not multiple of $SCENE_MAP_BYTES")
        }
        val count = raw.size / SCENE_MAP_BYTES
        val buf = raw.leBuffer()
        maps = Array(count) { s ->
            val base = s * SCENE_MAP_BYTES
            Array(MAP_LAYERS) { layer ->
                val layerOff = base + layer * MAP_SIZE * MAP_SIZE * 2
                Array(MAP_SIZE) { x ->
                    ShortArray(MAP_SIZE) { y ->
                        val off = layerOff + (x * MAP_SIZE + y) * 2
                        buf.s_le16(off)
                    }
                }
            }
        }
    }

    fun get(scene: Int, layer: Int, x: Int, y: Int): Int {
        return maps[scene][layer][x][y].toInt()
    }

    fun set(scene: Int, layer: Int, x: Int, y: Int, value: Int) {
        maps[scene][layer][x][y] = value.toShort()
    }

    fun toBytes(): ByteArray {
        val totalBytes = maps.size * SCENE_MAP_BYTES
        val buf = leBuffer(totalBytes)
        for (s in maps.indices) {
            for (layer in 0 until MAP_LAYERS) {
                val grid = maps[s][layer]
                for (x in 0 until MAP_SIZE) {
                    for (y in 0 until MAP_SIZE) {
                        val off = s * SCENE_MAP_BYTES + layer * MAP_SIZE * MAP_SIZE * 2 + (x * MAP_SIZE + y) * 2
                        buf.putLe16(off, grid[x][y])
                    }
                }
            }
        }
        return buf.array()
    }

    fun save(backup: Boolean = true) {
        val node = pathNode ?: throw IllegalStateException("not loaded")
        node.writeBytes(toBytes())
    }

    companion object {
        fun resolvePath(saveDir: VfsNode, slot: Int): VfsNode {
            if (slot <= 0) {
                for (name in listOf("allsin.grp", "Allsin.grp")) {
                    val p = saveDir.child(name)
                    if (p.exists()) return p
                }
                return saveDir.child("allsin.grp")
            }
            for (name in listOf("S$slot.grp", "s$slot.grp")) {
                val p = saveDir.child(name)
                if (p.exists()) return p
            }
            throw java.io.FileNotFoundException("S$slot.grp not found")
        }
    }
}
