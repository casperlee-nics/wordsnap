package com.wordsnap.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// 拍照找到的東西：words 是不重複的單字，tokens 是照原本順序排的所有字（用來找片語）
class ScanResult(val words: List<String>, val tokens: List<String>)

// 用 ML Kit 從照片裡找出英文單字
object TextScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val wordPattern = Regex("[A-Za-z]+(?:['-][A-Za-z]+)*")

    fun scan(
        context: Context,
        uri: Uri,
        onResult: (ScanResult) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            onError(e)
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { result -> onResult(ScanResult(extractWords(result.text), tokens(result.text))) }
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

    // 音標：放在 /.../ 或 [...] 裡、中間沒有空白的一小段，例如 /ˈæp.əl/、[ˋæpḷ]
    private val phoneticPattern = Regex("[/\\[][^/\\[\\]\\s]{1,30}[/\\]]")

    // 音標才會用到的符號（æ、ð、θ、ʃ、ə、重音 ˈ、長音 ː 等）
    private fun isPhoneticSymbol(c: Char): Boolean =
        c == 'æ' || c == 'ð' || c == 'θ' || c in '\u0250'..'\u02FF' || c in '\u0300'..'\u036F'

    // 字典裡標詞性的縮寫（n. 和 vt. 這種本來就會被略過）
    private val partOfSpeech = setOf("adj", "adv", "prep", "conj", "pron", "interj", "aux")

    // 沒有母音的不是單字（拆散的音標常變成 pl、str 這種）
    private fun hasVowel(word: String): Boolean = word.any { it in "aeiouy" }

    // 照原本順序排的所有英文字（轉小寫），音標會略過
    fun tokens(text: String): List<String> =
        text.replace(phoneticPattern, " ")
            .split(Regex("\\s+"))
            // 一整段裡有音標符號就整段跳過，不拆出零碎的字母
            .filterNot { chunk -> chunk.any(::isPhoneticSymbol) }
            .flatMap { chunk -> wordPattern.findAll(chunk).map { it.value.lowercase() } }

    // 只留下兩個字母以上的英文字，去掉重複；一看就不是單字的字母組合會略過
    fun extractWords(text: String): List<String> =
        tokens(text)
            .filter { it.length >= 2 && hasVowel(it) && it !in partOfSpeech }
            .distinct()
            .toList()
}
