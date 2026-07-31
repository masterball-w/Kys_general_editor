package com.kys.editor.fs

import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.InputStream

sealed interface VfsNode {
    val name: String
    val isDirectory: Boolean
    fun exists(): Boolean
    fun listFiles(): List<VfsNode>
    fun readBytes(): ByteArray
    fun writeBytes(data: ByteArray)
    fun child(name: String): VfsNode
    fun resolve(parts: List<String>): VfsNode {
        var cur = this
        for (p in parts) cur = cur.child(p)
        return cur
    }
}

class FileNode(private val file: java.io.File) : VfsNode {
    override val name: String get() = file.name
    override val isDirectory: Boolean get() = file.isDirectory
    override fun exists(): Boolean = file.exists()
    override fun listFiles(): List<VfsNode> =
        file.listFiles()?.map { FileNode(it) } ?: emptyList()
    override fun readBytes(): ByteArray = file.readBytes()
    override fun writeBytes(data: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }
    override fun child(name: String): VfsNode = FileNode(java.io.File(file, name))
}

/**
 * SAF node wrapper. Does NOT auto-create files on child() traversal.
 * writeBytes() creates the file/truncates existing.
 */
class SafNode(private val doc: DocumentFile) : VfsNode {
    override val name: String get() = doc.name ?: ""
    override val isDirectory: Boolean get() = doc.isDirectory
    override fun exists(): Boolean = doc.exists()
    override fun listFiles(): List<VfsNode> = doc.listFiles().map { SafNode(it) }

    override fun child(name: String): VfsNode {
        val existing = doc.findFile(name)
        if (existing != null) {
            return if (existing.isDirectory) SafNode(existing) else SafNode(existing)
        }
        // Return a lazy wrapper that will create on write, not now
        return SafLazyChild(doc, name)
    }

    override fun readBytes(): ByteArray {
        val ctx = SafHelper.appContext ?: throw IllegalStateException("No app context")
        ctx.contentResolver.openInputStream(doc.uri).use { ins ->
            return ins?.readAllBytes() ?: ByteArray(0)
        }
    }

    override fun writeBytes(data: ByteArray) {
        val ctx = SafHelper.appContext ?: throw IllegalStateException("No app context")
        ctx.contentResolver.openOutputStream(doc.uri, "wt").use { os ->
            os?.write(data)
        }
    }
}

/**
 * Lazy child reference - file doesn't exist yet; created on first writeBytes.
 */
private class SafLazyChild(private val parent: DocumentFile, private val childName: String) : VfsNode {
    override val name: String get() = childName
    override val isDirectory: Boolean get() = false
    override fun exists(): Boolean = parent.findFile(childName)?.exists() == true

    override fun listFiles(): List<VfsNode> = emptyList()

    override fun child(name: String): VfsNode {
        val existing = parent.findFile(childName)
        if (existing != null) return SafNode(existing)
        return SafLazyChild(parent, name)
    }

    private fun ensureDoc(): DocumentFile {
        parent.findFile(childName)?.let { return it }
        return parent.createFile("application/octet-stream", childName)
            ?: throw IllegalStateException("Cannot create $childName under ${parent.name}")
    }

    override fun readBytes(): ByteArray {
        val f = parent.findFile(childName) ?: throw IllegalStateException("$childName not found")
        val ctx = SafHelper.appContext ?: throw IllegalStateException("No app context")
        ctx.contentResolver.openInputStream(f.uri).use { ins ->
            return ins?.readAllBytes() ?: ByteArray(0)
        }
    }

    override fun writeBytes(data: ByteArray) {
        val f = parent.findFile(childName)
            ?: parent.createFile("application/octet-stream", childName)
            ?: throw IllegalStateException("Cannot create $childName")
        val ctx = SafHelper.appContext ?: throw IllegalStateException("No app context")
        ctx.contentResolver.openOutputStream(f.uri, "wt").use { os ->
            os?.write(data)
        }
    }
}

private fun InputStream.readAllBytes(): ByteArray {
    val baos = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (true) {
        val n = read(buf)
        if (n <= 0) break
        baos.write(buf, 0, n)
    }
    return baos.toByteArray()
}
