package com.wordsnap.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R

// 像素風配色：參考早期掌機遊戲的選單——羊皮紙底、深藍標題列、黑色粗框
val PixelInk = Color(0xFF202030)
val PixelBlue = Color(0xFF3A58C8)
val PixelPaper = Color(0xFFF8F0D8)
val PixelGold = Color(0xFFF8B830)
val PixelRed = Color(0xFFE04030)
val PixelWhite = Color(0xFFFFFCF0)

private val colors = lightColorScheme(
    primary = PixelBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8D4FF),
    onPrimaryContainer = PixelInk,
    secondary = PixelRed,
    onSecondary = Color.White,
    secondaryContainer = PixelGold,
    onSecondaryContainer = PixelInk,
    tertiary = Color(0xFF40A048),
    background = PixelPaper,
    onBackground = PixelInk,
    surface = PixelPaper,
    onSurface = PixelInk,
    surfaceVariant = Color(0xFFE8DCB8),
    onSurfaceVariant = Color(0xFF505060),
    surfaceContainerLowest = PixelWhite,
    surfaceContainerLow = PixelWhite,
    surfaceContainer = PixelWhite,
    surfaceContainerHigh = PixelWhite,
    surfaceContainerHighest = Color(0xFFE8DCB8),
    outline = PixelInk,
    outlineVariant = Color(0xFFB8A888),
    error = PixelRed,
)

// 俐方體 11 號（Cubic 11）：免費、可商用的繁體中文像素字型
private val PixelFont = FontFamily(Font(R.font.cubic_11))

private fun TextStyle.pixel() = copy(fontFamily = PixelFont)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.pixel(), displayMedium = displayMedium.pixel(), displaySmall = displaySmall.pixel(),
        headlineLarge = headlineLarge.pixel(), headlineMedium = headlineMedium.pixel(), headlineSmall = headlineSmall.pixel(),
        titleLarge = titleLarge.pixel(), titleMedium = titleMedium.pixel(), titleSmall = titleSmall.pixel(),
        bodyLarge = bodyLarge.pixel(), bodyMedium = bodyMedium.pixel(), bodySmall = bodySmall.pixel(),
        labelLarge = labelLarge.pixel(), labelMedium = labelMedium.pixel(), labelSmall = labelSmall.pixel(),
    )
}

// 切角代替圓角，看起來像像素方塊
val PixelShape = CutCornerShape(4.dp)
private val shapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = PixelShape,
    medium = PixelShape,
    large = CutCornerShape(6.dp),
    extraLarge = CutCornerShape(8.dp),
)

// 遊戲對話框那種黑色粗框
val PixelBorder = BorderStroke(3.dp, PixelInk)

@Composable
fun PixelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}

// 深藍標題列，文字和圖示是白色
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun pixelTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = PixelBlue,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)

// 標題列上的文字按鈕
@Composable
fun pixelTopBarButtonColors() = TextButtonDefaults.textButtonColors(
    contentColor = Color.White,
    disabledContentColor = Color.White.copy(alpha = 0.5f),
)
