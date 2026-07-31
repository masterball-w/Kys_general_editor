package com.kys.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp
import com.kys.editor.codec.Instruction
import com.kys.editor.codec.RangerArchive
import com.kys.editor.codec.TalkArchive
import com.kys.editor.codec.WarArchive
import com.kys.editor.codec.meta.OpcodeContext
import com.kys.editor.codec.meta.OpcodeZh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Bridges the loaded archives into the opcode meta resolver so instruction
 *  arguments can be rendered as readable text (role/item/scene/talk names). */
private class ScreenOpcodeContext(
    private val ranger: RangerArchive?,
    private val talk: TalkArchive?,
    private val war: WarArchive?
) : OpcodeContext {
    override fun talkGetText(id: Int): String? =
        talk?.let { runCatching { it.getText(id) }.getOrNull() }?.takeIf { it.isNotEmpty() }
    override fun nameGetText(id: Int): String? = null
    override fun itemName(id: Int): String? =
        ranger?.let { runCatching { it.itemName(id) }.getOrNull() }
    override fun roleName(id: Int): String? =
        ranger?.let { runCatching { it.roleName(id) }.getOrNull() }
    override fun magicName(id: Int): String? =
        ranger?.let { runCatching { it.magicName(id) }.getOrNull() }
    override fun sceneName(id: Int): String? =
        ranger?.let { runCatching { it.sceneName(id) }.getOrNull() }
    override fun battleName(id: Int): String? =
        war?.findByNum(id)?.name
}

/** Returns the talk entry id referenced by [ins], or -1 when the instruction
 *  does not reference talk text. Based on the opcode arg specs:
 *  - 1  Dialogue:  args[0] = 对话ID
 *  - 68 NewTalk:   args[1] = 对话ID
 *  - 70 ShowTitle: args[0] = 标题对话ID */
private fun talkIdFor(ins: Instruction): Int = when (ins.opcode) {
    1 -> if (ins.args.isNotEmpty()) ins.args[0] else -1
    68 -> if (ins.args.size > 1) ins.args[1] else -1
    70 -> if (ins.args.isNotEmpty()) ins.args[0] else -1
    else -> -1
}

@Composable
fun EventEditorScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext
    val kdef by ec.kdef.collectAsStateWithLifecycle()
    val talk by ec.talk.collectAsStateWithLifecycle()
    val ranger by ec.ranger.collectAsStateWithLifecycle()
    val war by ec.war.collectAsStateWithLifecycle()
    val isLoading by ec.isLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val k = kdef
    if (k == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先加载存档")
        }
        return
    }

    val opCtx = remember(ranger, talk, war) { ScreenOpcodeContext(ranger, talk, war) }
    var selectedScript by remember { mutableIntStateOf(1) }
    if (selectedScript < 1 || selectedScript > k.scriptCount) selectedScript = 1
    var savingTalkId by remember { mutableIntStateOf(-1) }
    var statusMsg by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize()) {
        // ---- Event list: ID + brief description ----
        LazyColumn(Modifier.width(150.dp)) {
            items(k.scriptCount, key = { it + 1 }) { idx ->
                val id = idx + 1
                val brief = remember(id, k, talk) {
                    runCatching {
                        val s = k.getScript(id)
                        val first = s.instructions.firstOrNull { it.opcode >= 0 }
                        if (first == null) {
                            "空"
                        } else {
                            val name = first.nameZh
                            val tid = talkIdFor(first)
                            val t = talk?.getText(tid)?.replace("\n", " ")?.trim()
                            if (t.isNullOrEmpty()) name else "$name: ${t.take(24)}"
                        }
                    }.getOrDefault("—")
                }
                Surface(
                    onClick = { selectedScript = id },
                    color = if (selectedScript == id) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("事件 #$id", style = MaterialTheme.typography.labelSmall)
                        Text(
                            brief,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        VerticalDivider()

        // ---- Event detail ----
        val script = remember(selectedScript, k) {
            runCatching { k.getScript(selectedScript) }.getOrNull()
        }
        val typeSummary = remember(selectedScript, k) {
            script?.instructions
                ?.filter { it.opcode >= 0 }
                ?.map { it.nameZh }
                ?.distinct()
                ?.take(8)
                ?.joinToString("、")
                .orEmpty()
        }

        LazyColumn(Modifier.weight(1f).padding(8.dp)) {
            val s = script
            if (s == null) {
                item {
                    Text(
                        "无法读取事件 #$selectedScript",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                return@LazyColumn
            }
            item {
                Text("事件 #$selectedScript", style = MaterialTheme.typography.titleMedium)
                Text(
                    "指令数: ${s.instructions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (typeSummary.isNotEmpty()) {
                    Text(
                        "事件类型: $typeSummary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (statusMsg.isNotEmpty()) {
                    Text(
                        statusMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }
            items(s.instructions.size, key = { it }) { idx ->
                val ins = s.instructions[idx]
                InstructionRow(ins, opCtx)
                val talkArch = talk
                val tid = talkIdFor(ins)
                if (tid >= 0 && talkArch != null) {
                    TalkEditorCard(
                        talkId = tid,
                        talk = talkArch,
                        saving = savingTalkId == tid,
                        onSave = { newText ->
                            savingTalkId = tid
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        talkArch.setText(tid, newText)
                                        talkArch.save()
                                    }
                                }.onSuccess {
                                    statusMsg = "对话 #$tid 已保存"
                                }.onFailure { e ->
                                    statusMsg = "保存失败: ${e.message}"
                                }
                                savingTalkId = -1
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(ins: Instruction, opCtx: OpcodeContext?) {
    val pc = ins.pc.toString(16).padStart(4, '0')
    val resolved = remember(ins.opcode, ins.args, opCtx) {
        if (ins.args.isEmpty()) "" else OpcodeZh.formatArgsTooltip(opCtx, ins.opcode, ins.args.toIntArray())
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            "$pc: ${ins.nameZh}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (ins.opcode < 0) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface
        )
        if (resolved.isNotEmpty()) {
            Text(
                resolved,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun TalkEditorCard(
    talkId: Int,
    talk: TalkArchive,
    saving: Boolean,
    onSave: (String) -> Unit
) {
    var text by remember(talkId, talk) { mutableStateOf(talk.getText(talkId)) }
    Card(
        Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("对话 #$talkId", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(text) },
                    enabled = !saving
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存")
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                minLines = 2
            )
        }
    }
}
