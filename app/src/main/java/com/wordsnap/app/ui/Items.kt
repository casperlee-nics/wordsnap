package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// 戰鬥道具：勝利時有一半機率掉寶箱，寶箱裡依機率抽一個道具（weight 加起來是 100）
// 玩家 HP 是 10 格，每答錯一題扣 2 格（1/5）

const val HERO_MAX_HP = 10
const val WRONG_DAMAGE = 2
const val CHEST_CHANCE = 0.5f

enum class ItemType(val label: String, val effect: String, val weight: Int, val icon: List<String>) {
    SmallPotion("小回復劑", "回復 1/5 的 HP", 30, IconSmallPotion),
    SmartPill("聰明藥", "這題直接顯示答案，算答對", 30, IconSmartPill),
    BigPotion("大回復劑", "回復一半的 HP", 15, IconBigPotion),
    Riot("村民暴動", "村民衝過怪物，怪物多扣 1/4 的血", 15, IconHorn),
    Elixir("聖靈藥", "HP 全部回滿", 5, IconElixir),
    Angel("天使召喚", "天使從天而降，怪物多扣一半的血", 5, IconBell);

    // 回復多少 HP（不是回復道具就是 0）
    val heal: Int
        get() = when (this) {
            SmallPotion -> 2
            BigPotion -> 5
            Elixir -> HERO_MAX_HP
            else -> 0
        }

    // 讓怪物多扣多少血（總題數的幾分之幾，至少 1）
    fun monsterDamage(total: Int): Int = when (this) {
        Riot -> maxOf(1, (total + 3) / 4)
        Angel -> maxOf(1, (total + 1) / 2)
        else -> 0
    }

    companion object {
        // 依機率抽一個道具
        fun roll(random: Random = Random): ItemType {
            var n = random.nextInt(entries.sumOf { it.weight })
            for (type in entries) {
                if (n < type.weight) return type
                n -= type.weight
            }
            return SmallPotion
        }

        fun from(name: String): ItemType? = entries.firstOrNull { it.name == name }
    }
}

// 道具圖示的顏色
val ItemPalette = ColorIconPalette + mapOf(
    'D' to Color(0xFFA06030),
    'A' to Color(0xFFF8F0A0),
    'G' to Color(0xFF40A048),
)

// IconSmallPotion
private val IconSmallPotion = listOf(
    "................",
    "................",
    "................",
    "......KKKK......",
    "......KDDK......",
    "......KKKK......",
    ".......KLK......",
    "......KLLLK.....",
    ".....KRRRRRK....",
    ".....KRWRRRK....",
    ".....KRRRRRK....",
    ".....KRRRRRK....",
    "......KKKKK.....",
    "................",
    "................",
    "................",
)

// IconBigPotion
private val IconBigPotion = listOf(
    "................",
    ".....KKKKKK.....",
    ".....KDDDDK.....",
    ".....KKKKKK.....",
    "......KLLK......",
    "......KLLK......",
    "....KKLLLLKK....",
    "...KRRRRRRRRK...",
    "..KRWWRRRRRRRK..",
    "..KRWRRRRRRRRK..",
    "..KRRRRRRRRRRK..",
    "..KRRRRRRRRRRK..",
    "..KRRRRRRRRRRK..",
    "...KRRRRRRRRK...",
    "....KKKKKKKK....",
    "................",
)

// IconElixir
private val IconElixir = listOf(
    "...Y........Y...",
    "..YAY.KKKK.YAY..",
    "...Y..KYYK..Y...",
    "......KKKK......",
    "......KLLK......",
    "....KKLLLLKK....",
    "...KAAAAAAAAK...",
    "..KAWWAAAAAAAK..",
    "..KAWAAYYAAAAK..",
    "..KAAAYYYYAAAK..",
    "..KAAAAYYAAAAK..",
    "..KAAAAAAAAAAK..",
    "...KAAAAAAAAK...",
    "....KKKKKKKK....",
    ".Y............Y.",
    "YAY..........YAY",
)

// IconSmartPill
private val IconSmartPill = listOf(
    "................",
    "................",
    "................",
    "..........KKK...",
    ".........KBBBK..",
    "........KBBBBBK.",
    ".......KWBBBBBK.",
    "......KWWKBBBK..",
    ".....KWWWWKBK...",
    "....KRRWWWWK....",
    "...KRRRRWWK.....",
    "..KRWRRRRK......",
    "..KRRRRRK.......",
    "...KRRRK........",
    "....KKK.........",
    "................",
)

// IconBell
private val IconBell = listOf(
    "................",
    ".......KK.......",
    "......KYYK......",
    ".....KYYYYK.....",
    "....KYWYYYYK....",
    "....KYWYYYYK....",
    "....KYYYYYYK....",
    "...KYYYYYYYYK...",
    "...KYYYYYYYYK...",
    "..KYYYYYYYYYYK..",
    ".KYYYYYYYYYYYYK.",
    ".KKKKKKKKKKKKKK.",
    "......KYYK......",
    ".......KK.......",
    "..A..........A..",
    "................",
)

