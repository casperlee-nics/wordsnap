package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wordsnap.app.data.Profile
import com.wordsnap.app.ui.theme.PixelBlue
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// 人物能力：等級、稱號、經驗值、已記住的單字數和四項能力
@Composable
fun HeroDialog(profile: Profile, remembered: Int, onDismiss: () -> Unit, onClassChange: ((Avatar) -> Unit)? = null) {
    var changingClass by remember { mutableStateOf(false) }
    val avatar = Avatar.from(profile.avatar)
    val race = avatar.race
    val info = levelInfo(profile.exp)
    val stats = race.stats(info.level)
    val nextTitleAt = race.nextTitleLevel(info.level)
    val tier = avatarTier(info.level)

    PixelWindowDialog(
        title = "人物能力",
        onDismiss = onDismiss,
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAvatar(avatar, size = 56.dp, tier = tier)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text("「${race.title(info.level)}」", style = MaterialTheme.typography.bodyMedium)
                }
                // 超過 10 級才出現轉職按鈕
                if (onClassChange != null && info.level >= CLASS_CHANGE_MIN_LEVEL) {
                    ClassChangeButton(onClick = { changingClass = true })
                }
            }
        },
    ) {
        Text("職業：${race.label}　Lv ${info.level}")
        Text("已記住的單字：$remembered 個")
        if (info.level < MAX_LEVEL) {
            Text("經驗值 ${info.current} / ${info.needed}", style = MaterialTheme.typography.bodySmall)
            Meter(info.current.toFloat() / info.needed, PixelBlue)
        }
        StatRow("力量", stats.str)
        StatRow("體力", stats.vit)
        StatRow("智慧", stats.intel)
        StatRow("運氣", stats.luk)
        Text("必殺技：${race.finisher.label(tier)}", style = MaterialTheme.typography.bodySmall)
        if (nextTitleAt != null) {
            Text(
                "Lv $nextTitleAt 可獲得稱號「${race.title(nextTitleAt)}」",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (changingClass && onClassChange != null) {
        ClassChangeDialog(
            current = avatar,
            level = info.level,
            onDismiss = { changingClass = false },
            onConfirm = { picked ->
                changingClass = false
                onClassChange(picked)
            },
        )
    }
}

// 轉職：選一個別的職業，確認會降 10 級
@Composable
private fun ClassChangeDialog(current: Avatar, level: Int, onDismiss: () -> Unit, onConfirm: (Avatar) -> Unit) {
    var picked by remember { mutableStateOf(Avatar.entries.first { it != current }) }
    val newLevel = (level - CLASS_CHANGE_PENALTY).coerceAtLeast(1)
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("轉職") },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("要轉成哪一種職業？轉職後等級會從 Lv $level 降到 Lv $newLevel。")
                AvatarPicker(selected = picked, onSelect = { if (it != current) picked = it }, exclude = current)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text("轉職成${picked.label}", color = PixelRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// 能力值長條，以 200 為滿格
@Composable
private fun StatRow(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(40.dp))
        Box(Modifier.weight(1f)) { Meter((value / 200f).coerceAtMost(1f), Color(0xFFE04030)) }
        Text("$value", Modifier.width(40.dp).padding(start = 8.dp))
    }
}

@Composable
private fun Meter(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(10.dp).border(2.dp, PixelInk).padding(2.dp)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

// 轉職按鈕：會慢慢轉動的魔法陣（金色外圈、深藍底、一格一格繞圈的符文光點），中間是交換箭頭，下面寫「轉職」
@Composable
private fun ClassChangeButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spin by rememberInfiniteTransition(label = "circle").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "spin",
    )
    Column(
        Modifier
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .graphicsLayer {
                val k = if (pressed) 0.9f else 1f
                scaleX = k
                scaleY = k
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(54.dp).drawBehind {
                val c = center
                val r = size.minDimension / 2
                val px = 2.dp.toPx()
                drawCircle(PixelInk, r, c)
                drawCircle(CircleGold, r - px, c)
                drawCircle(PixelInk, r - px * 2.5f, c)
                drawCircle(CircleInner, r - px * 3f, c)
                // 符文光點：每 1/16 圈跳一格
                val step = floor(spin * 16f) / 16f
                for (k in 0 until 8) {
                    val a = (step + k / 8f) * 2 * PI.toFloat()
                    val d = r - px * 4.5f
                    val glow = if (k % 2 == 0) CircleRune else CircleRuneDim
                    drawRect(glow, Offset(c.x + cos(a) * d - px / 2, c.y + sin(a) * d - px / 2), Size(px, px))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            PixelArt(IconClassChange, ColorIconPalette, Modifier.size(30.dp))
        }
        Text("轉職", style = MaterialTheme.typography.labelMedium, color = PixelInk)
    }
}

private val CircleGold = Color(0xFFE0B040)
private val CircleInner = Color(0xFF1E3C5A)
private val CircleRune = Color(0xFF8CE8FF)
private val CircleRuneDim = Color(0xFF4A90B0)
