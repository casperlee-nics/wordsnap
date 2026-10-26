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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordsnap.app.R
import com.wordsnap.app.data.Book
import com.wordsnap.app.data.Word
import com.wordsnap.app.i18n.tr
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
    val monsterLog by vm.monsterLog.collectAsState()
    val allWords by vm.allWords.collectAsState()
    val unlock = remember(allWords, selectedBookId) { vm.battleUnlock(selectedBookId) }
    var showMenu by remember { mutableStateOf(false) }
    var showHero by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showSkills by remember { mutableStateOf(false) }
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
                    // 點人物打開選單：角色資料、打開口袋、旅行商人、回到主畫面
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
                            DropdownMenuItem(text = { Text(tr(R.string.hero_title)) }, onClick = { showMenu = false; showHero = true })
                            DropdownMenuItem(text = { Text(tr(R.string.skill_title)) }, onClick = { showMenu = false; showSkills = true })
                            DropdownMenuItem(text = { Text(tr(R.string.item_shelf)) }, onClick = { showMenu = false; showItems = true })
                            DropdownMenuItem(text = { Text(tr(R.string.shop_title)) }, onClick = { showMenu = false; showShop = true })
                            DropdownMenuItem(text = { Text(tr(R.string.menu_home)) }, onClick = { showMenu = false; onHome() })
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
                reviewLabel = if (dueCount > 0) tr(R.string.action_review_count, dueCount) else tr(R.string.action_review),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr(R.string.word_count, words.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 6.dp),
                )
                // 剛剛在這本刪掉的字可以復原：放在播放和垃圾桶的正上方
                val lastDeleted = vm.deletedWords.lastOrNull()
                if (lastDeleted != null && lastDeleted.bookId == selectedBookId) {
                    Row(
                        Modifier.padding(end = 16.dp).width(88.dp).clickable { vm.undoDelete() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        PixelArt(IconUndo, ColorIconPalette, Modifier.size(30.dp))
                        Text(tr(R.string.undo_delete), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            if (unlock.exhausted) {
                Text(
                    tr(R.string.book_exhausted, MAX_TESTS),
                    style = MaterialTheme.typography.bodySmall.readable(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (hasBlank) TranslatorBanner(translatorStatus, onRetry = vm::retryTranslation)
            if (words.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Text(tr(R.string.book_empty), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge.readable())
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
                                    style = MaterialTheme.typography.bodyLarge.readable(),
                                )
                            },
                            trailingContent = {
                                Row {
                                    PixelIconPlain(IconPlayWord, tr(R.string.pronounce), onClick = { speaker.speak(word.text) }, size = 44.dp, palette = DarkWoodPalette)
                                    PixelIconPlain(IconTrash, tr(R.string.delete), onClick = { vm.delete(word) }, size = 44.dp)
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showSkills) {
        profile?.let {
            SkillDialog(
                skills = Skill.of(Avatar.from(it.avatar).race),
                level = levelInfo(it.exp).level,
                onDismiss = { showSkills = false },
            )
        }
    }
    if (showShop) {
        ShopDialog(
            counts = itemCounts,
            coins = profile?.coins ?: 0,
            onBuy = vm::buyItem,
            onSell = vm::sellItem,
            onDismiss = { showShop = false },
        )
    }
    if (showItems) {
        ItemBagDialog(counts = itemCounts, onDismiss = { showItems = false })
    }
    if (confirmPractice) {
        PixelAlertDialog(
            onDismissRequest = { confirmPractice = false },
            title = { Text(tr(R.string.practice_battle)) },
            text = { Text(tr(R.string.practice_battle_message, MAX_TESTS), style = MaterialTheme.typography.bodyMedium.readable()) },
            confirmButton = {
                TextButton(onClick = {
                    confirmPractice = false
                    selectedBookId?.let(onBattle)
                }) { Text(tr(R.string.practice_start)) }
            },
            dismissButton = { TextButton(onClick = { confirmPractice = false }) { Text(tr(R.string.cancel)) } },
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
                remembered = allWords.count { w -> w.box >= 1 } + it.keptRemembered,
                onDismiss = { showHero = false },
                onClassChange = { avatar -> vm.changeClass(it, avatar) },
                monsterLog = monsterLog,
            )
        }
    }
    if (showAdd) {
        WordDialog(
            title = tr(R.string.word_add_title),
            duplicateIn = { vm.booksContaining(it) },
            onDismiss = { showAdd = false },
            onSave = { text, meaning ->
                vm.add(text, meaning)
                showAdd = false
            },
        )
    }
    editing?.let { word ->
        WordDialog(
            title = tr(R.string.word_edit_title),
            initialText = word.text,
            initialMeaning = word.meaning,
            duplicateIn = { vm.booksContaining(it, except = word) },
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
    // 輸入的字已經在哪幾本單字簿裡，有的話提醒一下（還是可以存）
    duplicateIn: (String) -> List<Book> = { emptyList() },
) {
    var text by remember { mutableStateOf(initialText) }
    val duplicates = remember(text) { duplicateIn(text) }
    var meaning by remember { mutableStateOf(initialMeaning) }

    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // 兩張羊皮書卷：上面寫英文單字（字比較大），下面寫中文意思
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr(R.string.word_field_english), style = MaterialTheme.typography.labelLarge)
                ScrollTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = tr(R.string.word_field_english_hint),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    height = 64.dp,
                    keyboardType = KeyboardType.Ascii,
                )
                if (duplicates.isNotEmpty()) {
                    Text(
                        tr(R.string.word_duplicate, duplicates.joinToString("、") { it.name }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(tr(R.string.word_field_meaning), style = MaterialTheme.typography.labelLarge)
                ScrollTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    placeholder = tr(R.string.word_field_meaning_hint),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleMedium,
                    height = 60.dp,
                    onDone = { if (text.isNotBlank()) onSave(text, meaning) },
                )
            }
        },
        confirmButton = {
            RetroButton(tr(R.string.save), onClick = { onSave(text, meaning) }, enabled = text.isNotBlank(), textStyle = MaterialTheme.typography.titleSmall)
        },
        dismissButton = { RetroButton(tr(R.string.cancel), onClick = onDismiss, textStyle = MaterialTheme.typography.titleSmall) },
    )
}

// 中文意思還空著時，依翻譯檔狀態顯示不同提示
private fun blankMeaningHint(status: ModelStatus): String = when (status) {
    ModelStatus.Downloading -> tr(R.string.meaning_waiting)
    ModelStatus.Failed -> tr(R.string.meaning_failed)
    else -> tr(R.string.meaning_translating)
}

@Composable
private fun TranslatorBanner(status: ModelStatus, onRetry: () -> Unit) {
    val message = when (status) {
        ModelStatus.Downloading -> tr(R.string.translator_downloading)
        ModelStatus.Failed -> tr(R.string.translator_failed)
        else -> return
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (status == ModelStatus.Failed) TextButton(onClick = onRetry) { Text(tr(R.string.retry)) }
        }
    }
}

// 單字本下方的按鈕列；單字簿記住 10 個字或整本都記住，「戰鬥」就會亮起來
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
            RetroButton(tr(R.string.action_listen), onListen, Modifier.weight(1f), textStyle = style, textPadding = 2.dp)
            RetroButton(reviewLabel, onReview, Modifier.weight(1f), enabled = reviewEnabled, textStyle = style, textPadding = 2.dp)
            RetroButton(tr(R.string.action_add), onAdd, Modifier.weight(1f), textStyle = style, textPadding = 2.dp)
            RetroButton(tr(R.string.action_battle), onBattle, Modifier.weight(1f), enabled = battleEnabled, textStyle = style, textPadding = 2.dp)
        }
    }
}

// 加單字：上面是手動輸入（筆和筆記本），下面是拍照（相機），按相機直接打開相機
@Composable
private fun AddChoiceDialog(onDismiss: () -> Unit, onManual: () -> Unit, onSource: (ScanSource) -> Unit) {
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.action_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AddChoice(IconNotebook, tr(R.string.add_manual), onManual)
                AddChoice(IconCamera, tr(R.string.add_camera)) { onSource(ScanSource.Camera) }
                AddChoice(IconGallery, tr(R.string.add_gallery)) { onSource(ScanSource.Gallery) }
                AddChoice(IconOldBook, tr(R.string.add_document)) { onSource(ScanSource.Pdf) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
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

