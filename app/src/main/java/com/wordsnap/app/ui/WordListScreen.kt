package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.sp
import com.wordsnap.app.data.Word
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelShape
import com.wordsnap.app.ui.theme.pixelTopBarButtonColors
import com.wordsnap.app.ui.theme.pixelTopBarColors
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.translate.ModelStatus

// 單字本主畫面：列出所有單字，右下角可拍照或手動新增，點單字可編輯
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    vm: WordViewModel,
    onScan: () -> Unit,
    onReview: () -> Unit,
    onListen: () -> Unit,
    onBattle: (Long) -> Unit,
    onHome: () -> Unit,
) {
    val words by vm.words.collectAsState()
    val books by vm.books.collectAsState()
    val selectedBookId by vm.selectedBookId.collectAsState()
    val profile by vm.currentProfile.collectAsState()
    val allWords by vm.allWords.collectAsState()
    val unlock = remember(allWords, selectedBookId) { vm.battleUnlock(selectedBookId) }
    var showMenu by remember { mutableStateOf(false) }
    var showHero by remember { mutableStateOf(false) }
    val speaker = rememberSpeaker()
    val translatorStatus by vm.translatorStatus.collectAsState()
    val hasBlank = words.any { it.meaning.isBlank() }
    val now = System.currentTimeMillis()
    val dueCount = words.count { it.dueAt <= now }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Word?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 點人物打開選單：人物能力、離開遊戲
                    Box {
                        Row(
                            Modifier.clickable { showMenu = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PixelAvatar(Avatar.from(profile?.avatar), size = 32.dp)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(profile?.name ?: "", maxLines = 1)
                                Text("Lv ${levelInfo(profile?.exp ?: 0).level}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("人物能力") }, onClick = { showMenu = false; showHero = true })
                            DropdownMenuItem(text = { Text("離開遊戲") }, onClick = { showMenu = false; onHome() })
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onReview, enabled = dueCount > 0, colors = pixelTopBarButtonColors()) {
                        Text("複習（$dueCount）")
                    }
                },
                colors = pixelTopBarColors(),
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallFloatingActionButton(onClick = onListen) {
                    Text("朗讀", Modifier.padding(horizontal = 10.dp))
                }
                SmallFloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "手動新增單字")
                }
                FloatingActionButton(onClick = onScan) {
                    Text("拍照", Modifier.padding(horizontal = 16.dp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookPicker(
                    books = books,
                    selectedId = selectedBookId,
                    onSelect = vm::selectBook,
                    onCreate = { name -> vm.createBook(name) { id -> vm.selectBook(id) } },
                    label = "單字簿：",
                    allowAll = true,
                    modifier = Modifier.weight(1f),
                )
                Text("${words.size} 個字", style = MaterialTheme.typography.bodyLarge)
            }
            BattleBanner(selectedBookId, unlock, onBattle)
            if (hasBlank) TranslatorBanner(translatorStatus, onRetry = vm::retryTranslation)
            if (words.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("還沒有單字，按右下角「拍照」或 + 新增第一個")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(words, key = { it.id }) { word ->
                        ListItem(
                            modifier = Modifier.clickable { editing = word },
                            headlineContent = {
                                Text(word.text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                            },
                            supportingContent = {
                                Text(
                                    word.meaning.ifBlank { blankMeaningHint(translatorStatus) },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { speaker.speak(word.text) }) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "發音")
                                    }
                                    IconButton(onClick = { vm.delete(word) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "刪除")
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showHero) {
        profile?.let { HeroDialog(it, onDismiss = { showHero = false }) }
    }
    if (showAdd) {
        WordDialog(
            title = "新增單字",
            onDismiss = { showAdd = false },
            onSave = { text, meaning ->
                vm.add(text, meaning)
                showAdd = false
            },
        )
    }
    editing?.let { word ->
        WordDialog(
            title = "編輯單字",
            initialText = word.text,
            initialMeaning = word.meaning,
            onDismiss = { editing = null },
            onSave = { text, meaning ->
                vm.update(word, text, meaning)
                editing = null
            },
        )
    }
}

@Composable
private fun WordDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    initialText: String = "",
    initialMeaning: String = "",
) {
    var text by remember { mutableStateOf(initialText) }
    var meaning by remember { mutableStateOf(initialMeaning) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("英文單字") }, singleLine = true)
                OutlinedTextField(value = meaning, onValueChange = { meaning = it }, label = { Text("中文意思（空白會自動翻譯）") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, meaning) }, enabled = text.isNotBlank()) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// 中文意思還空著時，依翻譯檔狀態顯示不同提示
private fun blankMeaningHint(status: ModelStatus): String = when (status) {
    ModelStatus.Downloading -> "（等待翻譯檔下載…）"
    ModelStatus.Failed -> "（無法自動翻譯，點一下自己填寫）"
    else -> "（翻譯中，或點一下自己填寫）"
}

@Composable
private fun TranslatorBanner(status: ModelStatus, onRetry: () -> Unit) {
    val message = when (status) {
        ModelStatus.Downloading -> "第一次使用，正在下載翻譯檔（約 30 MB），完成後會自動翻譯"
        ModelStatus.Failed -> "翻譯檔下載失敗，請確認網路後重試"
        else -> return
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (status == ModelStatus.Failed) TextButton(onClick = onRetry) { Text("重試") }
        }
    }
}

// 戰鬥模式的入口：解鎖了就能出戰，還沒解鎖就顯示還差多少
@Composable
private fun BattleBanner(bookId: Long?, unlock: BattleUnlock, onBattle: (Long) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = PixelBorder,
        shape = PixelShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val text = when {
                bookId == null -> "選一本單字簿，就能挑戰戰鬥模式"
                unlock.unlocked -> "戰鬥模式已解鎖！"
                else -> "戰鬥模式：再記住 ${(BATTLE_SIZE - unlock.remembered).coerceAtMost(unlock.total - unlock.remembered).coerceAtLeast(1)} 個字解鎖"
            }
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (bookId != null && unlock.unlocked) {
                Button(onClick = { onBattle(bookId) }) { Text("出戰") }
            }
        }
    }
}
