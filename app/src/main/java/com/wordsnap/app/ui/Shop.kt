package com.wordsnap.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed

// 旅行商人：上面是錢包和「購買／販售」切換（右邊有商人走來走去），下面列出道具；買的價錢見 ItemType.price，賣是半價
@Composable
fun ShopDialog(
    counts: Map<ItemType, Int>,
    coins: Int,
    onBuy: (ItemType) -> Unit,
    onSell: (ItemType) -> Unit,
    onDismiss: () -> Unit,
) {
    var selling by remember { mutableStateOf(false) }
    // 剛剛買了或賣了什麼，顯示在錢包下面
    var note by remember { mutableStateOf<String?>(null) }
    PixelWindowDialog(
        title = tr(R.string.shop_title),
        onDismiss = onDismiss,
        header = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr(R.string.hero_wallet, coins), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroButton(
                        tr(R.string.shop_buy_tab),
                        onClick = { selling = false; note = null },
                        modifier = Modifier.width(92.dp).alpha(if (selling) 0.5f else 1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        textPadding = 4.dp,
                    )
                    RetroButton(
                        tr(R.string.shop_sell_tab),
                        onClick = { selling = true; note = null },
                        modifier = Modifier.width(92.dp).alpha(if (selling) 1f else 0.5f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        textPadding = 4.dp,
                    )
                    WalkingMerchant(Modifier.weight(1f).height(60.dp))
                }
                note?.let { Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium) }
            }
        },
    ) {
        val list = if (selling) ItemType.entries.filter { (counts[it] ?: 0) > 0 } else ItemType.entries
        if (selling && list.isEmpty()) {
            Text(tr(R.string.shop_nothing_to_sell), style = MaterialTheme.typography.bodyMedium.readable())
        }
        list.forEachIndexed { index, type ->
            val owned = counts[type] ?: 0
            val price = if (selling) type.sellPrice else type.price
            val enabled = if (selling) owned > 0 else coins >= type.price
            if (index > 0) Box(Modifier.fillMaxWidth().height(2.dp).background(ShopDivider))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(44.dp).border(2.dp, PixelInk).background(ShopSlot).padding(4.dp)) {
                    PixelArt(type.icon, ItemPalette, Modifier.size(36.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                    Text(type.effect, style = MaterialTheme.typography.bodySmall.readable())
                    Text(tr(R.string.shop_owned_price, owned, price), style = MaterialTheme.typography.bodySmall)
                }
                RetroButton(
                    tr(if (selling) R.string.shop_sell else R.string.shop_buy),
                    onClick = {
                        if (selling) onSell(type) else onBuy(type)
                        note = tr(if (selling) R.string.shop_sold else R.string.shop_bought, type.label, price)
                    },
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    textPadding = 8.dp,
                )
            }
        }
    }
}

private val ShopSlot = Color(0xFFE8D8B0)
private val ShopDivider = Color(0x40202030)

// 背著大背包的旅行商人，在按鈕右邊的空位來回踱步：往左走時左右翻過來，邊走邊換腳
@Composable
private fun WalkingMerchant(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "merchant")
    // 0→1 往右走、1→2 往左走
    val trip by anim.animateFloat(0f, 2f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "trip")
    val step by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "step")
    BoxWithConstraints(modifier) {
        val artHeight = maxHeight
        val artWidth = artHeight * MerchantA.first().length.toFloat() / MerchantA.size
        val room = (maxWidth - artWidth).coerceAtLeast(0.dp)
        val goingRight = trip < 1f
        val along = if (goingRight) trip else 2f - trip
        // 走到兩端時停一下：中間 80% 的時間在走
        val pos = ((along - 0.1f) / 0.8f).coerceIn(0f, 1f)
        val walking = along in 0.1f..0.9f
        val bob = if (walking && step >= 0.5f) 1.dp else 0.dp
        PixelArt(
            if (walking && step >= 0.5f) MerchantB else MerchantA,
            MerchantPalette,
            Modifier
                .offset(x = room * pos, y = -bob)
                .height(artHeight)
                .graphicsLayer { scaleX = if (goingRight) 1f else -1f },
        )
    }
}

private val MerchantPalette = mapOf(
    'K' to Color(0xFF202030),
    'H' to Color(0xFF784828),
    'h' to Color(0xFFC83C32),
    'S' to Color(0xFFF0C496),
    'E' to Color(0xFF202030),
    'B' to Color(0xFFE6E6E6),
    'C' to Color(0xFF3C825A),
    'c' to Color(0xFF285A3C),
    'P' to Color(0xFF966432),
    'p' to Color(0xFF64401E),
    'L' to Color(0xFF463C6E),
    'F' to Color(0xFF50321E),
    'G' to Color(0xFFF0C83C),
    'T' to Color(0xFFC8A06E),
)

private val MerchantA = listOf(
    "......KKKKK.....",
    ".....KHHHHHK....",
    ".....KhhhhhK....",
    "...KKHHHHHHHKK..",
    "..KPPKSSSSSK....",
    ".KPpPKSSSESK....",
    ".KPPPKSSSSSK....",
    ".KPpPKSBBBBK....",
    ".KPPPKKBBBK.....",
    ".KPGPKCCCCCK....",
    ".KPPPKCCCCCCK...",
    ".KPpPKCcCCCSK...",
    ".KPPPKCcCCCTK...",
    ".KPPPKCCCCCTK...",
    "..KKKKCCCCCK....",
    ".....KCCCCCK....",
    ".....KccccKK....",
    ".....KLLKLLK....",
    ".....KLLKLLK....",
    ".....KLLKLLK....",
    "....KFFFKFFFK...",
    "....KKKKKKKKK...",
)

// 跨步的那一格
private val MerchantB = listOf(
    "......KKKKK.....",
    ".....KHHHHHK....",
    ".....KhhhhhK....",
    "...KKHHHHHHHKK..",
    "..KPPKSSSSSK....",
    ".KPpPKSSSESK....",
    ".KPPPKSSSSSK....",
    ".KPpPKSBBBBK....",
    ".KPPPKKBBBK.....",
    ".KPGPKCCCCCK....",
    ".KPPPKCCCCCCK...",
    ".KPpPKCcCCCSK...",
    ".KPPPKCcCCCTK...",
    ".KPPPKCCCCCTK...",
    "..KKKKCCCCCK....",
    ".....KCCCCCK....",
    ".....KccccKK....",
    ".....KLLKLLK....",
    "....KLLK.KLLK...",
    "...KLLK...KLLK..",
    "..KFFFK...KFFFK.",
    "..KKKKK...KKKKK.",
)
