package com.wordsnap.app.translate

import android.content.Context
import com.wordsnap.app.i18n.AppLanguage
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 日文字典包：英文單字 → 日文解釋
// 中文解釋在內建的 ecdict.db；日文切換語言時才「下載」安裝到手機裡
//
// 字典包是一個 SQLite 檔，只要一個表：
//   CREATE TABLE dict (word TEXT PRIMARY KEY, meaning TEXT NOT NULL)
// word 是小寫英文（片語中間一個空白），meaning 最多 2～3 個意思，用「、」或「; 」隔開，不要詞性
// 過去式、複數這類變化形和片語清單共用 ecdict.db 的 forms、phrases 表，不用放在字典包裡
//
// 字典包從哪裡來由 DictionarySource 決定：
// - 現在（開發測試）：直接打包在 App 的 assets/dict/ja.db
// - 上架時：改成 Google Play 的按需下載資源包（Play Asset Delivery），一個語言一包，只要換掉 source
object DictionaryPacks {
    // 換新版字典包時把版本加一，App 會重新安裝
    private val VERSION = mapOf(AppLanguage.Japanese to 3)

    var source: DictionarySource = BundledAssetSource

    // 開 App 和設定畫面可能同時要安裝，一次只裝一個，免得兩邊寫同一個暫存檔
    private val installing = Mutex()

    fun needsPack(language: AppLanguage): Boolean = language != AppLanguage.Chinese

    fun file(context: Context, language: AppLanguage): File =
        File(context.noBackupFilesDir, "dict_${language.tag}_v${VERSION[language] ?: 1}.db")

    fun isInstalled(context: Context, language: AppLanguage): Boolean =
        !needsPack(language) || file(context, language).exists()

    // 安裝字典包；onProgress 是 0 到 1 的進度。找不到字典包（還沒準備好）回傳 false
    suspend fun install(context: Context, language: AppLanguage, onProgress: (Float) -> Unit = {}): Boolean {
        if (isInstalled(context, language)) return true
        return installing.withLock {
            // 等鎖的時候另一邊可能已經裝好了
            if (isInstalled(context, language)) return@withLock true
            copy(context, language, onProgress)
        }
    }

    private suspend fun copy(context: Context, language: AppLanguage, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = file(context, language)
                // 舊版的字典包刪掉，省空間
                context.noBackupFilesDir.listFiles()?.filter { it.name.startsWith("dict_${language.tag}_") }?.forEach { it.delete() }
                val (input, size) = source.open(context, language) ?: return@runCatching false
                val tmp = File(target.path + ".tmp")
                input.use { from ->
                    tmp.outputStream().use { to ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val n = from.read(buffer)
                            if (n < 0) break
                            to.write(buffer, 0, n)
                            copied += n
                            if (size > 0) onProgress((copied.toFloat() / size).coerceAtMost(1f))
                        }
                    }
                }
                tmp.renameTo(target)
            }.getOrDefault(false)
        }
}

// 字典包的來源：回傳檔案內容和大小（不知道大小就給 -1）；找不到回傳 null
interface DictionarySource {
    fun open(context: Context, language: AppLanguage): Pair<InputStream, Long>?
}

// 開發測試用：字典包放在 App 裡的 assets/dict/
object BundledAssetSource : DictionarySource {
    override fun open(context: Context, language: AppLanguage): Pair<InputStream, Long>? {
        val name = "dict/${language.tag}.db"
        val size = runCatching { context.assets.openFd(name).use { it.length } }.getOrDefault(-1L)
        val input = runCatching { context.assets.open(name) }.getOrNull() ?: return null
        return input to size
    }
}
