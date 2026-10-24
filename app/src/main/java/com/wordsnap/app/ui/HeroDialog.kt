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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.Profile
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelBlue
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed
import kotlin.math.floor

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
        title = tr(R.string.hero_title),
        onDismiss = onDismiss,
        header = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelAvatar(avatar, size = 56.dp, tier = tier)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(tr(R.string.quoted, race.title(info.level)), style = MaterialTheme.typography.bodyMedium)
                }
                // 超過 10 級才出現轉職按鈕
                if (onClassChange != null && info.level >= CLASS_CHANGE_MIN_LEVEL) {
                    ClassChangeButton(onClick = { changingClass = true })
                }
            }
        },
    ) {
        Text(tr(R.string.hero_class_level, race.label, info.level))
        Text(tr(R.string.hero_remembered, remembered))
        if (info.level < MAX_LEVEL) {
            Text(tr(R.string.hero_exp, info.current, info.needed), style = MaterialTheme.typography.bodySmall)
            Meter(info.current.toFloat() / info.needed, PixelBlue)
        }
        StatRow(tr(R.string.stat_str), stats.str)
        StatRow(tr(R.string.stat_vit), stats.vit)
        StatRow(tr(R.string.stat_int), stats.intel)
        StatRow(tr(R.string.stat_luk), stats.luk)
        Text(tr(R.string.hero_finisher, race.finisher.label(tier)), style = MaterialTheme.typography.bodySmall)
        if (nextTitleAt != null) {
            Text(
                tr(R.string.hero_next_title, nextTitleAt, race.title(nextTitleAt)),
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
        title = { Text(tr(R.string.class_change)) },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(tr(R.string.class_change_message, level, newLevel), style = MaterialTheme.typography.bodyMedium.readable())
                AvatarPicker(selected = picked, onSelect = { if (it != current) picked = it }, exclude = current)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text(tr(R.string.class_change_to, picked.label), color = PixelRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
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

// 轉職按鈕：一本古典魔法書（紅色皮革封面、金色書角和鑲邊、書背兩道金環，右邊和下面露出書頁）
// 封面中間貼著羊皮紙標籤，深棕色的「轉職」寫在淺色底上，看得清楚；上方的寶石會一閃一閃
@Composable
private fun ClassChangeButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glow by rememberInfiniteTransition(label = "gem").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "glow",
    )
    Box(
        Modifier
            .size(width = 60.dp, height = 72.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .graphicsLayer {
                val k = if (pressed) 0.92f else 1f
                scaleX = k
                scaleY = k
            }
            .drawBehind { drawSpellBook(bright = floor(glow * 4f).toInt() % 2 == 0) },
    ) {
        // 文字放在羊皮紙標籤上（標籤在書的第 6～25 格、第 12～21 格，一格 2dp）
        Box(
            Modifier.padding(start = 12.dp, top = 24.dp).size(width = 40.dp, height = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(tr(R.string.class_change), style = MaterialTheme.typography.labelLarge, color = BookLabelInk, maxLines = 1)
        }
    }
}

// 魔法書一格 2dp：寬 30 格、高 36 格
private fun DrawScope.drawSpellBook(bright: Boolean) {
    val u = 2.dp.toPx()
    fun cell(x: Float, y: Float, w: Float, h: Float, color: Color) =
        drawRect(color, Offset(x * u, y * u), Size(w * u, h * u))
    // 影子、露出來的書頁
    cell(2f, 2f, 28f, 34f, BookShadow)
    cell(2f, 1f, 27f, 34f, PixelInk)
    cell(3f, 2f, 25f, 32f, BookPage)
    for (y in 4 until 34 step 3) cell(27f, y.toFloat(), 1f, 1f, BookPageLine)
    for (x in 5 until 27 step 3) cell(x.toFloat(), 33f, 1f, 1f, BookPageLine)
    // 封面
    cell(0f, 0f, 27f, 33f, PixelInk)
    cell(1f, 1f, 25f, 31f, BookLeather)
    cell(1f, 1f, 25f, 1f, BookLeatherLight)
    // 書背：深色皮革和兩道金環
    cell(1f, 1f, 3f, 31f, BookSpine)
    cell(1f, 5f, 3f, 2f, BookGold)
    cell(1f, 26f, 3f, 2f, BookGold)
    // 金色鑲邊
    cell(5f, 3f, 20f, 1f, BookGold)
    cell(5f, 29f, 20f, 1f, BookGold)
    cell(5f, 3f, 1f, 27f, BookGold)
    cell(24f, 3f, 1f, 27f, BookGold)
    // 四個金色書角
    for ((x, y) in listOf(4f to 1f, 23f to 1f, 4f to 29f, 23f to 29f)) {
        cell(x, y, 3f, 3f, BookGold)
        cell(x + 1f, y + 1f, 1f, 1f, BookGoldDark)
    }
    // 上方的寶石，一閃一閃
    cell(13f, 5f, 3f, 1f, PixelInk)
    cell(12f, 6f, 5f, 3f, PixelInk)
    cell(13f, 9f, 3f, 1f, PixelInk)
    cell(13f, 6f, 3f, 3f, if (bright) BookGem else BookGemDim)
    cell(13f, 6f, 1f, 1f, Color.White)
    // 羊皮紙標籤
    cell(6f, 12f, 19f, 10f, PixelInk)
    cell(7f, 13f, 17f, 8f, BookLabel)
    cell(7f, 20f, 17f, 1f, BookLabelShade)
    // 下方的小花紋
    cell(12f, 25f, 5f, 1f, BookGold)
    cell(14f, 24f, 1f, 3f, BookGold)
}

private val BookShadow = Color(0x66000000)
private val BookPage = Color(0xFFF4ECD0)
private val BookPageLine = Color(0xFFB8A880)
private val BookLeather = Color(0xFF8A2A1C)
private val BookLeatherLight = Color(0xFFB04A30)
private val BookSpine = Color(0xFF561610)
private val BookGold = Color(0xFFE8B840)
private val BookGoldDark = Color(0xFF9A6A18)
private val BookGem = Color(0xFF7CE8FF)
private val BookGemDim = Color(0xFF3A90B8)
private val BookLabel = Color(0xFFF6E6BC)
private val BookLabelShade = Color(0xFFD8C090)
private val BookLabelInk = Color(0xFF2A1A0A)
