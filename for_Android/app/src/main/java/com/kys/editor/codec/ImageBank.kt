package com.kys.editor.codec

import android.graphics.Bitmap
import com.kys.editor.fs.VfsNode
import com.kys.editor.util.BitmapCache

interface ImageBank {
    val count: Int
    fun getBitmap(index: Int, maxDim: Int = 0): Bitmap?
    suspend fun getBitmapAsync(index: Int, maxDim: Int = 0): Bitmap? = getBitmap(index, maxDim)
}

class PicImageBank(val archive: PicArchive) : ImageBank {
    override val count: Int get() = archive.count
    override fun getBitmap(index: Int, maxDim: Int): Bitmap? {
        if (index < 0 || index >= count) return null
        return archive.getBitmap(index, maxDim)
    }
    override suspend fun getBitmapAsync(index: Int, maxDim: Int): Bitmap? {
        if (index < 0 || index >= count) return null
        return archive.getBitmapAsync(index, maxDim)
    }
}

class PngDirImageBank(val directory: VfsNode) : ImageBank {
    private val ids: List<Int>
    private val maxId: Int
    private val cachePrefix: String

    init {
        val found = mutableListOf<Int>()
        if (directory.exists() && directory.isDirectory) {
            for (f in directory.listFiles()) {
                val name = f.name
                if (!name.endsWith(".png", ignoreCase = true)) continue
                val stem = name.substringBeforeLast('.')
                val num = stem.toIntOrNull()
                if (num != null) found.add(num)
            }
        }
        found.sort()
        ids = found
        maxId = if (ids.isNotEmpty()) ids.max() + 1 else 0
        cachePrefix = "pngdir:${directory.name}:"
    }

    override val count: Int get() = maxId

    private fun pngBytes(index: Int): ByteArray? {
        if (index < 0) return null
        val child = directory.child("$index.png")
        if (!child.exists()) return null
        return try { child.readBytes() } catch (_: Exception) { null }
    }

    override fun getBitmap(index: Int, maxDim: Int): Bitmap? {
        if (index < 0) return null
        val key = "${cachePrefix}${index}:${maxDim}"
        BitmapCache.get(key)?.let { return it }
        val bytes = pngBytes(index) ?: return null
        return BitmapCache.decodePng(key, bytes, maxDim)
    }

    override suspend fun getBitmapAsync(index: Int, maxDim: Int): Bitmap? {
        if (index < 0) return null
        val key = "${cachePrefix}${index}:${maxDim}"
        return BitmapCache.getOrDecode(key, maxDim) { pngBytes(index) }
    }
}

class EmptyImageBank : ImageBank {
    override val count: Int get() = 0
    override fun getBitmap(index: Int, maxDim: Int): Bitmap? = null
}

object ImageBanks {

    fun loadHeadsBank(dataRoot: VfsNode, assets: AssetPaths): ImageBank {
        if (assets.headsMode == "pic") {
            for (rel in listOf(assets.headsPic, "resource/heads.pic")) {
                val path = dataRoot.resolve(rel.split('/'))
                if (path.exists() && !path.isDirectory) {
                    val pic = PicArchive.loadOrNull(path)
                    if (pic != null) return PicImageBank(pic)
                }
            }
        }
        if (assets.headsMode == "png_dir") {
            val d = dataRoot.resolve(assets.headsDir.split('/'))
            if (d.exists() && d.isDirectory) return PngDirImageBank(d)
        }
        for (rel in listOf("resource/Heads.Pic", "resource/heads.pic")) {
            val path = dataRoot.resolve(rel.split('/'))
            if (path.exists() && !path.isDirectory) {
                val pic = PicArchive.loadOrNull(path)
                if (pic != null) return PicImageBank(pic)
            }
        }
        val headDir = dataRoot.child("head")
        if (headDir.exists() && headDir.isDirectory) {
            val hasPng = headDir.listFiles().any { it.name.endsWith(".png", ignoreCase = true) }
            if (hasPng) return PngDirImageBank(headDir)
        }
        return EmptyImageBank()
    }

