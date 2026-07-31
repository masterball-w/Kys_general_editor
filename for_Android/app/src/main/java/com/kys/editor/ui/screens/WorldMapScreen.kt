package com.kys.editor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp
import com.kys.editor.codec.WORLD_MAP_SIZE
import kotlinx.coroutines.launch

@Composable
fun WorldMapScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext
    val wm by ec.worldMap.collectAsStateWithLifecycle()
    val ranger by ec.ranger.collectAsStateWithLifecycle()
    val isLoading by ec.isLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val entrances = remember(ranger) {
        val arch = ranger ?: return@remember emptyList()
        val list = mutableListOf<Triple<Int, Pair<Int, Int>, String>>()
        for (i in 0 until arch.scenes.count) {
            val rec = arch.scenes.records[i]
            if (rec.size >= 5) {
                val x = rec[3]
                val y = rec[4]
                if (x in 0..WORLD_MAP_SIZE && y in 0..WORLD_MAP_SIZE && (x != 0 || y != 0)) {
                    list.add(Triple(i, x to y, arch.sceneName(i)))
                }
            }
        }
        list.take(50)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { scope.launch { ec.loadWorldMap() } },
                enabled = !isLoading
            ) {
                Text("加载大地图")
            }
            if (wm != null) {
                Button(
                    onClick = { scope.launch { ec.loadWorldMap() } },
                    enabled = !isLoading
                ) {
                    Text("刷新")
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(bgColor, size = size)
                val map = wm
                if (map != null) {
                    val surfaceGrid = map.getLayer("surface")?.grid
                    if (surfaceGrid != null && surfaceGrid.isNotEmpty() && surfaceGrid[0].isNotEmpty()) {
                        val w = surfaceGrid.size
                        val h = surfaceGrid[0].size
                        val tileSize = size.minDimension / maxOf(w, h).toFloat()
                        for (x in 0 until minOf(w, WORLD_MAP_SIZE)) {
                            for (y in 0 until minOf(h, WORLD_MAP_SIZE)) {
                                val v = surfaceGrid[x][y]
                                val shade = ((v * 3) % 256) / 255f
                                drawCircle(
                                    Color(
                                        red = 0.2f + shade * 0.3f,
                                        green = 0.4f + shade * 0.4f,
                                        blue = 0.2f
                                    ),
                                    radius = tileSize / 2,
                                    center = Offset(x * tileSize + tileSize / 2, y * tileSize + tileSize / 2)
                                )
                            }
                        }
                    }
                }
            }
            if (wm == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("大地图未加载", Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(8.dp))
                    Text("请先加载存档后从目录中加载MMAP文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant)
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().height(200.dp).padding(8.dp)) {
            item { Text("场景入口点:", style = MaterialTheme.typography.titleSmall) }
            items(entrances) { (id, pos, name) ->
                Text("场景#$id \"$name\" @ (${pos.first},${pos.second})",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
