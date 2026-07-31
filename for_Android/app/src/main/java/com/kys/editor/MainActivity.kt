@file:OptIn(ExperimentalMaterial3Api::class)

package com.kys.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.fs.SafNode
import com.kys.editor.fs.SafHelper
import com.kys.editor.ui.screens.*
import com.kys.editor.ui.theme.KysEditorTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KysEditorApp
        val ec = app.editorContext

        setContent {
            KysEditorTheme {
                val ctx = LocalContext.current
                val scope = rememberCoroutineScope()
                val dataRoot by ec.dataRoot.collectAsStateWithLifecycle()
                val isLoading by ec.isLoading.collectAsStateWithLifecycle()
                val statusMsg by ec.statusMessage.collectAsStateWithLifecycle()
                val profile by ec.profile.collectAsStateWithLifecycle()
                var slot by remember { mutableIntStateOf(0) }
                var showSlotDialog by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableIntStateOf(0) }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    SafHelper.init(ctx)
                    val savedRoot = SafHelper.loadSavedRoot()
                    if (savedRoot != null) {
                        ec.setDataRoot(SafNode(savedRoot))
                    }
                }

                LaunchedEffect(statusMsg) {
                    if (statusMsg.isNotEmpty()) {
                        snackbarHostState.showSnackbar(statusMsg)
                    }
                }

                val dirPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri != null) {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        SafHelper.saveRootUri(uri)
                        val doc = DocumentFile.fromTreeUri(ctx, uri)
                        if (doc != null) {
                            ec.setDataRoot(SafNode(doc))
                            scope.launch {
                                snackbarHostState.showSnackbar("已选择: ${doc.name}")
                            }
                        }
                    }
                }

                val tabs = listOf(
                    "存档" to Icons.Default.Save,
                    "事件" to Icons.Default.Code,
                    "战斗" to Icons.Default.SportsMma,
                    "大地图" to Icons.Default.Map,
                    "贴图" to Icons.Default.Image,
                    "引用" to Icons.Default.Search,
                )

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("KYS编辑器", style = MaterialTheme.typography.titleMedium)
                                    val root = dataRoot
                                    if (root != null) {
                                        Text(profile.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            actions = {
                                if (dataRoot != null) {
                                    TextButton(onClick = { showSlotDialog = true }) {
                                        Text("档$slot")
                                    }
                                }
                                IconButton(onClick = { dirPickerLauncher.launch(null) }) {
                                    Icon(Icons.Default.FolderOpen, "选择目录")
                                }
                                IconButton(onClick = { scope.launch { ec.reloadAll() } }, enabled = !isLoading) {
                                    Icon(Icons.Default.Refresh, "重载")
                                }
                                IconButton(onClick = { scope.launch { ec.saveCurrent() } }, enabled = !isLoading && dataRoot != null) {
                                    Icon(Icons.Default.Save, "保存")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (dataRoot != null) {
                            NavigationBar {
                                tabs.forEachIndexed { i, (label, icon) ->
                                    NavigationBarItem(
                                        selected = selectedTab == i,
                                        onClick = { selectedTab = i },
                                        icon = { Icon(icon, label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else if (dataRoot == null) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.FolderOpen, null,
                                    Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text("请选择游戏数据目录", style = MaterialTheme.typography.titleLarge)
                                Text("包含 save/ 和 resource/ 子目录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { dirPickerLauncher.launch(null) }) {
                                    Icon(Icons.Default.FolderOpen, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("选择目录")
                                }
                            }
                        } else {
                            when (selectedTab) {
                                0 -> SaveEditorScreen()
                                1 -> EventEditorScreen()
                                2 -> BattleEditorScreen()
                                3 -> WorldMapScreen()
                                4 -> AssetViewerScreen()
                                5 -> CrossRefScreen()
                            }
                        }
                    }
                }

                if (showSlotDialog) {
                    AlertDialog(
                        onDismissRequest = { showSlotDialog = false },
                        title = { Text("选择存档位") },
                        text = {
                            Column {
                                (0..3).forEach { s ->
                                    TextButton(
                                        onClick = {
                                            slot = s
                                            showSlotDialog = false
                                            scope.launch { ec.loadSlot(s) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (s == 0) "Ranger.grp (自动存档)" else "R$s.grp (档$s)")
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
        }
    }
}
