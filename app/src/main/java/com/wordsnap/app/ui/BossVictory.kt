package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 大魔王討伐成功：整個畫面換成夜空，魔王一格一格碎掉往下掉，天上放煙火，
// 人物站在中間跳起來舉手歡呼，金色字「大魔王討伐成功！」閃閃發亮；播專屬的勝利凱歌
@Composable
fun BossVictoryScreen(monster: Monster, hero: Avatar, tier: Int, musicOn: Boolean, onClose: () -> Unit) {
    LoopingMusic(R.raw.bigboss_victory, enabled = musicOn, volume = 0.4f)
    // 前 3 秒：魔王碎掉；之後一直放煙火
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(3000, easing = LinearEasing)) }
    val anim = rememberInfiniteTransition(label = "bossWin")
    val clock by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "clock",
    )
    val hop by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "hop",
    )
    val t = intro.value

    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(VictoryNight)) {
            Canvas(Modifier.fillMaxSize()) {
                drawNightSky(clock)
                drawBossCrumble(monster, t)
                if (t > 0.5f) drawFireworks(clock, ((t - 0.5f) / 0.5f).coerceIn(0f, 1f))
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (t >= 0.6f) {
                    val blink = (clock * 12).toInt() % 2 == 0
                    Text(
                        tr(R.string.boss_victory_title),
                        color = if (blink) PixelGold else Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        tr(R.string.boss_victory_message, monster.name),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                // 人物在地上跳，最後一下跳得特別高
                Box(Modifier.height(110.dp), contentAlignment = Alignment.BottomCenter) {
                    PixelAvatar(
                        hero,
                        size = 80.dp,
                        tier = tier,
                        modifier = Modifier.padding(bottom = (if (t >= 0.6f) (hop * 4).toInt() * 6 else 0).dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(onClick = onClose, enabled = t >= 1f) { Text(tr(R.string.great)) }
            }
        }
    }
}

private val VictoryNight = Color(0xFF141838)

// 夜空：閃爍的星星，下方一道暗紫色的地平線
private fun DrawScope.drawNightSky(clock: Float) {
    val cell = 4.dp.toPx()
    val step = (clock * 12).toInt()
    for (i in 0 until 40) {
        val x = ((i * 53) % 97) / 97f * size.width
        val y = ((i * 31) % 61) / 61f * size.height * 0.6f
        if ((i + step) % 4 != 0) drawRect(if (i % 3 == 0) PixelGold else Color.White, Offset(x, y), Size(cell / 2, cell / 2))
    }
    drawRect(Color(0xFF2A2050), Offset(0f, size.height * 0.78f), Size(size.width, size.height * 0.22f))
    drawRect(Color(0xFF3A2C68), Offset(0f, size.height * 0.78f), Size(size.width, cell))
}

// 魔王在畫面上方：先閃白光抖動，接著從下往上一格一格碎掉，碎片往下掉、變淡
private fun DrawScope.drawBossCrumble(monster: Monster, t: Float) {
    val art = monster.art
    val cols = art.first().length
    val cell = size.width * 0.5f / cols
    val left = (size.width - cols * cell) / 2
    val top = size.height * 0.12f
    val shake = if (t < 0.3f) sin(t * 200f) * cell * 0.6f else 0f
    val flash = t < 0.3f && (t * 30).toInt() % 2 == 0
    val crumble = ((t - 0.3f) / 0.6f).coerceIn(0f, 1f)
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val color = ScenePalette[c] ?: return@forEachIndexed
            // 下面的格子先碎；每格錯開一點時間
            val start = (art.size - 1 - y) / art.size.toFloat() * 0.6f + ((x * 7 + y * 3) % 10) / 100f
            val p = ((crumble - start) / 0.35f).coerceIn(0f, 1f)
            if (p >= 1f) return@forEachIndexed
            val dx = ((x * 13 + y * 7) % 9 - 4) * cell * 0.6f * p
            val dy = p * p * size.height * 0.5f
            val alpha = 1f - p
            drawRect(
                (if (flash) Color.White else color).copy(alpha = alpha),
                Offset(left + x * cell + shake + dx, top + y * cell + dy),
                Size(cell + 0.5f, cell + 0.5f),
            )
        }
    }
}

// 煙火：五發輪流在天空炸開，每發一圈方塊火花往外散再往下垂
private fun DrawScope.drawFireworks(clock: Float, power: Float) {
    val cell = 4.dp.toPx()
    val colors = listOf(Color(0xFFE04030), PixelGold, Color(0xFF7CE8FF), Color(0xFF70E070), Color(0xFFF08090))
    for (k in 0 until 5) {
        val p = (clock * 2f + k / 5f) % 1f
        val center = Offset(size.width * (0.15f + 0.7f * ((k * 37) % 100) / 100f), size.height * (0.12f + 0.3f * ((k * 53) % 100) / 100f))
        val color = colors[k % colors.size].copy(alpha = power)
        if (p < 0.3f) {
            // 往上衝的小火光
            val rise = p / 0.3f
            val c = Offset(center.x, size.height * 0.78f - (size.height * 0.78f - center.y) * rise)
            drawRect(Color.White.copy(alpha = power), Offset(c.x - cell / 2, c.y), Size(cell, cell * 2))
        } else {
            val q = (p - 0.3f) / 0.7f
            val r = cell * 18 * kotlin.math.sqrt(q)
            for (i in 0 until 16) {
                val a = i * 2 * PI.toFloat() / 16
                val c = Offset(center.x + cos(a) * r, center.y + sin(a) * r + q * q * cell * 10)
                drawRect(color.copy(alpha = power * (1f - q)), Offset(c.x - cell / 2, c.y - cell / 2), Size(cell, cell))
            }
            if (q < 0.15f) drawRect(Color.White.copy(alpha = power * 0.6f), Offset(center.x - cell * 2, center.y - cell * 2), Size(cell * 4, cell * 4))
        }
    }
}
