package com.wordsnap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelPaper
import com.wordsnap.app.ui.theme.PixelWhite

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
        Canvas(Modifier.matchParentSize()) { drawHeaderBevel() }
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp),
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

// 深色底上的小點點花紋，外面只有一圈金色（不要框內框）
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
}

// 戰鬥、朗讀、複習的標題列：點點花紋＋粗的立體金框（左上亮、右下暗，像浮起來的金屬邊），只有一層
private fun DrawScope.drawHeaderBevel() {
    val dot = 2.dp.toPx()
    var y = dot * 3
    while (y < size.height) {
        var x = dot * 3 + if (((y / (dot * 6)).toInt()) % 2 == 0) 0f else dot * 3
        while (x < size.width) {
            drawRect(WindowTitleDot, Offset(x, y), Size(dot, dot))
            x += dot * 6
        }
        y += dot * 6
    }
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    // 最外圈黑線
    drawRect(PixelInk, Offset.Zero, Size(w, h), style = Stroke(px * 2))
    // 金色粗邊：上、左亮金，下、右暗金
    drawRect(BevelLight, Offset(px, px), Size(w - px * 2, px))
    drawRect(BevelLight, Offset(px, px), Size(px, h - px * 2))
    drawRect(BevelDark, Offset(px, h - px * 2), Size(w - px * 2, px))
    drawRect(BevelDark, Offset(w - px * 2, px), Size(px, h - px * 2))
    drawRect(PixelGold, Offset(px * 2, px * 2), Size(w - px * 4, px))
    drawRect(PixelGold, Offset(px * 2, px * 2), Size(px, h - px * 4))
    drawRect(BevelMid, Offset(px * 2, h - px * 3), Size(w - px * 4, px))
    drawRect(BevelMid, Offset(w - px * 3, px * 2), Size(px, h - px * 4))
    // 內側一條黑線，讓金邊浮起來
    drawRect(PixelInk, Offset(px * 3, px * 3), Size(w - px * 6, h - px * 6), style = Stroke(1.dp.toPx()))
}

private val BevelLight = Color(0xFFFFE890)
private val BevelMid = Color(0xFFC88820)
private val BevelDark = Color(0xFF805010)

// 像素視窗外框，主畫面選單也用這個
fun Modifier.pixelWindowFrame(): Modifier = pixelFrame(PixelPaper)

// 像素外框：缺角的黑框、金色一圈、再一圈細黑線，裡面填 fill；unit 是一格像素的大小
fun Modifier.pixelFrame(fill: Color, accent: Color = PixelGold, unit: Dp = 3.dp): Modifier = drawBehind {
    val px = unit.toPx()
    val w = size.width
    val h = size.height
    // 四個角各缺一格，看起來像像素畫出來的圓角
    drawRect(PixelInk, Offset(px, 0f), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(0f, px), Size(w, h - px * 2))
    drawRect(accent, Offset(px * 2, px), Size(w - px * 4, h - px * 2))
    drawRect(accent, Offset(px, px * 2), Size(w - px * 2, h - px * 4))
    drawRect(PixelInk, Offset(px * 2, px * 2), Size(w - px * 4, h - px * 4))
    drawRect(fill, Offset(px * 3, px * 3), Size(w - px * 6, h - px * 6))
}

// 有像素外框的卡片，取代單線黑框的 Surface
@Composable
fun PixelSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    unit: Dp = 3.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RectangleShape,
        color = Color.Transparent,
        contentColor = contentColorFor(color),
        modifier = modifier.pixelFrame(color, unit = unit),
        content = content,
    )
}

// 像素風的對話框，參數和 AlertDialog 一樣
@Composable
fun PixelAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier.fillMaxWidth().pixelWindowFrame().padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides PixelInk) {
                if (title != null) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
                }
                if (text != null) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

// 外框按鈕（例如「讀取遊戲」）也改成像素框
@Composable
fun PixelOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        border = null,
        contentPadding = contentPadding,
        modifier = modifier.alpha(if (enabled) 1f else 0.45f).pixelFrame(PixelWhite, unit = 2.dp),
        content = content,
    )
}

private val WindowTitleDark = Color(0xFF302848)
private val WindowTitleDot = Color(0xFF483868)

