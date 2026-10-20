package com.wordsnap.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// 用 ML Kit 從照片裡找出英文單字
object TextScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val wordPattern = Regex("[A-Za-z]+(?:['-][A-Za-z]+)*")

    fun scan(
        context: Context,
        uri: Uri,
        onResult: (List<String>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onError(e)
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { result -> onResult(extractWords(result.text)) }
            .addOnFailureListener { e -> onError(e) }
    }

    // 只留下兩個字母以上的英文字，轉小寫並去掉重複
    fun extractWords(text: String): List<String> =
        wordPattern.findAll(text)
            .map { it.value.lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .toList()
}
