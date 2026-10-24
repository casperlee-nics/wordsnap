package com.wordsnap.app.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

// 只排一行的文字：放不下時字自動縮小（最小到原本的 minScale），不會被切掉或擠出按鈕
// 按鈕、標題列、木牌這些寬度固定的地方用；日文、韓文的字通常比中文長
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = TextAlign.Center,
    minScale: Float = 0.55f,
) {
    var scale by remember(text, style) { mutableFloatStateOf(1f) }
    var ready by remember(text, style) { mutableStateOf(false) }
    Text(
        text,
        color = color,
        style = style.copy(fontSize = style.fontSize.times(scale), lineHeight = style.lineHeight.times(scale)),
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        // 縮到放得下才畫出來，不會先閃一下太大的字
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && scale > minScale) {
                scale = (scale - 0.06f).coerceAtLeast(minScale)
            } else {
                ready = true
            }
        },
    )
}

private fun TextUnit.times(scale: Float): TextUnit = if (isSpecified) this * scale else this
