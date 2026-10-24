package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wordsnap.app.ui.theme.PixelInk

// 建立角色畫面用的元件：木牌標題、魔法書、各職業的外框、職業故事書卷、「開始冒險」按鈕

// ---------- 木牌標題 ----------

// 不規則的木牌：邊緣一格一格高低不平、兩片木板拼成，釘著兩顆釘子，左上和右下纏著藤蔓
@Composable
fun WoodSignTitle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(250.dp)
            .height(84.dp)
            .drawBehind { drawWoodSign() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = SignText,
            style = MaterialTheme.typography.displaySmall.copy(
                shadow = Shadow(PixelInk, offset = Offset(3f, 3f), blurRadius = 0f),
            ),
        )
    }
}

private fun DrawScope.drawWoodSign() {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    val random = java.util.Random(11)
    val top0 = px * 3
    val bottom0 = h - px * 3
    var x = px
    // 木板本體：每一欄的上下緣隨機高低一點
    while (x < w - px) {
        val top = top0 + px * random.nextInt(2)
        val bottom = bottom0 - px * random.nextInt(2)
        drawRect(PixelInk, Offset(x - px, top - px), Size(px * 2, bottom - top + px * 2))
        x += px
    }
    x = px
    val split = h / 2
    val grain = java.util.Random(5)
    while (x < w - px * 2) {
        val top = top0 + px
        val bottom = bottom0 - px
        drawRect(SignWood, Offset(x, top), Size(px + 0.5f, bottom - top))
        // 上緣亮、下緣暗
        drawRect(SignLight, Offset(x, top), Size(px + 0.5f, px))
        drawRect(SignDark, Offset(x, bottom - px), Size(px + 0.5f, px))
        // 兩片木板中間的縫
        drawRect(SignDark, Offset(x, split - px / 2), Size(px + 0.5f, px * 0.6f))
        // 木紋
        if (grain.nextInt(4) == 0) drawRect(SignGrain, Offset(x, top + px * (2 + grain.nextInt(3))), Size(px * 2, px * 0.5f))
        if (grain.nextInt(4) == 0) drawRect(SignGrain, Offset(x, split + px * (1 + grain.nextInt(3))), Size(px * 2, px * 0.5f))
        x += px
    }
    // 兩顆釘子
    for (nx in listOf(px * 4, w - px * 6)) {
        drawRect(PixelInk, Offset(nx, split - px * 1.5f), Size(px * 2, px * 2))
        drawRect(SignNail, Offset(nx, split - px * 1.5f), Size(px, px))
    }
    // 藤蔓：左上角往右、往下爬，右下角也有一小段
    val vine = listOf(
        0 to 2, 1 to 1, 2 to 1, 3 to 0, 4 to 0, 5 to 1, 6 to 1, 7 to 0, 8 to 0, 9 to 1,
        0 to 3, 0 to 4, 1 to 5, 1 to 6, 0 to 7, 0 to 8, 1 to 9,
    )
    val leaves = listOf(2 to 0, 5 to 2, 8 to -1, 1 to 4, 2 to 7, 0 to 10)
    fun drawVine(ox: Float, oy: Float, flip: Float) {
        vine.forEach { (vx, vy) -> drawRect(VineDark, Offset(ox + flip * vx * px, oy + flip * vy * px), Size(px, px)) }
        leaves.forEach { (lx, ly) ->
            drawRect(VineLeaf, Offset(ox + flip * lx * px, oy + flip * ly * px), Size(px * 1.5f, px * 1.5f))
            drawRect(VineLight, Offset(ox + flip * lx * px, oy + flip * ly * px), Size(px * 0.6f, px * 0.6f))
        }
    }
    drawVine(px, px * 2, 1f)
    drawVine(w - px * 2, h - px * 3, -1f)
}

private val SignWood = Color(0xFF9A6A38)
private val SignLight = Color(0xFFC08A50)
private val SignDark = Color(0xFF6A4420)
private val SignGrain = Color(0xFF7A5028)
private val SignNail = Color(0xFFC8CCD8)
private val SignText = Color(0xFFFFF0C8)
private val VineDark = Color(0xFF2E6A2A)
private val VineLeaf = Color(0xFF58B048)
private val VineLight = Color(0xFFA8E070)

// ---------- 魔法書 ----------

