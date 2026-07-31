package com.kys.editor.codec

import com.kys.editor.util.cstr
import com.kys.editor.util.le16
import com.kys.editor.util.leBuffer
import com.kys.editor.util.putLe16
import java.nio.ByteBuffer
import java.nio.charset.Charset

class RecordTable(
    val words: Int,
    val records: MutableList<IntArray> = mutableListOf(),
    var textEncoding: Charset = Charsets.UTF_8
) {
    val count: Int get() = records.size

    fun getName(index: Int, startWord: Int = 1, wordCount: Int = 5, charset: Charset = textEncoding): String {
        if (index < 0 || index >= records.size) return ""
        val rec = records[index]
        val rawBytes = ByteArray(wordCount * 2)
        for (i in 0 until wordCount) {
            val w = if (startWord + i < rec.size) rec[startWord + i] else 0
            rawBytes[i * 2] = (w and 0xFF).toByte()
            rawBytes[i * 2 + 1] = ((w shr 8) and 0xFF).toByte()
        }
        var end = rawBytes.size
        while (end > 0) {
            val b = rawBytes[end - 1]
            if (b == 0.toByte() || b == 0xFF.toByte() || b == 0x20.toByte()) end--
            else break
        }
        if (end == 0) return ""
        return String(rawBytes, 0, end, charset)
    }

    fun setName(index: Int, name: String, startWord: Int = 1, wordCount: Int = 5, charset: Charset = textEncoding) {
        val bytes = name.toByteArray(charset)
        val rec = records[index]
        for (i in 0 until wordCount) {
            val wOff = i * 2
            val w = if (wOff + 1 < bytes.size) {
                (bytes[wOff].toInt() and 0xFF) or ((bytes[wOff + 1].toInt() and 0xFF) shl 8)
            } else 0
            if (startWord + i < rec.size) rec[startWord + i] = w.toShort().toInt()
        }
    }

    fun get(index: Int, word: Int): Int = records[index][word]
    fun set(index: Int, word: Int, value: Int) { records[index][word] = value }

    companion object {
        fun parse(buf: ByteBuffer, start: Int, end: Int, words: Int, charset: Charset): RecordTable {
            val table = RecordTable(words, mutableListOf(), charset)
            val byteSize = words * 2
            val count = (end - start) / byteSize
            for (i in 0 until count) {
                val off = start + i * byteSize
                val rec = IntArray(words)
                for (w in 0 until words) {
                    rec[w] = buf.le16(off + w * 2).toShort().toInt()
                }
                table.records.add(rec)
            }
            return table
        }

        fun pack(table: RecordTable, nbytes: Int): ByteArray {
            val words = table.words
            val count = nbytes / (words * 2)
            val out = ByteArray(nbytes)
            val buf = out.leBuffer()
            for (i in 0 until count) {
                val rec = if (i < table.records.size) table.records[i] else IntArray(words)
                val off = i * words * 2
                for (w in 0 until words) {
                    buf.putLe16(off + w * 2, if (w < rec.size) rec[w] else 0)
                }
            }
            return out
        }
    }
}
