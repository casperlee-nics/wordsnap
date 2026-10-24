package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelRed
import kotlin.math.sin

// 戰鬥勝利掉寶箱的小動畫：寶箱抖一抖 → 打開發光 → 道具從寶箱裡飛出來
private val ChestClosed = listOf(
    "................",
    "................",
    "................",
    "................",
    "..KKKKKKKKKKKK..",
    ".KeddddddddddeK.",
    ".KdNNNNNNNNNNdK.",
    ".KeddddddddddeK.",
    "KKKKKKKYYKKKKKKK",
    "KNNNNNKYYKNNNNNK",
    "KeddddKKKKddddeK",
    "KeddddddddddddeK",
    "KNNNNNNNNNNNNNNK",
    "KeddddddddddddeK",
    "KeddddddddddddeK",
    "KKKKKKKKKKKKKKKK",
)

private val ChestOpen = listOf(
    "..KKKKKKKKKKKK..",
    ".KeddddddddddeK.",
    ".KdNNNNNNNNNNdK.",
    ".KeddddddddddeK.",
    ".KKKKKKKKKKKKKK.",
    ".KYYYYYYYYYYYYK.",
    "KYYWYYYYYYYWYYYK",
    "KYYYYYYYYYYYYYYK",
    "KKKKKKKYYKKKKKKK",
    "KNNNNNKYYKNNNNNK",
    "KeddddKKKKddddeK",
    "KeddddddddddddeK",
    "KNNNNNNNNNNNNNNK",
    "KeddddddddddddeK",
    "KeddddddddddddeK",
    "KKKKKKKKKKKKKKKK",
)

@Composable
fun ChestScreen(item: ItemType, onClose: () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val t = anim.value
    Dialog(onDismissRequest = { }) {
        PixelSurface(color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(tr(R.string.chest_found), style = MaterialTheme.typography.headlineSmall, color = PixelRed)
                Box(Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(180.dp)) { drawChest(t, item) }
                }
                if (t >= 1f) {
                    Text(tr(R.string.chest_got, item.label), style = MaterialTheme.typography.titleLarge, color = PixelRed)
                    Text(item.effect, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.readable())
                    Button(onClick = onClose) { Text(tr(R.string.great)) }
                } else {
                    Text("……", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private fun DrawScope.drawChest(t: Float, item: ItemType) {
    val cell = 5.dp.toPx()
    val chestW = 16 * cell
    val left = (size.width - chestW) / 2
    val top = size.height - 16 * cell - 4.dp.toPx()
    val opened = t >= 0.4f
    // 打開前左右抖動，越來越快
    val shake = if (!opened) sin(t * 90f) * cell * (t / 0.4f) else 0f
    val center = Offset(size.width / 2, top + 6 * cell)
    if (opened) {
        // 寶箱裡射出的光芒，慢慢轉
        val glow = ((t - 0.4f) / 0.6f).coerceIn(0f, 1f)
        for (i in 0 until 8) {
            rotate(i * 45f + glow * 60f, center) {
                drawRect(
                    Color(0xFFF8E070).copy(alpha = 0.55f * (1f - glow * 0.5f)),
                    Offset(center.x - cell * 0.6f, center.y - size.height * 0.6f * glow - cell * 4),
                    Size(cell * 1.2f, size.height * 0.6f * glow + cell * 4),
                )
            }
        }
    }
    drawArt(if (opened) ChestOpen else ChestClosed, ScenePalette, left + shake, top, cell)
    if (opened) {
        // 道具從寶箱裡飛出來，越飛越大
        val rise = ((t - 0.45f) / 0.4f).coerceIn(0f, 1f)
        val iconCell = cell * (0.4f + 0.5f * rise)
        val iconSize = 16 * iconCell
        val y = top + 6 * cell - rise * (8 * cell) - iconSize / 2
        drawArt(item.icon, ItemPalette, size.width / 2 - iconSize / 2, y, iconCell)
        // 閃閃發亮的小星星
        if (rise > 0.5f) {
            for (k in 0 until 6) {
                val a = k * 1.05f + t * 6f
                val r = iconSize * 0.75f
                val on = ((t * 10).toInt() + k) % 2 == 0
                if (on) {
                    drawRect(
                        Color.White,
                        Offset(size.width / 2 + kotlin.math.cos(a) * r, y + iconSize / 2 + sin(a) * r),
                        Size(cell * 0.8f, cell * 0.8f),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawArt(art: List<String>, colors: Map<Char, Color>, left: Float, top: Float, cell: Float) {
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch ->
            val color = colors[ch] ?: return@forEachIndexed
            drawRect(color, Offset(left + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
