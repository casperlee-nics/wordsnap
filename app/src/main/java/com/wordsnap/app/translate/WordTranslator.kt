package com.wordsnap.app.translate

import android.content.Context
import android.icu.text.Transliterator
import android.os.Build
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wordsnap.app.i18n.AppLanguage
import com.wordsnap.app.i18n.appLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

// 翻譯檔的狀態，給畫面顯示提示用
enum class ModelStatus { Unknown, Downloading, Ready, Failed }

// 字典查不到的字，用 ML Kit 把英文翻成遊戲選的語言（中文、日文）
// ML Kit 中文只會翻成簡體，再用系統內建的轉換器轉成繁體
object WordTranslator {
    private var translator: Translator? = null
    private var translatorLanguage: AppLanguage? = null

    private val toTraditional: Transliterator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Hans-Hant") }.getOrNull()
        } else {
            null
        }

    private val _status = MutableStateFlow(ModelStatus.Unknown)
    val status: StateFlow<ModelStatus> = _status

    // 換了語言就換一個翻譯器（翻譯檔要重新下載）
    private fun translatorFor(language: AppLanguage): Translator {
        translator?.let { if (translatorLanguage == language) return it }
        translator?.close()
        _status.value = ModelStatus.Unknown
        return Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(language.translateTarget)
                .build()
        ).also {
            translator = it
            translatorLanguage = language
        }
    }

    // 失敗（例如第一次使用時沒有網路）回傳 null，下次開 App 會再試
    suspend fun translate(context: Context, text: String): String? {
        val language = appLanguage(context)
        val client = translatorFor(language)
        if (_status.value != ModelStatus.Ready) {
            _status.value = ModelStatus.Downloading
            try {
                // 已經下載過的話會馬上完成
                client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                _status.value = ModelStatus.Ready
            } catch (e: Exception) {
                _status.value = ModelStatus.Failed
                return null
            }
        }
        return try {
            val translated = client.translate(text).await()
            if (language == AppLanguage.Chinese) toTraditional?.transliterate(translated) ?: translated else translated
        } catch (e: Exception) {
            null
        }
    }
}
