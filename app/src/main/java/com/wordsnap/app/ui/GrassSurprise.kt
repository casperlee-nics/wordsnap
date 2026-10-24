package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.floor
import kotlin.random.Random

// 主畫面草地的小彩蛋（點下方草地三次）：
// 白天：草地一朵一朵長出花花草草；晚上：草叢裡冒出一堆眼睛發光的怪物，旁邊黑暗中也有眼睛在眨
private const val GARDEN_MS = 7000
private const val LURKERS_MS = 7000

// 草地圖寬 64 格，草的最上面一排是第 0 排，泥土從第 3 排開始
private const val GROUND_COLUMNS = 64
private const val GRASS_LINE = 3

@Composable
fun GrassSurprise(night: Boolean, show: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val clock = remember(show) { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(show) {
        clock.animateTo(1f, tween(if (night) LURKERS_MS else GARDEN_MS, easing = LinearEasing))
        done()
    }
    val flowers = remember(show) { plantFlowers() }
    val lurkers = remember(show) { gatherLurkers() }
    val eyes = remember(show) { darkEyes() }
    Canvas(modifier) {
        val cell = size.width / GROUND_COLUMNS
        val t = clock.value
        if (night) drawLurkers(lurkers, eyes, t, cell) else drawGarden(flowers, t, cell)
    }
}

// ---------- 白天：花花草草 ----------

private class Flower(val x: Int, val art: List<String>, val colors: Map<Char, Color>, val start: Float)

private fun plantFlowers(): List<Flower> {
    val random = Random(System.nanoTime())
    // 每 3～4 格種一株，順序打亂，一株接一株冒出來
    val spots = generateSequence(1) { it + 3 + random.nextInt(2) }.takeWhile { it < GROUND_COLUMNS - 5 }.toList()
    return spots.shuffled(random).mapIndexed { i, x ->
        val (art, colors) = PlantKinds.random(random)
        Flower(x, art, colors, start = i * 0.012f)
    }
}

private fun DrawScope.drawGarden(flowers: List<Flower>, t: Float, cell: Float) {
    // 最後 1 秒慢慢縮回草裡
    val fade = 1f - ((t - 0.86f) / 0.14f).coerceIn(0f, 1f)
    flowers.forEach { flower ->
        val grow = ((t - flower.start) / 0.12f).coerceIn(0f, 1f)
        if (grow <= 0f) return@forEach
        val rows = flower.art.size
        // 一格一格長高（像素遊戲的感覺），長完後輕輕左右晃
        val shown = (floor(grow * rows).toInt().coerceAtLeast(1) * fade).toInt()
        if (shown <= 0) return@forEach
        val sway = if (grow >= 1f && ((t * 6 + flower.x).toInt() % 2 == 0)) cell * 0.5f else 0f
        val top = (GRASS_LINE - shown) * cell
        clipRect(left = -size.width, top = -size.height * 4, right = size.width * 2, bottom = GRASS_LINE * cell) {
            drawArt(flower.art, flower.colors, flower.x * cell + sway, top, cell, fromRow = 0, rows = shown)
        }
    }
}

private val Stem = Color(0xFF2E8B3A)
private val Leaf = Color(0xFF58C048)

private val Daisy = listOf(
    ".W.W.",
    "WWYWW",
    ".WWW.",
    "..s..",
    ".ls..",
    "..sl.",
    "..s..",
) to mapOf('W' to Color(0xFFFFFFFF), 'Y' to Color(0xFFF8D030), 's' to Stem, 'l' to Leaf)

private val Tulip = listOf(
    ".R.R.",
    ".RRR.",
    ".RRR.",
    "..s..",
    "l.s.l",
    ".lsl.",
    "..s..",
) to mapOf('R' to Color(0xFFE84060), 's' to Stem, 'l' to Leaf)

private val Bluebell = listOf(
    "..B..",
    ".BbB.",
    ".BBB.",
    "..s.B",
    ".ls.b",
    "..sss",
) to mapOf('B' to Color(0xFF5878E8), 'b' to Color(0xFF98B8F8), 's' to Stem, 'l' to Leaf)

private val Sunflower = listOf(
    ".YYY.",
    "YOOOY",
    "YOOOY",
    ".YYY.",
    "..s..",
    "lls..",
    "..sll",
    "..s..",
) to mapOf('Y' to Color(0xFFF8D030), 'O' to Color(0xFF905020), 's' to Stem, 'l' to Leaf)

private val Clover = listOf(
    ".l.l.",
    "lllll",
    ".lPl.",
    "..s..",
) to mapOf('l' to Leaf, 'P' to Color(0xFFF8A0C0), 's' to Stem)

private val TallGrass = listOf(
    "l...l",
    "l.l.l",
    ".lll.",
    ".lsl.",
    "..s..",
) to mapOf('l' to Leaf, 's' to Stem)

private val Mushroom = listOf(
    ".RRR.",
    "RWRWR",
    "..h..",
    "..h..",
) to mapOf('R' to Color(0xFFE04030), 'W' to Color(0xFFFFFFFF), 'h' to Color(0xFFF0E0C0))

private val PlantKinds = listOf(Daisy, Tulip, Bluebell, Sunflower, Clover, TallGrass, Mushroom, Tulip, Daisy)

// ---------- 晚上：眼睛發光的怪物 ----------

private class Lurker(val x: Int, val art: List<String>, val body: Color, val eye: Color, val start: Float, val blink: Int)

private fun gatherLurkers(): List<Lurker> {
    val random = Random(System.nanoTime())
    val spots = generateSequence(0) { it + 6 + random.nextInt(3) }.takeWhile { it < GROUND_COLUMNS - 8 }.toList()
    return spots.shuffled(random).mapIndexed { i, x ->
        val (art, body) = LurkerKinds[i % LurkerKinds.size]
        Lurker(x, art, body, EyeColors.random(random), start = i * 0.035f, blink = random.nextInt(7))
    }
}

// 左右兩邊（選單擋不到的地方）黑暗中一對一對的眼睛
private class EyePair(val x: Float, val y: Float, val color: Color, val start: Float, val blink: Int)

private fun darkEyes(): List<EyePair> {
    val random = Random(System.nanoTime())
    return List(10) { i ->
        val left = i % 2 == 0
        val x = if (left) 1f + random.nextFloat() * 9f else 52f + random.nextFloat() * 9f
        val y = -4f - random.nextFloat() * 22f
        EyePair(x, y, EyeColors.random(random), start = 0.2f + random.nextFloat() * 0.3f, blink = random.nextInt(7))
    }
}

private fun DrawScope.drawLurkers(lurkers: List<Lurker>, eyes: List<EyePair>, t: Float, cell: Float) {
    // 最後 0.8 秒全部縮回草叢
    val leave = ((t - 0.88f) / 0.12f).coerceIn(0f, 1f)
    val glow = if ((t * 14).toInt() % 2 == 0) 0.55f else 0.35f
    lurkers.forEach { m ->
        val rise = ((t - m.start) / 0.08f).coerceIn(0f, 1f) * (1f - leave)
        if (rise <= 0f) return@forEach
        val rows = m.art.size
        // 一格一格從草裡冒出來，冒出來後上下抖動
        val shown = floor(rise * rows).toInt()
        if (shown <= 0) return@forEach
        val bob = if (rise >= 1f && ((t * 5 + m.x).toInt() % 2 == 0)) cell else 0f
        val top = (GRASS_LINE - shown) * cell - bob
        val blinking = rise >= 1f && ((t * 9).toInt() + m.blink) % 9 == 0
        clipRect(left = -size.width, top = -size.height * 4, right = size.width * 2, bottom = GRASS_LINE * cell) {
            m.art.forEachIndexed { y, row ->
                row.forEachIndexed { x, ch ->
                    val color = when (ch) {
                        'K' -> Color(0xFF101018)
                        'B' -> m.body
                        'E' -> if (blinking) m.body else m.eye
                        'T' -> Color(0xFFF0F0E0)
                        else -> return@forEachIndexed
                    }
                    drawRect(color, Offset(m.x * cell + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
                    if (ch == 'E' && !blinking) {
                        drawCircle(m.eye.copy(alpha = glow * 0.5f), cell * 1.6f, Offset(m.x * cell + (x + 0.5f) * cell, top + (y + 0.5f) * cell))
                    }
                }
            }
        }
    }
    eyes.forEach { e ->
        val on = ((t - e.start) / 0.05f).coerceIn(0f, 1f) * (1f - leave)
        if (on <= 0f || ((t * 9).toInt() + e.blink) % 8 == 0) return@forEach
        listOf(0f, 2f).forEach { dx ->
            val center = Offset((e.x + dx + 0.5f) * cell, (e.y + 0.5f) * cell)
            drawCircle(e.color.copy(alpha = glow * 0.5f * on), cell * 1.4f, center)
            drawRect(e.color.copy(alpha = on), Offset((e.x + dx) * cell, e.y * cell), Size(cell, cell))
        }
    }
}

private val EyeColors = listOf(
    Color(0xFFF8F040), Color(0xFFF84040), Color(0xFF58F8F0), Color(0xFF80FF60), Color(0xFFF870F8),
)

// 不同種類的怪物：史萊姆、蝙蝠、幽靈、小惡魔、獨眼怪、骷髏、狼、蘑菇怪
private val Slime = listOf(
    "...KK...",
    "..KBBK..",
    ".KBBBBK.",
    ".KEBBEK.",
    "KBBBBBBK",
    "KBBBBBBK",
) to Color(0xFF3A8040)

private val Bat = listOf(
    "K......K",
    "KK.KK.KK",
    "KBKBBKBK",
    "KBBEEBBK",
    ".KBBBBK.",
    "..KTTK..",
    "...KK...",
) to Color(0xFF503070)

private val Ghost = listOf(
    "..KKKK..",
    ".KBBBBK.",
    "KBEBBEBK",
    "KBBBBBBK",
    "KBBKKBBK",
    "KBBBBBBK",
    "KBBBBBBK",
) to Color(0xFF7888A8)

private val Imp = listOf(
    "K......K",
    "BK....KB",
    ".BKKKKB.",
    ".KBBBBK.",
    "KBEBBEBK",
    "KBBTTBBK",
    ".KBBBBK.",
) to Color(0xFF902828)

private val Cyclops = listOf(
    "..KKKK..",
    ".KBBBBK.",
    "KBBEEBBK",
    "KBBEEBBK",
    "KBBBBBBK",
    "KBTKTKBK",
    "KBBBBBBK",
) to Color(0xFF406060)

private val Skull = listOf(
    ".KKKKKK.",
    "KTTTTTTK",
    "KTKETKET",
    "KTTTTTTK",
    ".KTKTKT.",
    ".KTTTTK.",
) to Color(0xFFD0D0C0)

private val Wolf = listOf(
    "K.....K.",
    "BK...KB.",
    "KBKKKBBK",
    "KBEBBEBK",
    "KBBBBBBK",
    ".KBTTBK.",
    "..KBBK..",
) to Color(0xFF505058)

private val Shroom = listOf(
    "..KKKK..",
    ".KBTBBK.",
    "KBBBBTBK",
    "KKKKKKKK",
    ".KTEETK.",
    ".KTTTTK.",
) to Color(0xFF702858)

private val LurkerKinds = listOf(Slime, Bat, Ghost, Imp, Cyclops, Skull, Wolf, Shroom)

private fun DrawScope.drawArt(art: List<String>, colors: Map<Char, Color>, left: Float, top: Float, cell: Float, fromRow: Int, rows: Int) {
    for (y in fromRow until (fromRow + rows).coerceAtMost(art.size)) {
        art[y].forEachIndexed { x, ch ->
            val color = colors[ch] ?: return@forEachIndexed
            drawRect(color, Offset(left + x * cell, top + (y - fromRow) * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
