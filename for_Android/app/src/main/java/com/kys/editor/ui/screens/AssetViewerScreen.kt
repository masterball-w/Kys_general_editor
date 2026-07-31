package com.kys.editor.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp
import com.kys.editor.codec.ImageBank
import com.kys.editor.codec.ImageBanks
import com.kys.editor.codec.PicImageBank
import com.kys.editor.codec.PngDirImageBank
import com.kys.editor.ui.components.AsyncBankImage
import com.kys.editor.util.BitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AssetTab { Heads, Items, Fight, Eft, Tiles }

private fun getPngBytes(bank: ImageBank, index: Int): ByteArray? {
    return when (bank) {
        is PicImageBank -> {
            val bytes = bank.archive.getFrame(index).pngBytes
            if (bytes.isEmpty()) null else bytes
        }
        is PngDirImageBank -> {
            val child = bank.directory.child("$index.png")
            if (child.exists()) child.readBytes() else null
        }
        else -> null
    }
}

private fun replacePngBytes(bank: ImageBank, index: Int, bytes: ByteArray) {
    when (bank) {
        is PicImageBank -> {
            bank.archive.replaceFrame(index, bytes)
            bank.archive.save()
        }
        is PngDirImageBank -> {
            bank.directory.child("$index.png").writeBytes(bytes)
        }
    }
    BitmapCache.clear()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetViewerScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext
    val root by ec.dataRoot.collectAsStateWithLifecycle()
    val profile by ec.profile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(AssetTab.Heads) }
    var bank by remember(root, profile, tab) { mutableStateOf<ImageBank?>(null) }
    var bankLoading by remember(root, profile, tab) { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }
    var dialogIndex by remember { mutableStateOf<Int?>(null) }
    var pendingExportIndex by remember { mutableStateOf(-1) }
    var pendingReplaceIndex by remember { mutableStateOf(-1) }

    // Load the bank off main thread
    LaunchedEffect(root, profile, tab, reloadTrigger) {
        bank = null
        val r = root
        if (r == null) return@LaunchedEffect
        bankLoading = true
        bank = withContext(Dispatchers.IO) {
            try {
                when (tab) {
                    AssetTab.Heads -> ImageBanks.loadHeadsBank(r, profile.assets)
                    AssetTab.Items -> ImageBanks.loadItemsBank(r, profile.assets)
                    else -> null
                }
            } catch (_: Exception) { null }
        }
        bankLoading = false
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        val b = bank
        val idx = pendingExportIndex
        if (uri != null && b != null && idx >= 0) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val bytes = getPngBytes(b, idx) ?: return@withContext false
                    try {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        true
                    } catch (_: Exception) { false }
                }
                Toast.makeText(ctx, if (ok) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val replaceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val b = bank
        val idx = pendingReplaceIndex
        if (uri != null && b != null && idx >= 0) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val bytes = try {
                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (_: Exception) { null }
                    if (bytes == null || bytes.isEmpty()) return@withContext false
                    try {
                        replacePngBytes(b, idx, bytes)
                        true
                    } catch (_: Exception) { false }
                }
                if (ok) {
                    dialogIndex = null
                    reloadTrigger++
                }
                Toast.makeText(ctx, if (ok) "替换成功" else "替换失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab.ordinal) {
            AssetTab.entries.forEachIndexed { i, t ->
                val label = when (t) {
                    AssetTab.Heads -> "头像"
                    AssetTab.Items -> "物品图标"
                    AssetTab.Fight -> "战斗图"
                    AssetTab.Eft -> "特效"
                    AssetTab.Tiles -> "瓦片"
                }
                Tab(selected = tab.ordinal == i, onClick = { tab = AssetTab.entries[i] }, text = { Text(label) })
            }
        }
        val currentBank = bank
        when {
            root == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("请先选择游戏数据目录")
            }
            bankLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            currentBank == null || currentBank.count == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val msg = when (tab) {
                    AssetTab.Fight -> "战斗图 - 请从战斗编辑器中加载"
                    AssetTab.Eft -> "特效 - 请从武功/事件中查看"
                    AssetTab.Tiles -> "瓦片集 - 请从大地图中查看"
                    else -> "未找到图片资源"
                }
                Text(msg)
            }
            else -> {
                val itemCount = currentBank.count
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(64.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(itemCount) { i ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { dialogIndex = i }
                        ) {
                            AsyncBankImage(
                                bank = currentBank,
                                index = i,
                                modifier = Modifier.size(48.dp),
                                maxDim = 64
                            )
                            Text(
                                "#$i", style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    val idx = dialogIndex
    val b = bank
    if (idx != null && b != null && idx in 0 until b.count) {
        AlertDialog(
            onDismissRequest = { dialogIndex = null },
            title = { Text("图片 #${idx}") },
            text = {
                AsyncBankImage(
                    bank = b,
                    index = idx,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    maxDim = 512
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingExportIndex = idx
                    exportLauncher.launch("${tab.name.lowercase()}_$idx.png")
                }) { Text("导出PNG") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingReplaceIndex = idx
                    replaceLauncher.launch("image/png")
                }) { Text("替换图片") }
            }
        )
    }
}
