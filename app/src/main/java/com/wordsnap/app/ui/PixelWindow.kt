package com.wordsnap.app.ui

import androidx.compose.foundation.Canvas
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

// 標題列的底色和文字顏色：每個畫面各有一種中世紀風格的花紋
enum class HeaderTheme(val text: Color, val draw: DrawScope.() -> Unit) {
    // 羊皮紙＋黑字
    Parchment(PixelInk, { drawParchment() }),
    // 戰鬥：深色石牆＋白字
    Stone(Color.White, { drawStone() }),
    // 朗讀：酒紅色掛毯，金色菱格花紋＋米金色字
    Tapestry(Color(0xFFFFE8A8), { drawTapestry() }),
    // 複習：森林綠旗幟布，淡色十字紋＋米白字
    Banner(Color(0xFFF8F0D8), { drawBanner() }),
    // 記錄管理：深褐皮革，邊緣有一圈縫線＋米黃字
    Leather(Color(0xFFF8E0A8), { drawLeather() }),
}

// 戰鬥、朗讀、複習、記錄管理畫面上方的標題：依 theme 換底色，外面一圈深色木紋框
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelHeader(
    title: String,
    onBack: () -> Unit,
    backDescription: String = "返回",
    theme: HeaderTheme = HeaderTheme.Parchment,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind { theme.draw(this) }
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
    ) {
        Canvas(Modifier.matchParentSize()) { drawWoodFrame() }
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription, tint = theme.text)
            }
            Text(
                title,
                color = theme.text,
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

// 有像素外框的小按鈕，像遊戲選單裡的「關閉」
@Composable
fun PixelWindowButton(label: String, onClick: () -> Unit) {
    RetroButton(label, onClick, textStyle = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun WindowTitle(title: String) {
    // 視窗標題也是羊皮紙底、黑字
    Box(Modifier.fillMaxWidth().parchment(), contentAlignment = Alignment.Center) {
        Text(
            title,
            color = PixelInk,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
        )
    }
}


// 標題列的深色木紋框：外圈黑線、木頭本體（左上稍亮、右下稍暗，中間有木紋），內圈細黑線
private fun DrawScope.drawWoodFrame() {
    val px = 2.dp.toPx()
    val w = size.width
    val h = size.height
    val band = px * 3
    // 木頭本體
    drawRect(WoodMid, Offset.Zero, Size(w, band))
    drawRect(WoodMid, Offset(0f, h - band), Size(w, band))
    drawRect(WoodMid, Offset.Zero, Size(band, h))
    drawRect(WoodMid, Offset(w - band, 0f), Size(band, h))
    // 左上亮、右下暗，看起來有一點立體
    drawRect(WoodLight, Offset(px, px), Size(w - px * 2, px))
    drawRect(WoodLight, Offset(px, px), Size(px, h - px * 2))
    drawRect(WoodDark, Offset(px, h - px * 2), Size(w - px * 2, px))
    drawRect(WoodDark, Offset(w - px * 2, px), Size(px, h - px * 2))
    // 木紋：上下兩條邊上斷斷續續的深色短線
    var x = px * 4
    var k = 0
    while (x < w - px * 8) {
        val len = px * (3 + (k * 7) % 5)
        drawRect(WoodGrain, Offset(x, if (k % 2 == 0) px else px * 2), Size(len, px * 0.5f + 1f))
        drawRect(WoodGrain, Offset(x + px * 3, h - band + if (k % 2 == 0) px else px * 1.5f), Size(len, px * 0.5f + 1f))
        x += len + px * (4 + (k * 3) % 4)
        k++
    }
    // 外圈和內圈黑線
    drawRect(PixelInk, Offset.Zero, Size(w, h), style = Stroke(px))
    drawRect(PixelInk, Offset(band, band), Size(w - band * 2, h - band * 2), style = Stroke(1.dp.toPx()))
}

private val WoodLight = Color(0xFF9C6A3C)
private val WoodMid = Color(0xFF7A4E28)
private val WoodDark = Color(0xFF4E3018)
private val WoodGrain = Color(0xFF5A381C)

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

// 和標題列同一種深色木紋細框，裡面填滿底色（卡片、選字框、外框按鈕都用這個）
fun Modifier.woodFrame(fill: Color): Modifier = drawBehind {
    drawRect(fill)
    drawWoodFrame()
}

// 有木紋細框的卡片，取代單線黑框的 Surface（unit 留著相容舊的呼叫，木框粗細固定）
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
        modifier = modifier.woodFrame(color),
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
        modifier = modifier.alpha(if (enabled) 1f else 0.45f).woodFrame(PixelWhite),
        content = content,
    )
}


// 復古按鈕（主畫面選單、單字簿下方工具列）：復古的黃褐色像素方塊，左上亮邊、右下暗邊加上影子，看起來微微浮起；按下去會往下沉
@Composable
fun RetroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    // 文字左右留白；下方工具列一排四顆比較窄，要留少一點
    textPadding: Dp = 12.dp,
) {
    RetroButtonFrame(onClick, modifier, enabled) { lift ->
        Text(
            label,
            color = ButtonText,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = 1.5.dp - lift, y = 1.5.dp - lift).padding(horizontal = textPadding),
        )
    }
}

