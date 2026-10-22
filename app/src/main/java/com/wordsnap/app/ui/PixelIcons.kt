package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelShape

// 像素按鈕上的小圖示（11 x 11），"K" 是深色，"." 是透明
private val IconPalette = mapOf('K' to PixelInk)

// 上一個：向左的三角形
val IconPrev = listOf(
    "...........",
    ".......K...",
    "......KK...",
    ".....KKK...",
    "....KKKK...",
    "...KKKKK...",
    "....KKKK...",
    ".....KKK...",
    "......KK...",
    ".......K...",
    "...........",
)

// 下一個和播放：向右的三角形
val IconNext = listOf(
    "...........",
    "...K.......",
    "...KK......",
    "...KKK.....",
    "...KKKK....",
    "...KKKKK...",
    "...KKKK....",
    "...KKK.....",
    "...KK......",
    "...K.......",
    "...........",
)

val IconPlay = IconNext

// 暫停：兩個並排的細長長方形
val IconPause = listOf(
    "...........",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "..KK...KK..",
    "...........",
)

// 方形的像素按鈕，只有圖示沒有文字
@Composable
fun PixelIconButton(
    icon: List<String>,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Surface(
        shape = PixelShape,
        border = PixelBorder,
        color = color,
        modifier = Modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            PixelArt(icon, IconPalette, Modifier.padding(size / 5))
        }
    }
}
