package com.wordsnap.app.ocr

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

// 古書可以選的檔案：PDF、純文字（TXT、CSV）、Excel（.xlsx）
val DocumentMimeTypes = arrayOf(
    "application/pdf",
    "text/plain",
    "text/csv",
    "text/comma-separated-values",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
)

// 讀取古書：PDF 一頁一頁辨識；TXT、CSV 直接讀文字；Excel 讀出每一格的文字
object DocumentText {
    suspend fun scan(context: Context, uri: Uri, onPage: (Int, Int) -> Unit = { _, _ -> }): ScanResult {
        val kind = kindOf(context, uri)
        if (kind == Kind.Pdf) return TextScanner.scanPdf(context, uri, onPage)
        val text = withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("打不開這個檔案")
            input.use { if (kind == Kind.Excel) readExcel(it) else readText(it) }
        }
        return TextScanner.analyzeText(text)
    }

    private enum class Kind { Pdf, Excel, Text }

    // 先看檔案類型，看不出來再看副檔名
    private fun kindOf(context: Context, uri: Uri): Kind {
        val type = context.contentResolver.getType(uri).orEmpty()
        val name = fileName(context, uri).lowercase()
        return when {
            type == "application/pdf" || name.endsWith(".pdf") -> Kind.Pdf
            type.contains("spreadsheetml") || name.endsWith(".xlsx") -> Kind.Excel
            name.endsWith(".xls") -> throw IllegalArgumentException("舊版 Excel（.xls）請先另存成 .xlsx 或 CSV")
            else -> Kind.Text
        }
    }

    private fun fileName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
        } ?: uri.lastPathSegment.orEmpty()

    // 文字檔最多讀 2 MB，太大的檔案只讀前面
    private fun readText(input: InputStream): String {
        val bytes = input.readNBytesCompat(MAX_TEXT_BYTES)
        // 有 UTF-16 的 BOM 就用 UTF-16，其他當 UTF-8
        val utf16 = bytes.size >= 2 &&
            ((bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) || (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()))
        return String(bytes, if (utf16) Charsets.UTF_16 else Charsets.UTF_8)
    }

    // .xlsx 其實是一個 zip：文字放在 xl/sharedStrings.xml，直接打在格子裡的放在 xl/worksheets/sheetN.xml
    private fun readExcel(input: InputStream): String {
        val text = StringBuilder()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name == "xl/sharedStrings.xml" || (name.startsWith("xl/worksheets/") && name.endsWith(".xml"))) {
                    collectText(zip, text)
                }
                if (text.length > MAX_TEXT_BYTES) break
            }
        }
        if (text.isBlank()) throw IllegalArgumentException("這個 Excel 檔裡沒有找到文字")
        return text.toString()
    }

    // 把 XML 裡 <t>（共用文字、格子裡的文字）的內容收集起來；數字格 <v> 不需要
    private fun collectText(input: InputStream, out: StringBuilder) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // 不能讓 parser 關掉 zip，所以包一層不會關的 stream
        parser.setInput(NonClosing(input), "UTF-8")
        var inText = false
        while (true) {
            when (parser.next()) {
                XmlPullParser.END_DOCUMENT -> return
                XmlPullParser.START_TAG -> inText = parser.name == "t" || parser.name.endsWith(":t")
                XmlPullParser.END_TAG -> if (inText) {
                    inText = false
                    out.append(' ')
                }
                XmlPullParser.TEXT -> if (inText) out.append(parser.text)
            }
        }
    }

    private class NonClosing(private val input: InputStream) : InputStream() {
        override fun read(): Int = input.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)
        override fun close() = Unit
    }

    private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private const val MAX_TEXT_BYTES = 2 * 1024 * 1024
}
