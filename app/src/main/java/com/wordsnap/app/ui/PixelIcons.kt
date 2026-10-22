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
import androidx.compose.ui.draw.alpha
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

// 朗讀速度：1 倍
val IconSpeed1 = listOf(
    "...........",
    "...........",
    "..K........",
    ".KK........",
    "..K...K...K",
    "..K....K.K.",
    "..K.....K..",
    "..K....K.K.",
    ".KKK..K...K",
    "...........",
    "...........",
)

// 朗讀速度：2 倍
val IconSpeed2 = listOf(
    "...........",
    "...........",
    ".KK........",
    "K..K.......",
    "...K..K...K",
    "..K....K.K.",
    ".K......K..",
    "K......K.K.",
    "KKKK..K...K",
    "...........",
    "...........",
)

// 彩色的大圖示（16 x 16）用的顏色
val ColorIconPalette = mapOf(
    'K' to PixelInk,
    'H' to Color(0xFF704020),
    'S' to Color(0xFFF8C898),
    'B' to Color(0xFF3A58C8),
    'R' to Color(0xFFE04030),
    'Y' to Color(0xFFF8D030),
    'W' to Color(0xFFFFFFFF),
    'L' to Color(0xFF90D0F8),
    'M' to Color(0xFFB0B8C8),
    'm' to Color(0xFF687080),
)

// 複習：不記得（人物頭上一個問號）
val IconForgot = listOf(
    "...........RRRR.",
    "..........RR..RR",
    "..............RR",
    ".............RR.",
    "............RR..",
    "............RR..",
    "..KKKKK.........",
    ".KHHHHHK....RR..",
    ".KSSSSSK....RR..",
    ".KSKSKSK........",
    ".KSSSSSK........",
    "..KSSSK.........",
    ".KKBBBKK........",
    "KBBBBBBBK.......",
    "KBBBBBBBK.......",
    "KKKKKKKKK.......",
)

// 複習：記得（人物頭上一顆燈泡）
val IconRemember = listOf(
    "...........KKK..",
    "..........KYYYK.",
    ".........KYYWYYK",
    ".........KYWYYYK",
    ".........KYYYYYK",
    "..........KYYYK.",
    "..KKKKK....KMK..",
    ".KHHHHHK...KmK..",
    ".KSSSSSK...KKK..",
    ".KSKSKSK........",
    ".KSSSSSK........",
    "..KSSSK.........",
    ".KKBBBKK........",
    "KBBBBBBBK.......",
    "KBBBBBBBK.......",
    "KKKKKKKKK.......",
)

// 加單字：手動輸入（筆和筆記本）
val IconNotebook = listOf(
    "................",
    "..........KK....",
    ".........KYYK...",
    "..KKKKKKKYYK....",
    ".KWWWWWWYYKK....",
    ".KWLLLLYYKBK....",
    ".KWWWWYYKWBK....",
    ".KWLLLKKLLBK....",
    ".KWWWWKWWWBK....",
    ".KWLLLLLLLBK....",
    ".KWWWWWWWWBK....",
    ".KWLLLLLLLBK....",
    ".KWWWWWWWWBK....",
    ".KBBBBBBBBBK....",
    "..KKKKKKKKK.....",
    "................",
)

// 加單字：拍照（相機）
val IconCamera = listOf(
    "................",
    "................",
    "....KKKK........",
    "...KmmmmK..KK...",
    ".KKKKKKKKKKYYKK.",
    "KMMMMMMMMMMMMMMK",
    "KMMMMKKKKKMMMMMK",
    "KMMMKLLLLLKMMMMK",
    "KmmKLLWLLLLKmmmK",
    "KmmKLWLLLLLKmmmK",
    "KmmKLLLLLLLKmmmK",
    "KMMKLLLLLLLKMMMK",
    "KMMMKLLLLLKMMMMK",
    "KMMMMKKKKKMMMMMK",
    ".KKKKKKKKKKKKKK.",
    "................",
)

// 戰鬥：再聽一次（大聲公）
val IconMegaphone = listOf(
    "................",
    "................",
    "...........K....",
    "..........KK..K.",
    "........KKMK.K..",
    "......KKMMMK....",
    "..KKKKMMMMMK.KK.",
    "..KRRKMMMMMK....",
    "..KRRKMMMMMK.KK.",
    "..KKKKMMMMMK....",
    "....KKKKMMMK.K..",
    "....KMK.KKMK..K.",
    "....KMK...KK....",
    "....KKK....K....",
    "................",
    "................",
)

// 戰鬥：攻擊（劍）
val IconSword = listOf(
    "................",
    "..............KK",
    ".............KWK",
    "............KWMK",
    "...........KWMK.",
    "..........KWMK..",
    ".........KWMK...",
    "........KWMK....",
    "...KK..KWMK.....",
    "...KYKKWMK......",
    "....KYKMK.......",
    ".....KYK........",
    "....KHKYK.......",
    "...KHK.KYK......",
    "..KHK...KK......",
    "..KK............",
)

// 戰鬥：下一回（向右的箭頭）
val IconArrowRight = listOf(
    "................",
    "................",
    "........K.......",
    "........KK......",
    "........KYK.....",
    "..KKKKKKKYYK....",
    "..KYYYYYYYYYK...",
    "..KYYYYYYYYYYK..",
    "..KYYYYYYYYYK...",
    "..KKKKKKKYYK....",
    "........KYK.....",
    "........KK......",
    "........K.......",
    "................",
    "................",
    "................",
)

// 方形的像素按鈕，只有圖示沒有文字
@Composable
fun PixelIconButton(
    icon: List<String>,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    palette: Map<Char, Color> = IconPalette,
    enabled: Boolean = true,
) {
    Surface(
        shape = PixelShape,
        border = PixelBorder,
        color = color,
        modifier = Modifier
            .size(size)
            // 不能按的時候變淡
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            PixelArt(icon, palette, Modifier.padding(size / 5))
        }
    }
}
