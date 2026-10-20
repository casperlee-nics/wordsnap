package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// 單字本主畫面：列出所有單字，右下角按鈕可新增
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(vm: WordViewModel = viewModel()) {
    val words by vm.words.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的單字本（${words.size}）") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新增單字")
            }
        },
    ) { padding ->
        if (words.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("還沒有單字，按右下角 + 新增第一個")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(words, key = { it.id }) { word ->
                    ListItem(
                        headlineContent = { Text(word.text) },
                        supportingContent = { Text(word.meaning) },
                        trailingContent = {
                            IconButton(onClick = { vm.delete(word) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "刪除")
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAdd) {
        AddWordDialog(
            onDismiss = { showAdd = false },
            onSave = { text, meaning ->
                vm.add(text, meaning)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AddWordDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增單字") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("英文單字") }, singleLine = true)
                OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("中文意思") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, meaning) }, enabled = text.isNotBlank()) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
