package com.kys.editor.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

// Kotlin equivalents of Python struct.pack/unpack with little-endian
// Matches Python's "<h", "<H", "<i", "<I", "<f", "<b", "<B" formats exactly.

fun ByteBuffer.le16(off: Int): Int {
    val lo = this[off].toInt() and 0xFF
    val hi = this[off + 1].toInt() and 0xFF
    return lo or (hi shl 8)
}

fun ByteBuffer.s_le16(off: Int): Short {
    val v = le16(off)
    return if (v >= 0x8000) (v - 0x10000).toShort() else v.toShort()
}

fun ByteBuffer.le32(off: Int): Int {
    val b0 = this[off].toInt() and 0xFF
    val b1 = this[off + 1].toInt() and 0xFF
    val b2 = this[off + 2].toInt() and 0xFF
    val b3 = this[off + 3].toInt() and 0xFF
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}

fun ByteBuffer.u8(off: Int): Int = this[off].toInt() and 0xFF

fun ByteBuffer.putLe16(off: Int, value: Int) {
    this.put(off, (value and 0xFF).toByte())
    this.put(off + 1, ((value shr 8) and 0xFF).toByte())
}

fun ByteBuffer.putLe16(off: Int, value: Short) = putLe16(off, value.toInt() and 0xFFFF)

fun ByteBuffer.putLe32(off: Int, value: Int) {
    this.put(off, (value and 0xFF).toByte())
    this.put(off + 1, ((value shr 8) and 0xFF).toByte())
    this.put(off + 2, ((value shr 16) and 0xFF).toByte())
    this.put(off + 3, ((value shr 24) and 0xFF).toByte())
}

// Read null-terminated GBK string starting at offset, up to maxLen bytes.
fun ByteBuffer.cstr(off: Int, maxLen: Int, charset: java.nio.charset.Charset): String {
    var end = off
    val limit = off + maxLen
    while (end < limit && this[end].toInt() != 0) end++
    val len = end - off
    if (len <= 0) return ""
    val bytes = ByteArray(len)
    for (i in 0 until len) bytes[i] = this[off + i]
    return String(bytes, charset)
}

// Read fix-length string (strip trailing nulls/spaces)
fun ByteBuffer.fixedStr(off: Int, len: Int, charset: java.nio.charset.Charset): String {
    val bytes = ByteArray(len)
    for (i in 0 until len) bytes[i] = this[off + i]
    var end = len
    while (end > 0 && (bytes[end - 1] == 0.toByte() || bytes[end - 1] == 0x20.toByte())) end--
    return if (end <= 0) "" else String(bytes, 0, end, charset)
}

// Write a null-terminated string into buffer, padding with zeros.
fun ByteBuffer.putCstr(off: Int, maxLen: Int, s: String, charset: java.nio.charset.Charset) {
    val bytes = s.toByteArray(charset)
    val writeLen = minOf(bytes.size, maxLen - 1)
    for (i in 0 until writeLen) this.put(off + i, bytes[i])
    for (i in writeLen until maxLen) this.put(off + i, 0)
}

// Allocate a little-endian ByteBuffer
fun leBuffer(size: Int): ByteBuffer {
    val buf = ByteBuffer.allocate(size)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    return buf
}

fun ByteArray.leBuffer(): ByteBuffer {
    val buf = ByteBuffer.wrap(this)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    return buf
}

// Signed/unsigned interpretation helpers
fun Int.toSigned16(): Short = if (this >= 0x8000) (this - 0x10000).toShort() else this.toShort()
fun Short.toUnsigned(): Int = this.toInt() and 0xFFFF
fun Int.toUnsignedLong(): Long = this.toLong() and 0xFFFFFFFFL
