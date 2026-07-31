package com.kys.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp

@Composable
fun CrossRefScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext
    val ranger by ec.ranger.collectAsStateWithLifecycle()

    var searchId by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf("item") }
    var results by remember { mutableStateOf(listOf<String>()) }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchId, onValueChange = { searchId = it },
                label = { Text("ID") },
                modifier = Modifier.width(100.dp), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            listOf("item" to "物品", "magic" to "武功", "role" to "角色").forEach { (k, v) ->
                FilterChip(
                    selected = searchType == k,
                    onClick = { searchType = k },
                    label = { Text(v) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val id = searchId.toIntOrNull() ?: return@Button
                val r = ranger
                results = if (r != null) buildList {
                    when (searchType) {
                        "item" -> {
                            // Inventory
                            r.header.inventory.forEachIndexed { i, slot ->
                                if (slot.number == id) add("背包槽#${i+1}: 数量${slot.amount}")
                            }
                            // Shops
                            for (si in 0 until r.shops.count) {
                                val rec = r.shops.records[si]
                                for (w in 1 until rec.size step 2) {
                                    if (w+1 < rec.size && rec[w] == id) {
                                        add("商店#$si 位置${w/2+1}: 数量${rec[w+1]}")
                                    }
                                }
                            }
                        }
                        "magic" -> {
                            for (ri in 0 until r.roles.count) {
                                val rec = r.roles.records[ri]
                                for (w in 10 until minOf(30, rec.size)) {
                                    if (rec[w] == id) add("角色#${ri}(${r.roleName(ri)}) 武功位${w-10}")
                                }
                            }
                        }
                        "role" -> {
                            r.header.team.forEachIndexed { i, tid ->
                                if (tid == id) add("队伍队友#${i+1}")
                            }
                        }
                    }
                    if (isEmpty()) add("未找到引用")
                } else listOf("请先加载存档")
            }) { Text("搜索") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(results) { line ->
                Text(line, Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
            }
        }
    }
}
