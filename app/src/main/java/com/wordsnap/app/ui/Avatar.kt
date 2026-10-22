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

// 像素人物：每個字元是一格像素，"." 是透明；大多是 12 x 12 格，貓咪是 16 x 16
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
    'V' to Color(0xFF7040B0),
    'y' to Color(0xFF2060A8),
    'r' to Color(0xFFA02828),
    'A' to Color(0xFFF8F0A0),
    'E' to Color(0xFF30A0A0),
)

// 每個職業的像素頭像；稱號每升一階（每 10 級）頭像就變強一點，共 6 種：
// 0 原本的樣子、1 衣服換色、2 加披風、3 衣服再換色、4 戴上皇冠、5 金色衣服加光環
// cloth 是衣服（或身體）的顏色字元，clothTiers 是 6 個階段要換成的顏色
enum class Avatar(val label: String, val art: List<String>, val cloth: Char, val clothTiers: String) {
    Hero(
        "男勇者",
        listOf(
            "....KKKK....",
            "...KHHHHK...",
            "..KRRRRRRK..",
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
        cloth = 'B',
        clothTiers = "ByMMMY",
    ),
    Mage(
        "女魔法師",
        listOf(
            "......KK....",
            ".....KVVK...",
            "....KVVVK...",
            "...KVVYVVK..",
            ".KKKKKKKKKK.",
            "..KhSSSShK..",
            "..KSKSSKSK..",
            "..KhSSSShK..",
            "..KhSPPShK..",
            "..KKVVVVKK..",
            ".KVVVYVVVVK.",
            ".KKKKKKKKKK.",
        ),
        cloth = 'V',
        clothTiers = "VyyrrY",
    ),
    ElfArcher(
        "女精靈弓箭手",
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
        cloth = 'g',
        clothTiers = "gGGEEY",
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
        cloth = 'D',
        clothTiers = "DddmmY",
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
        cloth = 'O',
        clothTiers = "ORRMMY",
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
        cloth = 'D',
        clothTiers = "DRRVVY",
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
        cloth = 'G',
        clothTiers = "GEERRY",
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
        cloth = 'm',
        clothTiers = "mBBRRY",
    );

    private val tierArts = arrayOfNulls<List<String>>(AVATAR_TIERS)

    // 第 tier 階（0 到 5）的頭像，算過一次就記起來
    fun artFor(tier: Int): List<String> {
        val t = tier.coerceIn(0, AVATAR_TIERS - 1)
        return tierArts[t] ?: buildTierArt(art, cloth, clothTiers[t], t).also { tierArts[t] = it }
    }

    companion object {
        const val TIER_COUNT = AVATAR_TIERS

        // 資料庫存的是名稱，找不到就用男勇者；舊版的人物換成相近的新職業
        fun from(key: String?): Avatar = when (key) {
            "Man" -> Hero
            "Woman" -> Mage
            "ElfMan", "ElfWoman", "GlassesMan", "GlassesWoman" -> ElfArcher
            "Dog" -> Cat
            else -> entries.firstOrNull { it.name == key } ?: Hero
        }
    }
}

// 頭像有幾個階段；放在 enum 外面，enum 常數建立時才讀得到
private const val AVATAR_TIERS = 6

// 等級換成頭像階段：每 10 級升一階，最多到第 5 階（跟稱號一樣）
fun avatarTier(level: Int): Int = (level / 10).coerceIn(0, Avatar.TIER_COUNT - 1)

// 把原本的頭像放進比較大的正方形畫布（留空間給披風、皇冠、光環），再依階段加上裝飾
private fun buildTierArt(art: List<String>, cloth: Char, clothColor: Char, tier: Int): List<String> {
    val w = art.first().length
    val h = art.size
    val size = maxOf(w, h) + 4
    val left = (size - w) / 2
    val top = size - h - 1
    val grid = Array(size) { CharArray(size) { '.' } }
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            if (c != '.') grid[top + y][left + x] = if (c == cloth) clothColor else c
        }
    }
    val rows = (0 until size).filter { y -> grid[y].any { it != '.' } }
    fun setIfEmpty(y: Int, x: Int, c: Char) {
        if (y in 0 until size && x in 0 until size && grid[y][x] == '.') grid[y][x] = c
    }
    // 披風：下半身兩側各多一格深紅色
    if (tier >= 2) {
        for (y in rows.takeLast(5)) {
            val xs = (0 until size).filter { grid[y][it] != '.' }
            setIfEmpty(y, xs.first() - 1, 'r')
            setIfEmpty(y, xs.last() + 1, 'r')
        }
    }
    // 皇冠：戴在頭頂上
    if (tier >= 4) {
        val y0 = rows.first()
        val xs = (0 until size).filter { grid[y0][it] != '.' }
        val cx = (xs.first() + xs.last()) / 2
        listOf("Y.Y.Y", "YYYYY").forEachIndexed { i, row ->
            row.forEachIndexed { j, c -> if (c != '.') setIfEmpty(y0 - 2 + i, cx - 2 + j, c) }
        }
    }
    // 光環：整個人物外圍一圈淡金色
    if (tier >= 5) {
        val snapshot = grid.map { it.copyOf() }
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (snapshot[y][x] != '.') continue
                val near = listOf(y - 1 to x, y + 1 to x, y to x - 1, y to x + 1).any { (ny, nx) ->
                    ny in 0 until size && nx in 0 until size && snapshot[ny][nx] != '.'
                }
                if (near) grid[y][x] = 'A'
            }
        }
    }
    return grid.map { String(it) }
}

// tier 是頭像階段（見 avatarTier），選角色時用 0
@Composable
fun PixelAvatar(avatar: Avatar, size: Dp = 48.dp, modifier: Modifier = Modifier, tier: Int = 0) {
    PixelArt(avatar.artFor(tier), palette, modifier.size(size))
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