// 復古按鈕（主畫面選單、單字簿下方工具列）：復古的黃褐色像素方塊，左上亮邊、右下暗邊加上影子，看起來微微浮起；按下去會往下沉
@Composable
fun RetroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    RetroButtonFrame(onClick, modifier, enabled) { lift ->
        Text(
            label,
            color = ButtonText,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = 1.5.dp - lift, y = 1.5.dp - lift).padding(horizontal = 12.dp),
        )
    }
}

// 只有像素圖示的復古按鈕（例如打開冒險日誌清單）
@Composable
fun RetroIconButton(
    icon: List<String>,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: Map<Char, Color> = ColorIconPalette,
) {
    RetroButtonFrame(onClick, modifier.width(48.dp).semantics { this.contentDescription = contentDescription }, true) { lift ->
        PixelArt(icon, palette, Modifier.offset(x = 1.5.dp - lift, y = 1.5.dp - lift).size(30.dp))
    }
}

@Composable
private fun RetroButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable (lift: Dp) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift = if (pressed) 0.dp else 3.dp
    Box(
        modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .drawBehind {
                val px = 3.dp.toPx()
                val up = lift.toPx()
                val w = size.width - 3.dp.toPx()
                val h = size.height - 3.dp.toPx()
                // 影子：往右下偏一點的深褐色方塊
                if (up > 0f) {
                    drawRect(ButtonShadow, Offset(px * 2, px * 2), Size(w - px, h - px))
                }
                val top = 3.dp.toPx() - up
                val left = 3.dp.toPx() - up
                // 缺角的黑框
                drawRect(PixelInk, Offset(left + px, top), Size(w - px * 2, h))
                drawRect(PixelInk, Offset(left, top + px), Size(w, h - px * 2))
                // 黃褐色本體
                drawRect(ButtonBody, Offset(left + px, top + px), Size(w - px * 2, h - px * 2))
                // 左上亮邊、右下暗邊
                drawRect(ButtonLight, Offset(left + px, top + px), Size(w - px * 2, px))
                drawRect(ButtonLight, Offset(left + px, top + px), Size(px, h - px * 2))
                drawRect(ButtonDark, Offset(left + px, top + h - px * 2), Size(w - px * 2, px))
                drawRect(ButtonDark, Offset(left + w - px * 2, top + px), Size(px, h - px * 2))
            },
        contentAlignment = Alignment.Center,
    ) {
        content(lift)
    }
}

// 羊皮紙紋路：米黃底、深淺斑點和幾條纖維紋，底下一條黑線
fun Modifier.parchment(): Modifier = drawBehind {
    drawRect(ParchmentBase)
    val px = 2.dp.toPx()
    val cols = (size.width / px).toInt()
    val rows = (size.height / px).toInt()
    // 固定的亂數，每次畫出來的紋路都一樣
    val random = java.util.Random(7)
    repeat(cols * rows / 14) {
        val x = random.nextInt(cols.coerceAtLeast(1)) * px
        val y = random.nextInt(rows.coerceAtLeast(1)) * px
        drawRect(if (random.nextBoolean()) ParchmentDark else ParchmentLight, Offset(x, y), Size(px, px))
    }
    repeat(rows / 6 + 1) {
        val y = random.nextInt(rows.coerceAtLeast(1)) * px
        val x = random.nextInt(cols.coerceAtLeast(1)) * px
        drawRect(ParchmentFiber, Offset(x, y), Size(px * (8 + random.nextInt(16)), px))
    }
    drawRect(ParchmentEdge, Offset(0f, size.height - px * 3), Size(size.width, px * 2))
    drawRect(PixelInk, Offset(0f, size.height - px), Size(size.width, px))
}

private val ParchmentBase = Color(0xFFEAD7A8)
private val ParchmentLight = Color(0xFFF6E8C4)
private val ParchmentDark = Color(0xFFD2B67C)
private val ParchmentFiber = Color(0xFFDCC48E)
private val ParchmentEdge = Color(0xFFB8945A)

private val ButtonBody = Color(0xFFC89858)
private val ButtonLight = Color(0xFFF0D098)
private val ButtonDark = Color(0xFF8C5C28)
private val ButtonShadow = Color(0xFF4A3018)
private val ButtonText = Color(0xFF3A2410)
