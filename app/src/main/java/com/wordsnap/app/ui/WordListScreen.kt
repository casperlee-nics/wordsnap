package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordsnap.app.R
import com.wordsnap.app.data.Word
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.translate.ModelStatus
import com.wordsnap.app.ui.theme.PixelInk

// 單字本主畫面：列出所有單字，右下角可拍照或手動新增，點單字可編輯
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    vm: WordViewModel,
    onScan: (ScanSource) -> Unit,
    onReview: () -> Unit,
    onListen: () -> Unit,
    onBattle: (Long) -> Unit,
    onHome: () -> Unit,
    // 從拍照／相簿／古書返回時，一進來就打開加單字選單
    startWithAddChoice: Boolean = false,
    onAddChoiceShown: () -> Unit = {},
) {
    val words by vm.words.collectAsState()
    val books by vm.books.collectAsState()
    val selectedBookId by vm.selectedBookId.collectAsState()
    val profile by vm.currentProfile.collectAsState()
    val allWords by vm.allWords.collectAsState()
    val unlock = remember(allWords, selectedBookId) { vm.battleUnlock(selectedBookId) }
    var showMenu by remember { mutableStateOf(false) }
    var showHero by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    val itemCounts by vm.items.collectAsState()
    val speaker = rememberSpeaker()
    val translatorStatus by vm.translatorStatus.collectAsState()
    val hasBlank = words.any { it.meaning.isBlank() }
    val now = System.currentTimeMillis()
    val dueCount = words.count { it.dueAt <= now }
    var showAdd by remember { mutableStateOf(false) }
    // 按「加單字」先選手動輸入或拍照
    var choosingAdd by remember { mutableStateOf(startWithAddChoice) }
    LaunchedEffect(Unit) { if (startWithAddChoice) onAddChoiceShown() }
    // 單字簿頁面播放清淡的背景音樂，念單字時調小聲
    val context = LocalContext.current
    val musicOn = remember { isMusicOn(context) }
    LoopingMusic(R.raw.book_bgm, enabled = musicOn, volume = if (speaker.speaking) 0.05f else 0.35f)
    var editing by remember { mutableStateOf<Word?>(null) }
    // 整本都答對兩次了，進戰鬥前先提醒是練習戰
    var confirmPractice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // 羊皮紙紋路的標題列：左邊人物（點了打開選單），右邊冒險日誌按鈕
            Box(
                Modifier
                    .fillMaxWidth()
                    .parchment()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 2.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 點人物打開選單：人物能力、道具欄、回到主畫面
                    Box(Modifier.weight(1f)) {
                        Row(
                            Modifier.clickable { showMenu = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PixelAvatar(Avatar.from(profile?.avatar), size = 48.dp, tier = avatarTier(levelInfo(profile?.exp ?: 0).level))
                            Column(Modifier.padding(start = 6.dp)) {
                                Text(
                                    profile?.name ?: "",
                                    color = PixelInk,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Lv ${levelInfo(profile?.exp ?: 0).level}",
                                    color = PixelInk,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("人物能力") }, onClick = { showMenu = false; showHero = true })
                            DropdownMenuItem(text = { Text("道具欄") }, onClick = { showMenu = false; showItems = true })
                            DropdownMenuItem(text = { Text("回到主畫面") }, onClick = { showMenu = false; onHome() })
                        }
                    }
                    // 右上角切換冒險日誌
                    BookPicker(
                        books = books,
                        selectedId = selectedBookId,
                        onSelect = vm::selectBook,
                        onCreate = { name -> vm.createBook(name) { id -> vm.selectBook(id) } },
                        label = "",
                        allowAll = true,
                        onTopBar = true,
                        onDelete = vm::deleteBook,
                    )
                }
            }
        },
        // 下方一排按鈕：朗讀、複習、加單字、戰鬥；放在底部不會蓋住最後一個單字
        bottomBar = {
            val battleBook = selectedBookId
            BottomActions(
                onListen = onListen,
                // 全部都記住了也能按，進去會隨機複習整本
                reviewLabel = if (dueCount > 0) "複習 $dueCount" else "複習",
                reviewEnabled = words.isNotEmpty(),
                onReview = onReview,
                onAdd = { choosingAdd = true },
                battleEnabled = battleBook != null && unlock.unlocked,
                onBattle = {
                    if (battleBook != null) {
                        if (unlock.exhausted) confirmPractice = true else onBattle(battleBook)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "共 ${words.size} 個字",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (unlock.exhausted) {
                Text(
                    "這本的單字都在戰鬥答對 $MAX_TESTS 次了，加新單字就能再拿到完整的經驗值和寶箱",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (hasBlank) TranslatorBanner(translatorStatus, onRetry = vm::retryTranslation)
            if (words.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Text("還沒有單字，按下方「加單字」新增第一個", textAlign = TextAlign.Center)
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
                                    PixelIconPlain(IconPlayWord, "發音", onClick = { speaker.speak(word.text) }, size = 44.dp, palette = DarkWoodPalette)
                                    PixelIconPlain(IconTrash, "刪除", onClick = { vm.delete(word) }, size = 44.dp)
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showItems) {
        ItemBagDialog(counts = itemCounts, onDismiss = { showItems = false })
    }
    if (confirmPractice) {
        PixelAlertDialog(
            onDismissRequest = { confirmPractice = false },
            title = { Text("練習戰") },
            text = { Text("這本的單字都已經在戰鬥中答對 $MAX_TESTS 次了。還是可以繼續練習，但經驗值只有 1/4，也不會掉寶箱。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmPractice = false
                    selectedBookId?.let(onBattle)
                }) { Text("開始練習") }
            },
            dismissButton = { TextButton(onClick = { confirmPractice = false }) { Text("取消") } },
        )
    }
    if (choosingAdd) {
        AddChoiceDialog(
            onDismiss = { choosingAdd = false },
            onManual = { choosingAdd = false; showAdd = true },
            onSource = { source -> choosingAdd = false; onScan(source) },
        )
    }
    if (showHero) {
        profile?.let {
            HeroDialog(
                it,
                remembered = allWords.count { w -> w.box >= 1 },
                onDismiss = { showHero = false },
                onClassChange = { avatar -> vm.changeClass(it, avatar) },
            )
        }
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

    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 英文單字的字比中文大一點，比較好看清楚拼字
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("英文單字") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                )
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

// 單字本下方的按鈕列；單字簿記住 15 個字或整本都記住，「戰鬥」就會亮起來
@Composable
private fun BottomActions(
    onListen: () -> Unit,
    reviewLabel: String,
    reviewEnabled: Boolean,
    onReview: () -> Unit,
    onAdd: () -> Unit,
    battleEnabled: Boolean,
    onBattle: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 和主畫面一樣的復古黃褐色像素按鈕
            val style = MaterialTheme.typography.labelLarge
            RetroButton("朗讀", onListen, Modifier.weight(1f), textStyle = style, textPadding = 2.dp)
            RetroButton(reviewLabel, onReview, Modifier.weight(1f), enabled = reviewEnabled, textStyle = style, textPadding = 2.dp)
            RetroButton("加單字", onAdd, Modifier.weight(1f), textStyle = style, textPadding = 2.dp)
            RetroButton("戰鬥", onBattle, Modifier.weight(1f), enabled = battleEnabled, textStyle = style, textPadding = 2.dp)
        }
    }
}

// 加單字：上面是手動輸入（筆和筆記本），下面是拍照（相機），按相機直接打開相機
@Composable
private fun AddChoiceDialog(onDismiss: () -> Unit, onManual: () -> Unit, onSource: (ScanSource) -> Unit) {
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加單字") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AddChoice(IconNotebook, "手動輸入單字怪物", onManual)
                AddChoice(IconCamera, "相機找出隱藏怪物") { onSource(ScanSource.Camera) }
                AddChoice(IconGallery, "相簿新增怪物") { onSource(ScanSource.Gallery) }
                AddChoice(IconOldBook, "讀取古書上的怪物") { onSource(ScanSource.Pdf) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun AddChoice(icon: List<String>, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixelArt(icon, ColorIconPalette, Modifier.size(56.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
}

