package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 掌上遊戲機的配色
private val Body = Color(0xFF5048A8)
private val BodyDark = Color(0xFF3A3480)
private val Bezel = Color(0xFF2A2A38)
private val Label = Color(0xFFC8C4F0)
private val ButtonRed = Color(0xFFC83048)
private val Led = Color(0xFFE03040)

// 把整個 App 放進掌上遊戲機的螢幕裡：上面是電源燈和機身字樣，下面是十字鍵和 A、B 鍵（裝飾用）
@Composable
fun GameFrame(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Body)
            // 機身鋪滿整個畫面，內容避開狀態列和導覽列
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Led))
            Text("  POWER", color = Label, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("PIXEL WORDS", color = Label, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("♪♪", color = Label, fontSize = 9.sp)
        }
        // 螢幕外圈（右下角較圓，像掌機的螢幕框）
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 28.dp))
                .background(Bezel)
                .padding(8.dp),
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
        Controls()
    }
}

// 下方的十字鍵、START/SELECT 和 A、B 鍵
@Composable
private fun Controls() {
    Row(
        Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DPad()
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(); Pill()
            }
            Text("SELECT  START", color = Label, fontSize = 8.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("B", Modifier.padding(top = 14.dp))
            RoundButton("A", Modifier.padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun DPad() {
    val arm = 16.dp
    Box(Modifier.size(arm * 3)) {
        Box(Modifier.align(Alignment.Center).width(arm * 3).height(arm).background(Bezel, RoundedCornerShape(3.dp)))
        Box(Modifier.align(Alignment.Center).width(arm).height(arm * 3).background(Bezel, RoundedCornerShape(3.dp)))
        Box(Modifier.align(Alignment.Center).size(arm / 2).background(BodyDark, CircleShape))
    }
}

@Composable
private fun Pill() {
    Box(Modifier.width(26.dp).height(7.dp).background(Bezel, RoundedCornerShape(4.dp)))
}

@Composable
private fun RoundButton(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.size(30.dp).clip(CircleShape).background(ButtonRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
