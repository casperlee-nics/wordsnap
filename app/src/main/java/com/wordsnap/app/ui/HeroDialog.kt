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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.MonsterLog
import com.wordsnap.app.data.Profile
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelBlue
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed
import kotlin.math.floor

// 人物能力：等級、稱號、經驗值、已記住的單字數和四項能力
@Composable
fun HeroDialog(
    profile: Profile,
    remembered: Int,
    onDismiss: () -> Unit,
    onClassChange: ((Avatar) -> Unit)? = null,
    monsterLog: Map<String, MonsterLog> = emptyMap(),
) {
    var changingClass by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var classLocked by remember { mutableStateOf(false) }
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
                // 轉職（紅色魔法書）在上、冒險履歷（藍色的書）在下；轉職按鈕一直都在，還沒超過 10 級時按了只會提示還不能轉職
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (onClassChange != null) {
                        BookButton(tr(R.string.class_change), SpellBookColors, onClick = {
                            if (info.level >= CLASS_CHANGE_MIN_LEVEL) changingClass = true else classLocked = true
                        })
                    }
                    BookButton(tr(R.string.adventure_log), LogBookColors, onClick = { showLog = true })
                }
            }
        },
    ) {
        Text(tr(R.string.hero_class_level, race.label, info.level))
        Text(tr(R.string.hero_remembered, remembered))
        Text(tr(R.string.hero_wallet, profile.coins))
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

    if (classLocked) {
        PixelAlertDialog(
            onDismissRequest = { classLocked = false },
            title = { Text(tr(R.string.class_change)) },
            text = { Text(tr(R.string.class_change_locked, CLASS_CHANGE_MIN_LEVEL, info.level), style = MaterialTheme.typography.bodyMedium.readable()) },
            confirmButton = { TextButton(onClick = { classLocked = false }) { Text(tr(R.string.close)) } },
        )
    }

    if (showLog) AdventureLogDialog(monsterLog, onDismiss = { showLog = false })

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
        FitText(label, Modifier.width(76.dp), textAlign = TextAlign.Start)
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

// 書本按鈕：一本古典的書（皮革封面、金色書角和鑲邊、書背兩道金環，右邊和下面露出書頁），上方的寶石會一閃一閃
// 書下面掛一塊小木牌寫字；紅色是轉職的魔法書，藍色是冒險履歷；整個按鈕往左下偏一點，不會擠到視窗的右邊
@Composable
private fun BookButton(label: String, colors: BookColors, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glow by rememberInfiniteTransition(label = "gem").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "glow",
    )
    Column(
        Modifier
            .offset(x = (-10).dp, y = 6.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .graphicsLayer {
                val k = if (pressed) 0.92f else 1f
                scaleX = k
                scaleY = k
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 書是 30 x 36 格，畫成 35 x 42dp
        Box(
            Modifier
                .size(width = 35.dp, height = 42.dp)
                .drawBehind { drawSpellBook(bright = floor(glow * 4f).toInt() % 2 == 0, colors) },
        )
        // 書下面的小木牌：淺色木板、木紋、深棕色字
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(54.dp)
                .height(20.dp)
                .drawBehind { drawSmallPlank() },
            contentAlignment = Alignment.Center,
        ) {
            FitText(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = BookLabelInk,
                modifier = Modifier.padding(horizontal = 5.dp),
            )
        }
    }
}

private fun DrawScope.drawSmallPlank() {
    val px = 2.dp.toPx()
    val w = size.width
    val h = size.height
    // 缺角的黑框、淺色木頭、上亮下暗、兩條木紋、兩端小釘子
    drawRect(PixelInk, Offset(px, 0f), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(0f, px), Size(w, h - px * 2))
    drawRect(PlankWood, Offset(px, px), Size(w - px * 2, h - px * 2))
    drawRect(PlankLight, Offset(px, px), Size(w - px * 2, px))
    drawRect(PlankDark, Offset(px, h - px * 2), Size(w - px * 2, px))
    drawRect(PlankGrain, Offset(px * 4, h * 0.35f), Size(w * 0.3f, px * 0.5f))
    drawRect(PlankGrain, Offset(w * 0.55f, h * 0.65f), Size(w * 0.3f, px * 0.5f))
    drawRect(PlankDark, Offset(px * 2, h / 2 - px / 2), Size(px, px))
    drawRect(PlankDark, Offset(w - px * 3, h / 2 - px / 2), Size(px, px))
}

private val PlankWood = Color(0xFFD8B078)
private val PlankLight = Color(0xFFF0D0A0)
private val PlankDark = Color(0xFF9A6E3C)
private val PlankGrain = Color(0xFFB88C54)

// 魔法書寬 30 格、高 36 格，一格是寬度的 1/30
private fun DrawScope.drawSpellBook(bright: Boolean, colors: BookColors) {
    val u = size.width / 30f
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
    cell(1f, 1f, 25f, 31f, colors.leather)
    cell(1f, 1f, 25f, 1f, colors.light)
    // 書背：深色皮革和兩道金環
    cell(1f, 1f, 3f, 31f, colors.spine)
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
    cell(13f, 6f, 3f, 3f, if (bright) colors.gem else colors.gemDim)
    cell(13f, 6f, 1f, 1f, Color.White)
    // 封面中間的金色魔法陣徽章
    cell(12f, 13f, 6f, 1f, BookGold)
    cell(12f, 21f, 6f, 1f, BookGold)
    cell(10f, 15f, 1f, 5f, BookGold)
    cell(19f, 15f, 1f, 5f, BookGold)
    cell(11f, 14f, 1f, 1f, BookGold)
    cell(18f, 14f, 1f, 1f, BookGold)
    cell(11f, 20f, 1f, 1f, BookGold)
    cell(18f, 20f, 1f, 1f, BookGold)
    cell(14f, 15f, 2f, 5f, BookGoldDark)
    cell(12f, 17f, 6f, 1f, BookGoldDark)
    cell(14f, 17f, 2f, 1f, if (bright) colors.gem else colors.gemDim)
    // 下方的小花紋
    cell(12f, 25f, 5f, 1f, BookGold)
    cell(14f, 24f, 1f, 3f, BookGold)
}

private val BookShadow = Color(0x66000000)
private val BookPage = Color(0xFFF4ECD0)
private val BookPageLine = Color(0xFFB8A880)
private val BookGold = Color(0xFFE8B840)
private val BookGoldDark = Color(0xFF9A6A18)

// 書的封面顏色：皮革、亮邊、書背、寶石（亮、暗）
private class BookColors(val leather: Color, val light: Color, val spine: Color, val gem: Color, val gemDim: Color)

private val SpellBookColors = BookColors(Color(0xFF8A2A1C), Color(0xFFB04A30), Color(0xFF561610), Color(0xFF7CE8FF), Color(0xFF3A90B8))
private val LogBookColors = BookColors(Color(0xFF24489C), Color(0xFF4A72D0), Color(0xFF122A62), Color(0xFFFF7A6A), Color(0xFFB83A30))
private val BookLabelInk = Color(0xFF2A1A0A)

// 冒險履歷：遇過的怪物（一般怪物、中魔王、大魔王依序），每隻的圖、遇到和打倒的次數、簡介
@Composable
private fun AdventureLogDialog(log: Map<String, MonsterLog>, onDismiss: () -> Unit) {
    val all = NormalMonsters + MidBosses + BigBosses
    val met = all.filter { (log[it.key]?.met ?: 0) > 0 }
    PixelWindowDialog(title = tr(R.string.adventure_log), onDismiss = onDismiss) {
        Text(tr(R.string.adventure_log_summary, met.size, all.size, met.sumOf { log[it.key]?.wins ?: 0 }))
        if (met.isEmpty()) Text(tr(R.string.adventure_log_empty), style = MaterialTheme.typography.bodyMedium.readable())
        met.forEach { monster ->
            val record = log.getValue(monster.key)
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.width(48.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    PixelArt(monster.art, ScenePalette, Modifier.width(if (monster.rank == MonsterRank.Normal) 36.dp else 46.dp))
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    val rank = when (monster.rank) {
                        MonsterRank.Big -> tr(R.string.boss_big) + "・"
                        MonsterRank.Mid -> tr(R.string.boss_mid) + "・"
                        MonsterRank.Normal -> ""
                    }
                    Text(rank + monster.name, style = MaterialTheme.typography.titleSmall, color = if (monster.rank == MonsterRank.Normal) PixelInk else PixelRed)
                    Text(tr(R.string.adventure_log_counts, record.met, record.wins), style = MaterialTheme.typography.bodySmall)
                    Text(monster.desc, style = MaterialTheme.typography.bodySmall.readable())
                }
            }
        }
    }
}
