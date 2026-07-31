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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp
import com.kys.editor.codec.RangerArchive
import com.kys.editor.codec.WarFieldArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BattleEditorScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext
    val war by ec.war.collectAsStateWithLifecycle()
    val ranger by ec.ranger.collectAsStateWithLifecycle()
    val dataRoot by ec.dataRoot.collectAsStateWithLifecycle()
    val isLoading by ec.isLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val w = war
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先加载存档 (War.sta)")
        }
        return
    }

    var selected by remember { mutableIntStateOf(0) }
    if (selected !in w.records.indices) selected = 0
    val rec = w.records[selected]
    val lay = rec.layout

    // On-demand battle field (warfld.idx/grp) load for the map preview.
    var warField by remember { mutableStateOf<WarFieldArchive?>(null) }
    var fieldLoading by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dataRoot) {
        val root = dataRoot
        if (root == null) {
            warField = null
            return@LaunchedEffect
        }
        fieldLoading = true
        fieldError = null
        withContext(Dispatchers.IO) {
            runCatching {
                val wf = WarFieldArchive()
                wf.load(root.child("resource"))
                warField = wf
            }.onFailure { fieldError = it.message ?: "未知错误" }
        }
        fieldLoading = false
    }

    var nameText by remember(selected) { mutableStateOf(rec.name) }
    var statusMsg by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize()) {
        // ---- Battle list ----
        LazyColumn(Modifier.width(160.dp)) {
            items(w.records.size, key = { it }) { i ->
                val r = w.records[i]
                Surface(
                    onClick = { selected = i },
                    color = if (selected == i) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(6.dp)) {
                        Text("#${r.battleNum}", style = MaterialTheme.typography.labelSmall)
                        Text(r.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }

        VerticalDivider()

        // ---- Battle detail ----
        LazyColumn(Modifier.weight(1f).padding(8.dp)) {
            item {
                Text("战斗 #${rec.battleNum}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { s ->
                        nameText = s
                        rec.name = s
                    },
                    label = { Text("战斗名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            saving = true
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { w.save() }
                                }.onSuccess {
                                    statusMsg = "战斗数据已保存"
                                }.onFailure { e ->
                                    statusMsg = "保存失败: ${e.message}"
                                }
                                saving = false
                            }
                        },
                        enabled = !saving
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存战斗")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (statusMsg.isNotEmpty()) {
                        Text(
                            statusMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "编号: ${rec.battleNum}    地图ID: ${rec.battleMap}    " +
                        "经验: ${rec.exp}    音乐: ${rec.music}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
            }

            // ---- Battle map preview ----
            item {
                BattleMapPreview(
                    wf = warField,
                    fieldId = rec.battleMap,
                    loading = fieldLoading,
                    error = fieldError
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }

            // ---- Mate list (allies) ----
            item {
                Text("队友 (${lay.mateCount})", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
            }
            items(lay.mateCount, key = { "m$it" }) { i ->
                val m = rec.mate(i)
                val mx = rec.mateX(i)
                val my = rec.mateY(i)
                Text(
                    "  [$i] ${roleLabel(ranger, m)}  位置($mx,$my)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            item { Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp)) }

            // ---- Enemy list ----
            item {
                Text("敌人 (${lay.enemyCount})", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
            }
            items(lay.enemyCount, key = { "e$it" }) { i ->
                val e = rec.enemy(i)
                val ex = rec.enemyX(i)
                val ey = rec.enemyY(i)
                Text(
                    "  [$i] ${roleLabel(ranger, e)}  位置($ex,$ey)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            // ---- Auto-mate list ----
            if (lay.autoMateOff >= 0 && lay.autoMateCount > 0) {
                item {
                    Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                    Text("自动队友 (${lay.autoMateCount})", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(2.dp))
                }
                items(lay.autoMateCount, key = { "a$it" }) { i ->
                    val a = rec.autoMate(i)
                    Text(
                        "  [$i] ${roleLabel(ranger, a)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ---- Event triggers ----
            item {
                Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                Text("事件触发", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
                if (lay.boutEventOff >= 0) {
                    Text(
                        "  回合事件: ${rec.boutEvent}  (kdef 脚本 ID)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (lay.operationEventOff >= 0) {
                    Text(
                        "  操作事件: ${rec.operationEvent}  (kdef 脚本 ID)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ---- Rewards ----
            item {
                Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                Text("奖励", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(2.dp))
            }
            if (lay.getKongfuOff >= 0 && lay.getKongfuCount > 0) {
                item {
                    val ks = (0 until lay.getKongfuCount)
                        .map { rec.getKongfu(it) }
                        .joinToString(", ") { if (it < 0) "—" else it.toString() }
                    Text(
                        "  武功: $ks",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (lay.getItemsOff >= 0 && lay.getItemsCount > 0) {
                item {
                    val its = (0 until lay.getItemsCount)
                        .map { rec.getItems(it) }
                        .joinToString(", ") { v ->
                            if (v < 0) "—" else "$v:${ranger?.itemName(v) ?: "?"}"
                        }
                    Text(
                        "  物品: $its",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (lay.getMoneyOff >= 0) {
                item {
                    Text(
                        "  金钱: ${rec.getMoney}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun roleLabel(ranger: RangerArchive?, id: Int): String {
    if (id < 0) return "(空)"
    val n = ranger?.let { runCatching { it.roleName(id) }.getOrNull() }
    return if (n.isNullOrEmpty()) "角色#$id" else "「$n」(#$id)"
}

@Composable
private fun BattleMapPreview(
    wf: WarFieldArchive?,
    fieldId: Int,
    loading: Boolean,
    error: String?
) {
    Text("战场地图预览 (MapID=$fieldId)", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    when {
        loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("加载 warfld 中…", style = MaterialTheme.typography.bodySmall)
            }
        }
        error != null -> Text(
            "地图加载失败: $error",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        wf == null -> Text(
            "未加载 warfld.idx/grp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        fieldId !in 0 until wf.count -> Text(
            "地图 #$fieldId 不在 warfld 范围 (0..${wf.count - 1})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> {
            val field = fieldId
            Canvas(Modifier.fillMaxWidth().height(240.dp)) {
                val n = WarFieldArchive.FIELD_SIZE
                val tile = size.minDimension / n
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        val v = runCatching { wf.get(field, 0, x, y) }.getOrDefault(0)
                        drawRect(
                            color = Color(
                                red = ((v * 7) and 0xFF) / 255f,
                                green = ((v * 13) and 0xFF) / 255f,
                                blue = ((v * 29) and 0xFF) / 255f
                            ),
                            topLeft = Offset(x * tile, y * tile),
                            size = Size(tile, tile)
                        )
                    }
                }
            }
        }
    }
}
