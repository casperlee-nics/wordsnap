package com.wordsnap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 像素人物：每個字元是一格像素，"." 是透明
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
    GlassesMan(
        "戴眼鏡男子",
        listOf(
            "....KKKK....",
            "...KHHHHK...",
            "..KHHHHHHK..",
            "..KHSSSSHK..",
            ".KKLKLLKLKK.",
            "..KSSSSSSK..",
            "...KSPPSK...",
            "....KSSK....",
            "..KKBBBBKK..",
            ".KBBBBBBBBK.",
            ".KBBBBBBBBK.",
            ".KKKKKKKKKK.",
        ),
    ),
    GlassesWoman(
        "戴眼鏡女子",
        listOf(
            "....KKKK....",
            "...KhhhhK...",
            "..KhhhhhhK..",
            ".KhhSSSShhK.",
            ".KKLKLLKLKK.",
            ".KhSSSSSShK.",
            ".KhhSPPShhK.",
            ".KhhKSSKhhK.",
            "..KKRRRRKK..",
            ".KRRRRRRRRK.",
            ".KRRRRRRRRK.",
            ".KKKKKKKKKK.",
        ),
    ),
    Cat(
        "貓咪",
        listOf(
            ".KK......KK.",
            ".KOK....KOK.",
            ".KOOKKKKOOK.",
            ".KOOOOOOOOK.",
            "KOOKOOOOKOOK",
            "KOOOOOOOOOOK",
            "KOoooPPoooOK",
            "KOooKooKooOK",
            ".KOooooooOK.",
            "..KOOOOOOK..",
            "..KOKKKKOK..",
            "..KK....KK..",
        ),
    ),
    Dog(
        "狗",
        listOf(
            "..KKKKKKKK..",
            ".KDDDDDDDDK.",
            "KdKDDDDDDKdK",
            "KdKDKDDKDKdK",
            "KdKDDDDDDKdK",
            "KdKDdKKdDKdK",
            ".KKDddddDKK.",
            "...KDPPDK...",
            "...KDDDDK...",
            "..KDDDDDDK..",
            "..KDKDDKDK..",
            "..KK.KK.KK..",
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
        // 資料庫存的是名稱，找不到就用男子
        fun from(key: String?): Avatar = entries.firstOrNull { it.name == key } ?: Man
    }
}

@Composable
fun PixelAvatar(avatar: Avatar, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val cell = this.size.width / avatar.art.size
        avatar.art.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = palette[ch] ?: return@forEachIndexed
                // 每格多畫 0.5 像素，避免格子之間出現細縫
                drawRect(color, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
