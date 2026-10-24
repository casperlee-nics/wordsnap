package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.wordsnap.app.data.Book
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelPaper
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
            // 冒險日誌名稱和下拉捲軸放在同一個復古按鈕框裡
            RetroButtonWithIcon(
                "$label$current",
                IconBookList,
                onClick = { expanded = true },
                modifier = Modifier.widthIn(max = 180.dp),
                // 比一般按鈕矮一點，不會壓到標題列的外框
                height = 40.dp,
                iconSize = 22.dp,
            )
        } else {
            PixelOutlinedButton(onClick = { expanded = true }) { Text("$label$current ▼") }
        }
        // 下拉清單也是羊皮紙底＋木紋細框，和整個遊戲的風格一樣
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RectangleShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.woodFrame(PixelPaper).padding(horizontal = 6.dp),
        ) {
            if (allowAll) {
                BookMenuRow("全部", chosen = selectedId == null, onClick = { onSelect(null); expanded = false })
            }
            books.forEach { book ->
                BookMenuRow(
                    bookLabel(book),
                    chosen = book.id == selectedId,
                    onClick = { onSelect(book.id); expanded = false },
                    onDelete = if (onDelete != null && books.size > 1) {
                        { deleting = book; expanded = false }
                    } else {
                        null
                    },
                )
            }
            Box(Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth().height(2.dp).background(MenuDivider))
            BookMenuRow("新增冒險日誌", icon = IconPencil, onClick = { creating = true; expanded = false })
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
            // 和新冒險一樣：寫在羊皮書卷上，右邊蓋章確認
            ScrollNameInput(
                value = name,
                onValueChange = { name = it.take(16) },
                placeholder = "例如：課本、多益",
                onConfirm = { onCreate(name.trim()) },
            )
        },
        // 點視窗外面就能關掉，不需要「取消」
        confirmButton = {},
    )
}

// 下拉清單的一列：目前這本底下有淡淡的金色；右邊可以放像素垃圾桶
@Composable
private fun BookMenuRow(
    text: String,
    onClick: () -> Unit,
    chosen: Boolean = false,
    icon: List<String>? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (chosen) PixelGold.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) PixelArt(icon, ColorIconPalette, Modifier.padding(end = 8.dp).size(24.dp))
        Text(text, color = PixelInk, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
        if (onDelete != null) {
            PixelIconPlain(IconTrash, "刪除 $text", onClick = onDelete, size = 32.dp)
        }
    }
}

private val MenuDivider = Color(0x40202030)

// 通過戰鬥考試的單字簿前面加一顆星
private fun bookLabel(book: Book) = if (book.clearedAt != null) "★${book.name}" else book.name
