package com.wordsnap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelPaper

// 戰鬥、朗讀、複習畫面上方的標題：深色點點底加上像素風的雙線金框，和下方場景連在一起
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelHeader(title: String, onBack: () -> Unit, backDescription: String = "返回") {
    Box(
        Modifier
            .fillMaxWidth()
            .background(WindowTitleDark)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
    ) {
        Canvas(Modifier.matchParentSize()) { drawHeaderPanel() }
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription, tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
            )
        }
    }
}

// 像素遊戲風的視窗：羊皮紙底、黑金雙線外框、上方深色標題列，下面一排像素按鈕
@Composable
fun PixelWindowDialog(
    title: String,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .pixelWindowFrame()
                .padding(10.dp),
        ) {
            WindowTitle(title)
            CompositionLocalProvider(LocalContentColor provides PixelInk) {
                if (header != null) Box(Modifier.padding(top = 10.dp, start = 6.dp, end = 6.dp)) { header() }
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PixelWindowButton("關閉", onDismiss)
            }
        }
    }
}

// 有像素外框的小按鈕，像遊戲選單裡的「▶ 關閉」
@Composable
fun PixelWindowButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .drawBehind {
                val px = 2.dp.toPx()
                drawRect(WindowTitleDark, Offset(px, px), Size(size.width - px * 2, size.height - px * 2))
                drawRect(PixelGold, Offset(px, px), Size(size.width - px * 2, size.height - px * 2), style = Stroke(px))
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text("▶ $label", color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WindowTitle(title: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(WindowTitleDark)
            drawHeaderPanel()
        }
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
        )
    }
}

// 深色底上的小點點花紋，外面一圈金色、隔一格、裡面一圈淡金色
private fun DrawScope.drawHeaderPanel() {
    val px = 2.dp.toPx()
    var y = px * 3
    while (y < size.height) {
        var x = px * 3 + if (((y / (px * 6)).toInt()) % 2 == 0) 0f else px * 3
        while (x < size.width) {
            drawRect(WindowTitleDot, Offset(x, y), Size(px, px))
            x += px * 6
        }
        y += px * 6
    }
    drawRect(PixelGold, Offset(px, px), Size(size.width - px * 2, size.height - px * 2), style = Stroke(px))
    drawRect(WindowGoldLight, Offset(px * 4, px * 4), Size(size.width - px * 8, size.height - px * 8), style = Stroke(px))
}

// 像素視窗外框，主畫面選單也用這個
fun Modifier.pixelWindowFrame(): Modifier = drawBehind { drawWindowFrame() }

// 外框：缺角的黑框、金色一圈、再一圈細黑線，裡面是羊皮紙
private fun DrawScope.drawWindowFrame() {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    // 四個角各缺一格，看起來像像素畫出來的圓角
    drawRect(PixelInk, Offset(px, 0f), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(0f, px), Size(w, h - px * 2))
    drawRect(PixelGold, Offset(px * 2, px), Size(w - px * 4, h - px * 2))
    drawRect(PixelGold, Offset(px, px * 2), Size(w - px * 2, h - px * 4))
    drawRect(PixelInk, Offset(px * 2, px * 2), Size(w - px * 4, h - px * 4))
    drawRect(PixelPaper, Offset(px * 3, px * 3), Size(w - px * 6, h - px * 6))
}

private val WindowTitleDark = Color(0xFF302848)
private val WindowTitleDot = Color(0xFF483868)
private val WindowGoldLight = Color(0xFFF8F0A0)
