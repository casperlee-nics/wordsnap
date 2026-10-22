package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wordsnap.app.data.Profile
import com.wordsnap.app.ui.theme.PixelBlue
import com.wordsnap.app.ui.theme.PixelInk

// 人物能力：等級、稱號、經驗值、已記住的單字數和四項能力
@Composable
fun HeroDialog(profile: Profile, remembered: Int, onDismiss: () -> Unit) {
    val avatar = Avatar.from(profile.avatar)
    val race = avatar.race
    val info = levelInfo(profile.exp)
    val stats = race.stats(info.level)
    val nextTitleAt = race.nextTitleLevel(info.level)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAvatar(avatar, size = 56.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(profile.name)
                    Text("「${race.title(info.level)}」", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("種族：${race.label}　Lv ${info.level}")
                Text("已記住的單字：$remembered 個")
                if (info.level < MAX_LEVEL) {
                    Text("經驗值 ${info.current} / ${info.needed}", style = MaterialTheme.typography.bodySmall)
                    Meter(info.current.toFloat() / info.needed, PixelBlue)
                }
                StatRow("力量", stats.str)
                StatRow("體力", stats.vit)
                StatRow("智慧", stats.intel)
                StatRow("運氣", stats.luk)
                Text("必殺技：${race.finisher.label}", style = MaterialTheme.typography.bodySmall)
                if (nextTitleAt != null) {
                    Text(
                        "Lv $nextTitleAt 可獲得稱號「${race.title(nextTitleAt)}」",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
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
