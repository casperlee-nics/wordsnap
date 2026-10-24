package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk

// 在羊皮書卷上寫暱稱：書卷邊緣是不規則的破邊，左右兩端捲起來；右邊一顆淺色木頭印章，按下去像蓋章確認
@Composable
fun ScrollNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canConfirm = value.isNotBlank()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScrollTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            onDone = { if (canConfirm) onConfirm() },
            modifier = Modifier.weight(1f),
        )
        SealButton(enabled = canConfirm, onClick = onConfirm, modifier = Modifier.padding(start = 10.dp))
    }
}

// 不規則羊皮書卷的輸入框（沒有印章），手動新增單字也用這個
@Composable
fun ScrollTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    height: Dp = 76.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier
            .height(height)
            .drawBehind { drawScroll() }
            .padding(start = 28.dp, end = 26.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            // 提示字只排一行，太長就縮小字，不會換行
            Text(
                placeholder,
                color = ScrollHint,
                style = if (placeholder.length > 7) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            // 文字大小設定成「大」時，輸入的字放大 1.2 倍
            textStyle = textStyle.readable().copy(color = PixelInk),
            cursorBrush = SolidColor(PixelInk),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = keyboardType),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// 淺色木頭印章：外圈不規則、木紋、左上亮右下暗，中間「確定」；按下去會往下壓
@Composable
fun SealButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, label: String = tr(R.string.confirm)) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(58.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .graphicsLayer {
                val s = if (pressed) 0.88f else 1f
                scaleX = s
                scaleY = s
                rotationZ = -8f
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind { drawSeal() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = SealText, style = MaterialTheme.typography.titleMedium)
    }
}

// 羊皮書卷：上下邊緣一格一格高低不平，左右兩端是捲起來的圓筒
private fun DrawScope.drawScroll() {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    val roll = px * 5
    // 紙張本體，上下邊緣不規則
    val random = java.util.Random(3)
    var x = roll
    while (x < w - roll) {
        val top = px * (1 + random.nextInt(3))
        val bottom = h - px * (1 + random.nextInt(3))
        drawRect(PixelInk, Offset(x, top - px), Size(px + 0.5f, bottom - top + px * 2))
        drawRect(ScrollPaper, Offset(x, top), Size(px + 0.5f, bottom - top))
        // 紙上的斑點和邊緣的陰影
        drawRect(ScrollShade, Offset(x, bottom - px), Size(px + 0.5f, px))
        if (random.nextInt(5) == 0) drawRect(ScrollSpot, Offset(x, top + px * (2 + random.nextInt(8)).coerceAtMost(((bottom - top) / px).toInt() - 2)), Size(px, px))
        x += px
    }
    // 寫字的底線
    drawRect(ScrollLine, Offset(roll + px * 2, h * 0.72f), Size(w - roll * 2 - px * 4, px * 0.6f))
    // 左右兩端捲起來的圓筒
    for (left in listOf(0f, w - roll)) {
        drawRect(PixelInk, Offset(left, px), Size(roll, h - px * 2))
        drawRect(ScrollRoll, Offset(left + px * 0.7f, px * 1.7f), Size(roll - px * 1.4f, h - px * 3.4f))
        drawRect(ScrollRollLight, Offset(left + px * 1.4f, px * 1.7f), Size(px, h - px * 3.4f))
        drawRect(ScrollShade, Offset(left + roll - px * 2f, px * 1.7f), Size(px, h - px * 3.4f))
    }
}

private fun DrawScope.drawSeal() {
    val c = Offset(size.width / 2, size.height / 2)
    val r = size.minDimension / 2
    // 右下一點點影子，看起來微微浮起
    drawCircle(SealShadow, r * 0.9f, Offset(c.x + r * 0.07f, c.y + r * 0.08f))
    // 外圈不規則的邊
    for (k in 0 until 8) {
        val a = k * Math.PI / 4
        drawCircle(SealEdge, r * 0.2f, Offset(c.x + (r * 0.74f * kotlin.math.cos(a)).toFloat(), c.y + (r * 0.74f * kotlin.math.sin(a)).toFloat()))
    }
    drawCircle(SealEdge, r * 0.84f, c)
    drawCircle(SealWax, r * 0.78f, c)
    // 木紋：幾條彎彎的深色細紋
    for (k in -2..2) {
        val y = c.y + k * r * 0.26f
        val half = kotlin.math.sqrt((r * 0.7f) * (r * 0.7f) - (k * r * 0.26f) * (k * r * 0.26f))
        drawLine(SealGrain, Offset(c.x - half, y + r * 0.04f * k), Offset(c.x + half, y - r * 0.05f), strokeWidth = r * 0.04f)
    }
    // 左上亮、右下暗，帶一點點立體感
    drawArc(SealLight, 150f, 120f, false, Offset(c.x - r * 0.72f, c.y - r * 0.72f), Size(r * 1.44f, r * 1.44f), style = Stroke(r * 0.08f))
    drawArc(SealDark, -30f, 120f, false, Offset(c.x - r * 0.72f, c.y - r * 0.72f), Size(r * 1.44f, r * 1.44f), style = Stroke(r * 0.08f))
    // 中間壓出來的圓框
    drawCircle(SealDark, r * 0.58f, c, style = Stroke(r * 0.05f))
}

private val ScrollPaper = Color(0xFFEFDDAE)
private val ScrollShade = Color(0xFFCDB27A)
private val ScrollSpot = Color(0xFFD8C08A)
private val ScrollLine = Color(0xFFB09060)
private val ScrollRoll = Color(0xFFD9BE84)
private val ScrollRollLight = Color(0xFFF6E6BE)
private val ScrollHint = Color(0xFF9C8458)
private val SealWax = Color(0xFFC89A62)
private val SealEdge = Color(0xFF7A5230)
private val SealDark = Color(0xFF8C6238)
private val SealLight = Color(0xFFE8C894)
private val SealGrain = Color(0xFFA87A48)
private val SealShadow = Color(0x55301C0C)
private val SealText = Color(0xFF3A2410)
