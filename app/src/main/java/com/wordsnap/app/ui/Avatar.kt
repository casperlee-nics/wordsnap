package com.wordsnap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 像素人物：每個字元是一格像素，"." 是透明；大多是 12 x 12 格，都是正方形
private val palette = mapOf(
    'K' to Color(0xFF202030),
    'S' to Color(0xFFF8C898),
    'H' to Color(0xFF704020),
    'h' to Color(0xFFA03028),
    'B' to Color(0xFF3A58C8),
    'R' to Color(0xFFE04030),
    'W' to Color(0xFFFFFFFF),
    'G' to Color(0xFF40A048),
    'g' to Color(0xFFA0D870),
    'O' to Color(0xFFF09030),
    'o' to Color(0xFFF8D8A8),
    'D' to Color(0xFFA06030),
    'd' to Color(0xFFE8C090),
    'M' to Color(0xFFB0B8C8),
    'm' to Color(0xFF687080),
    'L' to Color(0xFF90D0F8),
    'P' to Color(0xFFF08090),
    'Y' to Color(0xFFF8D030),
)

enum class Avatar(val label: String, val art: List<String>) {
    Man(
        "男子",
        listOf(
            "....KKKK....",
            "...KHHHHK...",
            "..KHHHHHHK..",
            "..KHSSSSHK..",
            "..KSKSSKSK..",
            "..KSSSSSSK..",
            "...KSPPSK...",
            "....KSSK....",
            "..KKBBBBKK..",
            ".KBBBBBBBBK.",
            ".KBBBBBBBBK.",
            ".KKKKKKKKKK.",
        ),
    ),
    Woman(
        "女子",
        listOf(
            "....KKKK....",
            "...KhhhhK...",
            "..KhhhhhhK..",
            ".KhhSSSShhK.",
            ".KhSKSSKShK.",
            ".KhSSSSSShK.",
            ".KhhSPPShhK.",
            ".KhhKSSKhhK.",
            "..KKRRRRKK..",
            ".KRRRRRRRRK.",
            ".KRRRRRRRRK.",
            ".KKKKKKKKKK.",
        ),
    ),
    ElfMan(
        "男精靈",
        listOf(
            "....KKKK....",
            "...KYYYYK...",
            "..KYYYYYYK..",
            "K.KYSSSSYK.K",
            "KSKSKSSKSKSK",
            ".KKSSSSSSKK.",
            "...KSPPSK...",
            "....KSSK....",
            "..KKGGGGKK..",
            ".KGGGYYGGGK.",
            ".KGGGGGGGGK.",
            ".KKKKKKKKKK.",
        ),
    ),
    ElfWoman(
        "女精靈",
        listOf(
            "....KKKK....",
            "...KYYYYK...",
            "..KYYYYYYK..",
            "K.KYSSSSYK.K",
            "KSKYKSSKYKSK",
            ".KKYSSSSYKK.",
            "..KYSPPSYK..",
            "..KYKSSKYK..",
            "..KKggggKK..",
            ".KggggggggK.",
            ".KgGgggggGK.",
            ".KKKKKKKKKK.",
        ),
    ),
    Goblin(
        "哥布林",
        listOf(
            "............",
            "...KKKKKK...",
            "..KggggggK..",
            "K.KggggggK.K",
            "KgKgRggRgKgK",
            ".KKggggggKK.",
            "..KgKWWKgK..",
            "...KggggK...",
            "..KKDDDDKK..",
            ".KgDDDDDDgK.",
            ".KgDDDDDDgK.",
            ".KKKKKKKKKK.",
        ),
    ),
    Dwarf(
        "矮人",
        listOf(
            "............",
            "....KKKK....",
            "...KMMMMK...",
            "..KMMMMMMK..",
            "..KKKKKKKK..",
            "..KSKSSKSK..",
            "..KSSPPSSK..",
            ".KOOOOOOOOK.",
            ".KOOOOOOOOK.",
            "KDKOOOOOOKDK",
            "KDDKOOOOKDDK",
            ".KKKKKKKKKK.",
        ),
    ),
    // 貓咪用 16 x 16 格，左右各兩根鬍鬚伸出臉外
    Cat(
        "貓咪",
        listOf(
            "................",
            "................",
            "...K........K...",
            "..KOK......KOK..",
            "..KPOK....KOPK..",
            "..KPPOKKKKOPPK..",
            "..KOOODOODOOOK..",
            "..KOOOODDOOOOK..",
            "..KOWKOOOOWKOK..",
            "..KOKKOOOOKKOK..",
            "..KPOOOPPOOOPK..",
            "KKKoooKooKoooKKK",
            "..KooooooooooK..",
            "KKKKKooooooKKKKK",
            ".....KKKKKK.....",
            "................",
        ),
    ),
    Dragon(
        "龍",
        listOf(
            ".K........K.",
            ".KK......KK.",
            "..KKKKKKKK..",
            ".KGGGGGGGGK.",
            "KGGKWGGKWGGK",
            "KGGKKGGKKGGK",
            "KGGGGGGGGGGK",
            "KGggggggggGK",
            ".KGWgWgWgGK.",
            "..KGGGGGGK..",
            ".KGKGGGGKGK.",
            ".KK.KKKK.KK.",
        ),
    ),
    Robot(
        "機器人",
        listOf(
            ".....KK.....",
            ".....RR.....",
            "..KKKKKKKK..",
            "..KMMMMMMK..",
            "..KMLLMLLK..",
            "..KMMMMMMK..",
            "..KMKKKKMK..",
            "..KKKKKKKK..",
            ".KKmmmmmmKK.",
            "KMKmRmmYmKMK",
            "KMKmmmmmmKMK",
            ".K.KKKKKK.K.",
        ),
    );

    companion object {
        // 資料庫存的是名稱，找不到就用男子；已移除的人物換成相近的
        fun from(key: String?): Avatar = when (key) {
            "GlassesMan" -> ElfMan
            "GlassesWoman" -> ElfWoman
            "Dog" -> Cat
            else -> entries.firstOrNull { it.name == key } ?: Man
        }
    }
}

@Composable
fun PixelAvatar(avatar: Avatar, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    PixelArt(avatar.art, palette, modifier.size(size))
}

// 把字元陣列畫成像素圖，寬度跟著版面，高度依比例
@Composable
fun PixelArt(art: List<String>, colors: Map<Char, Color>, modifier: Modifier = Modifier) {
    val columns = art.first().length
    Canvas(modifier.aspectRatio(columns.toFloat() / art.size)) {
        val cell = this.size.width / columns
        art.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = colors[ch] ?: return@forEachIndexed
                // 每格多畫 0.5 像素，避免格子之間出現細縫
                drawRect(color, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
