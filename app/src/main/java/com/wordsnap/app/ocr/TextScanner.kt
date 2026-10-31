package com.wordsnap.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// 拍照找到的東西：words 是不重複的單字，tokens 是照原本順序排的所有字（用來找片語，空字串代表中間斷開）
// headPhrases 是單字書裡「音標前面那幾個字」組成的片語，例如 TV stand [ˋtiˋvi] [stænd]
class ScanResult(val words: List<String>, val tokens: List<String>, val headPhrases: List<String> = emptyList())

// 用 ML Kit 從照片裡找出英文單字
object TextScanner {
    // 用中文模型：中英文都認得，中文字不會被硬猜成 kkii、ah 這種英文字母
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
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
        // 照片轉過方向時，寬高要對調才是辨識結果用的座標
        val turned = image.rotationDegrees == 90 || image.rotationDegrees == 270
        val width = if (turned) image.height else image.width
        val height = if (turned) image.width else image.height
        recognizer.process(image)
            .addOnSuccessListener { result -> onResult(analyze(listOf(linesOf(result, width, height)))) }
            .addOnFailureListener { e -> onError(e) }
    }

    // 讀取 PDF（古書）：一頁一頁畫成圖片再辨識文字，最多讀前 MAX_PDF_PAGES 頁
    suspend fun scanPdf(context: Context, uri: Uri, onPage: (Int, Int) -> Unit = { _, _ -> }): ScanResult {
        val pagesText = mutableListOf<List<List<Chunk>>>()
        withContext(Dispatchers.IO) {
            val file = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException(tr(R.string.file_open_failed))
            file.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val pages = minOf(renderer.pageCount, MAX_PDF_PAGES)
                    for (i in 0 until pages) {
                        withContext(Dispatchers.Main) { onPage(i + 1, pages) }
                        val bitmap = renderer.openPage(i).use { page ->
                            // 寬度放大到約 1600 像素，小字也認得出來
                            val scale = PDF_RENDER_WIDTH.toFloat() / page.width
                            val bitmap = Bitmap.createBitmap(
                                PDF_RENDER_WIDTH, (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888,
                            )
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                        val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                        bitmap.recycle()
                        // PDF 的頁面是完整的，不會有被切掉的字
                        pagesText += linesOf(result, 0, 0)
                    }
                }
            }
        }
        return analyze(pagesText)
    }

    // 照片裡的一小段文字（以空白分開）；cut 表示碰到照片邊緣，可能只拍到一半
    class Chunk(val text: String, val cut: Boolean = false)

    // 把辨識結果整理成一行一行；每一塊文字（段落）中間插一個空行，片語不會跨過去
    // width、height 是 0 表示不檢查邊緣
    private fun linesOf(result: Text, width: Int, height: Int): List<List<Chunk>> {
        val marginX = width * EDGE_MARGIN
        val marginY = height * EDGE_MARGIN
        val lines = mutableListOf<List<Chunk>>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                lines += line.elements.map { element ->
                    val box = element.boundingBox
                    val cut = width > 0 && height > 0 && box != null && (
                        box.left <= marginX || box.right >= width - marginX ||
                            box.top <= marginY || box.bottom >= height - marginY
                        )
                    Chunk(element.text, cut)
                }
            }
            lines += emptyList<Chunk>()
        }
        return lines
    }

    // 碰到照片邊緣 1.5% 以內的字，當作被切掉了
    private const val EDGE_MARGIN = 0.015f
    private const val MAX_PDF_PAGES = 20
    private const val PDF_RENDER_WIDTH = 1600

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

    // 音標才會用到的符號（æ、ð、θ、ŋ、ʃ、ə、重音 ˈ ˋ、長音 ː 等）
    private fun isPhoneticSymbol(c: Char): Boolean =
        c == 'æ' || c == 'ð' || c == 'θ' || c == 'ŋ' || c in '\u0250'..'\u02FF' || c in '\u0300'..'\u036F'

    // 重音記號：音標的開頭常常是這些，辨識時也常變成 ' 或 `
    private const val STRESS_MARKS = "`'ˋˊ´‘’ˈ"

    // 音標的開頭：[ 或 /，或是被認成 l、I、| 的左括號後面接重音記號（例如 [ˋsilɪŋ] 被認成 l'silin），
    // 或是括號不見了、直接以重音記號開頭（引號 ' ‘ 在一般文章很常見，不算）
    private val phoneticStart = Regex("^(?:[\\[［]|/(?=\\S)|[lI|1!(\\[{][" + STRESS_MARKS + "]|[`ˋˊˈ][A-Za-z])")

    // I'm、I'll 這種縮寫不是音標
    private val contraction = Regex("^[Ii]['’](?:m|ll|ve|d|s)\\b")

    private fun opensPhonetic(chunk: String): Boolean =
        (phoneticStart.containsMatchIn(chunk) && !contraction.containsMatchIn(chunk)) || chunk.any(::isPhoneticSymbol)

    private fun closesPhonetic(chunk: String): Boolean =
        chunk.contains(']') || chunk.contains('］') || (chunk.length > 1 && chunk.endsWith('/'))

    // 字典裡標詞性的縮寫（n. 和 vt. 這種本來就會被略過）
    private val partOfSpeech = setOf("adj", "adv", "prep", "conj", "pron", "interj", "aux")

    // 沒有母音的不是單字（拆散的音標常變成 pl、str 這種）
    private fun hasVowel(word: String): Boolean = word.any { it in "aeiouy" }

    private fun isWord(token: String): Boolean =
        token.length >= 2 && hasVowel(token) && token !in partOfSpeech

    // 整理一行一行的文字：
    // 1. [ ] 或 / / 裡的音標整段略過，括號被認錯也盡量認出來
    // 2. 碰到照片邊緣的字略過
    // 3. 一行開頭的 2~4 個字後面緊接著音標，就是單字書裡的片語（TV stand、floor lamp）
    // 一行整理完的結果，第二輪再看前後行決定片語
    private class LineInfo(
        val head: List<String>,
        val words: List<String>,
        // 開頭的字後面緊接著音標
        val headBeforePhonetic: Boolean,
        // 這一行只有開頭的字（音標換到下一行，例如 chicken nugget(s) 下一行才是 [ˋtʃɪkən]）
        val headOnly: Boolean,
        // 這一行一開始就是音標
        val startsWithPhonetic: Boolean,
    )

    // 複數寫法 (s)、(es)：nugget(s) → nugget
    private val pluralMark = Regex("\\((?:e?s)\\)?")

    fun analyze(pages: List<List<List<Chunk>>>): ScanResult {
        val tokens = mutableListOf<String>()
        val words = LinkedHashSet<String>()
        val headPhrases = LinkedHashSet<String>()
        for (lines in pages) {
            // null 是段落之間的空行
            val infos = lines.map { line -> if (line.isEmpty()) { tokens += ""; null } else readLine(line, tokens) }
            for ((i, info) in infos.withIndex()) {
                if (info == null) continue
                val phrase = info.head.size in 2..4 && (
                    info.headBeforePhonetic ||
                        (info.headOnly && infos.drop(i + 1).firstOrNull { it != null }?.startsWithPhonetic == true)
                    )
                if (phrase) {
                    // 全大寫的縮寫（TV）保留大寫，其他轉小寫
                    headPhrases += info.head.joinToString(" ") { if (it.length >= 2 && it.all(Char::isUpperCase)) it else it.lowercase() }
                    // 片語裡的字不另外列出來（同一行其他地方出現的照樣列）
                    val inHead = info.head.map { it.lowercase() }.toMutableList()
                    for (w in info.words) if (!inHead.remove(w) && isWord(w)) words += w
                } else {
                    for (w in info.words) if (isWord(w)) words += w
                }
            }
        }
        return ScanResult(words.toList(), tokens, headPhrases.toList())
    }

    private fun readLine(line: List<Chunk>, tokens: MutableList<String>): LineInfo {
        var inPhonetic = false
        // 這一行開頭的英文字（還沒遇到音標之前）
        val head = mutableListOf<String>()
        var headOpen = true
        var headBeforePhonetic = false
        var startsWithPhonetic = false
        var sawPhonetic = false
        val lineWords = mutableListOf<String>()
        for ((index, chunk) in line.withIndex()) {
            val raw = chunk.text
            if (!inPhonetic && opensPhonetic(raw)) {
                if (headOpen && head.isNotEmpty()) headBeforePhonetic = true
                if (head.isEmpty() && lineWords.isEmpty() && !sawPhonetic) startsWithPhonetic = true
                inPhonetic = true
                sawPhonetic = true
            }
            if (inPhonetic || closesPhonetic(raw)) {
                if (closesPhonetic(raw)) inPhonetic = false
                headOpen = false
                tokens += ""
                continue
            }
            // 一行最前面的編號（1、❶，有時被認成 O 這種單一字母）略過
            if (index == 0 && !chunk.cut && raw.length == 1) continue
            val text = raw.replace(pluralMark, "")
            // 單獨的 (s) 直接略過，不算斷開
            if (text.isBlank() && !chunk.cut) continue
            val found = wordPattern.findAll(text).map { it.value }.toList()
            if (chunk.cut || found.isEmpty()) {
                // 一行最前面的編號（10、⑩）不算斷開
                if (!(index == 0 && !chunk.cut)) headOpen = false
                tokens += ""
                continue
            }
            if (headOpen) {
                if (found.size == 1 && text.trimEnd(',', '.', ';', ':').length == found[0].length) head += found[0]
                else headOpen = false
            }
            for (w in found) {
                tokens += w.lowercase()
                lineWords += w.lowercase()
            }
        }
        tokens += ""
        val headOnly = headOpen && !sawPhonetic && head.isNotEmpty() && lineWords.size == head.size
        return LineInfo(head, lineWords, headBeforePhonetic, headOnly, startsWithPhonetic)
    }

    // 只出現在片語裡的字（doggie bag 裡的 doggie、bag），不用再分開列出來
    fun wordsOnlyInPhrases(tokens: List<String>, phrases: List<String>): Set<String> {
        val covered = BooleanArray(tokens.size)
        for (phrase in phrases) {
            val parts = phrase.lowercase().split(' ')
            if (parts.size < 2) continue
            for (i in 0..tokens.size - parts.size) {
                if ((parts.indices).all { tokens[i + it] == parts[it] }) for (k in parts.indices) covered[i + k] = true
            }
        }
        val outside = tokens.filterIndexed { i, _ -> !covered[i] }.toSet()
        return tokens.filterIndexed { i, _ -> covered[i] }.filterTo(HashSet()) { it !in outside }
    }

    // 純文字（TXT、CSV、Excel）：每一行用空白分開再整理
    fun analyzeText(text: String): ScanResult =
        analyze(listOf(text.lines().map { line -> line.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { Chunk(it) } }))
}
