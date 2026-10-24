package com.wordsnap.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// 整個遊戲是照 S23 Ultra 這種大螢幕（約 400 x 800dp 以上）排的
// 比較小的手機就把整個畫面（圖、字、按鈕、視窗）等比例縮小，排版和大手機一樣，不用上下捲動
private const val DESIGN_WIDTH_DP = 400f
private const val DESIGN_HEIGHT_DP = 800f

// 最多縮到 75%，再小字會看不清楚（這時候才需要捲動）
private const val MIN_SCALE = 0.75f

@Composable
fun FitToScreen(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scale = remember(config.screenWidthDp, config.screenHeightDp) {
        minOf(1f, config.screenWidthDp / DESIGN_WIDTH_DP, config.screenHeightDp / DESIGN_HEIGHT_DP).coerceAtLeast(MIN_SCALE)
    }
    if (scale >= 1f) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale), content = content)
    }
}
