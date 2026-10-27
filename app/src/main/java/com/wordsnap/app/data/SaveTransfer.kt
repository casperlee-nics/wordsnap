package com.wordsnap.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

// 存檔搬家：把整個資料庫（冒險者、單字簿、單字、道具、冒險履歷）和遊戲設定打包成一個 zip
// 匯出成檔案或上傳雲端都用同一包；匯入時先檢查、放到暫存區，重新開啟 App 後才換上，避免換到一半還有畫面在讀舊資料
object SaveTransfer {
    private const val DB_NAME = "wordsnap.db"
    private const val ENTRY_INFO = "info.json"
    private const val ENTRY_DB = "wordsnap.db"
    private const val ENTRY_WAL = "wordsnap.db-wal"
    private const val ENTRY_PREFS = "prefs.json"
    private const val FORMAT = 1
    private const val MAX_ENTRY_BYTES = 50L * 1024 * 1024

    // 測試存檔是否已經建立過：這台手機原本的值要留著，不然匯入後會被測試存檔洗掉
    private const val KEEP_PREFIX = "test_saves"

    sealed class ImportResult {
        data object Ok : ImportResult()
        data object NotASave : ImportResult()
        data object TooNew : ImportResult()
    }

    // 打包目前的存檔
    suspend fun export(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        // 把還在 WAL 暫存檔裡的資料寫回主檔，複製主檔就是完整的存檔
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val version = db.openHelper.readableDatabase.version
        val dbFile = context.getDatabasePath(DB_NAME)
        val walFile = File(dbFile.path + "-wal")

        val info = JSONObject()
            .put("format", FORMAT)
            .put("dbVersion", version)
            .put("createdAt", System.currentTimeMillis())
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_INFO))
            zip.write(info.toString().toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ENTRY_DB))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            if (walFile.length() > 0) {
                zip.putNextEntry(ZipEntry(ENTRY_WAL))
                walFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry(ENTRY_PREFS))
            zip.write(prefsToJson(context.gamePrefs().all).toString().toByteArray())
            zip.closeEntry()
        }
        out.toByteArray()
    }

    // 檢查一包存檔，沒問題就放進暫存區，等 restartApp() 之後換上
    suspend fun stageImport(context: Context, bytes: ByteArray): ImportResult = withContext(Dispatchers.IO) {
        val dir = pendingDir(context)
        dir.deleteRecursively()
        dir.mkdirs()
        val names = mutableSetOf<String>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (name !in setOf(ENTRY_INFO, ENTRY_DB, ENTRY_WAL, ENTRY_PREFS)) continue
                    File(dir, name).outputStream().use { out ->
                        var total = 0L
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = zip.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > MAX_ENTRY_BYTES) error("存檔太大")
                            out.write(buf, 0, n)
                        }
                    }
                    names += name
                }
            }
            if (ENTRY_INFO !in names || ENTRY_DB !in names) return@withContext fail(dir, ImportResult.NotASave)
            val info = JSONObject(File(dir, ENTRY_INFO).readText())
            if (info.optInt("format") > FORMAT) return@withContext fail(dir, ImportResult.TooNew)
            val version = checkDatabase(File(dir, ENTRY_DB)) ?: return@withContext fail(dir, ImportResult.NotASave)
            if (version > AppDatabase.VERSION) return@withContext fail(dir, ImportResult.TooNew)
            ImportResult.Ok
        } catch (e: Exception) {
            fail(dir, ImportResult.NotASave)
        }
    }

    // App 一開始、資料庫打開之前呼叫：有暫存的存檔就換上
    @Synchronized
    fun applyPending(context: Context) {
        val dir = pendingDir(context)
        val staged = File(dir, ENTRY_DB)
        if (!staged.exists()) return
        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.parentFile?.mkdirs()
        for (suffix in listOf("", "-wal", "-shm", "-journal")) File(dbFile.path + suffix).delete()
        staged.copyTo(dbFile, overwrite = true)
        File(dir, ENTRY_WAL).takeIf { it.exists() }?.copyTo(File(dbFile.path + "-wal"), overwrite = true)

        File(dir, ENTRY_PREFS).takeIf { it.exists() }?.let { file ->
            val prefs = context.gamePrefs()
            val keep = prefs.all.filterKeys { it.startsWith(KEEP_PREFIX) }
            val edit = prefs.edit().clear()
            jsonToPrefs(JSONObject(file.readText()), edit)
            keep.forEach { (key, value) -> putValue(edit, key, value) }
            edit.commit()
        }
        dir.deleteRecursively()
    }

    // 關掉再重新打開 App，讓暫存的存檔換上去
    fun restartApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val component: ComponentName = launch.component ?: return
        context.startActivity(Intent.makeRestartActivityTask(component))
        exitProcess(0)
    }

    // 這包是不是我們的資料庫：打得開、檢查沒壞、有冒險者資料表；回傳資料庫版本
    private fun checkDatabase(file: File): Int? {
        val wal = File(file.parentFile, ENTRY_WAL)
        if (wal.exists()) wal.renameTo(File(file.path + "-wal"))
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            val ok = db.rawQuery("PRAGMA quick_check", null).use { it.moveToFirst() && it.getString(0) == "ok" }
            val hasProfiles = db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'profiles'",
                null,
            ).use { it.moveToFirst() }
            // 把 WAL 合併進主檔，換上時只要一個檔案
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            if (ok && hasProfiles && db.version > 0) db.version else null
        }.also {
            for (suffix in listOf("-wal", "-shm")) File(file.path + suffix).delete()
        }
    }

    private fun fail(dir: File, result: ImportResult): ImportResult {
        dir.deleteRecursively()
        return result
    }

    // 放在不會被系統備份的地方
    private fun pendingDir(context: Context) = File(context.noBackupFilesDir, "restore_pending")

    private fun prefsToJson(all: Map<String, *>): JSONObject {
        val json = JSONObject()
        for ((key, value) in all) {
            val item = when (value) {
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is String -> JSONObject().put("t", "s").put("v", value)
                is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray(value.map { it.toString() }))
                else -> null
            } ?: continue
            json.put(key, item)
        }
        return json
    }

    private fun jsonToPrefs(json: JSONObject, edit: android.content.SharedPreferences.Editor) {
        for (key in json.keys()) {
            val item = json.optJSONObject(key) ?: continue
            when (item.optString("t")) {
                "b" -> edit.putBoolean(key, item.getBoolean("v"))
                "i" -> edit.putInt(key, item.getInt("v"))
                "l" -> edit.putLong(key, item.getLong("v"))
                "f" -> edit.putFloat(key, item.getDouble("v").toFloat())
                "s" -> edit.putString(key, item.getString("v"))
                "ss" -> {
                    val array = item.getJSONArray("v")
                    edit.putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun putValue(edit: android.content.SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            is String -> edit.putString(key, value)
            is Set<*> -> edit.putStringSet(key, value as Set<String>)
        }
    }
}
