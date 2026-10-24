package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import kotlin.math.PI
import kotlin.math.sin

// 打開 App 時的公司開場動畫（約 1.9 秒）：
// 笑臉工具箱彈跳落下 → 星星從箱子裡跳出來 → 閃光亮起 → 「Make It Fun STUDIO」浮上來 → 整個淡出進遊戲
private const val SPLASH_MS = 1900

@Composable
fun CompanySplash(onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        clock.animateTo(SPLASH_MS / 1000f, tween(SPLASH_MS, easing = LinearEasing))
        done()
    }
    // 啵一聲落地，接著星星跳出來時叮叮叮
    OneShotSound(R.raw.logo_chime, play = true, volume = 0.7f)

    val t = clock.value
    // 工具箱：0～0.45 秒從上方落下，落地壓扁一下再彈回來
    val drop = phase(t, 0f, 0.3f)
    val squash = when {
        t < 0.3f -> 1f
        t < 0.38f -> 1f - 0.18f * phase(t, 0.3f, 0.38f)
        t < 0.5f -> 0.82f + 0.18f * easeOutBack(phase(t, 0.38f, 0.5f))
        else -> 1f
    }
    // 星星：0.45～0.85 秒從箱子裡往上跳，轉一下
    val rise = easeOutBack(phase(t, 0.45f, 0.85f))
    val wiggle = if (t > 0.85f) sin(phase(t, 0.85f, 1.25f) * PI.toFloat() * 3) * 8f * (1f - phase(t, 0.85f, 1.25f)) else 0f
    // 閃光：0.8～1.0 秒彈出來
    val spark = easeOutBack(phase(t, 0.8f, 1.0f))
    // 文字：0.9～1.25 秒浮上來
    val text = phase(t, 0.9f, 1.25f)
    // 最後 0.25 秒整個淡出
    val fade = 1f - phase(t, 1.65f, 1.9f)

    Box(
        Modifier.fillMaxSize().background(SplashBackground).graphicsLayer { alpha = fade },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 220.dp, height = 210.dp)) {
                val boxLayer = Modifier
                    .size(width = 220.dp, height = 210.dp)
                    .graphicsLayer {
                        translationY = -(1f - drop) * 500.dp.toPx()
                        transformOrigin = TransformOrigin(0.5f, 0.85f)
                        scaleY = squash
                        scaleX = 2f - squash
                    }
                Image(painterResource(R.drawable.splash_back), contentDescription = null, modifier = boxLayer)
                Image(
                    painterResource(R.drawable.splash_star),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 220.dp, height = 210.dp)
                        .graphicsLayer {
                            // 一開始藏在箱子裡（被前面的箱子擋住）
                            translationY = (1f - rise) * 70.dp.toPx()
                            alpha = if (t < 0.45f) 0f else 1f
                            transformOrigin = TransformOrigin(0.5f, 0.38f)
                            rotationZ = (1f - rise) * -30f + wiggle
                        },
                )
                Image(painterResource(R.drawable.splash_front), contentDescription = null, modifier = boxLayer)
                Image(
                    painterResource(R.drawable.splash_sparks),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 220.dp, height = 210.dp)
                        .graphicsLayer {
                            alpha = spark.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0.5f, 0.38f)
                            scaleX = 0.5f + 0.5f * spark
                            scaleY = 0.5f + 0.5f * spark
                        },
                )
            }
            Image(
                painterResource(R.drawable.splash_text),
                contentDescription = "Make It Fun Studio",
                modifier = Modifier
                    .width(260.dp)
                    .padding(top = 4.dp)
                    .graphicsLayer {
                        alpha = text
                        translationY = (1f - text) * 24.dp.toPx()
                    },
            )
        }
    }
}

// t 在 start～end 之間時從 0 走到 1
private fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

// 衝過頭一點再回來，看起來有彈性
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val p = x - 1f
    return 1f + c3 * p * p * p + c1 * p * p
}

private val SplashBackground = Color(0xFFFFF4E0)
