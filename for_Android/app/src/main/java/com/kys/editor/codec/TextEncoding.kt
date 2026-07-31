package com.kys.editor.codec

import java.nio.charset.Charset

enum class TextEnc(val displayName: String, val charset: Charset) {
    GBK("GBK 简体", Charset.forName("GBK")),
    BIG5("Big5 繁体", Charset.forName("Big5")),
    UTF8("UTF-8", Charsets.UTF_8);

    companion object {
        val AUTO = GBK
    }
}

fun xor0xFF(data: ByteArray): ByteArray {
    val out = ByteArray(data.size)
    for (i in data.indices) out[i] = (data[i].toInt() xor 0xFF).toByte()
    return out
}

private val TALK_AUTO_ORDER = listOf("GBK", "Big5", "UTF-8")
private val RANGER_AUTO_ORDER = listOf("GBK", "Big5", "ISO-8859-1")

private fun tryDecode(raw: ByteArray, encodings: List<String>): String? {
    for (enc in encodings) {
        try {
            val cs = Charset.forName(enc)
            return String(raw, cs)
        } catch (_: Exception) {
        }
    }
    return null
}

fun decodeBytes(raw: ByteArray, enc: TextEnc = TextEnc.AUTO, autoOrder: List<String> = RANGER_AUTO_ORDER): String {
    if (raw.isEmpty()) return ""
    return when (enc) {
        TextEnc.GBK -> tryDecode(raw, listOf("GBK")) ?: String(raw, Charsets.ISO_8859_1)
        TextEnc.BIG5 -> tryDecode(raw, listOf("Big5")) ?: String(raw, Charsets.ISO_8859_1)
        TextEnc.UTF8 -> tryDecode(raw, listOf("UTF-8")) ?: String(raw, Charsets.ISO_8859_1)
        else -> tryDecode(raw, autoOrder) ?: String(raw, Charsets.ISO_8859_1)
    }
}

fun encodeText(text: String, enc: TextEnc = TextEnc.AUTO): ByteArray {
    val writeEnc = if (enc == TextEnc.AUTO) TextEnc.GBK else enc
    val charsetName = when (writeEnc) {
        TextEnc.GBK -> "GBK"
        TextEnc.BIG5 -> "Big5"
        TextEnc.UTF8 -> "UTF-8"
        else -> "GBK"
    }
    return try {
        text.toByteArray(Charset.forName(charsetName))
    } catch (_: Exception) {
        text.toByteArray(Charsets.ISO_8859_1)
    }
}

fun decodeTalkPayload(raw: ByteArray, enc: TextEnc = TextEnc.AUTO): String {
    val dec = xor0xFF(raw)
    val out = mutableListOf<Byte>()
    for (b in dec) {
        val bi = b.toInt() and 0xFF
        if (bi == 0x00 || bi == 0xFF) break
        out.add(b)
    }
    val stripped = out.toByteArray()
    val order = if (enc == TextEnc.AUTO) TALK_AUTO_ORDER else RANGER_AUTO_ORDER
    return decodeBytes(stripped, enc, order)
}

fun encodeTalkPayload(text: String, enc: TextEnc = TextEnc.AUTO): ByteArray {
    val raw = encodeText(text, enc) + byteArrayOf(0x00)
    return xor0xFF(raw)
}
