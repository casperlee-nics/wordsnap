package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 仿第一代 Switch 的配色：左邊電光藍、右邊電光紅、中間深灰機身
private val NeonBlue = Color(0xFF00BBDD)
private val NeonRed = Color(0xFFFF4B55)
private val Body = Color(0xFF3A3A3E)
private val Bezel = Color(0xFF1E1E22)
private val Label = Color(0xFFB8B8C0)
private val Key = Color(0xFF2A2A2E)

private val SideWidth = 10.dp

// 把整個 App 放進掌上遊戲機的螢幕裡：上面是機身字樣，下面是兩邊手把的按鍵（裝飾用）
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameFrame(content: @Composable () -> Unit) {
    // 打字時把下方按鍵收起來，讓出空間給輸入框
    val typing = WindowInsets.isImeVisible
    Column(
        Modifier
            .fillMaxSize()
            .drawBehind {
                // 左右兩側畫成藍、紅手把
                val side = SideWidth.toPx()
                drawRect(Body)
                drawRect(NeonBlue, size = Size(side, size.height))
                drawRect(NeonRed, topLeft = Offset(size.width - side, 0f), size = Size(side, size.height))
            }
            // 機身鋪滿整個畫面，內容避開狀態列、導覽列和鍵盤
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = SideWidth + 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text("PIXEL WORDS", color = Label, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
        }
        // 螢幕外圈
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Bezel)
                .padding(6.dp),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.Black)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(2.dp)),
            ) {
                content()
            }
        }
        if (typing) Spacer(Modifier.height(6.dp)) else Controls()
    }
}

// 下方的按鍵：左手把是方向鍵和 －，右手把是 ＋ 和 X、Y、A、B
@Composable
private fun Controls() {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 22.dp)).background(NeonBlue),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Diamond(listOf("", "", "", ""), Modifier.padding(start = 16.dp))
            Spacer(Modifier.weight(1f))
            Text("－", color = Key, fontSize = 14.sp, modifier = Modifier.padding(end = 14.dp))
        }
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 22.dp)).background(NeonRed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("＋", color = Key, fontSize = 14.sp, modifier = Modifier.padding(start = 14.dp))
            Spacer(Modifier.weight(1f))
            Diamond(listOf("X", "Y", "A", "B"), Modifier.padding(end = 16.dp))
        }
    }
}

// 四顆按鍵排成菱形，順序是上、左、右、下
@Composable
private fun Diamond(labels: List<String>, modifier: Modifier = Modifier, key: Dp = 13.dp) {
    Box(modifier.size(key * 3)) {
        val spots = listOf(Alignment.TopCenter, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter)
        spots.zip(labels).forEach { (spot, label) ->
            Box(
                Modifier.align(spot).size(key).clip(CircleShape).background(Key),
                contentAlignment = Alignment.Center,
            ) {
                if (label.isNotEmpty()) Text(label, color = Label, fontSize = 7.sp)
            }
        }
    }
}
