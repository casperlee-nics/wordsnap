package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import com.wordsnap.app.ui.theme.PixelInk

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

// 加單字：相簿新增怪物（一張山和太陽的照片）
val IconGallery = listOf(
    "................",
    "................",
    ".KKKKKKKKKKKKKK.",
    ".KWWWWWWWWWWWWK.",
    ".KWLLLLLLLLLLWK.",
    ".KWLLLLLLLYYLWK.",
    ".KWLLLLLLLYYLWK.",
    ".KWLLLKLLLLLLWK.",
    ".KWLLKBKLLLLLWK.",
    ".KWLKBBBKLKLLWK.",
    ".KWKBBBBBKBKLWK.",
    ".KWBBBBBBBBBKWK.",
    ".KWBBBBBBBBBBWK.",
    ".KWWWWWWWWWWWWK.",
    ".KKKKKKKKKKKKKK.",
    "................",
)

// 加單字：讀取古書上的怪物（有金色鑲邊和紅寶石的古書）
val IconOldBook = listOf(
    "................",
    "................",
    "...KKKKKKKKKKK..",
    "..KHHHHHHHHHHHK.",
    "..KHYYYYYYYYYHK.",
    "..KHYHHHHHHHYHK.",
    "..KHYHHRRHHHYHK.",
    "..KHYHRYYRHHYHK.",
    "..KHYHHRRHHHYHK.",
    "..KHYHHHHHHHYHK.",
    "..KHYYYYYYYYYHK.",
    "..KHHHHHHHHHHHK.",
    "..KSSSSSSSSSSSK.",
    "..KSWSWSWSWSWSK.",
    "...KKKKKKKKKKK..",
    "................",
)

// 人物能力：轉職（上下兩個相反方向的箭頭）
val IconClassChange = listOf(
    "................",
    "................",
    "..........K.....",
    "..........KK....",
    "...KKKKKKKKYK...",
    "...KYYYYYYYYYK..",
    "...KKKKKKKKYK...",
    "..........KK....",
    ".....K....K.....",
    "....KK..........",
    "...KLKKKKKKKK...",
    "..KLLLLLLLLLK...",
    "...KLKKKKKKKK...",
    "....KK..........",
    ".....K..........",
    "................",
)

// 主畫面：遊戲說明（問號）
val IconHelp = listOf(
    "................",
    "................",
    ".....KKKKKK.....",
    "....KYYYYYYK....",
    "...KYYKKKKYYK...",
    "...KYK....KYK...",
    "....K.....KYK...",
    ".........KYYK...",
    "........KYYK....",
    ".......KYYK.....",
    ".......KYK......",
    ".......KKK......",
    "................",
    ".......KYK......",
    ".......KKK......",
    "................",
)

// 主畫面右下角：插在草地上、寫著問號的小木牌（有木紋和裂縫）
val IconHelpSign = listOf(
    "....................",
    ".KKKKKKKKKKKKKKKKKK.",
    "KLMMMMLLLLLLLLLDLLLK",
    "KLLLLLLLQQQQLLMDMLLK",
    "KMDMLLLQQLLQQLDLLLLK",
    "KLLLLLLLLLLQQLDMMMLK",
    "KLLLLLLLLLQQLLMDDMLK",
    "KLLLLLLLLQQLLLLLLLLK",
    "KLMDMLLLLQQLLLLLLLLK",
    "KLLLLLLLLLLLLLLMMMMK",
    "KLLDLLLLLQQLLMMMMLLK",
    "KLMDDMMLLQQLLLLLLLLK",
    "KLLLDLLLLLMMDMMLLLLK",
    ".KKKKKKKKKKKKKKKKKK.",
    "........KPPK........",
    "........KPPK........",
    "........KPPK........",
    ".......KKPPKK.......",
)

val SignPalette = mapOf(
    'K' to PixelInk,
    'L' to Color(0xFFE8C088),
    'M' to Color(0xFFC89058),
    'D' to Color(0xFF7A4C20),
    'Q' to Color(0xFF4A2810),
    'P' to Color(0xFF704020),
)

// 單字簿：發音（藍色的播放三角形）
val IconPlayWord = listOf(
    "................",
    "................",
    "....KK..........",
    "....KLKK........",
    "....KLLLKK......",
    "....KLBBLLKK....",
    "....KLBBBBLLKK..",
    "....KLBBBBBBBLK.",
    "....KLBBBBBBBLK.",
    "....KLBBBBLLKK..",
    "....KLBBLLKK....",
    "....KLLLKK......",
    "....KLKK........",
    "....KK..........",
    "................",
    "................",
)

// 記錄管理：改名字（鉛筆）
val IconPencil = listOf(
    "................",
    "............KK..",
    "...........KRRK.",
    "..........KMRRRK",
    ".........KYKMRK.",
    "........KYYYKK..",
    ".......KYYYYK...",
    "......KYYYYK....",
    ".....KYYYYK.....",
    "....KYYYYK......",
    "...KSYYYK.......",
    "...KSSYK........",
    "..KKSSK.........",
    "..KKKK..........",
    "................",
    "................",
)

// 記錄管理：刪除紀錄（垃圾桶）
val IconTrash = listOf(
    "................",
    "......KKKK......",
    "..KKKKMMMMKKKK..",
    "..KWWWWWWWWWWK..",
    "..KKKKKKKKKKKK..",
    "...KMMMMMMMMK...",
    "...KMmMmMmMMK...",
    "...KMmMmMmMMK...",
    "...KMmMmMmMMK...",
    "...KMmMmMmMMK...",
    "...KMmMmMmMMK...",
    "...KMmMmMmMMK...",
    "...KMMMMMMMMK...",
    "....KKKKKKKK....",
    "................",
    "................",
)

// 沒有外框的像素圖示按鈕，放在已經有外框的卡片或視窗裡（不要框中框）
@Composable
fun PixelIconPlain(
    icon: List<String>,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    palette: Map<Char, Color> = ColorIconPalette,
) {
    Box(
        Modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PixelArt(icon, palette, Modifier.padding(size / 10))
    }
}

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
    PixelSurface(
        color = color,
        unit = 2.dp,
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
