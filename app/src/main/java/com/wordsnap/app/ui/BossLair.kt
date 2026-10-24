package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.sin

// 魔王戰的背景（64 x 28，和一般場景一樣大）

// 大魔王：魔王城、會流動的熔岩河、骷髏
val BigBossLair = listOf(
    "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz",
    "zzzmzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzmzzzzzzzzzzzzzzzzzzzzzzz",
    "zzzzzzzzzzzzzzzzzzzzzzzzKzzzzzzzzzzzzzzKzzzzzzzzzzmrrrzzzzzzzzzz",
    "zzzzzzzzzzzzmzzzzzzzzzzKKKzzzzzzzzzzzzKKKzzzzzzzzzrRRRrzzzzzzzzz",
    "zzzzzzzzzzzzzzzzzzzzzzKKKKKzzzzKKzzzzKKKKKzzzzzzzzrRRRrzzzmzzzzz",
    "XXXXXXXXXXXXXXXXXXXXXXKxKxKXXXKKKKXXXKxKxKXXXXXXXXrRRRrXXXXXXXXX",
    "XXXXXXXXXXXXXXXXXXXXXXKKKKKmXKKKKKKXXKKKKKXXXXXXXXXrrrXXXXXXXXXX",
    "XXXXXXXmXXXXXXXXXXXXXXKKlKKXXKxKxKxXXKKlKKXXXXXXXXXXXXXXXXXXXXXX",
    "XXXXXXXXXXXXXXXXXXXXXXKKKKKXXKKKKKKXXKKKKKXXXXXXXXXXXXXXXXXXXXXX",
    "QQQQQQQQQQQQQQQQQQQQQQKKKKKKKKKlKKKKKKKKKKQQQQQQQQQQQQQQQQQQQQQQ",
    "QQQQQQQQQQQQQQQQQQQQQQKKKKKKKKKKKKKKKKKKKKQQQQQQQQQQQQQQQQQQlQQQ",
    "QQQQQQQQQQQQQQQQQQQQQQKKlKKKKlKKKKlKKKKlKKQQQQQQQQQQQQQQQQQQQQQQ",
    "QQlxxxxxQQQQQQQQQQQQQQKKKKKKKKKKKKKKKKKKKKQQQQQQQQQQQQQQQQQQQQQQ",
    "QQxxxxxxxxQQQQQQQQQQQKxKxKxKxKxKxKxKxKxKxKxQQQQQQQQQQQQlQQQQQQQQ",
    "rxxxxxxxxxxrrrrrrrrrrKKKKKKKKKKKKKKKKKKKKKKrrrxxxxxrrrrrrrrrrrrx",
    "xxxxxxxxxxxxrrrrrrrrrKKKKKKKKKrrrrKKKKKKKKKrrxxxxxxxxrrrrrrrrrxx",
    "xxxxxxxxxxxxxrrrrrrrrKKlKKKKKrrrrrrKKKKKlKKrxxxxxxxxxxrrrrrrrxxx",
    "xxxxxxxxxxxxxxrrrrrrxKKKKKKKKrrYYrrKKKKKKKKxxxxxxxxxxxxxrrrxxxxx",
    "xxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxX",
    "xxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxxxxxxxxxxXxx",
    "lllllrrrrrrrllOOOOOlllllllOOYYYOOlrrrrrrrllllllrrrrrrllOYYYYOOll",
    "llrrrrrrrrlOOOOOlllllllOOYYYOOlrrrrrrrllllllrrrrrrllOYYYYOOlllrl",
    "rrrrrrrlOOOOOOllllllOOYYYOOllrrrrrrllllllrrrrrrllOOYYYOOlllrlllO",
    "rrrrlOOOOOOllllllOOYYYOOllrrrrrrllllllrrrrrrrlOOYYYYOlllrlllOOOO",
    "KxxxxwwwxxKxxxxKxxxxKxxwwwxxxxKxxXxKxxxwwwXxxKxxxxKXxxxwwwxxKxxx",
    "xXxxwKwKwxXxxxxxxxxXxxwKwKwxXxxxxxxxxXwKwKwxxxXxxxxxxxwKwKwxxxxx",
    "xxxxwwwwwxxxxxwxwxxxxxwwwwwxxxwxwxxxxxwwwwwxxxwxwxXxxxwwwwwXxxxx",
    "XxxxxwKwxXxxxxwwwxXxxxxwKwxXxxwwwxxxXxxwKwxxxXwwwxxxxxXwKwxxxxxX",
)

// 熔岩河所在的列（不含 LAVA_BOTTOM）
private const val LAVA_TOP = 20
private const val LAVA_BOTTOM = 24

