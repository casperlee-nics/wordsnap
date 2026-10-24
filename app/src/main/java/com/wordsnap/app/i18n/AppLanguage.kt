package com.wordsnap.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

// 遊戲可以選的語言：介面文字和單字解釋都會換成這個語言
// 介面文字放在 res/values（中文）、values-ja（日文）
enum class AppLanguage(val tag: String, val nativeName: String, val translateTarget: String) {
    Chinese("zh-TW", "中文", TranslateLanguage.CHINESE),
    Japanese("ja", "日本語", TranslateLanguage.JAPANESE);

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        // 手機系統是日文就用日文，其他都用中文
        fun fromSystem(): AppLanguage {
            val system = Resources.getSystem().configuration.locales[0].language
            return when (system) {
                "ja" -> Japanese
                else -> Chinese
            }
        }
    }
}

private const val PREFS = "wordsnap"
private const val KEY_LANGUAGE = "language"

// 目前選的語言：還沒手動選過就跟著手機系統
fun appLanguage(context: Context): AppLanguage {
    val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
    return AppLanguage.entries.find { it.name == saved } ?: AppLanguage.fromSystem()
}

fun saveAppLanguage(context: Context, language: AppLanguage) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, language.name).commit()
    Strings.init(context)
}

// 換成遊戲選的語言的 Context，讀到的文字就是那個語言
fun Context.withAppLanguage(): Context {
    val locale = appLanguage(this).locale
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(locale))
    return createConfigurationContext(config)
}

// 在任何地方（畫面、資料、通知）都能讀到目前語言的文字
object Strings {
    private var context: Context? = null

    fun init(base: Context) {
        val app = base.applicationContext ?: base
        context = app.withAppLanguage()
    }

    fun get(@StringRes id: Int, vararg args: Any): String {
        val ctx = context ?: error("Strings.init 還沒呼叫")
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }

    fun array(@ArrayRes id: Int): Array<String> {
        val ctx = context ?: error("Strings.init 還沒呼叫")
        return ctx.resources.getStringArray(id)
    }
}

// 讀一句介面文字，例如 tr(R.string.cancel)、tr(R.string.word_count, 12)
fun tr(@StringRes id: Int, vararg args: Any): String = Strings.get(id, *args)

fun trArray(@ArrayRes id: Int): Array<String> = Strings.array(id)