    fun loadItemsBank(dataRoot: VfsNode, assets: AssetPaths): ImageBank {
        if (assets.itemsMode == "pic") {
            for (rel in listOf(assets.itemsPic, "resource/items.pic")) {
                val path = dataRoot.resolve(rel.split('/'))
                if (path.exists() && !path.isDirectory) {
                    val pic = PicArchive.loadOrNull(path)
                    if (pic != null) return PicImageBank(pic)
                }
            }
        }
        if (assets.itemsMode == "png_dir") {
            val d = dataRoot.resolve(assets.itemsDir.split('/'))
            if (d.exists() && d.isDirectory) return PngDirImageBank(d)
        }
        for (rel in listOf("resource/Items.Pic", "resource/items.pic")) {
            val path = dataRoot.resolve(rel.split('/'))
            if (path.exists() && !path.isDirectory) {
                val pic = PicArchive.loadOrNull(path)
                if (pic != null) return PicImageBank(pic)
            }
        }
        val itemDir = dataRoot.child("item")
        if (itemDir.exists() && itemDir.isDirectory) {
            val hasPng = itemDir.listFiles().any { it.name.endsWith(".png", ignoreCase = true) }
            if (hasPng) return PngDirImageBank(itemDir)
        }
        return EmptyImageBank()
    }

    fun loadFightPicArchive(
        dataRoot: VfsNode, assets: AssetPaths, fightId: Int, mode: Int
    ): PicArchive? {
        if (assets.fightMode != "pic_tree") return null
        val rel = String.format(assets.fightPicFmt, fightId, mode)
        val path = dataRoot.resolve(rel.split('/'))
        if (!path.exists() || path.isDirectory) return null
        return PicArchive.loadOrNull(path)
    }

    fun loadFightTilePack(
        dataRoot: VfsNode, assets: AssetPaths, fightId: Int
    ): RleTilePack? {
        if (assets.fightMode != "idx_grp") return null
        val idxRel = String.format(assets.fightIdxFmt, fightId)
        val grpRel = String.format(assets.fightGrpFmt, fightId)
        val idxNode = dataRoot.resolve(idxRel.split('/'))
        val grpNode = dataRoot.resolve(grpRel.split('/'))
        if (!idxNode.exists() || !grpNode.exists()) return null
        return RleTilePack.loadOrNull(idxNode, grpNode)
    }

    fun resolveEftPicPath(dataRoot: VfsNode, assets: AssetPaths, ami: Int): VfsNode? {
        if (assets.eftMode != "pic_file") return null
        val rel = String.format(assets.eftPicFmt, ami)
        val path = dataRoot.resolve(rel.split('/'))
        if (path.exists() && !path.isDirectory) return path
        val altRel = "eft/eft$ami.pic"
        val altPath = dataRoot.resolve(altRel.split('/'))
        if (altPath.exists() && !altPath.isDirectory) return altPath
        return null
    }

    fun loadEftPreviewBitmap(dataRoot: VfsNode, assets: AssetPaths, ami: Int): Bitmap? {
        if (assets.eftMode == "pic_file") {
            val path = resolveEftPicPath(dataRoot, assets, ami) ?: return null
            val pic = PicArchive.loadOrNull(path) ?: return null
            if (pic.count <= 0) return null
            return pic.getBitmap(0)
        }
        return null
    }

    fun loadEftTilePack(dataRoot: VfsNode, assets: AssetPaths): RleTilePack? {
        if (assets.eftMode != "idx_grp") return null
        val idxNode = dataRoot.resolve(assets.eftIdx.split('/'))
        val grpNode = dataRoot.resolve(assets.eftGrp.split('/'))
        if (!idxNode.exists() || !grpNode.exists()) return null
        return RleTilePack.loadOrNull(idxNode, grpNode)
    }

    fun loadPalette(dataRoot: VfsNode): IntArray? {
        val palNode = RleTilePack.findPalette(dataRoot) ?: return null
        return try {
            RleTilePack.loadPalette(palNode)
        } catch (_: Exception) {
            null
        }
    }

    fun loadSceneTilePack(dataRoot: VfsNode): RleTilePack? {
        return RleTilePack.loadTilePackPair(
            dataRoot,
            listOf("smp.idx", "Smp.idx", "SMP.IDX"),
            listOf("sdx.grp", "Sdx.grp", "SDX.GRP", "smp.grp", "Smp.grp")
        )
    }

    fun loadMmapTilePack(dataRoot: VfsNode): RleTilePack? {
        return RleTilePack.loadTilePackPair(
            dataRoot,
            listOf("mmap.idx", "Mmap.idx", "MMAP.IDX"),
            listOf("mmap.grp", "Mmap.grp", "MMAP.GRP")
        )
    }
}