// 中魔王：有柱子、旗幟、火把的石造大廳（不會動）
val MidBossHall = listOf(
    "xmmKKKKKKmmmmmmmxmmmmKKKKKKmmmmmxmmmmKKKKKKmmmmmxmmmmmmKKKKKKmmm",
    "xmmKhhhsKmmmmmmmxmmmmKhhhsKmmmmmxmmmmKhhhsKmmmmmxmmmmmmKhhhsKmmm",
    "xxxxKhsKxxxxxxKKKKKKxxKhsKxxxxKKKKxxxxKhsKxxKKKKKKxxxxxxKhsKxxxx",
    "mmmmKhsKmmmmxmKrrrrKxmKhsKmmxKpqqpKmxmKhsKmmKrrrrKmmxmmmKhsKxmmm",
    "mmmmKhsKmmmmxmKrYYrKxmKhsKmmKpqLLqpKxmKhsKmmKrYYrKmmxmmmKhsKxmmm",
    "xxxxKhsKxxxxxxKrYYrKxxKhsKxxKqLLLLqKxxKhsKxxKrYYrKxxxxxxKhsKxxxx",
    "xmmmKhsKxmmmmmKrrrrKmmKhsKmmKpqLLqpKmmKhsKmmKrrrrKmmmmmmKhsKmmmm",
    "xmmmKhsKxmmYmmKrrrrKYmKhsKmmKqLLLLqKmmKhsKmYKrrrrKmmYmmmKhsKmmmm",
    "xxxxKhsKxxYOYxKrrrrYOYKhsKxxKpqLLqpKxxKhsKYOYrrrrKxYOYxxKhsKxxxx",
    "mmmmKhsKmmmOxmmKrrKmOmKhsKmmKKKKKKKKxmKhsKmOxKrrKmmmOmmmKhsKxmmm",
    "mmmmKhsKmmKDKmmmKKmKDKKhsKmmxmmmmmmmxmKhsKKDKmKKmmmKDKmmKhsKxmmm",
    "xxxxKhsKxxxDxxxxxxxxDxKhsKxxxxxxxxxxxxKhsKxDxxxxxxxxDxxxKhsKxxxx",
    "xmmmKhsKxmmDmmmmxmmmDmKhsKmmmmmmxmmmmmKhsKmDmmmmxmmmDmmmKhsKmmmm",
    "xmmmKhsKxmmmmmmmxmmmmmKhsKmmmmmmxmmmmmKhsKmmmmmmxmmmmmmmKhsKmmmm",
    "xxxxKhsKxxxxxxxxxxxxxxKhsKxxxxxxxxxxxxKhsKxxxxxxxxxxxxxxKhsKxxxx",
    "mmmmKhsKmmmmxmmmmmmmxmKhsKmmxmmmmmmmxmKhsKmmxmmmmmmmxmmmKhsKxmmm",
    "mmmmKhsKmmmmxmmmmmmmxmKhsKmmxmmmmmmmxmKhsKmmxmmmmmmmxmmmKhsKxmmm",
    "xxxxKhsKxxxxxxxxxxxxxxKhsKxxxxxxxxxxxxKhsKxxxxxxxxxxxxxxKhsKxxxx",
    "xmmKhhhsKmmmmmmmxmmmmKhhhsKmmmmmxmmmmKhhhsKmmmmmxmmmmmmKhhhsKmmm",
    "xmmKKKKKKmmmmmmmxmmmmKKKKKKmmmmmxmmmmKKKKKKmmmmmxmmmmmmKKKKKKmmm",
    "xxxxXXXXxxxxXXXXxxxxXXXXxxxxYrrrrrrYXXXXxxxxXXXXxxxxXXXXxxxxXXXX",
    "xxxxXXXXxxxxXXXXxxxxXXXXxxxYrrrrrrrrYXXXxxxxXXXXxxxxXXXXxxxxXXXX",
    "XXXXxxxxXXXXxxxxXXXXxxxxXXYrrrrrrrrrrYxxXXXXxxxxXXXXxxxxXXXXxxxx",
    "XXXXxxxxXXXXxxxxXXXXxxxxXYrrrrrrrrrrrrYxXXXXxxxxXXXXxxxxXXXXxxxx",
    "xxxxXXXXxxxxXXXXxxxxXXXXYrrrrrrrrrrrrrrYxxxxXXXXxxxxXXXXxxxxXXXX",
    "xxxxXXXXxxxxXXXXxxxxXXXYrrrrrrrrrrrrrrrrYxxxXXXXxxxxXXXXxxxxXXXX",
    "XXXXxxxxXXXXxxxxXXXXxxYrrrrrrrrrrrrrrrrrrYXXxxxxXXXXxxxxXXXXxxxx",
    "XXXXxxxxXXXXxxxxXXXXxYrrrrrrrrrrrrrrrrrrrrYXxxxxXXXXxxxxXXXXxxxx",
)

// 戰鬥背景：一般怪物用隨機場景，魔王用專屬背景
@Composable
fun BattleBackdrop(scene: ReviewScene, rank: MonsterRank, modifier: Modifier = Modifier) {
    when (rank) {
        MonsterRank.Normal -> PixelArt(scene.art, ScenePalette, modifier.fillMaxWidth())
        MonsterRank.Mid -> PixelArt(MidBossHall, ScenePalette, modifier.fillMaxWidth())
        MonsterRank.Big -> Box(modifier.fillMaxWidth()) {
            PixelArt(BigBossLair, ScenePalette, Modifier.fillMaxWidth())
            LavaRiver(Modifier.matchParentSize())
        }
    }
}

// 熔岩一格一格往左流，亮的地方會變黃
@Composable
private fun LavaRiver(modifier: Modifier) {
    val flow = rememberInfiniteTransition(label = "lava")
    val time by flow.animateFloat(
        0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing), RepeatMode.Restart), label = "time",
    )
    // 每秒換 6 張，看起來像像素遊戲
    val frame = (time * 6).toInt()
    val columns = BigBossLair.first().length
    Canvas(modifier) {
        val cell = size.width / columns
        for (y in LAVA_TOP until LAVA_BOTTOM) {
            for (x in 0 until columns) {
                val v = sin(x * 0.45f - frame * 0.6f + y * 1.3f) + 0.7f * sin(x * 0.17f - frame * 0.35f + y * 0.5f)
                val ch = when {
                    v > 1.2f -> 'Y'
                    v > 0.5f -> 'O'
                    v > -0.4f -> 'l'
                    else -> 'r'
                }
                drawRect(ScenePalette.getValue(ch), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}