// IconHorn
private val IconHorn = listOf(
    "................",
    "................",
    "...........KK...",
    "..........KYYK..",
    ".........KYYYK..",
    "........KYYYYK..",
    "...KK..KYDYYYK..",
    "..KDDKKDDDYYYK..",
    ".KDDDDDDDDYYYK..",
    "..KDDKKDDDYYYK..",
    "...KK..KYDYYYK..",
    "........KYYYYK..",
    ".........KYYYK..",
    "..........KYYK..",
    "...........KK...",
    "................",
)

// IconBag
val IconBag = listOf(
    "................",
    "......KKKK......",
    ".....KDKKDK.....",
    "......KDDK......",
    "....KKKKKKKK....",
    "...KDDDDDDDDK...",
    "..KDDDDDDDDDDK..",
    "..KDDKKKKKKDDK..",
    "..KDDKYYYYKDDK..",
    "..KDDKYKKYKDDK..",
    "..KDDKKKKKKDDK..",
    "..KDDDDDDDDDDK..",
    "..KDDDDDDDDDDK..",
    "...KDDDDDDDDK...",
    "....KKKKKKKK....",
    "................",
)


// 道具袋：列出擁有的道具（圖示、名稱、說明、數量）
// 戰鬥中傳 onUse，點一下就使用；單字簿的「道具欄」只能看不能用
@Composable
fun ItemBagDialog(
    counts: Map<ItemType, Int>,
    onDismiss: () -> Unit,
    canUse: (ItemType) -> Boolean = { true },
    onUse: ((ItemType) -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onUse != null) "道具袋" else "道具欄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val owned = ItemType.entries.filter { (counts[it] ?: 0) > 0 }
                if (owned.isEmpty()) {
                    Text("道具袋是空的。戰鬥勝利時有機會打開寶箱拿到道具！")
                }
                owned.forEach { type ->
                    val enabled = onUse == null || canUse(type)
                    Row(
                        Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)
                            .clickable(enabled = enabled && onUse != null) { onUse?.invoke(type) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PixelArt(type.icon, ItemPalette, Modifier.size(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text(type.label, style = MaterialTheme.typography.titleMedium)
                            Text(type.effect, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("x${counts[type] ?: 0}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}

// 在戰鬥畫面上畫道具的效果動畫，t 從 0 到 1
private val SpritePalette = ScenePalette + mapOf('S' to Color(0xFFF8C898), 'H' to Color(0xFF704020))

private val AngelArt = listOf(
    "....YYYYY....",
    "....Y...Y....",
    "....YYYYY....",
    "WW...KKK...WW",
    "WWW.KSSSK.WWW",
    "WWWWKSKSKWWWW",
    ".WWWKSSSKWWW.",
    "..WWWKKKWWW..",
    "....KWWWK....",
    "...KWWWWWK...",
    "...KWWWWWK...",
    "....KKKKK....",
)

private val VillagerArt = listOf(
    "...KKKK..M.",
    "..KHHHHK.M.",
    "..KSSSSK.M.",
    "..KSKSKK.M.",
    "..KSSSSK.M.",
    "..KGGGGKKM.",
    ".KSGGGGSKM.",
    "..KGGGGK.M.",
    "..KDDDDK...",
    "..KDKKDK...",
    "..KK..KK...",
)

private val BulbArt = listOf(
    "..KKK..",
    ".KYYYK.",
    "KYYWYYK",
    "KYWYYYK",
    "KYYYYYK",
    ".KYYYK.",
    "..KMK..",
    "..KmK..",
    "..KKK..",
)

// 在 (left, top) 畫一張小像素圖，每格 cell 像素
private fun DrawScope.drawSprite(art: List<String>, left: Float, top: Float, cell: Float, alpha: Float = 1f, recolor: Map<Char, Char> = emptyMap()) {
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val color = SpritePalette[recolor[c] ?: c] ?: return@forEachIndexed
            drawRect(color.copy(alpha = alpha), Offset(left + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

// 回血：綠色光點從人物腳下往上冒，回越多光點越多；聖靈藥還會全身發出金光
fun DrawScope.drawHeal(type: ItemType, t: Float, hero: Offset, heroSize: Float) {
    val count = when (type) {
        ItemType.SmallPotion -> 8
        ItemType.BigPotion -> 16
        else -> 30
    }
    val px = 3.dp.toPx()
    val colors = listOf(Color(0xFF70E070), Color(0xFFA0F8A0), Color.White)
    if (type == ItemType.Elixir) {
        drawCircle(Color(0xFFF8F0A0).copy(alpha = sin(t * PI.toFloat()) * 0.7f), heroSize * (0.6f + t * 0.5f), hero)
        drawRect(Color(0xFFF8F0A0).copy(alpha = sin(t * PI.toFloat()) * 0.25f))
    }
    for (i in 0 until count) {
        val p = ((t * 1.4f) - (i % 8) * 0.06f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val x = hero.x + ((i * 37) % 100 / 100f - 0.5f) * heroSize
        val y = hero.y + heroSize / 2 - p * heroSize * 1.4f
        val s = px * (1f + (i % 3) * 0.5f)
        drawRect(colors[i % colors.size].copy(alpha = 1f - p), Offset(x - s / 2, y - s / 2), Size(s, s))
    }
}

// 聰明藥：人物頭上亮出一顆燈泡，一閃一閃
fun DrawScope.drawBulb(t: Float, hero: Offset, heroSize: Float) {
    val cell = heroSize / 14f
    val top = hero.y - heroSize / 2 - BulbArt.size * cell - cell * (2 + sin(t * 6 * PI.toFloat()))
    val left = hero.x - BulbArt.first().length * cell / 2
    val glow = if ((t * 8).toInt() % 2 == 0) 0.6f else 0.2f
    drawCircle(Color(0xFFF8F0A0).copy(alpha = glow), cell * 6, Offset(hero.x, top + cell * 3))
    drawSprite(BulbArt, left, top, cell)
}

// 天使召喚：三位天使從天而降，衝向怪物，碰到時發出白光
fun DrawScope.drawAngels(t: Float, monster: Offset) {
    val cell = 3.dp.toPx()
    val w = AngelArt.first().length * cell
    val offsets = listOf(-1.2f, 0f, 1.2f)
    offsets.forEachIndexed { i, dx ->
        val p = ((t - i * 0.12f) / 0.6f).coerceIn(0f, 1f)
        if (p <= 0f) return@forEachIndexed
        val x = monster.x + dx * w * (1f - p * 0.6f) - w / 2
        val y = -AngelArt.size * cell + (monster.y - AngelArt.size * cell * 0.5f + AngelArt.size * cell) * p
        // 拍翅膀：上下輕輕晃
        val flap = if (((t * 12).toInt() + i) % 2 == 0) 0f else cell
        if (t < 0.85f) drawSprite(AngelArt, x, y - flap, cell)
    }
    if (t > 0.6f) {
        val f = ((t - 0.6f) / 0.4f).coerceIn(0f, 1f)
        drawCircle(Color.White.copy(alpha = (1f - f) * 0.9f), 60.dp.toPx() * f + 10.dp.toPx(), monster)
        for (k in 0 until 8) {
            val a = k * PI.toFloat() / 4
            val r = 70.dp.toPx() * f
            drawRect(Color(0xFFF8D030).copy(alpha = 1f - f), Offset(monster.x + kotlin.math.cos(a) * r, monster.y + sin(a) * r), Size(cell * 2, cell * 2))
        }
    }
}

// 畫一個村民（升級畫面的城鎮用）：bottom 是腳底的位置，cheering 時兩手舉高
fun DrawScope.drawVillager(left: Float, bottom: Float, cell: Float, shirt: Char, cheering: Boolean) {
    val top = bottom - VillagerArt.size * cell
    drawSprite(VillagerArt, left, top, cell, recolor = mapOf('G' to shirt))
    if (cheering) {
        val skin = SpritePalette.getValue('S')
        drawRect(skin, Offset(left + 1 * cell, top + 3 * cell), Size(cell, cell * 3))
        drawRect(skin, Offset(left + 8 * cell, top + 3 * cell), Size(cell, cell * 3))
    }
}

// 村民暴動：一群拿著草叉的村民從左邊衝過怪物
fun DrawScope.drawRiot(t: Float, groundY: Float) {
    val cell = 3.dp.toPx()
    val w = VillagerArt.first().length * cell
    val h = VillagerArt.size * cell
    val shirts = listOf('G', 'R', 'B', 'O', 'p', 'y')
    for (i in 0 until 7) {
        val start = -w - i * w * 0.9f
        val x = start + (size.width + w * 8) * t
        if (x < -w || x > size.width) continue
        val hop = if (((t * 20).toInt() + i) % 2 == 0) 0f else cell
        val shirt = shirts[i % shirts.size]
        drawSprite(VillagerArt, x, groundY - h - hop - (i % 2) * cell * 3, cell, recolor = mapOf('G' to shirt))
    }
    // 揚起的塵土
    for (k in 0 until 10) {
        val x = (size.width + w * 8) * t - k * w * 0.7f - w
        drawRect(Color(0xFFD0B080).copy(alpha = 0.6f), Offset(x, groundY - cell * (1 + k % 3)), Size(cell * 2, cell * 2))
    }
}
