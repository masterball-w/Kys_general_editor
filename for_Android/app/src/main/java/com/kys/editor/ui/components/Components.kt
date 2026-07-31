package com.kys.editor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    min: Int = -32768,
    max: Int = 32767,
    modifier: Modifier = Modifier,
    hexMode: Boolean = false
) {
    var text by remember(value) { mutableStateOf(if (hexMode) "0x${value.toString(16)}" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            text = s
            val v = try {
                if (hexMode && s.startsWith("0x")) s.substring(2).toInt(16)
                else s.toInt()
            } catch (_: NumberFormatException) { null }
            v?.let { if (it in min..max) onValueChange(it) }
        },
        label = { Text(label) },
        modifier = modifier.width(110.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamedIdCombo(
    currentId: Int,
    ids: List<Int>,
    nameForId: (Int) -> String,
    onSelect: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "$currentId: ${nameForId(currentId)}",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ids.forEach { id ->
                DropdownMenuItem(
                    text = { Text("$id: ${nameForId(id)}") },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun EditableTable(
    headers: List<String>,
    rows: List<List<Any>>,
    onCellEdit: (row: Int, col: Int, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier) {
        item {
            Row(Modifier.fillMaxWidth()) {
                headers.forEach { h ->
                    Text(h, Modifier.weight(1f).padding(4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider()
        }
        items(rows.size) { r ->
            Row(Modifier.fillMaxWidth()) {
                rows[r].forEachIndexed { c, cell ->
                    var text by remember(r, c) { mutableStateOf(cell.toString()) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it; onCellEdit(r, c, it) },
                        modifier = Modifier.weight(1f).padding(2.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )
                }
            }
        }
    }
}
