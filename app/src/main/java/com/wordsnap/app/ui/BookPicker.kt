package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wordsnap.app.data.Book

// 選單字簿的下拉選單；selectedId 為 null 代表「全部」
@Composable
fun BookPicker(
    books: List<Book>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    label: String,
    allowAll: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val current = books.firstOrNull { it.id == selectedId }?.name ?: if (allowAll) "全部" else "選擇單字簿"

    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }) { Text("$label$current ▼") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowAll) {
                DropdownMenuItem(text = { Text("全部") }, onClick = { onSelect(null); expanded = false })
            }
            books.forEach { book ->
                DropdownMenuItem(text = { Text(book.name) }, onClick = { onSelect(book.id); expanded = false })
            }
            DropdownMenuItem(text = { Text("+ 新增單字簿") }, onClick = { creating = true; expanded = false })
        }
    }

    if (creating) {
        NewBookDialog(
            onDismiss = { creating = false },
            onCreate = { name ->
                onCreate(name)
                creating = false
            },
        )
    }
}

@Composable
private fun NewBookDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增單字簿") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(16) },
                label = { Text("名稱，例如：課本、多益") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("建立") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
