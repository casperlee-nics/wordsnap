package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordsnap.app.data.Book
import com.wordsnap.app.ui.theme.PixelRed

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
    // 放在單字簿標題列上時，用復古按鈕加上打開清單的捲軸按鈕
    onTopBar: Boolean = false,
    // 有傳進來才會在每一本後面顯示垃圾桶；只剩一本時不能刪
    onDelete: ((Book) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Book?>(null) }
    val current = books.firstOrNull { it.id == selectedId }?.let(::bookLabel) ?: if (allowAll) "全部" else "選擇單字簿"

    Box(modifier) {
        if (onTopBar) {
            // 冒險日誌名稱做成復古按鈕，右邊再一顆捲軸按鈕打開清單（兩個都能打開）
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroButton(
                    "$label$current",
                    onClick = { expanded = true },
                    modifier = Modifier.widthIn(max = 170.dp),
                    textStyle = MaterialTheme.typography.labelLarge,
                )
                RetroIconButton(IconBookList, "選擇冒險日誌", onClick = { expanded = true })
            }
        } else {
            PixelOutlinedButton(onClick = { expanded = true }) { Text("$label$current ▼") }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowAll) {
                DropdownMenuItem(text = { Text("全部") }, onClick = { onSelect(null); expanded = false })
            }
            books.forEach { book ->
                DropdownMenuItem(
                    text = { Text(bookLabel(book)) },
                    onClick = { onSelect(book.id); expanded = false },
                    trailingIcon = if (onDelete != null && books.size > 1) {
                        {
                            IconButton(onClick = { deleting = book; expanded = false }) {
                                Icon(Icons.Filled.Delete, contentDescription = "刪除 ${book.name}")
                            }
                        }
                    } else {
                        null
                    },
                )
            }
            DropdownMenuItem(text = { Text("+ 新增冒險日誌") }, onClick = { creating = true; expanded = false })
        }
    }

    deleting?.let { book ->
        PixelAlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("刪除「${book.name}」？") },
            text = { Text("這本冒險日誌裡的單字和複習進度都會一起刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke(book); deleting = null }) { Text("刪除", color = PixelRed) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
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
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增冒險日誌") },
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

// 通過戰鬥考試的單字簿前面加一顆星
private fun bookLabel(book: Book) = if (book.clearedAt != null) "★${book.name}" else book.name
