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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk

// 建立角色畫面用的元件：木牌標題、魔法書、職業故事書卷、「開始冒險」按鈕

// ---------- 木牌標題 ----------

// 不規則的木牌：邊緣一格一格高低不平、兩片木板拼成，釘著兩顆釘子，左上和右下纏著藤蔓
@Composable
fun WoodSignTitle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            // 日文名稱「たんご勇者」有 5 個字，木牌跟著加寬，字不用縮小
            .width(if (text.length > 4) 212.dp else 176.dp)
            .height(60.dp)
            .drawBehind { drawWoodSign() },
        contentAlignment = Alignment.Center,
    ) {
        FitText(
            text,
            color = SignText,
            style = MaterialTheme.typography.headlineMedium.copy(
                shadow = Shadow(PixelInk, offset = Offset(3f, 3f), blurRadius = 0f),
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun DrawScope.drawWoodSign() {
    val px = 2.dp.toPx()
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

// ---------- 職業故事書卷 ----------

// 每個職業的小故事
val Avatar.story: String
    get() = when (this) {
        Avatar.Hero -> tr(R.string.story_hero)
        Avatar.Mage -> tr(R.string.story_mage)
        Avatar.ElfArcher -> tr(R.string.story_archer)
        Avatar.Goblin -> tr(R.string.story_goblin)
        Avatar.Dwarf -> tr(R.string.story_dwarf)
        Avatar.Cat -> tr(R.string.story_cat)
        Avatar.Dragon -> tr(R.string.story_dragon)
        Avatar.Demon -> tr(R.string.story_demon)
        Avatar.Robot -> tr(R.string.story_robot)
    }

// 直的古書卷：上下兩端是捲起來的木軸，中間羊皮紙寫著職業名稱和故事
@Composable
fun StoryScroll(title: String, story: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .drawBehind { drawStoryScroll() }
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Text(title, color = PixelInk, style = MaterialTheme.typography.labelLarge)
        Text(story, color = PixelInk, style = MaterialTheme.typography.bodySmall.readable(), modifier = Modifier.padding(top = 4.dp))
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

// 鐵框木門牌按鈕：深色木板、四角包著鉚釘鐵片，左邊插著一面小旗子；按下去會往下沉
@Composable
fun AdventureButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift = if (pressed) 0.dp else 3.dp
    Box(
        modifier
            .width(220.dp)
            .height(54.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind { drawAdventureButton(lift.toPx()) },
        contentAlignment = Alignment.Center,
    ) {
        FitText(
            label,
            color = Color(0xFFFFF0C8),
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = Shadow(PixelInk, offset = Offset(2f, 2f), blurRadius = 0f),
            ),
            // 左邊留給小旗子
            modifier = Modifier.offset(x = 10.dp + 1.5.dp - lift / 2, y = 1.5.dp - lift / 2).padding(start = 30.dp, end = 20.dp),
        )
    }
}

private fun DrawScope.drawAdventureButton(up: Float) {
    val px = 3.dp.toPx()
    val w = size.width - px
    val h = size.height - px
    // 影子
    if (up > 0f) drawRect(AdvShadow, Offset(px, px), Size(w, h))
    val left = px - up
    val top = px - up
    // 缺角黑框、木板
    drawRect(PixelInk, Offset(left + px, top), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(left, top + px), Size(w, h - px * 2))
    drawRect(AdvWood, Offset(left + px, top + px), Size(w - px * 2, h - px * 2))
    drawRect(AdvWoodLight, Offset(left + px, top + px), Size(w - px * 2, px))
    drawRect(AdvWoodDark, Offset(left + px, top + h - px * 2), Size(w - px * 2, px))
    // 直的木板縫和木紋
    var x = left + px * 9
    while (x < left + w - px * 4) {
        drawRect(AdvWoodDark, Offset(x, top + px * 2), Size(px * 0.5f, h - px * 4))
        drawRect(AdvGrain, Offset(x + px * 2, top + h * 0.35f), Size(px * 2.5f, px * 0.5f))
        drawRect(AdvGrain, Offset(x + px * 4, top + h * 0.65f), Size(px * 2f, px * 0.5f))
        x += px * 8
    }
    // 四角的鐵片和鉚釘
    for ((cx, cy) in listOf(
        left + px to top + px,
        left + w - px * 4 to top + px,
        left + px to top + h - px * 4,
        left + w - px * 4 to top + h - px * 4,
    )) {
        drawRect(AdvIron, Offset(cx, cy), Size(px * 3, px * 3))
        drawRect(AdvRivet, Offset(cx + px, cy + px), Size(px, px))
    }
    // 左邊的小旗子
    val fx = left + px * 6
    val fy = top + h / 2 - px * 3.5f
    drawRect(PixelInk, Offset(fx - px * 0.3f, fy - px * 0.3f), Size(px * 1.6f, px * 7.6f))
    drawRect(AdvPole, Offset(fx, fy), Size(px, px * 7))
    drawRect(PixelInk, Offset(fx + px, fy - px * 0.3f), Size(px * 4.3f, px * 3.6f))
    drawRect(AdvFlag, Offset(fx + px, fy), Size(px * 4, px * 3))
    drawRect(AdvFlagLight, Offset(fx + px, fy), Size(px * 4, px))
}

private val AdvShadow = Color(0xFF2A180A)
private val AdvWood = Color(0xFF7A4E28)
private val AdvWoodLight = Color(0xFFA87440)
private val AdvWoodDark = Color(0xFF4E3018)
private val AdvGrain = Color(0xFF5E3C1C)
private val AdvIron = Color(0xFF505868)
private val AdvRivet = Color(0xFFC8CCD8)
private val AdvPole = Color(0xFFE0B040)
private val AdvFlag = Color(0xFFC83040)
private val AdvFlagLight = Color(0xFFF06060)

// ---------- 選職業頁上方的名字 ----------

// 藍色的緞帶橫幅：中間寫「○○，選擇你的職業」，兩端是剪成燕尾的緞帶尾巴
@Composable
fun NameRibbon(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawBehind { drawRibbon() }
            // 字放在緞帶藍色的那一段正中間再高一點，不會碰到下緣
            .padding(start = 34.dp, end = 34.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                color = RibbonName,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(PixelInk, offset = Offset(2f, 2f), blurRadius = 0f),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(tr(R.string.class_pick_suffix), color = RibbonText, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}

private fun DrawScope.drawRibbon() {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    val tail = px * 8
    // 兩端的尾巴：比中間低一點、顏色深一點，末端剪成燕尾
    val y0 = px * 3
    val th = h - px * 3
    for (flip in listOf(false, true)) {
        fun xs(x: Float) = if (flip) w - x else x
        fun tailPath(inset: Float) = Path().apply {
            moveTo(xs(inset), y0 + inset)
            lineTo(xs(tail), y0 + inset)
            lineTo(xs(tail), y0 + th - inset)
            lineTo(xs(inset), y0 + th - inset)
            lineTo(xs(px * 3 + inset), y0 + th / 2)
            close()
        }
        drawPath(tailPath(0f), PixelInk)
        drawPath(tailPath(px * 0.8f), RibbonDark)
    }
    // 中間的緞帶：金色細邊
    val left = tail - px * 2
    val right = w - tail + px * 2
    drawRect(PixelInk, Offset(left, 0f), Size(right - left, h - px * 3))
    drawRect(RibbonGold, Offset(left + px * 0.7f, px * 0.7f), Size(right - left - px * 1.4f, h - px * 4.4f))
    drawRect(RibbonBlue, Offset(left + px * 1.4f, px * 1.4f), Size(right - left - px * 2.8f, h - px * 5.8f))
    drawRect(RibbonLight, Offset(left + px * 1.4f, px * 1.4f), Size(right - left - px * 2.8f, px))
}

private val RibbonBlue = Color(0xFF2E4A8A)
private val RibbonLight = Color(0xFF4A6AB0)
private val RibbonDark = Color(0xFF1E3260)
private val RibbonGold = Color(0xFFE0B040)
private val RibbonName = Color(0xFFFFE070)
private val RibbonText = Color(0xFFF8F0D8)

// ---------- 主畫面的木板按鈕 ----------

// 淺色木板按鈕（和「確定」印章同一種木頭）：有木紋、兩端各一顆小釘子，左上亮右下暗、底下一點影子，按下去會往下沉
@Composable
fun PlankButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift = if (pressed || !enabled) 0.dp else 3.dp
    Box(
        modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind { drawPlank(lift.toPx(), label.length) },
        contentAlignment = Alignment.Center,
    ) {
        FitText(
            label,
            color = PlankText,
            style = MaterialTheme.typography.titleMedium,
            // 兩端留給釘子
            modifier = Modifier.offset(x = 1.5.dp - lift / 2, y = 1.5.dp - lift / 2).padding(horizontal = 18.dp),
        )
    }
}

private fun DrawScope.drawPlank(up: Float, seed: Int) {
    val px = 3.dp.toPx()
    val w = size.width - px
    val h = size.height - px
    if (up > 0f) drawRect(PlankShadow, Offset(px, px), Size(w, h))
    val left = px - up
    val top = px - up
    drawRect(PixelInk, Offset(left + px, top), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(left, top + px), Size(w, h - px * 2))
    drawRect(PlankWood, Offset(left + px, top + px), Size(w - px * 2, h - px * 2))
    drawRect(PlankLight, Offset(left + px, top + px), Size(w - px * 2, px))
    drawRect(PlankLight, Offset(left + px, top + px), Size(px, h - px * 2))
    drawRect(PlankDark, Offset(left + px, top + h - px * 2), Size(w - px * 2, px))
    drawRect(PlankDark, Offset(left + w - px * 2, top + px), Size(px, h - px * 2))
    // 木紋：每顆按鈕的紋路不太一樣
    val random = java.util.Random(seed * 31L + 7)
    var x = left + px * 6
    while (x < left + w - px * 10) {
        val y = top + px * (2 + random.nextInt(((h / px).toInt() - 4).coerceAtLeast(1)))
        val len = px * (2 + random.nextInt(4))
        drawRect(PlankGrain, Offset(x, y), Size(len, px * 0.5f))
        x += len + px * (2 + random.nextInt(4))
    }
    // 兩端的小釘子
    for (nx in listOf(left + px * 3, left + w - px * 4.5f)) {
        drawRect(PlankNail, Offset(nx, top + h / 2 - px * 0.75f), Size(px * 1.5f, px * 1.5f))
    }
}

private val PlankWood = Color(0xFFC89A62)
private val PlankLight = Color(0xFFE8C894)
private val PlankDark = Color(0xFF8C6238)
private val PlankGrain = Color(0xFFA87A48)
private val PlankNail = Color(0xFF5A3A1C)
private val PlankShadow = Color(0xFF3A2410)
private val PlankText = Color(0xFF3A2410)
