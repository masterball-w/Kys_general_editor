package com.kys.editor.codec

import com.kys.editor.fs.VfsNode
import com.kys.editor.util.le32
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe32

open class IdxGrpTextArchive(val kind: String) {
    var idxNode: VfsNode? = null
    var grpNode: VfsNode? = null
    var offsets: MutableList<Int> = mutableListOf()
    var entries: MutableList<ByteArray> = mutableListOf()
    var textEncoding: TextEnc = TextEnc.AUTO

    val count: Int get() = entries.size

    fun load(resourceDir: VfsNode) {
        val names = when (kind) {
            "talk" -> listOf(
                listOf("talk.idx", "Talk.idx"),
                listOf("talk.grp", "Talk.grp")
            )
            "name" -> listOf(
                listOf("name.idx", "Name.idx"),
                listOf("name.grp", "Name.grp")
            )
            else -> throw IllegalArgumentException("Unknown archive kind: $kind")
        }
        var idx: VfsNode? = null
        var grp: VfsNode? = null
        for (n in names[0]) {
            val p = resourceDir.child(n)
            if (p.exists()) { idx = p; break }
        }
        for (n in names[1]) {
            val p = resourceDir.child(n)
            if (p.exists()) { grp = p; break }
        }
        if (idx == null || grp == null) {
            throw java.io.FileNotFoundException("$kind idx/grp not found")
        }
        idxNode = idx
        grpNode = grp
        loadFromBytes(idx.readBytes(), grp.readBytes())
    }

    fun loadFromBytes(idxData: ByteArray, grpData: ByteArray) {
        val idxBuf = idxData.leBuffer()
        val idxCount = idxData.size / 4
        offsets = mutableListOf()
        for (i in 0 until idxCount) {
            offsets.add(idxBuf.le32(i * 4))
        }
        entries = mutableListOf()
        for (i in offsets.indices) {
            val off = offsets[i]
            val end = if (i + 1 < offsets.size) offsets[i + 1] else grpData.size
            entries.add(grpData.copyOfRange(off, end))
        }
    }

    fun getText(entryId: Int): String {
        if (entryId <= 0 || entryId > entries.size) return ""
        return decodeTalkPayload(entries[entryId - 1], textEncoding)
    }

    fun setText(entryId: Int, text: String) {
        if (entryId <= 0 || entryId > entries.size) throw IndexOutOfBoundsException("entryId=$entryId")
        entries[entryId - 1] = encodeTalkPayload(text, textEncoding)
    }

    fun appendText(text: String): Int {
        entries.add(encodeTalkPayload(text, textEncoding))
        return entries.size
    }

    private fun rebuildOffsets() {
        val newOffsets = mutableListOf<Int>()
        var cursor = 0
        for (e in entries) {
            newOffsets.add(cursor)
            cursor += e.size
        }
        offsets = newOffsets
    }

    fun toIdxBytes(): ByteArray {
        rebuildOffsets()
        val out = ByteArray(offsets.size * 4)
        val buf = out.leBuffer()
        for (i in offsets.indices) {
            buf.putLe32(i * 4, offsets[i])
        }
        return out
    }

    fun toGrpBytes(): ByteArray {
        var total = 0
        for (e in entries) total += e.size
        val out = ByteArray(total)
        var pos = 0
        for (e in entries) {
            System.arraycopy(e, 0, out, pos, e.size)
            pos += e.size
        }
        return out
    }

    fun save(backup: Boolean = true) {
        val idx = idxNode ?: throw IllegalStateException("not loaded")
        val grp = grpNode ?: throw IllegalStateException("not loaded")
        idx.writeBytes(toIdxBytes())
        grp.writeBytes(toGrpBytes())
    }

    companion object {
        fun findIdx(resourceDir: VfsNode, kind: String): VfsNode {
            val names = when (kind) {
                "talk" -> listOf("talk.idx", "Talk.idx")
                "name" -> listOf("name.idx", "Name.idx")
                else -> throw IllegalArgumentException("Unknown archive kind: $kind")
            }
            for (n in names) {
                val child = resourceDir.child(n)
                if (child.exists()) return child
            }
            error("${names.first()} not found")
        }

        fun findGrp(resourceDir: VfsNode, kind: String): VfsNode {
            val names = when (kind) {
                "talk" -> listOf("talk.grp", "Talk.grp")
                "name" -> listOf("name.grp", "Name.grp")
                else -> throw IllegalArgumentException("Unknown archive kind: $kind")
            }
            for (n in names) {
                val child = resourceDir.child(n)
                if (child.exists()) return child
            }
            error("${names.first()} not found")
        }
    }
}

class TalkArchive : IdxGrpTextArchive("talk")

class NameArchive : IdxGrpTextArchive("name")
