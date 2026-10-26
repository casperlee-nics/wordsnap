package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed

// 道具屋：上面是錢包和「購買／販售」切換，下面列出道具；買的價錢見 ItemType.price，賣是半價
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroButton(
                        tr(R.string.shop_buy_tab),
                        onClick = { selling = false; note = null },
                        modifier = Modifier.alpha(if (selling) 0.5f else 1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                    RetroButton(
                        tr(R.string.shop_sell_tab),
                        onClick = { selling = true; note = null },
                        modifier = Modifier.alpha(if (selling) 1f else 0.5f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
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