// 攤開的魔法書：書頁微微彎起、中間有書背的陰影、紅色書籤垂下來，上面的星光一閃一閃
@Composable
fun MagicBook(modifier: Modifier = Modifier) {
    val twinkle by rememberInfiniteTransition(label = "book").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse), label = "twinkle",
    )
    val palette = remember(twinkle > 0.5f) {
        BookPalette + mapOf(
            'W' to if (twinkle > 0.5f) Color(0xFFFFF6B0) else Color(0xFFF8D030),
            'Y' to if (twinkle > 0.5f) Color(0xFFF8D030) else Color(0xFFFFF6B0),
        )
    }
    Box(
        modifier.drawBehind {
            // 書後面淡淡的金色光暈
            drawCircle(Color(0xFFF8E070).copy(alpha = 0.18f + 0.12f * twinkle), size.minDimension * 0.55f, center)
        },
        contentAlignment = Alignment.Center,
    ) {
        PixelArt(BookArt, palette, Modifier.fillMaxWidth())
    }
}

private val BookPalette = mapOf(
    'K' to Color(0xFF202030),
    'P' to Color(0xFFF8F0D8),
    'p' to Color(0xFFD6C496),
    'q' to Color(0xFFE8DCBA),
    's' to Color(0xFFBEAA7C),
    'l' to Color(0xFF9C8458),
    'C' to Color(0xFF8A3A24),
    'Y' to Color(0xFFF8D030),
    'R' to Color(0xFFE04060),
    'r' to Color(0xFFA02840),
    'W' to Color(0xFFFFFFFF),
)

private val BookArt = listOf(
    "............................................",
    "..............Y.....................Y.......",
    ".....................W......................",
    "....................W.W.......W.............",
    "........Y............W.......W.W............",
    "..............................W.............",
    "......KKKKKKKKK..............KKKKKKKKK......",
    "...KKKPPPPPPPPPKKK........KKKPPPPPPPPPKKK...",
    ".KKPPPPPPPPPPPPPPPKK....KKPPPPPPPPPPPPPPPKK.",
    ".KPPPPPPPPPPPPPPPPPqKKKKqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPlllllllllPPqqssqqPllllllllllPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPllllllllllPPqqssqqPlllllllllPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPllllllllPPqqssqqPlllllllllPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPlllllllllllPPqqssqqPlllllllllPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqqssqqPPPPPPPPPPPPPPPPPK.",
    ".KPPPPPPPPPPPPPPPPPqpKKpqPPPPPPPPPPPPPPPPPK.",
    ".KppppppppppppppppppsKKsppppppppppppppppppK.",
    ".KssssssssssssssssssCCRrssssssssssssssssssK.",
    ".YCCCCCCCCCCCCCCCCCCCCRrCCCCCCCCCCCCCCCCCCY.",
    ".YYCCCCCCCCCCCCCCCCCKKRrCCCCCCCCCCCCCCCCCYY.",
    "..KKKKKKKKKKKKKKKKKK.KRrKKKKKKKKKKKKKKKKKK..",
    "......................R.....................",
    "............................................",
)

// ---------- 各職業的外框 ----------

// fill 是框裡的底色，band 是外框的顏色，motif 是四個角上的小圖案（a、b 兩種顏色）
private class ClassStyle(val fill: Color, val band: Color, val a: Color, val b: Color, val motif: List<String>)

private val Diamond = listOf("..a..", ".aba.", "abbba", ".aba.", "..a..")
private val Star = listOf("..a..", "..a..", "aaaaa", ".a.a.", "a...a")
private val Leaf = listOf("...aa", "..aba", ".aba.", "aba..", "a....")
private val Spike = listOf("a....", "aa...", "aba..", "abba.", "aaaaa")
private val Rivet = listOf(".....", ".aaa.", ".aba.", ".aaa.", ".....")
private val Paw = listOf("a.a.a", ".....", ".aaa.", "aaaaa", ".aaa.")
private val Flame = listOf("..a..", ".aa..", ".aba.", "abbba", ".aba.")
private val Horn = listOf("....a", "...aa", "..aba", ".abba", "abbba")
private val Bolt = listOf("aaaaa", "a.b.a", "abbba", "a.b.a", "aaaaa")

