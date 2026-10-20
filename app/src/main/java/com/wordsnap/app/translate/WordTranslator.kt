package com.wordsnap.app.translate

import android.icu.text.Transliterator
import android.os.Build
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

// 翻譯檔的狀態，給畫面顯示提示用
enum class ModelStatus { Unknown, Downloading, Ready, Failed }

// 英文翻成繁體中文：ML Kit 只會翻成簡體，再用系統內建的轉換器轉成繁體
object WordTranslator {
    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
    )

    private val toTraditional: Transliterator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Hans-Hant") }.getOrNull()
        } else {
            null
        }

    private val _status = MutableStateFlow(ModelStatus.Unknown)
    val status: StateFlow<ModelStatus> = _status

    // 失敗（例如第一次使用時沒有網路）回傳 null，下次開 App 會再試
    suspend fun translate(text: String): String? {
        if (_status.value != ModelStatus.Ready) {
            _status.value = ModelStatus.Downloading
            try {
                // 已經下載過的話會馬上完成
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                _status.value = ModelStatus.Ready
            } catch (e: Exception) {
                _status.value = ModelStatus.Failed
                return null
            }
        }
        return try {
            val simplified = translator.translate(text).await()
            toTraditional?.transliterate(simplified) ?: simplified
        } catch (e: Exception) {
            null
        }
    }
}
