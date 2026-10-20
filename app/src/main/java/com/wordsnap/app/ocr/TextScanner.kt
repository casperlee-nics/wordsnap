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

    // 太基本的字，拍照時自動略過
    private val commonWords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "so", "as", "at", "by", "for", "from", "in", "into",
        "of", "on", "to", "up", "with", "out", "off", "over", "about", "than", "then", "too", "very",
        "is", "am", "are", "was", "were", "be", "been", "being", "do", "does", "did", "done",
        "have", "has", "had", "will", "would", "can", "could", "shall", "should", "may", "might", "must",
        "i", "me", "my", "mine", "you", "your", "yours", "he", "him", "his", "she", "her", "hers",
        "it", "its", "we", "us", "our", "ours", "they", "them", "their", "theirs",
        "this", "that", "these", "those", "there", "here", "what", "which", "who", "whom", "whose",
        "when", "where", "why", "how", "all", "any", "some", "no", "not", "yes", "each", "every",
        "more", "most", "much", "many", "one", "two", "three", "also", "just", "only", "now",
        "go", "goes", "went", "get", "got", "make", "made", "say", "said", "see", "saw", "come", "came",
        "like", "know", "think", "want", "use", "good", "new", "big", "small", "day", "time",
        "don't", "it's", "i'm", "can't", "isn't", "didn't", "doesn't", "won't",
    )

    fun isCommon(word: String): Boolean = word in commonWords

    // 只留下兩個字母以上的英文字，轉小寫並去掉重複
    fun extractWords(text: String): List<String> =
        wordPattern.findAll(text)
            .map { it.value.lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .toList()
}