private val ClassStyles = mapOf(
    // 勇者：銀色鎧甲框、金色菱形
    Avatar.Hero to ClassStyle(Color(0xFFDCE6F4), Color(0xFF8C9AB4), Color(0xFFF8D030), Color(0xFFFFF6B0), Diamond),
    // 魔法師：深藍色星空框、金色星星
    Avatar.Mage to ClassStyle(Color(0xFFDCE0F8), Color(0xFF3A4AA8), Color(0xFFF8D030), Color(0xFFFFFFFF), Star),
    // 精靈弓手：森林綠框、嫩綠葉子
    Avatar.ElfArcher to ClassStyle(Color(0xFFDDF0CC), Color(0xFF3E7A36), Color(0xFFA8E070), Color(0xFF58B048), Leaf),
    // 哥布林：橄欖綠框、骨頭色尖刺
    Avatar.Goblin to ClassStyle(Color(0xFFE6EAC8), Color(0xFF6A7A2A), Color(0xFFF0E8C8), Color(0xFFB8A878), Spike),
    // 矮人：鐵灰色框、鉚釘
    Avatar.Dwarf to ClassStyle(Color(0xFFEADCC8), Color(0xFF5E5E68), Color(0xFFC8CCD8), Color(0xFF8A8E9A), Rivet),
    // 貓咪：粉紅色框、白色貓掌
    Avatar.Cat to ClassStyle(Color(0xFFFCE6EE), Color(0xFFD87898), Color(0xFFFFFFFF), Color(0xFFF8C8D8), Paw),
    // 龍：火紅色框、火焰
    Avatar.Dragon to ClassStyle(Color(0xFFFCE2CC), Color(0xFFB83820), Color(0xFFF8D030), Color(0xFFF08030), Flame),
    // 魔族：暗紅色框、骨白色的角
    Avatar.Demon to ClassStyle(Color(0xFFEEDCE2), Color(0xFF5A1A2A), Color(0xFFF0E0C8), Color(0xFFA07080), Horn),
    // 機器人：鋼藍色框、發光的螺絲
    Avatar.Robot to ClassStyle(Color(0xFFD8F0F0), Color(0xFF3E6070), Color(0xFF78E8F8), Color(0xFFD8FFFF), Bolt),
)

