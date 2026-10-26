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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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

// 轉職：從哪個職業（和那時的稱號階級）變成哪個職業
data class ClassChange(val from: Avatar, val to: Avatar, val fromTier: Int, val toTier: Int)

// 轉職特效：夜色裡地上亮起魔法陣，人物越轉越快、光柱從天而降，白光一閃變成新職業，四周噴出星星
@Composable
fun ClassChangeScreen(change: ClassChange, onClose: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(CLASS_FX_DURATION, easing = LinearEasing)) }
    val t = progress.value
    val clock by rememberInfiniteTransition(label = "classFx").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "clock",
    )
    OneShotSound(R.raw.levelup_jingle, play = t >= CLASS_FX_SWAP)
    val swapped = t >= CLASS_FX_SWAP
    // 變身前越轉越快（左右翻面），變身後正面站好
    val spin = if (!swapped) {
        val p = t / CLASS_FX_SWAP
        cos(p * p * 14f * PI.toFloat())
    } else {
        1f
    }

    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(ClassFxNight)) {
            Canvas(Modifier.fillMaxSize()) { drawClassChangeFx(t, clock) }
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.height(140.dp), contentAlignment = Alignment.BottomCenter) {
                    PixelAvatar(
                        if (swapped) change.to else change.from,
                        size = 112.dp,
                        tier = if (swapped) change.toTier else change.fromTier,
                        modifier = Modifier.graphicsLayer {
                            scaleX = spin
                            translationY = -sin(t.coerceAtMost(CLASS_FX_SWAP) / CLASS_FX_SWAP * PI.toFloat()) * 20.dp.toPx()
                        },
                    )
                }
                if (swapped) {
                    Text(
                        tr(R.string.class_change_done, change.to.label),
                        color = if ((clock * 8).toInt() % 2 == 0) PixelGold else Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(onClick = onClose, enabled = t >= 1f) { Text(tr(R.string.great)) }
            }
        }
    }
}

private const val CLASS_FX_DURATION = 3200
private const val CLASS_FX_SWAP = 0.6f
private val ClassFxNight = Color(0xFF1C1440)

private fun DrawScope.drawClassChangeFx(t: Float, clock: Float) {
    val cell = 4.dp.toPx()
    val center = Offset(size.width / 2, size.height / 2 + 30.dp.toPx())
    // 地上的魔法陣：兩圈圓、中間的六芒星，一直轉
    val circle = (t / 0.2f).coerceIn(0f, 1f)
    val r = size.width * 0.38f
    val ring = Color(0xFFC090E8).copy(alpha = circle)
    drawOval(ring, Offset(center.x - r, center.y - r * 0.3f), Size(r * 2, r * 0.6f), style = Stroke(cell / 2))
    drawOval(ring, Offset(center.x - r * 0.8f, center.y - r * 0.24f), Size(r * 1.6f, r * 0.48f), style = Stroke(cell / 3))
    for (k in 0 until 6) {
        val a1 = clock * 2f * PI.toFloat() + k * PI.toFloat() / 3
        val a2 = a1 + 2f * PI.toFloat() / 3
        drawLine(ring, Offset(center.x + cos(a1) * r * 0.8f, center.y + sin(a1) * r * 0.24f), Offset(center.x + cos(a2) * r * 0.8f, center.y + sin(a2) * r * 0.24f), cell / 3)
        drawRect(Color(0xFFF8E8A0).copy(alpha = circle), Offset(center.x + cos(a1) * r - cell / 2, center.y + sin(a1) * r * 0.3f - cell / 2), Size(cell, cell))
    }
    // 光粒子往上飄、往中間收
    if (t < CLASS_FX_SWAP) {
        for (k in 0 until 16) {
            val p = ((t * 3f) + k / 16f) % 1f
            val a = k * 2f * PI.toFloat() / 16
            val c = Offset(center.x + cos(a) * r * (1f - p), center.y - p * size.height * 0.25f + sin(a) * r * 0.3f * (1f - p))
            drawRect(Color(0xFFF8E8A0).copy(alpha = 1f - p), Offset(c.x - cell / 2, c.y - cell / 2), Size(cell, cell))
        }
    }
    // 光柱從天而降
    val beam = ((t - 0.35f) / 0.2f).coerceIn(0f, 1f) * (1f - ((t - 0.7f) / 0.2f).coerceIn(0f, 1f))
    if (beam > 0f) {
        val w = size.width * 0.3f * beam
        drawRect(Color(0xFFF8E8A0).copy(alpha = 0.4f), Offset(center.x - w / 2, 0f), Size(w, center.y))
        drawRect(Color.White.copy(alpha = 0.5f), Offset(center.x - w / 6, 0f), Size(w / 3, center.y))
    }
    // 變身的瞬間白光
    if (t in CLASS_FX_SWAP - 0.04f..CLASS_FX_SWAP + 0.06f) drawRect(Color.White.copy(alpha = 0.85f))
    // 變身後四周噴出星星
    if (t > CLASS_FX_SWAP) {
        val p = ((t - CLASS_FX_SWAP) / 0.35f).coerceIn(0f, 1f)
        for (k in 0 until 12) {
            val a = k * 2f * PI.toFloat() / 12
            val c = Offset(center.x + cos(a) * r * p, center.y - size.height * 0.12f + sin(a) * r * p * 0.8f)
            val col = (if (k % 2 == 0) PixelGold else Color.White).copy(alpha = 1f - p)
            drawRect(col, Offset(c.x - cell / 2, c.y - cell * 1.5f), Size(cell, cell * 3))
            drawRect(col, Offset(c.x - cell * 1.5f, c.y - cell / 2), Size(cell * 3, cell))
        }
    }
}
