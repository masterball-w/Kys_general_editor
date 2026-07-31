package com.kys.editor.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.kys.editor.R
import com.kys.editor.ui.context.EditorTab
import com.kys.editor.ui.screens.*

data class TabItem(val tab: EditorTab, val label: String, val icon: ImageVector)

val tabs = listOf(
    TabItem(EditorTab.Save, "存档", Icons.Default.Save),
    TabItem(EditorTab.Events, "事件", Icons.Default.Code),
    TabItem(EditorTab.Battle, "战斗", Icons.Default.SportsMma),
    TabItem(EditorTab.WorldMap, "大地图", Icons.Default.Map),
    TabItem(EditorTab.Assets, "贴图", Icons.Default.Image),
    TabItem(EditorTab.CrossRef, "交叉引用", Icons.Default.Search),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    onSelectRoot: () -> Unit,
    onReload: () -> Unit,
    onSave: () -> Unit,
    content: @Composable (EditorTab) -> Unit = { when(it) {
        EditorTab.Save -> SaveEditorScreen()
        EditorTab.Events -> EventEditorScreen()
        EditorTab.Battle -> BattleEditorScreen()
        EditorTab.WorldMap -> WorldMapScreen()
        EditorTab.Assets -> AssetViewerScreen()
        EditorTab.CrossRef -> CrossRefScreen()
    }}
) {
    var selected by remember { mutableStateOf(EditorTab.Save) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KYS编辑器") },
                actions = {
                    IconButton(onClick = onSelectRoot) { Icon(Icons.Default.FolderOpen, "选择目录") }
                    IconButton(onClick = onReload) { Icon(Icons.Default.Refresh, "重载") }
                    IconButton(onClick = onSave) { Icon(Icons.Default.Save, "保存") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = selected == item.tab,
                        onClick = { selected = item.tab },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            content(selected)
        }
    }
}