// 依職業畫外框；選中的外面再多一圈發亮的金框
fun Modifier.classFrame(avatar: Avatar, selected: Boolean): Modifier = drawBehind {
    val style = ClassStyles[avatar] ?: return@drawBehind
    val px = 2.dp.toPx()
    val w = size.width
    val h = size.height
    if (selected) {
        drawRect(Color(0xFFF8D030), Offset(-px, -px), Size(w + px * 2, h + px * 2))
        drawRect(Color(0xFFFFF6B0), Offset(-px * 0.5f, -px * 0.5f), Size(w + px, h + px))
    }
    // 缺角的黑框、職業顏色的寬框、內圈細黑線、底色
    drawRect(PixelInk, Offset(px, 0f), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(0f, px), Size(w, h - px * 2))
    val band = px * 4
    drawRect(style.band, Offset(px, px), Size(w - px * 2, h - px * 2))
    drawRect(PixelInk, Offset(band, band), Size(w - band * 2, h - band * 2))
    drawRect(style.fill, Offset(band + px * 0.5f, band + px * 0.5f), Size(w - band * 2 - px, h - band * 2 - px))
    // 四個角的小圖案（右邊和下面的鏡射過來）
    val cell = (band - px) / 5f
    for ((cx, cy, flipX, flipY) in listOf(
        Corner(px * 0.5f, px * 0.5f, false, false),
        Corner(w - px * 0.5f - cell * 5, px * 0.5f, true, false),
        Corner(px * 0.5f, h - px * 0.5f - cell * 5, false, true),
        Corner(w - px * 0.5f - cell * 5, h - px * 0.5f - cell * 5, true, true),
    )) {
        style.motif.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = when (c) {
                    'a' -> style.a
                    'b' -> style.b
                    else -> return@forEachIndexed
                }
                val dx = if (flipX) 4 - x else x
                val dy = if (flipY) 4 - y else y
                drawRect(color, Offset(cx + dx * cell, cy + dy * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

private data class Corner(val x: Float, val y: Float, val flipX: Boolean, val flipY: Boolean)

// ---------- 職業故事書卷 ----------

// 每個職業的小故事
val Avatar.story: String
    get() = when (this) {
        Avatar.Hero -> "出身王國邊境的年輕人，拿起祖傳的長劍，立誓打倒奪走文字的魔王，讓王國再次響起讀書聲。"
        Avatar.Mage -> "在古老塔樓裡研讀咒語的學徒。相信每個單字都是一道咒文，念得越準，魔法就越強。"
        Avatar.ElfArcher -> "守護森林的精靈射手，聽得見遠方的低語。用一支支箭，把躲在書頁裡的怪物射出來。"
        Avatar.Goblin -> "愛搗蛋又機靈的哥布林，偷來一本字典後竟然愛上學習，決定改邪歸正去冒險。"
        Avatar.Dwarf -> "住在山腳下的鐵匠戰士，意志比鐵還硬。把單字一個個敲進腦袋，像打造最堅固的盔甲。"
        Avatar.Cat -> "看起來懶洋洋的貓族，好奇心卻比誰都強。哪裡有新單字，就會踮著腳尖悄悄靠近。"
        Avatar.Dragon -> "從火山蛋裡孵出來的小龍，翅膀還不夠大，吐出的火焰卻能燒光一整頁的怪物。"
        Avatar.Demon -> "離開魔王軍的魔族劍士，為了贖罪踏上旅程，用黑暗的力量守護光明。"
        Avatar.Robot -> "在古代遺跡裡醒來的機械兵，記憶一片空白。每學會一個單字，系統就升級一次。"
    }

// 直的古書卷：上下兩端是捲起來的木軸，中間羊皮紙寫著職業名稱和故事
@Composable
fun StoryScroll(title: String, story: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .drawBehind { drawStoryScroll() }
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Text(title, color = PixelInk, style = MaterialTheme.typography.titleMedium)
        Text(story, color = PixelInk, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

private fun DrawScope.drawStoryScroll() {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    val roll = px * 3
    // 羊皮紙：左右邊緣有一點點不規則
    val random = java.util.Random(9)
    var y = roll
    while (y < h - roll) {
        val left = px * (1 + random.nextInt(2))
        val right = w - px * (1 + random.nextInt(2))
        drawRect(PixelInk, Offset(left - px * 0.7f, y), Size(right - left + px * 1.4f, px + 0.5f))
        drawRect(StoryPaper, Offset(left, y), Size(right - left, px + 0.5f))
        drawRect(StoryShade, Offset(right - px, y), Size(px, px + 0.5f))
        y += px
    }
    // 上下兩根捲軸
    for (top in listOf(0f, h - roll)) {
        drawRect(PixelInk, Offset(0f, top), Size(w, roll))
        drawRect(StoryRod, Offset(px * 0.7f, top + px * 0.7f), Size(w - px * 1.4f, roll - px * 1.4f))
        drawRect(StoryRodLight, Offset(px * 0.7f, top + px * 0.7f), Size(w - px * 1.4f, px * 0.6f))
    }
}

private val StoryPaper = Color(0xFFEFDDAE)
private val StoryShade = Color(0xFFCDB27A)
private val StoryRod = Color(0xFF7A4E28)
private val StoryRodLight = Color(0xFFB07A44)

// ---------- 開始冒險按鈕 ----------

// 深紅色的旗幟按鈕：金色鑲邊，左右各一把小劍；按下去會往下沉
@Composable
fun AdventureButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift = if (pressed) 0.dp else 4.dp
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind { drawAdventureButton(lift.toPx()) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color(0xFFFFF0C8),
            style = MaterialTheme.typography.headlineSmall.copy(
                shadow = Shadow(PixelInk, offset = Offset(2f, 2f), blurRadius = 0f),
            ),
            modifier = Modifier.offset(x = 2.dp - lift / 2, y = 2.dp - lift / 2),
        )
    }
}

private fun DrawScope.drawAdventureButton(up: Float) {
    val px = 3.dp.toPx()
    val w = size.width - px * 2
    val h = size.height - px * 2
    // 影子
    if (up > 0f) drawRect(Color(0xFF3A1810), Offset(px * 2, px * 2), Size(w, h))
    val left = px * 2 - up
    val top = px * 2 - up
    drawRect(PixelInk, Offset(left + px, top), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(left, top + px), Size(w, h - px * 2))
    // 金色鑲邊、深紅色布面
    drawRect(AdvGold, Offset(left + px, top + px), Size(w - px * 2, h - px * 2))
    drawRect(AdvGoldDark, Offset(left + px * 2, top + h - px * 2), Size(w - px * 3, px))
    drawRect(AdvRed, Offset(left + px * 2, top + px * 2), Size(w - px * 4, h - px * 4))
    drawRect(AdvRedLight, Offset(left + px * 2, top + px * 2), Size(w - px * 4, px))
    drawRect(AdvRedDark, Offset(left + px * 2, top + h - px * 3), Size(w - px * 4, px))
    // 左右兩把小劍（劍尖朝內）
    val mid = top + h / 2
    for ((start, dir) in listOf(left + px * 4 to 1f, left + w - px * 5 to -1f)) {
        // 握把和護手
        drawRect(AdvHilt, Offset(start, mid - px * 0.5f), Size(px, px))
        drawRect(AdvGold, Offset(start + dir * px, mid - px * 1.5f), Size(px, px * 3))
        // 劍刃
        for (k in 2..6) drawRect(AdvBlade, Offset(start + dir * k * px, mid - px * 0.5f), Size(px, px))
        drawRect(Color.White, Offset(start + dir * 2 * px, mid - px * 0.5f), Size(px * 0.5f, px * 0.5f))
    }
}

private val AdvGold = Color(0xFFE0B040)
private val AdvGoldDark = Color(0xFF9A7020)
private val AdvRed = Color(0xFFA8282E)
private val AdvRedLight = Color(0xFFD04848)
private val AdvRedDark = Color(0xFF6E161C)
private val AdvBlade = Color(0xFFD8E0F0)
private val AdvHilt = Color(0xFF805030)
