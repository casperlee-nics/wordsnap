package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// 主畫面：上方是像素世界（城堡下有四個人物在動），中間是選單，下方是草地
@Composable
fun TitleScreen(
    hasSaves: Boolean,
    onNewGame: () -> Unit,
    onLoadGame: () -> Unit,
    onManageSaves: () -> Unit,
) {
    val context = LocalContext.current
    var level by remember { mutableStateOf(musicLevel(context)) }
    // 每次進主畫面從 5 首裡隨機挑一首
    val music = remember { TitleMusic.random() }
    LoopingMusic(music, enabled = true, volume = 0.6f)
    // 依手機時間：早上 6 點到晚上 6 點是白天的城堡，其他時間是夜晚燈火通明的城堡
    val night = remember { isNight() }
    // 夜晚的小彩蛋：點城堡會放煙火
    var fireworksShow by remember { mutableIntStateOf(0) }
    var fireworksOn by remember { mutableStateOf(false) }
    OneShotSound(R.raw.fireworks_sfx, play = fireworksOn, volume = 0.7f)

    Box(Modifier.fillMaxSize().background(if (night) SceneGrassNight else SceneGrass)) {
        Column(Modifier.fillMaxSize()) {
            Box {
                PixelArt(if (night) TitleSkyNight else TitleSky, ScenePalette, Modifier.fillMaxWidth())
                TitleParty(Modifier.matchParentSize())
                if (night) {
                    if (fireworksOn) {
                        CastleFireworks(fireworksShow, onDone = { fireworksOn = false }, Modifier.matchParentSize())
                    }
                    BoxWithConstraints(Modifier.matchParentSize()) {
                        val cell = maxWidth / 64
                        // 看不見的按鈕蓋在城堡上，不會擋到下面的人物
                        Box(
                            Modifier
                                .offset(x = cell * CastleLeft, y = cell * CastleTop)
                                .size(width = cell * CastleWidth, height = cell * CastleHeight)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (!fireworksOn) {
                                        fireworksShow++
                                        fireworksOn = true
                                    }
                                },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f))
            PixelArt(if (night) TitleGroundNight else TitleGround, ScenePalette, Modifier.fillMaxWidth())
        }

        // 選單放在草地中間，不會擋到上方的城堡
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            // 像素風的視窗外框（和人物能力、道具欄同一種），比之前稍微往上
            Box(Modifier.padding(bottom = 110.dp).width(250.dp).pixelWindowFrame()) {
                CompositionLocalProvider(LocalContentColor provides PixelInk) {
                    Column(
                        Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("像素單字", style = MaterialTheme.typography.headlineMedium)
                        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) { Text("新遊戲") }
                        OutlinedButton(onClick = onLoadGame, enabled = hasSaves, modifier = Modifier.fillMaxWidth()) {
                            Text("讀取遊戲")
                        }
                        OutlinedButton(onClick = onManageSaves, enabled = hasSaves, modifier = Modifier.fillMaxWidth()) {
                            Text("記錄管理")
                        }
                        // 背景音樂音量：關、小、大（念單字的聲音不受影響）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♪ 音樂：", style = MaterialTheme.typography.bodyMedium)
                            MusicLevel.entries.forEach { option ->
                                val chosen = option == level
                                Box(
                                    Modifier
                                        .padding(horizontal = 3.dp)
                                        .border(2.dp, if (chosen) PixelInk else Color.Transparent)
                                        .background(if (chosen) PixelGold else Color.Transparent)
                                        .clickable {
                                            level = option
                                            setMusicLevel(context, option)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(option.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 夜晚城堡在天空圖（64 x 36 格）裡的位置
private const val CastleLeft = 22
private const val CastleTop = 3
private const val CastleWidth = 20
private const val CastleHeight = 23

// 煙火：6 發依序從城堡升空，升到天上炸開成一圈像素火花
// 時間要和 fireworks_sfx.ogg 的咻～碰對齊
private val FireworkLaunches = listOf(0f, 0.5f, 1.0f, 1.6f, 2.1f, 2.5f)
private const val FIREWORK_RISE = 0.45f
private const val FIREWORK_BURST = 1.0f
private const val FIREWORK_TOTAL_MS = 3800
private val FireworkColors = listOf(
    Color(0xFFF84838), Color(0xFFF8D030), Color(0xFF58D8F8),
    Color(0xFF78F858), Color(0xFFF878F8), Color(0xFFFFFFFF),
)
private val FireworkTrail = Color(0xFFF8E8A0)

private class Firework(val fromX: Float, val toX: Float, val toY: Float, val color: Color, val sparkle: Color)

@Composable
private fun CastleFireworks(show: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val shots = remember(show) {
        FireworkLaunches.map {
            val colors = FireworkColors.shuffled()
            Firework(
                fromX = (28..36).random().toFloat(),
                toX = (8..56).random().toFloat(),
                toY = (3..9).random().toFloat(),
                color = colors[0],
                sparkle = colors[1],
            )
        }
    }
    val clock = remember(show) { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(show) {
        clock.animateTo(FIREWORK_TOTAL_MS / 1000f, tween(FIREWORK_TOTAL_MS, easing = LinearEasing))
        done()
    }
    Canvas(modifier) {
        val cell = size.width / 64
        // 對齊格子畫一個像素點，看起來才像像素遊戲
        fun dot(x: Float, y: Float, color: Color, alpha: Float) {
            drawRect(color.copy(alpha = alpha.coerceIn(0f, 1f)), Offset(floor(x) * cell, floor(y) * cell), Size(cell, cell))
        }
        val t = clock.value
        shots.forEachIndexed { i, shot ->
            val local = t - FireworkLaunches[i]
            if (local < 0f) return@forEachIndexed
            if (local < FIREWORK_RISE) {
                // 升空：一個亮點拖著尾巴
                val p = local / FIREWORK_RISE
                val x = shot.fromX + (shot.toX - shot.fromX) * p
                val y = CastleTop + 1 + (shot.toY - CastleTop - 1) * p
                dot(x, y, FireworkTrail, 1f)
                dot(x, y + 1, FireworkTrail, 0.5f)
                dot(x, y + 2, FireworkTrail, 0.25f)
                return@forEachIndexed
            }
            val p = (local - FIREWORK_RISE) / FIREWORK_BURST
            if (p > 1f) return@forEachIndexed
            val alpha = 1f - p
            val fall = p * p * 2f
            if (p < 0.15f) dot(shot.toX, shot.toY, Color.White, 1f)
            // 外圈 16 顆火花往外散開，慢慢往下掉、變淡
            val radius = 1f + p * 6f
            for (k in 0 until 16) {
                val angle = k * 2f * PI.toFloat() / 16
                val color = if (k % 2 == 0) shot.color else shot.sparkle
                dot(shot.toX + cos(angle) * radius, shot.toY + sin(angle) * radius + fall, color, alpha)
            }
            // 內圈 8 顆，比較快消失
            if (p < 0.6f) {
                for (k in 0 until 8) {
                    val angle = (k + 0.5f) * 2f * PI.toFloat() / 8
                    dot(shot.toX + cos(angle) * radius * 0.5f, shot.toY + sin(angle) * radius * 0.5f + fall, shot.sparkle, alpha)
                }
            }
        }
    }
}

// 主畫面的背景音樂
private val TitleMusic = listOf(R.raw.title_bgm, R.raw.title2_bgm, R.raw.title3_bgm, R.raw.hope_bgm, R.raw.load_bgm)

private fun isNight(): Boolean {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return hour < 6 || hour >= 18
}

// 人物在原地做的小動作
private enum class Idle { Hop, Sway, LookAround, Squash, Spin }

// 城堡下的四個人物在第幾格（主畫面的天空圖寬 64 格）
private val PartyColumns = listOf(14, 23, 33, 42)

// 隨機挑四個人物，各做一種小動作，一直循環
@Composable
private fun TitleParty(modifier: Modifier = Modifier) {
    val cast = remember { Avatar.entries.shuffled().take(4).zip(Idle.entries.shuffled()) }
    // 每個人物隨機一種稱號階級的樣子
    val tiers = remember { List(cast.size) { (0 until Avatar.TIER_COUNT).random() } }
    val time by rememberInfiniteTransition(label = "party").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "time",
    )
    BoxWithConstraints(modifier) {
        val cell = maxWidth / 64
        cast.forEachIndexed { i, (avatar, idle) ->
            // 每個人錯開一點，不會同時動
            val p = (time + i * 0.27f) % 1f
            PixelAvatar(
                avatar,
                size = cell * 8,
                tier = tiers[i],
                modifier = Modifier
                    .offset(x = cell * PartyColumns[i], y = cell * 26)
                    .graphicsLayer {
                        val px = cell.toPx()
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        when (idle) {
                            // 一格一格往上跳，像像素遊戲
                            Idle.Hop -> translationY = -floor(sin(p * PI.toFloat()) * 3.99f) * px
                            // 左右搖擺，只有兩個畫格
                            Idle.Sway -> rotationZ = if (p < 0.5f) -10f else 10f
                            // 左看看、右看看
                            Idle.LookAround -> scaleX = if (p < 0.5f) 1f else -1f
                            // 壓扁再彈起來
                            Idle.Squash -> {
                                val s = if (p < 0.25f) 0.8f else if (p < 0.4f) 1.12f else 1f
                                scaleY = s
                                scaleX = 2f - s
                            }
                            // 原地轉一圈
                            Idle.Spin -> scaleX = cos(p * 2 * PI.toFloat())
                        }
                    },
            )
        }
    }
}
