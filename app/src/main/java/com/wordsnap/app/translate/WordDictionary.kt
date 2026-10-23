package com.wordsnap.app.translate

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// 離線英漢字典：ECDICT（MIT 授權）挑出最常用的 5 萬個字，事先用 OpenCC 轉成台灣繁體，放在 assets/ecdict.db
// 查不到的字再交給 WordTranslator（ML Kit）翻譯
object WordDictionary {
    // 換新版字典檔時把版本加一，App 會重新複製
    private const val VERSION = 2
    private const val ASSET = "ecdict.db"

    private val lock = Mutex()
    private var db: SQLiteDatabase? = null

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
    // 字典裡的解釋很長，只留最前面 2 個中文意思，並拿掉 n. v. 這類詞性
    suspend fun lookup(context: Context, word: String): String? {
        val database = open(context) ?: return null
        val key = word.trim().lowercase().replace(Regex("\\s+"), " ")
        if (key.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            meaning(database, key)?.let(::simplify) ?: lemma(database, key)?.let { base ->
                meaning(database, base)?.let { "($base) ${simplify(it)}" }
            }
        }
    }

    // 從照片裡依順序排的字找出片語（2 到 4 個字，例如 give up、look forward to）
    // 變化形會換回原形：gave up → give up
    suspend fun findPhrases(context: Context, tokens: List<String>): List<String> {
        val database = open(context) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            val found = LinkedHashSet<String>()
            for (i in tokens.indices) {
                for (n in 4 downTo 2) {
                    if (i + n > tokens.size) continue
                    val candidate = tokens.subList(i, i + n).joinToString(" ")
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
