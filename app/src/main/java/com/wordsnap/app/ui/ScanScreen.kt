package com.wordsnap.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wordsnap.app.R
import com.wordsnap.app.data.Book
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ocr.DocumentMimeTypes
import com.wordsnap.app.ocr.DocumentText
import com.wordsnap.app.ocr.ScanResult
import com.wordsnap.app.ocr.TextScanner
import com.wordsnap.app.translate.WordDictionary
import java.io.File
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.launch

// 加單字的三種來源
enum class ScanSource { Camera, Gallery, Pdf }

// 拍照、選圖或讀古書（PDF、Excel、TXT）→ 辨識單字 → 點選要加入的字
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanScreen(
    // 已經收錄的字 → 在哪幾本單字簿裡
    existing: Map<String, Set<Long?>>,
    books: List<Book>,
    defaultBookId: Long?,
    onCreateBook: (String, (Long) -> Unit) -> Unit,
    onAdd: (Collection<String>, Long?) -> Unit,
    onBack: () -> Unit,
    // 從「加單字」選了哪一種：相機直接打開相機、相簿打開相簿、古書打開「我的檔案」選 PDF、Excel 或 TXT
    source: ScanSource = ScanSource.Camera,
) {
    val context = LocalContext.current
    // 捕捉怪物專屬的音樂（打開相機或選檔案時 App 到背景，音樂會自動暫停）
    LoopingMusic(R.raw.hunt_bgm, volume = 0.45f)
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<String>?>(null) }
    // 字典查不到的字（多半是辨識錯的），收起來放在最後，要的話再點開
    var unclear by remember { mutableStateOf<List<String>>(emptyList()) }
    var showUnclear by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    // 相機開著時 App 可能被系統收掉，照片位置要存起來，回來才找得到照片
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showAimHint by remember { mutableStateOf(false) }
    // 要存進哪一本單字簿，預設是單字本畫面目前選的那本
    var targetBookId by remember { mutableStateOf(defaultBookId ?: books.firstOrNull()?.id) }

    // 找到的片語排最前面，接著是單字
    suspend fun show(result: ScanResult) {
        val phrases = (result.headPhrases + WordDictionary.findPhrases(context, result.tokens))
            .distinctBy { it.lowercase() }
        val words = result.words.filter { it !in phrases }
        val unknown = WordDictionary.unknownWords(context, words)
        found = phrases + words.filter { it !in unknown }
        unclear = words.filter { it in unknown }
        showUnclear = false
        scanning = false
        progress = null
    }

    fun startPdf(uri: Uri) {
        scanning = true
        error = null
        selected.clear()
        scope.launch {
            try {
                show(DocumentText.scan(context, uri) { page, pages -> progress = tr(R.string.scan_reading_page, page, pages) })
            } catch (e: Exception) {
                error = tr(R.string.scan_read_failed, e.message.orEmpty())
                scanning = false
                progress = null
            }
        }
    }

    fun startScan(uri: Uri) {
        scanning = true
        error = null
        selected.clear()
        TextScanner.scan(
            context, uri,
            onResult = { result -> scope.launch { show(result) } },
            onError = { e ->
                error = tr(R.string.scan_ocr_failed, e.message.orEmpty())
                scanning = false
            },
        )
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) startScan(uri) else if (found == null) onBack()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startScan(uri) else if (found == null) onBack()
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) startPdf(uri) else if (found == null) onBack()
    }
    fun openGallery() = pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun openPdf() = pickPdf.launch(DocumentMimeTypes)
    // 一進來就直接打開選的那一種，轉動螢幕不會重複打開
    var sourceOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!sourceOpened) {
            sourceOpened = true
            when (source) {
                ScanSource.Camera -> {
                    val uri = newPhotoUri(context)
                    photoUri = uri
                    takePicture.launch(uri)
                }
                ScanSource.Gallery -> openGallery()
                ScanSource.Pdf -> openPdf()
            }
        }
    }

    Scaffold(
        topBar = {
            PixelHeader(tr(R.string.scan_title), onBack, theme = HeaderTheme.HuntNet)
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 找完之後可以用同一種方式再找一次（取消就回到加單字選單）
            if (found != null && !scanning) {
                RetroButton(
                    when (source) {
                        ScanSource.Camera -> tr(R.string.scan_again_camera)
                        ScanSource.Gallery -> tr(R.string.scan_again_gallery)
                        ScanSource.Pdf -> tr(R.string.scan_again_document)
                    },
                    onClick = {
                        when (source) {
                            ScanSource.Camera -> showAimHint = true
                            ScanSource.Gallery -> openGallery()
                            ScanSource.Pdf -> openPdf()
                        }
                    },
                )
            }

            when {
                scanning -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    progress?.let { Text(it, Modifier.padding(top = 8.dp)) }
                }
                error != null -> Text(error!!)
                found == null -> Text(tr(R.string.scan_intro), style = MaterialTheme.typography.bodyLarge.readable())
                found!!.none { ' ' in it || !TextScanner.isCommon(it) } && unclear.isEmpty() ->
                    Text(tr(R.string.scan_none), style = MaterialTheme.typography.bodyLarge.readable())
                else -> {
                    // 濾掉 the、and 這類常見字；已經收錄的字照樣列出但不能選
                    val all = found!!
                    // 片語由常見字組成，不能被常見字過濾掉
                    val words = all.filter { ' ' in it || !TextScanner.isCommon(it) }
                    val skipped = all.size - words.size
                    // 要存的那本已經有的字不能選；別本有的字可以選，但標出來提醒
                    // existing 用小寫查（片語可能有大寫，例如 TV stand）
                    fun owned(word: String) = existing[word.lowercase()]?.contains(targetBookId) == true
                    fun inOtherBook(word: String) = !owned(word) && existing[word.lowercase()]?.isNotEmpty() == true
                    fun label(word: String) = when {
                        owned(word) -> tr(R.string.scan_owned, word)
                        inOtherBook(word) -> tr(R.string.scan_in_other_book, word)
                        else -> word
                    }
                    @Composable
                    fun chip(word: String) = FilterChip(
                        selected = word in selected && !owned(word),
                        onClick = { if (word in selected) selected.remove(word) else selected.add(word) },
                        label = { Text(label(word), style = MaterialTheme.typography.labelLarge.readable()) },
                        enabled = !owned(word),
                    )
                    val selectable = words.filter { !owned(it) }
                    val allSelected = selectable.isNotEmpty() && selectable.all { it in selected }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr(R.string.scan_pick), Modifier.weight(1f))
                        RetroButton(
                            tr(if (allSelected) R.string.scan_select_none else R.string.scan_select_all),
                            onClick = {
                                selected.clear()
                                if (!allSelected) selected.addAll(selectable)
                            },
                            enabled = selectable.isNotEmpty(),
                            textStyle = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (skipped > 0) {
                        Text(tr(R.string.scan_skipped, skipped), style = MaterialTheme.typography.bodySmall.readable())
                    }
                    if ((words + unclear).any { inOtherBook(it) }) {
                        Text(tr(R.string.scan_other_book_hint), style = MaterialTheme.typography.bodySmall.readable(), color = MaterialTheme.colorScheme.error)
                    }
                    // 選字的外框，上緣站著幾隻像素怪物
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        FlowRow(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 18.dp)
                                .woodFrame(MaterialTheme.colorScheme.surface)
                                .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            words.forEach { chip(it) }
                            // 字典查不到的字收在最後，點一下才展開
                            if (unclear.isNotEmpty()) {
                                TextButton(onClick = { showUnclear = !showUnclear }, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        if (showUnclear) tr(R.string.scan_unclear_hide) else tr(R.string.scan_unclear_show, unclear.size),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (showUnclear) {
                                    unclear.forEach { chip(it) }
                                }
                            }
                        }
                        LurkingMonsters(Modifier.fillMaxWidth().padding(horizontal = 12.dp))
                    }
                    BookPicker(
                        books = books,
                        selectedId = targetBookId,
                        onSelect = { targetBookId = it },
                        onCreate = { name -> onCreateBook(name) { id -> targetBookId = id } },
                        label = tr(R.string.scan_save_to),
                        allowAll = false,
                        // 和單字簿右上角的冒險日誌按鈕同一種樣子
                        onTopBar = true,
                    )
                    RetroButton(
                        tr(R.string.scan_add, selected.count { !owned(it) }),
                        onClick = { onAdd(selected.filter { !owned(it) }, targetBookId) },
                        enabled = selected.any { !owned(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 按下拍照先提示，按「準備好了」才打開相機
    if (showAimHint) {
        PixelAlertDialog(
            onDismissRequest = { showAimHint = false },
            title = { Text(tr(R.string.scan_aim_title)) },
            text = { Text(tr(R.string.scan_aim)) },
            confirmButton = {
                Button(onClick = {
                    showAimHint = false
                    val uri = newPhotoUri(context)
                    photoUri = uri
                    takePicture.launch(uri)
                }) { Text(tr(R.string.scan_ready)) }
            },
            dismissButton = {
                TextButton(onClick = { showAimHint = false }) { Text(tr(R.string.cancel)) }
            },
        )
    }
}

private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(dir, "scan.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// 選字框上緣的 10 隻小怪物：每次隨機挑，各自一格一格跳、左右張望
@Composable
private fun LurkingMonsters(modifier: Modifier = Modifier) {
    val cast = remember { NormalMonsters.shuffled().take(10).map { it.art } }
    val time by rememberInfiniteTransition(label = "lurk").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "time",
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        cast.forEachIndexed { i, art ->
            Box(Modifier.size(28.dp), contentAlignment = Alignment.BottomCenter) {
                PixelArt(
                    art,
                    ScenePalette,
                    Modifier.fillMaxWidth().graphicsLayer {
                        // 每隻錯開，不會一起動
                        val p = (time + i * 0.137f) % 1f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        when (i % 3) {
                            // 一格一格往上跳
                            0 -> translationY = -floor(sin(p * PI.toFloat()) * 2.99f) * 2.dp.toPx()
                            // 左看看右看看
                            1 -> scaleX = if (p < 0.5f) 1f else -1f
                            // 壓扁再彈起來
                            else -> {
                                val sq = if (p < 0.3f) 0.85f else 1f
                                scaleY = sq
                                scaleX = 2f - sq
                            }
                        }
                    },
                )
            }
        }
    }
}
