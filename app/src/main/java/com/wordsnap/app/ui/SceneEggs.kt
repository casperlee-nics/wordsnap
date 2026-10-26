package com.wordsnap.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

// 場景彩蛋：在戰場背景的某個地方連點幾下會發生事情（只有一般怪物的戰鬥有）
// 監獄鐵門點兩下：囚犯衝出來打怪物；船上的海面點兩下：海豚跳起來（只是好看）；
// 羅馬競技場的觀眾席點三下：觀眾丟石頭，人物和怪物都扣 1 格血；雪地中間點三下：堆出一個雪人，一直站到戰鬥結束
enum class SceneEgg(val taps: Int) { PrisonDoor(2), Dolphin(2), Stones(3), Snowman(3) }

val SceneEggScenes = setOf(ReviewScene.Prison, ReviewScene.Ship, ReviewScene.Colosseum, ReviewScene.Snow)

// 點到的格子（場景 64 x 28 格）是哪個彩蛋
fun sceneEggAt(scene: ReviewScene, x: Int, y: Int): SceneEgg? = when (scene) {
    ReviewScene.Prison -> SceneEgg.PrisonDoor.takeIf { x in PRISON_DOOR_LEFT..PRISON_DOOR_RIGHT && y in PRISON_DOOR_TOP..PRISON_DOOR_BOTTOM }
    ReviewScene.Ship -> SceneEgg.Dolphin.takeIf { y in 10..15 }
    ReviewScene.Colosseum -> SceneEgg.Stones.takeIf { y in 7..19 }
    ReviewScene.Snow -> SceneEgg.Snowman.takeIf { x in 16..47 && y >= 20 }
    else -> null
}

// 海面在背景的高度（比例）
const val DOLPHIN_SEA_Y = 12.5f / 28f

// 觀眾丟石頭的動畫長度、石頭砸到的時間（毫秒）
const val STONE_DURATION = 1800
const val STONE_HIT_AT = 1000

private val DolphinArt = listOf(
    "..........KK......",
    ".........KyyK.....",
    ".....KKKKyyyyKKK..",
    "KK..KyyyyyyyyyyyK.",
    "KyKKyyyyyyyyyyKyyK",
    ".KyyyyyyyyyyyyyyyK",
    "KyKKLLLLLLLLLLLKK.",
    "KK..KKKKKKKKKKK...",
)

// 海豚從點的地方跳出海面，畫一道弧線再鑽回水裡，前後濺起水花
fun DrawScope.drawDolphinJump(t: Float, x: Float, sea: Float) {
    val cell = 3.dp.toPx()
    val w = DolphinArt.first().length * cell
    val h = DolphinArt.size * cell
    val jump = cell * 22
    val c = Offset(x - cell * 14 + t * cell * 28, sea - sin(t * PI.toFloat()) * jump)
    // 往上跳時頭朝上，往下時頭朝下
    rotate(-60f + t * 120f, c) { drawSprite(DolphinArt, c.x - w / 2, c.y - h / 2, cell) }
    for ((at, sx) in listOf(0.05f to x - cell * 14, 0.92f to x + cell * 14)) {
        val p = ((t - at) / 0.25f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        for (k in 0 until 5) {
            val dx = (k - 2) * cell * 2 * (0.5f + p)
            val dy = sin(p * PI.toFloat()) * cell * (4 + (k % 2) * 3)
            drawRect(Color.White.copy(alpha = 1f - p), Offset(sx + dx, sea - dy), Size(cell, cell))
        }
    }
}

private val SnowmanArt = listOf(
    "....KKK....",
    "...KKKKK...",
    "..KKKKKKK..",
    "...WWWWW...",
    "..WKWWWKW..",
    "..WWWOOWW..",
    "...WWWWW...",
    "..RRRRRRR..",
    ".WWWWRWWWW.",
    "DWWWWKWWWWD",
    ".DWWWWWWWD.",
    ".WWWWKWWWW.",
    "WWWWWWWWWWW",
    "WWWWWKWWWWW",
    "WWWWWWWWWWW",
    ".WWWWWWWWW.",
)

// 雪人：站在點的地方，偶爾揮一下樹枝手，頭上飄雪
fun DrawScope.drawSnowman(x: Float, ground: Float, clock: Float) {
    val cell = 3.dp.toPx()
    val w = SnowmanArt.first().length * cell
    val h = SnowmanArt.size * cell
    val left = x - w / 2
    val top = ground - h - cell * 2
    drawRect(Color(0xFFB8B0A0).copy(alpha = 0.5f), Offset(left + cell, ground - cell * 2.5f), Size(w - cell * 2, cell))
    drawSprite(SnowmanArt, left, top, cell)
    if ((clock * 3).toInt() % 3 == 0) drawRect(Color(0xFF805030), Offset(left + w, top + cell * 7), Size(cell, cell * 2))
    for (k in 0 until 4) {
        val p = (clock + k / 4f) % 1f
        drawRect(Color.White.copy(alpha = 1f - p), Offset(left + k * cell * 3, top - cell * 8 + p * cell * 8), Size(cell, cell))
    }
}

// 觀眾丟石頭：一堆石頭從上面（觀眾席）丟下來，砸到人物和怪物，頭上冒星星
fun DrawScope.drawStoneRain(t: Float, hero: Offset, monster: Offset) {
    val cell = 3.dp.toPx()
    val hit = STONE_HIT_AT / STONE_DURATION.toFloat()
    for (k in 0 until 10) {
        val target = if (k % 2 == 0) hero else monster
        // 一顆接一顆丟，全部在 hit 的時候砸到
        val start = 0.025f * k
        val fall = ((t - start) / (hit - start)).coerceIn(0f, 1f)
        if (fall <= 0f || fall >= 1f) continue
        val sx = target.x + ((k * 37) % 9 - 4) * cell * 3
        val c = Offset(sx + (target.x - sx) * fall, -cell * 4 + fall * fall * (target.y - cell * 6 + cell * 4))
        rotate(fall * 360f, c) {
            drawRect(Color(0xFF202030), Offset(c.x - cell * 1.5f, c.y - cell * 1.5f), Size(cell * 3, cell * 3))
            drawRect(Color(0xFF8A8478), Offset(c.x - cell, c.y - cell), Size(cell * 2, cell * 2))
        }
    }
    if (t > hit) {
        val p = ((t - hit) / (1f - hit)).coerceIn(0f, 1f)
        for (target in listOf(hero, monster)) {
            for (k in 0 until 3) {
                val star = Offset(target.x + (k - 1) * cell * 6, target.y - cell * 14 - p * cell * 4)
                drawRect(Color(0xFFF8D030).copy(alpha = 1f - p), Offset(star.x - cell / 2, star.y - cell * 1.5f), Size(cell, cell * 3))
                drawRect(Color(0xFFF8D030).copy(alpha = 1f - p), Offset(star.x - cell * 1.5f, star.y - cell / 2), Size(cell * 3, cell))
            }
        }
    }
}
