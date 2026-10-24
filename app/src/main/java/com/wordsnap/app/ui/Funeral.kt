package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import kotlinx.coroutines.delay

// 被怪物打倒時的小動畫：有人拖著棺材，一路走到教堂門口
// 整個畫面是 64 x 30 格的像素，顏色用 ScenePalette

private const val COLUMNS = 64
private const val ROWS = 30
// 地面所在的那一列（人物和棺材的腳踩在這一列）
private const val GROUND = 23
private const val CHURCH_X = 44
// 走到教堂門口時，拖棺材的人左上角所在的格子
private const val STOP_X = 47
private const val START_X = -12

private val Church = listOf(
    "........Y........",
    ".......YYY.......",
    "........Y........",
    ".......KKK.......",
    "......KhhhK......",
    ".....KrrrrrK.....",
    "....KrrrrrrrK....",
    "...KrrrrrrrrrK...",
    "..KrrrrrrrrrrrK..",
    ".KrrrrrrrrrrrrrK.",
    "KKKKKKKKKKKKKKKKK",
    ".KhhhhhhhhhhhhhK.",
    ".KhLLhhhhhhhLLhK.",
    ".KhLLhhKKKhhLLhK.",
    ".KhhhhKDDDKhhhhK.",
    ".KhhhhKDDDKhhhhK.",
    ".KhhhhKDDYKhhhhK.",
    ".KhhhhKDDDKhhhhK.",
    ".KssssKDDDKssssK.",
    "KKKKKKKKKKKKKKKKK",
)

// 晚上的教堂：窗戶和門口透出黃色燈光
private val ChurchNight = Church.map { row -> row.replace('L', 'Y') }

// 晚上的月亮和星星
private val Moon = listOf(
    "..nnn..",
    ".nnnnn.",
    "nnnnnnn",
    "nnnnnnn",
    "nnnnnnn",
    ".nnnnn.",
    "..nnn..",
)
private val NightStars = listOf(3 to 1, 20 to 3, 27 to 1, 33 to 6, 38 to 2, 52 to 1, 60 to 4, 17 to 9, 24 to 11, 58 to 9)

private val Coffin = listOf(
    ".KKKKKKKK.",
    "KDDDYDDDDK",
    "KDDYYYDDDK",
    "KDDDYDDDDK",
    ".KKKKKKKK.",
)

// 拖棺材的人（面向右邊，左手拉著繩子），兩種腳步輪流
private val WalkerBody = listOf(
    "..KKKK..",
    ".KeeeeK.",
    ".KeeSSK.",
    ".KSSKSK.",
    "..KSSSK.",
    ".KppppK.",
    "KSppppK.",
    ".KppppK.",
    ".KppppK.",
    "..KDDK..",
)
private val WalkerStepA = listOf(
    "..K..K..",
    ".KK..KK.",
)
private val WalkerStepB = listOf(
    "...KK...",
    "..KKK...",
)

private val Rope = Color(0xFF986030)

@Composable
fun FuneralScene(modifier: Modifier = Modifier, onEnd: () -> Unit) {
    val endCallback by rememberUpdatedState(onEnd)
    val walk = remember { Animatable(START_X.toFloat()) }
    // 依手機時間：白天是藍天草地，晚上是星空、月亮和亮著燈的教堂
    val night = remember { isNight() }
    LaunchedEffect(Unit) {
        delay(400)
        walk.animateTo(STOP_X.toFloat(), tween(5000, easing = LinearEasing))
        delay(1200)
        endCallback()
    }
    Canvas(modifier.fillMaxWidth().aspectRatio(COLUMNS.toFloat() / ROWS)) {
        val cell = size.width / COLUMNS
        // 天空和草地
        for (y in 0 until ROWS) {
            val key = when {
                y < 12 -> if (night) 'x' else 'a'
                y <= GROUND -> if (night) 'X' else 'b'
                y == GROUND + 1 -> if (night) 'i' else 'g'
                else -> if (night) 'j' else 'G'
            }
            drawRect(ScenePalette.getValue(key), Offset(0f, y * cell), Size(size.width, cell + 0.5f))
        }
        if (night) {
            NightStars.forEach { (sx, sy) -> drawRect(Color.White, Offset(sx * cell, sy * cell), Size(cell, cell)) }
            drawArt(Moon, 8, 2, cell)
        }
        drawArt(if (night) ChurchNight else Church, CHURCH_X, GROUND - Church.size + 1, cell)

        // 一格一格往前走，看起來像像素遊戲
        val x = walk.value.toInt()
        val stepB = x % 2 != 0 && x < STOP_X
        val walkerTop = GROUND - (WalkerBody.size + 2) + 1 + if (stepB) -1 else 0
        val coffinX = x - Coffin.first().length - 3
        val coffinTop = GROUND - Coffin.size + 1
        // 繩子：從棺材前端拉到人的左手
        drawLine(
            Rope,
            Offset((coffinX + Coffin.first().length) * cell, (coffinTop + 2.5f) * cell),
            Offset(x * cell, (walkerTop + 6.5f) * cell),
            strokeWidth = cell * 0.6f,
        )
        drawArt(Coffin, coffinX, coffinTop, cell)
        drawArt(WalkerBody, x, walkerTop, cell)
        drawArt(if (stepB) WalkerStepB else WalkerStepA, x, walkerTop + WalkerBody.size, cell)
    }
}

// 在第 (left, top) 格畫一張像素圖
private fun DrawScope.drawArt(art: List<String>, left: Int, top: Int, cell: Float) {
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            val color = ScenePalette[ch] ?: return@forEachIndexed
            drawRect(color, Offset((left + x) * cell, (top + y) * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
