package com.wordsnap.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wordsnap.app.data.Book
import com.wordsnap.app.ocr.TextScanner
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelShape
import com.wordsnap.app.ui.theme.pixelTopBarColors
import java.io.File

// 拍照或選圖 → 辨識單字 → 點選要加入的字
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanScreen(
    existing: Set<String>,
    books: List<Book>,
    defaultBookId: Long?,
    onCreateBook: (String, (Long) -> Unit) -> Unit,
    onAdd: (Collection<String>, Long?) -> Unit,
    onBack: () -> Unit,
    // 從「加單字」按相機進來時，直接打開相機
    openCamera: Boolean = false,
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<String>?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showAimHint by remember { mutableStateOf(false) }
    // 要存進哪一本單字簿，預設是單字本畫面目前選的那本
    var targetBookId by remember { mutableStateOf(defaultBookId ?: books.firstOrNull()?.id) }

    fun startScan(uri: Uri) {
        scanning = true
        error = null
        selected.clear()
        TextScanner.scan(
            context, uri,
            onResult = { words ->
                found = words
                scanning = false
            },
            onError = { e ->
                error = "辨識失敗：${e.message}"
                scanning = false
            },
        )
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) startScan(uri)
    }
    var cameraOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (openCamera && !cameraOpened) {
            cameraOpened = true
            val uri = newPhotoUri(context)
            photoUri = uri
            takePicture.launch(uri)
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startScan(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("捕捉怪物") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = pixelTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showAimHint = true }) { Text("拍照") }
                OutlinedButton(onClick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("從相簿選") }
            }

            when {
                scanning -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(error!!)
                found == null -> Text("拍一張課本或文章的照片，我會幫你找出隱藏的單字怪物。")
                found!!.none { !TextScanner.isCommon(it) } -> Text("沒有找到英文單字，換一張清楚一點的照片試試。")
                else -> {
                    // 濾掉 the、and 這類常見字；已經收錄的字照樣列出但不能選
                    val all = found!!
                    val words = all.filterNot(TextScanner::isCommon)
                    val skipped = all.size - words.size
                    val selectable = words.filter { it !in existing }
                    val allSelected = selectable.isNotEmpty() && selected.size == selectable.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("要新增的怪物（找到 ${words.size} 隻）", Modifier.weight(1f))
                        TextButton(onClick = {
                            selected.clear()
                            if (!allSelected) selected.addAll(selectable)
                        }, enabled = selectable.isNotEmpty()) { Text(if (allSelected) "全不選" else "全選") }
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
                                .border(PixelBorder, PixelShape)
                                .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 8.dp)
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
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Monsters.forEach { art ->
                                PixelArt(art, ScenePalette, Modifier.size(30.dp))
                            }
                        }
                    }
                    BookPicker(
                        books = books,
                        selectedId = targetBookId,
                        onSelect = { targetBookId = it },
                        onCreate = { name -> onCreateBook(name) { id -> targetBookId = id } },
                        label = "存到：",
                        allowAll = false,
                    )
                    Button(
                        onClick = { onAdd(selected.toList(), targetBookId) },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("加入單字本（${selected.size}）") }
                }
            }
        }
    }

    // 按下拍照先提示，按「準備好了」才打開相機
    if (showAimHint) {
        AlertDialog(
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
