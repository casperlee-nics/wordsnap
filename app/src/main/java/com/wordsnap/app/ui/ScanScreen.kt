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
import com.wordsnap.app.data.Book
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
    existing: Set<String>,
    books: List<Book>,
    defaultBookId: Long?,
    onCreateBook: (String, (Long) -> Unit) -> Unit,
    onAdd: (Collection<String>, Long?) -> Unit,
    onBack: () -> Unit,
    // 從「加單字」選了哪一種：相機直接打開相機、相簿打開相簿、古書打開「我的檔案」選 PDF、Excel 或 TXT
    source: ScanSource = ScanSource.Camera,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<String>?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showAimHint by remember { mutableStateOf(false) }
    // 要存進哪一本單字簿，預設是單字本畫面目前選的那本
    var targetBookId by remember { mutableStateOf(defaultBookId ?: books.firstOrNull()?.id) }

    // 找到的片語排最前面，接著是單字
    suspend fun show(result: ScanResult) {
        val phrases = WordDictionary.findPhrases(context, result.tokens)
        found = phrases + result.words.filter { it !in phrases }
        scanning = false
        progress = null
    }

    fun startPdf(uri: Uri) {
        scanning = true
        error = null
        selected.clear()
        scope.launch {
            try {
                show(DocumentText.scan(context, uri) { page, pages -> progress = "正在翻閱古書：第 $page / $pages 頁" })
            } catch (e: Exception) {
                error = "讀取失敗：${e.message}"
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
                error = "辨識失敗：${e.message}"
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
            PixelHeader("捕捉怪物", onBack, theme = HeaderTheme.HuntNet)
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
                        ScanSource.Camera -> "再拍一張"
                        ScanSource.Gallery -> "換一張圖"
                        ScanSource.Pdf -> "換一本古書"
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
                found == null -> Text("拍一張課本或文章的照片，或打開一本古書（PDF、Excel、TXT），我會幫你找出隱藏的單字怪物。")
                found!!.none { ' ' in it || !TextScanner.isCommon(it) } -> Text("沒有找到英文單字，換一張清楚一點的照片試試。")
                else -> {
                    // 濾掉 the、and 這類常見字；已經收錄的字照樣列出但不能選
                    val all = found!!
                    // 片語由常見字組成，不能被常見字過濾掉
                    val words = all.filter { ' ' in it || !TextScanner.isCommon(it) }
                    val skipped = all.size - words.size
                    val selectable = words.filter { it !in existing }
                    val allSelected = selectable.isNotEmpty() && selected.size == selectable.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("要新增的怪物", Modifier.weight(1f))
                        RetroButton(
                            if (allSelected) "全不選" else "全選",
                            onClick = {
                                selected.clear()
                                if (!allSelected) selected.addAll(selectable)
                            },
                            enabled = selectable.isNotEmpty(),
                            textStyle = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (skipped > 0) {
                        Text("已略過 $skipped 個常見字（如 the、and、is）", style = MaterialTheme.typography.bodySmall)
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
                            words.forEach { word ->
                                val owned = word in existing
                                FilterChip(
                                    selected = word in selected,
                                    onClick = { if (word in selected) selected.remove(word) else selected.add(word) },
                                    label = { Text(if (owned) "$word（已有）" else word) },
                                    enabled = !owned,
                                )
                            }
                        }
                        LurkingMonsters(Modifier.fillMaxWidth().padding(horizontal = 12.dp))
                    }
                    BookPicker(
                        books = books,
                        selectedId = targetBookId,
                        onSelect = { targetBookId = it },
                        onCreate = { name -> onCreateBook(name) { id -> targetBookId = id } },
                        label = "存到：",
                        allowAll = false,
                        // 和單字簿右上角的冒險日誌按鈕同一種樣子
                        onTopBar = true,
                    )
                    RetroButton(
                        "加入單字本（${selected.size}）",
                        onClick = { onAdd(selected.toList(), targetBookId) },
                        enabled = selected.isNotEmpty(),
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
            title = { Text("怪物出現了！") },
            text = { Text("請將鏡頭對準怪物") },
            confirmButton = {
                Button(onClick = {
                    showAimHint = false
                    val uri = newPhotoUri(context)
                    photoUri = uri
                    takePicture.launch(uri)
                }) { Text("準備好了") }
            },
            dismissButton = {
                TextButton(onClick = { showAimHint = false }) { Text("取消") }
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
