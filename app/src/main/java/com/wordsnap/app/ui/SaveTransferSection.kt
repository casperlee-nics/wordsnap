package com.wordsnap.app.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.CloudSave
import com.wordsnap.app.data.SaveTransfer
import com.wordsnap.app.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 遊戲設定裡的「存檔搬家」和「雲端存檔」：換手機時把冒險者和單字帶過去
@Composable
fun SaveTransferSection(activity: Activity?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 正在匯出、匯入或連雲端時，按鈕先停用
    var busy by remember { mutableStateOf(false) }
    // 做完之後跳出來的一句話
    var message by remember { mutableStateOf<String?>(null) }
    // 準備匯入的存檔，等玩家按確定（note 是額外說明，例如雲端存檔的時間）
    var pendingImport by remember { mutableStateOf<Pair<ByteArray, String?>?>(null) }
    // 準備上傳時，先讓玩家看雲端上舊存檔的時間
    var confirmUpload by remember { mutableStateOf<String?>(null) }
    val cloudReady = activity != null && CloudSave.isConfigured(context)

    fun launchTask(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) launchTask {
            message = if (writeExport(context, uri)) tr(R.string.transfer_exported) else tr(R.string.transfer_export_failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) launchTask {
            val bytes = readFile(context, uri)
            if (bytes == null) message = tr(R.string.transfer_not_a_save) else pendingImport = bytes to null
        }
    }

    Text("▶ ${tr(R.string.transfer_title)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RetroButton(
            tr(R.string.transfer_export),
            onClick = { exportLauncher.launch(tr(R.string.transfer_file_name, today())) },
            modifier = Modifier.weight(1f),
            enabled = !busy,
        )
        RetroButton(
            tr(R.string.transfer_import),
            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            modifier = Modifier.weight(1f),
            enabled = !busy,
        )
    }
    Text(tr(R.string.transfer_hint), style = MaterialTheme.typography.bodySmall.readable())

    Text("▶ ${tr(R.string.cloud_title)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RetroButton(
            tr(R.string.cloud_upload),
            onClick = {
                launchTask {
                    val act = activity ?: return@launchTask
                    try {
                        if (!CloudSave.signIn(act)) {
                            message = tr(R.string.cloud_sign_in_failed)
                            return@launchTask
                        }
                        confirmUpload = CloudSave.info(act)?.let { formatTime(it.savedAt) } ?: ""
                    } catch (e: Exception) {
                        message = tr(R.string.cloud_failed)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !busy && cloudReady,
        )
        RetroButton(
            tr(R.string.cloud_download),
            onClick = {
                launchTask {
                    val act = activity ?: return@launchTask
                    try {
                        if (!CloudSave.signIn(act)) {
                            message = tr(R.string.cloud_sign_in_failed)
                            return@launchTask
                        }
                        val info = CloudSave.info(act)
                        val bytes = if (info == null) null else CloudSave.download(act)
                        if (info == null || bytes == null) {
                            message = tr(R.string.cloud_none)
                        } else {
                            pendingImport = bytes to tr(R.string.cloud_last, formatTime(info.savedAt))
                        }
                    } catch (e: Exception) {
                        message = tr(R.string.cloud_failed)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            enabled = !busy && cloudReady,
        )
    }
    Text(
        tr(if (cloudReady) R.string.cloud_hint else R.string.cloud_not_ready),
        style = MaterialTheme.typography.bodySmall.readable(),
    )
    if (busy) Text(tr(R.string.transfer_working), style = MaterialTheme.typography.bodySmall)

    confirmUpload?.let { lastTime ->
        PixelAlertDialog(
            onDismissRequest = { confirmUpload = null },
            title = { Text(tr(R.string.cloud_title)) },
            text = {
                Text(if (lastTime.isEmpty()) tr(R.string.cloud_upload_confirm_new) else tr(R.string.cloud_upload_confirm, lastTime))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUpload = null
                    launchTask {
                        val act = activity ?: return@launchTask
                        message = try {
                            val bytes = SaveTransfer.export(context)
                            if (CloudSave.upload(act, bytes, formatTime(System.currentTimeMillis()))) {
                                tr(R.string.cloud_uploaded)
                            } else {
                                tr(R.string.cloud_too_big)
                            }
                        } catch (e: Exception) {
                            tr(R.string.cloud_failed)
                        }
                    }
                }) { Text(tr(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmUpload = null }) { Text(tr(R.string.cancel)) } },
        )
    }

    pendingImport?.let { (bytes, note) ->
        PixelAlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(tr(R.string.transfer_import)) },
            text = { Text(listOfNotNull(note, tr(R.string.transfer_import_confirm)).joinToString("\n")) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    launchTask {
                        when (SaveTransfer.stageImport(context, bytes)) {
                            SaveTransfer.ImportResult.Ok -> SaveTransfer.restartApp(context)
                            SaveTransfer.ImportResult.TooNew -> message = tr(R.string.transfer_too_new)
                            SaveTransfer.ImportResult.NotASave -> message = tr(R.string.transfer_not_a_save)
                        }
                    }
                }) { Text(tr(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(tr(R.string.cancel)) } },
        )
    }

    message?.let { text ->
        PixelAlertDialog(
            onDismissRequest = { message = null },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(tr(R.string.confirm)) } },
        )
    }
}

private suspend fun writeExport(context: Context, uri: Uri): Boolean = try {
    val bytes = SaveTransfer.export(context)
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } != null
    }
} catch (e: Exception) {
    false
}

private suspend fun readFile(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}

private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