// 文字加右邊一個小圖示，同一個復古按鈕框（例如冒險日誌名稱＋下拉捲軸）
@Composable
fun RetroButtonWithIcon(
    label: String,
    icon: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    palette: Map<Char, Color> = ColorIconPalette,
) {
    RetroButtonFrame(onClick, modifier, true) { lift ->
        Row(
            Modifier.offset(x = 1.5.dp - lift, y = 1.5.dp - lift).padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = ButtonText,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            PixelArt(icon, palette, Modifier.padding(start = 4.dp).size(26.dp))
        }
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
fun Modifier.parchment(): Modifier = drawBehind { drawParchment() }

private fun DrawScope.drawParchment() {
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

// 石牆：一排一排錯開的石塊，石塊顏色深淺不一，上緣有一條亮邊
private fun DrawScope.drawStone() {
    drawRect(StoneMortar)
    val px = 2.dp.toPx()
    val rowH = px * 7
    val random = java.util.Random(11)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else -px * 6
        while (x < size.width) {
            val w = px * (10 + random.nextInt(8))
            val color = StoneColors[random.nextInt(StoneColors.size)]
            drawRect(color, Offset(x + px * 0.5f, y + px * 0.5f), Size(w - px, rowH - px))
            drawRect(StoneLight, Offset(x + px * 0.5f, y + px * 0.5f), Size(w - px, px * 0.75f))
            // 石頭上的小斑點
            drawRect(StoneSpeck, Offset(x + px * (2 + random.nextInt(6)), y + px * (2 + random.nextInt(3))), Size(px, px))
            x += w
        }
        y += rowH
        row++
    }
}

// 掛毯：酒紅底，金色小點排成菱格
private fun DrawScope.drawTapestry() {
    drawRect(TapestryBase)
    val px = 2.dp.toPx()
    val step = px * 6
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else step / 2
        while (x < size.width) {
            drawRect(TapestryDark, Offset(x, y), Size(px * 2, px * 2))
            drawRect(TapestryGold, Offset(x + step / 2, y + step / 2), Size(px, px))
            x += step
        }
        y += step / 2
        row++
    }
}

// 旗幟布：森林綠底，淡淡的直條紋和小十字
private fun DrawScope.drawBanner() {
    drawRect(BannerBase)
    val px = 2.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawRect(BannerStripe, Offset(x, 0f), Size(px, size.height))
        x += px * 4
    }
    var y = px * 4
    var row = 0
    while (y < size.height) {
        var cx = px * (6 + (row % 2) * 8)
        while (cx < size.width) {
            drawRect(BannerCross, Offset(cx - px, y), Size(px * 3, px))
            drawRect(BannerCross, Offset(cx, y - px), Size(px, px * 3))
            cx += px * 16
        }
        y += px * 8
        row++
    }
}

// 皮革：深褐底，深淺斑紋，內側一圈虛線縫線
private fun DrawScope.drawLeather() {
    drawRect(LeatherBase)
    val px = 2.dp.toPx()
    val random = java.util.Random(5)
    val cols = (size.width / px).toInt().coerceAtLeast(1)
    val rows = (size.height / px).toInt().coerceAtLeast(1)
    repeat(cols * rows / 10) {
        drawRect(if (random.nextBoolean()) LeatherDark else LeatherLight, Offset(random.nextInt(cols) * px, random.nextInt(rows) * px), Size(px, px))
    }
    val inset = px * 5
    var x = inset
    while (x < size.width - inset) {
        drawRect(LeatherStitch, Offset(x, inset), Size(px * 2, px * 0.75f))
        drawRect(LeatherStitch, Offset(x, size.height - inset), Size(px * 2, px * 0.75f))
        x += px * 4
    }
}

private val StoneMortar = Color(0xFF26282E)
private val StoneColors = listOf(Color(0xFF50545E), Color(0xFF5A5E68), Color(0xFF464A54), Color(0xFF545864))
private val StoneLight = Color(0xFF6E727C)
private val StoneSpeck = Color(0xFF3A3D45)
private val TapestryBase = Color(0xFF5E1A26)
private val TapestryDark = Color(0xFF4A121C)
private val TapestryGold = Color(0xFFB8883A)
private val BannerBase = Color(0xFF22402C)
private val BannerStripe = Color(0xFF284A33)
private val BannerCross = Color(0xFF3E6A4A)
private val LeatherBase = Color(0xFF5E3A1E)
private val LeatherDark = Color(0xFF4A2C16)
private val LeatherLight = Color(0xFF6E4626)
private val LeatherStitch = Color(0xFFC8A060)

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
