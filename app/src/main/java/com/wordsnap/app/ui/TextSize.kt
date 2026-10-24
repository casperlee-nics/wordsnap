package com.wordsnap.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.wordsnap.app.R

private const val PREFS = "wordsnap"
private const val KEY_LARGE_TEXT = "large_text"

// 文字大小：小（原本的大小）、大（放大 1.2 倍）
// 只放大輸入的字和講解的文字（單字、解釋、說明），按鈕上的字不變
enum class TextSize(val labelRes: Int, val scale: Float) {
    Small(R.string.settings_text_small, 1f),
    Large(R.string.settings_text_large, 1.2f),
}

// 目前的文字大小，設定一改，畫面馬上跟著變
private var currentTextSize by mutableStateOf<TextSize?>(null)

fun loadTextSize(context: Context): TextSize {
    currentTextSize?.let { return it }
    val large = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LARGE_TEXT, false)
    return (if (large) TextSize.Large else TextSize.Small).also { currentTextSize = it }
}

fun setTextSize(context: Context, size: TextSize) {
    currentTextSize = size
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LARGE_TEXT, size == TextSize.Large).apply()
}

// 輸入框和講解文字用：設定成「大」時字放大 1.2 倍
@Composable
fun TextStyle.readable(): TextStyle {
    val scale = (currentTextSize ?: TextSize.Small).scale
    if (scale == 1f) return this
    return copy(fontSize = fontSize.scaled(scale), lineHeight = lineHeight.scaled(scale))
}

private fun TextUnit.scaled(scale: Float): TextUnit = if (isSpecified) this * scale else this
