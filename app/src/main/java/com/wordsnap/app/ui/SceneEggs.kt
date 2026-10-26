package com.wordsnap.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

// 場景彩蛋：在戰場背景的某個地方連點幾下會發生事情（只有一般怪物的戰鬥有）
// 監獄鐵門點兩下：囚犯衝出來打怪物；船上的海面點兩下：海豚跳起來（只是好看）；
// 雪地中間點三下：堆出一個雪人，一直站到戰鬥結束
// 月球的星球點三下：飛碟飛出來往左飛走（只是好看）；墓園中間的墓碑點三下：殭屍爬出來，隨機打人物或怪物 1 格血再爬回去
enum class SceneEgg(val taps: Int) { PrisonDoor(2), Dolphin(2), Snowman(3), Ufo(3), Zombie(3) }

val SceneEggScenes = setOf(ReviewScene.Prison, ReviewScene.Ship, ReviewScene.Snow, ReviewScene.Moon, ReviewScene.Graveyard)

// 點到的格子（場景 64 x 28 格）是哪個彩蛋
fun sceneEggAt(scene: ReviewScene, x: Int, y: Int): SceneEgg? = when (scene) {
    ReviewScene.Prison -> SceneEgg.PrisonDoor.takeIf { x in PRISON_DOOR_LEFT..PRISON_DOOR_RIGHT && y in PRISON_DOOR_TOP..PRISON_DOOR_BOTTOM }
    ReviewScene.Ship -> SceneEgg.Dolphin.takeIf { y in 10..15 }
    ReviewScene.Snow -> SceneEgg.Snowman.takeIf { x in 16..47 && y >= 20 }
    ReviewScene.Moon -> SceneEgg.Ufo.takeIf { x in UFO_PLANET_X - 7..UFO_PLANET_X + 7 && y <= 11 }
    ReviewScene.Graveyard -> SceneEgg.Zombie.takeIf { x in GRAVE_X - 3..GRAVE_X + 3 && y in 14..24 }
    else -> null
}

// 海面在背景的高度（比例）
const val DOLPHIN_SEA_Y = 12.5f / 28f

// 月球的星球、墓園中間的墓碑在場景的哪一格
const val UFO_PLANET_X = 51
const val UFO_PLANET_Y = 5
const val GRAVE_X = 30

// 飛碟、殭屍的動畫長度；殭屍打到人的時間（毫秒）
const val UFO_DURATION = 2600
const val ZOMBIE_DURATION = 4200
const val ZOMBIE_HIT_AT = 2100

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

private val UfoArt = listOf(
    "....KKKKK....",
    "...KLLLWLK...",
    "..KLLLLLLLK..",
    ".KKKKKKKKKKK.",
    "KMMMMMMMMMMMK",
    ".KKmmmmmmmKK.",
    "...KKKKKKK...",
)

// 飛碟：從星球後面冒出來，晃一晃、燈一閃一閃，然後越飛越快往左邊飛出畫面
fun DrawScope.drawUfo(t: Float, planet: Offset) {
    val cell = 3.dp.toPx()
    val w = UfoArt.first().length * cell
    val h = UfoArt.size * cell
    val rise = (t / 0.25f).coerceIn(0f, 1f)
    val go = ((t - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val wobble = sin(t * 40f) * cell * (1f - go)
    val c = Offset(planet.x - go * go * (planet.x + w * 2), planet.y - rise * cell * 4 + wobble - go * cell * 3)
    val tilt = if (go > 0f) -12f else sin(t * 20f) * 6f
    rotate(tilt, c) {
        drawSprite(UfoArt, c.x - w / 2, c.y - h / 2, cell, alpha = 0.3f + 0.7f * rise)
        // 底下一排燈輪流亮
        for (k in 0 until 4) {
            if (((t * 16).toInt() + k) % 2 == 0) {
                drawRect(Color(0xFFF8D030), Offset(c.x - w / 2 + cell * (2 + k * 3), c.y + cell * 0.5f), Size(cell, cell))
            }
        }
    }
    // 飛走時後面拖一條光
    if (go > 0f) {
        for (j in 1..4) drawRect(Color(0xFF90D0F8).copy(alpha = 0.5f - j * 0.1f), Offset(c.x + w / 2 + j * cell * 3, c.y - cell / 2), Size(cell * 2, cell))
    }
}

// 殭屍：綠皮膚、紅眼睛、破破的藍衣服，兩手往前伸（圖是面向右邊）
private val ZombieArt = listOf(
    "...KKKKK.....",
    "..KgggggK....",
    "..KgRgRgK....",
    "..KgggggK....",
    "..KgKKKgK....",
    "...KgggK.....",
    "..KvvvvvKKKKK",
    ".KvuvvvuvgggK",
    ".KvvuvvvKKKKK",
    ".KgvvvuvK....",
    "..KvvvvvK....",
    "..KuuKuuK....",
    "..KuK.KuK....",
    "..KgK.KgK....",
    "..KKK.KKK....",
)

// 殭屍從墓碑前的土裡爬出來，一拐一拐走到人物或怪物旁邊抓一下，再走回去鑽回土裡
fun DrawScope.drawZombie(t: Float, graveX: Float, ground: Float, target: Offset) {
    val cell = 3.dp.toPx()
    val w = ZombieArt.first().length * cell
    val h = ZombieArt.size * cell
    val hit = ZOMBIE_HIT_AT / ZOMBIE_DURATION.toFloat()
    // 走到目標前面一點點停下
    val toRight = target.x > graveX
    val stop = target.x + (if (toRight) -1 else 1) * cell * 12
    val out = ((t - 0.15f) / (hit - 0.2f)).coerceIn(0f, 1f)
    val back = ((t - hit - 0.08f) / 0.27f).coerceIn(0f, 1f)
    val lunge = if (t in hit - 0.05f..hit + 0.05f) (if (toRight) cell * 3 else -cell * 3) else 0f
    val x = graveX + (stop - graveX) * (out - back) + lunge
    // 爬出來／鑽回去：只畫地面以上的部分
    val rise = (t / 0.15f).coerceIn(0f, 1f) * (1f - ((t - 0.85f) / 0.15f).coerceIn(0f, 1f))
    val walking = out in 0.01f..0.99f || back in 0.01f..0.99f
    val bob = if (walking && (t * 20).toInt() % 2 == 0) cell else 0f
    val top = ground - h * rise + bob
    // 往回走時面向反方向
    val facingRight = if (back > 0f) !toRight else toRight
    clipRect(bottom = ground) {
        scale(if (facingRight) 1f else -1f, 1f, Offset(x, top + h / 2)) {
            drawSprite(ZombieArt, x - w / 2, top, cell)
        }
    }
    // 爬出來、鑽回去時土塊噴起來
    if (rise in 0.01f..0.99f) {
        for (k in 0 until 5) {
            val dx = (k - 2) * cell * 2.5f
            val dy = cell * (2 + (k * 7 + (t * 30).toInt()) % 4)
            drawRect(Color(0xFF805030), Offset(x + dx, ground - dy), Size(cell, cell))
        }
    }
    // 抓一下：目標身上三道爪痕
    if (t in hit..hit + 0.12f) {
        val p = (t - hit) / 0.12f
        for (k in 0 until 3) {
            val sx = target.x + (k - 1) * cell * 3
            drawLine(Color(0xFFE04030).copy(alpha = 1f - p), Offset(sx - cell * 3, target.y - cell * 6), Offset(sx + cell * 3, target.y + cell * 4), cell)
        }
    }
}

