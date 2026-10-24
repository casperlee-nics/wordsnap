package com.wordsnap.app.translate

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wordsnap.app.i18n.AppLanguage
import com.wordsnap.app.i18n.appLanguage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 離線英漢字典：ECDICT（MIT 授權）挑出最常用的 5 萬個字，加上約 2 萬個常見組合字（floor lamp、alley cat），事先用 OpenCC 轉成台灣繁體，放在 assets/ecdict.db
// 遊戲語言是日文時，解釋改從字典包（DictionaryPacks）查；變化形、片語還是用 ecdict.db
// 查不到的字再交給 WordTranslator（ML Kit）翻譯
object WordDictionary {
    // 換新版字典檔時把版本加一，App 會重新複製
    private const val VERSION = 3
    private const val ASSET = "ecdict.db"

    private val lock = Mutex()
    private var db: SQLiteDatabase? = null
    private var packDb: SQLiteDatabase? = null
    private var packLanguage: AppLanguage? = null

    // 日文的字典包（還沒安裝就是 null）
    private suspend fun openPack(context: Context, language: AppLanguage): SQLiteDatabase? = lock.withLock {
        if (packLanguage == language) packDb?.let { return@withLock it }
        val file = DictionaryPacks.file(context, language)
        if (!file.exists()) return@withLock null
        withContext(Dispatchers.IO) {
            packDb?.close()
            runCatching { SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY) }.getOrNull()
        }.also {
            packDb = it
            packLanguage = language
        }
    }

    // 第一次使用時把字典從 assets 複製到手機裡再打開（只讀）
    private suspend fun open(context: Context): SQLiteDatabase? = lock.withLock {
        db?.let { return@withLock it }
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.noBackupFilesDir, "ecdict_v$VERSION.db")
                // 舊版的字典檔刪掉，省空間
                for (old in 1 until VERSION) File(context.noBackupFilesDir, "ecdict_v$old.db").delete()
                if (!file.exists()) {
                    val tmp = File(file.path + ".tmp")
                    context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    tmp.renameTo(file)
                }
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrNull()
        }.also { db = it }
    }

    // 查一個字；過去式、複數這類變化形會找回原形，例如 went → (go) 去、走
    // 字典裡的解釋很長，只留最前面 2 個意思，並拿掉 n. v. 這類詞性
    suspend fun lookup(context: Context, word: String): String? {
        val database = open(context) ?: return null
        val language = appLanguage(context)
        // 中文查 ecdict.db；日文查字典包，字典包還沒裝好就交給 ML Kit 翻譯
        val meanings = if (DictionaryPacks.needsPack(language)) openPack(context, language) ?: return null else database
        val key = word.trim().lowercase().replace(Regex("\\s+"), " ")
        if (key.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            meaning(meanings, key)?.let(::simplify) ?: lemma(database, key)?.let { base ->
                meaning(meanings, base)?.let { "($base) ${simplify(it)}" }
            }
        }
    }

    // 從照片裡依順序排的字找出片語（2 到 4 個字，例如 give up、look forward to、floor lamp）
    // 變化形會換回原形：gave up → give up；空字串是斷開的地方（換段落、音標），片語不會跨過去
    suspend fun findPhrases(context: Context, tokens: List<String>): List<String> {
        val database = open(context) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val found = LinkedHashSet<String>()
            for (i in tokens.indices) {
                for (n in 4 downTo 2) {
                    if (i + n > tokens.size) continue
                    val part = tokens.subList(i, i + n)
                    if (part.any { it.isEmpty() }) continue
                    val candidate = part.joinToString(" ")
                    val hit = database.rawQuery("SELECT 1 FROM phrases WHERE phrase = ?", arrayOf(candidate)).use { it.moveToFirst() }
                    if (hit) {
                        found += lemma(database, candidate) ?: candidate
                        break
                    }
                }
            }
            found.toList()
        }
    }

    // 字典裡查不到的字（連變化形也找不到），通常是辨識錯的字；字典打不開時當作全部都查得到
    suspend fun unknownWords(context: Context, words: List<String>): Set<String> {
        val database = open(context) ?: return emptySet()
        return withContext(Dispatchers.IO) {
            words.filterTo(LinkedHashSet()) { word ->
                val key = word.lowercase()
                meaning(database, key) == null && lemma(database, key) == null
            }
        }
    }

    // 字典格式的解釋（例如「vi. 去、走、達到\nvt. 對付」）精簡成「去、走」；原形的括號會保留
    fun simplify(raw: String): String {
        val base = BASE.find(raw)
        val body = if (base != null) raw.substring(base.value.length) else raw
        val senses = body.lines()
            .flatMap { line -> line.replace(POS, "").split('、', '；', ';', '，', ',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_SENSES)
        val text = senses.joinToString("、")
        return if (base != null) "${base.value}$text" else text
    }

    // 看起來是舊版字典格式（開頭有 n. vi. adj. 這類詞性）的解釋，才需要精簡
    fun looksLikeDictionary(meaning: String): Boolean =
        DICT_FORMAT.containsMatchIn(meaning)

    private const val MAX_SENSES = 2
    private val BASE = Regex("^\\([a-zA-Z' -]+\\) ")
    private val POS = Regex("^\\s*([a-z]{1,5}\\.\\s*)+")
    private val DICT_FORMAT = Regex("^(\\([a-zA-Z' -]+\\) )?[a-z]{1,5}\\. ")

    private fun meaning(db: SQLiteDatabase, word: String): String? =
        db.rawQuery("SELECT meaning FROM dict WHERE word = ?", arrayOf(word)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun lemma(db: SQLiteDatabase, form: String): String? =
        db.rawQuery("SELECT lemma FROM forms WHERE form = ?", arrayOf(form)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
