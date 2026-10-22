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
    private const val VERSION = 1
    private const val ASSET = "ecdict.db"

    private val lock = Mutex()
    private var db: SQLiteDatabase? = null

    // 第一次使用時把字典從 assets 複製到手機裡再打開（只讀）
    private suspend fun open(context: Context): SQLiteDatabase? = lock.withLock {
        db?.let { return@withLock it }
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.noBackupFilesDir, "ecdict_v$VERSION.db")
                if (!file.exists()) {
                    val tmp = File(file.path + ".tmp")
                    context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    tmp.renameTo(file)
                }
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrNull()
        }.also { db = it }
    }

    // 查一個字；過去式、複數這類變化形會找回原形，例如 went → (go) vi. 去…
    suspend fun lookup(context: Context, word: String): String? {
        val database = open(context) ?: return null
        val key = word.trim().lowercase()
        if (key.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            meaning(database, key) ?: lemma(database, key)?.let { base ->
                meaning(database, base)?.let { "($base) $it" }
            }
        }
    }

    private fun meaning(db: SQLiteDatabase, word: String): String? =
        db.rawQuery("SELECT meaning FROM dict WHERE word = ?", arrayOf(word)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    private fun lemma(db: SQLiteDatabase, form: String): String? =
        db.rawQuery("SELECT lemma FROM forms WHERE form = ?", arrayOf(form)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
