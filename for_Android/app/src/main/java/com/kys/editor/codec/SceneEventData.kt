package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le16
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16

const val EVENTS_PER_SCENE = 200
const val EVENT_WORDS = 11
const val EVENT_BYTES = EVENTS_PER_SCENE * EVENT_WORDS * 2

class SceneEventData {
    var pathNode: VfsNode? = null
    var scenes: MutableList<MutableList<IntArray>> = mutableListOf()

    val sceneCount: Int get() = scenes.size

    companion object {
        fun resolvePath(saveDir: VfsNode, slot: Int): VfsNode {
            if (slot <= 0) {
                for (name in listOf("alldef.grp", "Alldef.grp")) {
                    val p = saveDir.child(name)
                    if (p.exists()) return p
                }
                return saveDir.child("alldef.grp")
            }
            for (name in listOf("D$slot.grp", "d$slot.grp")) {
                val p = saveDir.child(name)
                if (p.exists()) return p
            }
            throw java.io.FileNotFoundException("D$slot.grp not found")
        }
    }

    fun load(node: VfsNode) {
        pathNode = node
        val raw = node.readBytes()
        if (raw.size % EVENT_BYTES != 0) {
            throw IllegalArgumentException("alldef size ${raw.size} not multiple of $EVENT_BYTES")
        }
        val count = raw.size / EVENT_BYTES
        val buf = raw.leBuffer()
        scenes = mutableListOf()
        for (s in 0 until count) {
            val base = s * EVENT_BYTES
            val events = mutableListOf<IntArray>()
            for (e in 0 until EVENTS_PER_SCENE) {
                val off = base + e * EVENT_WORDS * 2
                val ev = IntArray(EVENT_WORDS)
                for (w in 0 until EVENT_WORDS) {
                    ev[w] = buf.le16(off + w * 2).toShort().toInt()
                }
                events.add(ev)
            }
            scenes.add(events)
        }
    }

    fun get(scene: Int, event: Int, word: Int): Int = scenes[scene][event][word]

    fun set(scene: Int, event: Int, word: Int, value: Int) {
        scenes[scene][event][word] = value
    }

    fun toBytes(): ByteArray {
        val out = ByteArray(scenes.size * EVENT_BYTES)
        val buf = out.leBuffer()
        for ((s, events) in scenes.withIndex()) {
            val base = s * EVENT_BYTES
            for ((e, ev) in events.withIndex()) {
                val off = base + e * EVENT_WORDS * 2
                for (w in 0 until EVENT_WORDS) {
                    val v = if (w < ev.size) ev[w] else 0
                    buf.putLe16(off + w * 2, v)
                }
            }
        }
        return out
    }

    fun save(backup: Boolean = true) {
        val node = pathNode ?: throw IllegalStateException("not loaded")
        node.writeBytes(toBytes())
    }

    fun findFreeEvent(scene: Int): Int {
        val evs = scenes[scene]
        for ((e, ev) in evs.withIndex()) {
            if (ev.size >= 6 && ev[2] <= 0 && ev[3] <= 0 && ev[4] <= 0 && ev[5] == 0) {
                return e
            }
        }
        return -1
    }
}
