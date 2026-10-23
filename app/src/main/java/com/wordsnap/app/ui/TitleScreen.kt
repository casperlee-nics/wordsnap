package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelBorder
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
    onDeleteSave: () -> Unit,
) {
    val context = LocalContext.current
    var musicOn by remember { mutableStateOf(isMusicOn(context)) }
    LoopingMusic(R.raw.title_bgm, enabled = musicOn, volume = 0.6f)

    Box(Modifier.fillMaxSize().background(SceneGrass)) {
        Column(Modifier.fillMaxSize()) {
            Box {
                PixelArt(TitleSky, ScenePalette, Modifier.fillMaxWidth())
                TitleParty(Modifier.matchParentSize())
            }
            Box(Modifier.weight(1f))
            PixelArt(TitleGround, ScenePalette, Modifier.fillMaxWidth())
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
                    OutlinedButton(onClick = onDeleteSave, enabled = hasSaves, modifier = Modifier.fillMaxWidth()) {
                        Text("刪除冒險者")
                    }
                    TextButton(onClick = {
                        musicOn = !musicOn
                        setMusicOn(context, musicOn)
                    }) { Text(if (musicOn) "♪ 音樂：開" else "♪ 音樂：關") }
                }
            }
        }
    }
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
