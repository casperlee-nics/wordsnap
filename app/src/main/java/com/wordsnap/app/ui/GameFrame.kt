package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 早期黑白掌上遊戲機的米白色配色（原創設計，不使用任何官方圖案）
private val Body = Color(0xFFD8D4CC)
private val BodyShade = Color(0xFFBDB8AE)
private val Bezel = Color(0xFF5C6070)
private val Stripe1 = Color(0xFF8A3A6A)
private val Stripe2 = Color(0xFF2C3A7A)
private val Label = Color(0xFF283C8C)
private val BezelText = Color(0xFFC8CAD4)
private val Keys = Color(0xFF2A2A30)
private val ButtonRed = Color(0xFFA0285A)
private val Led = Color(0xFFE03040)
private val Backdrop = Color(0xFF1A1A1E)

// 把整個 App 放進米白色掌上遊戲機的螢幕裡：螢幕框上有電源燈和兩條細線，
// 下面是機身字樣、十字鍵、A/B 鍵和喇叭孔（裝飾用）；機身右下角是大圓角
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameFrame(content: @Composable () -> Unit) {
    // 打字時把下方按鍵收起來，讓出空間給輸入框
    val typing = WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize().background(Backdrop)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomEnd = if (typing) 0.dp else 40.dp))
                .background(Body)
                // 機身鋪滿整個畫面，內容避開狀態列、導覽列和鍵盤
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp),
        ) {
            // 機身最上方的分隔線，像機殼上的刻痕
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp).background(BodyShade))
            Spacer(Modifier.height(6.dp))
            // 螢幕外框：深灰色，右下角較圓
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 32.dp))
                    .background(Bezel)
                    .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
            ) {
                BezelHeader()
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 左邊的電源燈
                    Column(
                        Modifier.width(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Led))
                        Text("POWER", color = BezelText, fontSize = 5.sp)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(2.dp, Color.Black)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    ) {
                        content()
                    }
                    Spacer(Modifier.width(10.dp))
                }
            }
            if (typing) {
                Spacer(Modifier.height(6.dp))
            } else {
                Text(
                    "PIXEL WORDS",
                    color = Label,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
                Controls()
            }
        }
    }
}

// 螢幕框上方的兩條彩色細線和小字
@Composable
private fun BezelHeader() {
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Stripe1))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Stripe2))
        }
        Text("  WORD MATRIX  ", color = BezelText, fontSize = 7.sp)
        Column(Modifier.width(24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Stripe1))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Stripe2))
        }
    }
}

// 下方的十字鍵、A/B 鍵、SELECT/START 和右下角的喇叭孔
@Composable
private fun Controls() {
    Box(Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 10.dp)) {
        DPad(Modifier.align(Alignment.TopStart).padding(top = 4.dp))
        Row(
            Modifier.align(Alignment.TopEnd).padding(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundButton("B", Modifier.padding(top = 14.dp))
            RoundButton("A", Modifier.padding(top = 2.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill(); Pill()
            }
            Text("SELECT   START", color = Label, fontSize = 7.sp)
        }
        Speaker(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 2.dp))
    }
}

@Composable
private fun DPad(modifier: Modifier = Modifier) {
    val arm = 13.dp
    Box(modifier.size(arm * 3)) {
        Box(Modifier.align(Alignment.Center).width(arm * 3).height(arm).background(Keys, RoundedCornerShape(2.dp)))
        Box(Modifier.align(Alignment.Center).width(arm).height(arm * 3).background(Keys, RoundedCornerShape(2.dp)))
        Box(Modifier.align(Alignment.Center).size(arm / 2).background(Color(0xFF45454D), CircleShape))
    }
}

@Composable
private fun Pill() {
    Box(
        Modifier
            .rotate(-25f)
            .width(24.dp)
            .height(7.dp)
            .background(Color(0xFF8C8A90), RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun RoundButton(label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(ButtonRed))
        Text(label, color = Label, fontSize = 9.sp)
    }
}

// 斜斜的喇叭孔
@Composable
private fun Speaker(modifier: Modifier = Modifier) {
    Row(modifier.rotate(-28f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(5) {
            Box(Modifier.width(4.dp).height(26.dp).background(BodyShade, RoundedCornerShape(2.dp)))
        }
    }
}
