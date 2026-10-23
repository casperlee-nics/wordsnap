package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelShape
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

    Box(Modifier.fillMaxSize().background(if (night) SceneGrassNight else SceneGrass)) {
        Column(Modifier.fillMaxSize()) {
            Box {
                PixelArt(if (night) TitleSkyNight else TitleSky, ScenePalette, Modifier.fillMaxWidth())
                TitleParty(Modifier.matchParentSize())
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
            Surface(
                shape = PixelShape,
                border = PixelBorder,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(bottom = 64.dp).width(240.dp),
            ) {
                Column(
                    Modifier.padding(16.dp),
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
