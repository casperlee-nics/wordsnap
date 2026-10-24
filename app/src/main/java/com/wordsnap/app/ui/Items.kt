package com.wordsnap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.wordsnap.app.ui.theme.PixelInk
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// 防護盾、混亂藥劑持續幾回合
const val ITEM_TURNS = 3

// 戰鬥道具：勝利時有一半機率掉寶箱，寶箱裡依機率抽一個道具（weight 加起來是 100）
// 玩家 HP 是 10 格，每答錯一題扣 2 格（1/5）

const val HERO_MAX_HP = 10
const val WRONG_DAMAGE = 2
const val CHEST_CHANCE = 0.5f

enum class ItemType(val label: String, val effect: String, val weight: Int, val icon: List<String>) {
    SmallPotion("小回復劑", "回復 1/5 的 HP", 30, IconSmallPotion),
    SmartPill("聰明藥", "這題直接顯示答案，算答對", 20, IconSmartPill),
    BigPotion("大回復劑", "回復一半的 HP", 15, IconBigPotion),
    Riot("村民暴動", "村民衝過怪物，怪物多扣 1/4 的血", 7, IconHorn),
    Elixir("聖靈藥", "HP 全部回滿", 1, IconElixir),
    Angel("天使召喚", "天使從天而降，怪物損血一半", 2, IconBell),
    Shield("防護盾", "3 回合內答錯，被攻擊也不會扣血", 10, IconShield),
    Confusion("混亂藥劑", "3 回合內答錯，怪物反而會攻擊自己", 10, IconConfusion),
    Catapult("召喚投石車", "投石車丟大石頭，怪物扣 1/3 的血", 5, IconCatapult);

    // 寶箱裡抽到這個道具的機率（%）
    val dropPercent: Int get() = weight

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
        Catapult -> maxOf(1, (total + 2) / 3)
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
    'P' to Color(0xFF9058D0),
    'p' to Color(0xFFC8A0F0),
)

// 防護盾：藍色盾牌、金色鑲邊、中間白色十字
private val IconShield = listOf(
    "................",
    "..KKKKKKKKKKKK..",
    "..KYYYYYYYYYYK..",
    "..KYBBBWWBBBYK..",
    "..KYBBBWWBBBYK..",
    "..KYBWWWWWWBYK..",
    "..KYBWWWWWWBYK..",
    "..KYBBBWWBBBYK..",
    "..KYLBBWWBBBYK..",
    "...KYLBWWBBYK...",
    "...KYBBBBBBYK...",
    "....KYBBBBYK....",
    ".....KYBBYK.....",
    "......KYYK......",
    ".......KK.......",
    "................",
)

// 混亂藥劑：紫色圓瓶，裡面有漩渦
private val IconConfusion = listOf(
    "................",
    "......KKKK......",
    "......KDDK......",
    "......KKKK......",
    ".......KLK......",
    ".......KLK......",
    ".....KKPPKK.....",
    "....KPPPPPPK....",
    "...KPpppppPPK...",
    "...KPpPPPpPPK...",
    "...KPpPpPpPPK...",
    "...KPpPppPPPK...",
    "...KPPPPPPPPK...",
    "....KPPPPPPK....",
    ".....KKKKKK.....",
    "................",
)

// 投石車：木頭架子、長長的投臂，勺子裡放著大石頭
private val IconCatapult = listOf(
    "................",
    "..KKK...........",
    ".KMMMK..........",
    ".KMmMK..........",
    "..KKKDK.........",
    "......DK........",
    ".......DK.......",
    "........DK......",
    ".....KK..DK.....",
    ".....KHK..DK....",
    "....KHHHKKDDK...",
    "...KHHKHHHHHHK..",
    "..KKKKKKKKKKKKK.",
    "..KHHHHHHHHHHHK.",
    "..KMK......KMK..",
    "...K........K...",
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
    PixelWindowDialog(title = if (onUse != null) "道具袋" else "道具欄", onDismiss = onDismiss) {
        val owned = ItemType.entries.filter { (counts[it] ?: 0) > 0 }
        if (owned.isEmpty()) {
            Text("道具袋是空的。把怪物打飛時有機會打開寶箱拿到道具！")
        }
        owned.forEachIndexed { index, type ->
            val enabled = onUse == null || canUse(type)
            if (index > 0) Box(Modifier.fillMaxWidth().height(2.dp).background(ItemDivider))
            Row(
                Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)
                    .clickable(enabled = enabled && onUse != null) { onUse?.invoke(type) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 道具圖示放在小小的像素格子裡
                Box(
                    Modifier.size(48.dp).border(2.dp, PixelInk).background(ItemSlot).padding(4.dp),
                ) {
                    PixelArt(type.icon, ItemPalette, Modifier.size(40.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(type.label, style = MaterialTheme.typography.titleMedium)
                    Text(type.effect, style = MaterialTheme.typography.bodySmall)
                }
                Text("x${counts[type] ?: 0}", style = MaterialTheme.typography.titleMedium)
            }
        }
        // 道具欄最下面列出寶箱的掉落機率
        if (onUse == null) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(ItemDivider))
            Text("寶箱掉落機率", style = MaterialTheme.typography.titleMedium)
            Text(
                "把怪物打飛時有 ${(CHEST_CHANCE * 100).toInt()}% 機率出現寶箱，寶箱裡的道具：",
                style = MaterialTheme.typography.bodySmall,
            )
            ItemType.entries.sortedByDescending { it.dropPercent }.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelArt(type.icon, ItemPalette, Modifier.size(24.dp))
                    Text(type.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${type.dropPercent}%", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private val ItemSlot = Color(0xFFE8D8B0)
private val ItemDivider = Color(0x40202030)

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

// 天使的劍：握把在 hand，angle 是劍往右偏的角度（-90 是直直朝上）
private fun DrawScope.drawAngelSword(hand: Offset, cell: Float, angle: Float) {
    rotate(angle, pivot = hand) {
        // 劍刃往上：銀色，中間一條亮線
        drawRect(PixelInk, Offset(hand.x - cell, hand.y - cell * 9), Size(cell * 2, cell * 8))
        drawRect(Color(0xFFD8E0F0), Offset(hand.x - cell * 0.5f, hand.y - cell * 8.5f), Size(cell, cell * 7.5f))
        drawRect(Color.White, Offset(hand.x - cell * 0.5f, hand.y - cell * 8.5f), Size(cell * 0.5f, cell * 6f))
        // 金色護手和握把
        drawRect(PixelInk, Offset(hand.x - cell * 2.5f, hand.y - cell * 1.5f), Size(cell * 5, cell * 1.5f))
        drawRect(Color(0xFFF8D030), Offset(hand.x - cell * 2f, hand.y - cell * 1.2f), Size(cell * 4, cell * 0.9f))
        drawRect(Color(0xFF805030), Offset(hand.x - cell * 0.5f, hand.y), Size(cell, cell * 2))
    }
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
        if (t < 0.85f) {
            drawSprite(AngelArt, x, y - flap, cell)
            // 右手的劍：往下飛時舉在頭上，快碰到怪物時一格一格往下揮砍
            val swing = ((p - 0.75f) / 0.25f).coerceIn(0f, 1f)
            val angle = -70f + 140f * floor(swing * 4f) / 4f
            drawAngelSword(Offset(x + cell * 10f, y - flap + cell * 7f), cell, angle)
        }
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

// 投石車的樣子：左邊木架、右邊轉軸，投臂由 drawCatapult 另外畫
private val CatapultBase = listOf(
    "....KK......",
    "...KdeK.....",
    "..KdKKeK....",
    ".KdK..KeK...",
    "KKKKKKKKKKKK",
    "KddddddddddK",
    "KKKKKKKKKKKK",
    ".KmK....KmK.",
    ".KKK....KKK.",
)

// 召喚投石車：投石車從左邊推出來，投臂甩起來把大石頭丟到怪物頭上，碰的一聲碎石飛濺
fun DrawScope.drawCatapult(t: Float, groundY: Float, monster: Offset) {
    val cell = 3.dp.toPx()
    val baseW = CatapultBase.first().length * cell
    val baseH = CatapultBase.size * cell
    // 前 0.2 推進畫面，最後 0.15 退出去
    val enter = (t / 0.2f).coerceIn(0f, 1f)
    val exit = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
    val left = -baseW + (baseW + cell * 4) * enter - (baseW + cell * 4) * exit
    val top = groundY - baseH
    drawSprite(CatapultBase, left, top, cell)
    // 投臂：從轉軸往左後方斜躺，0.2～0.35 秒一格一格甩到直立
    val pivot = Offset(left + cell * 4.5f, top + cell * 1f)
    val swing = ((t - 0.2f) / 0.15f).coerceIn(0f, 1f)
    val angle = (-150f + 90f * floor(swing * 4f) / 4f) * PI.toFloat() / 180f
    val armLen = cell * 9
    val tip = Offset(pivot.x + kotlin.math.cos(angle) * armLen, pivot.y + sin(angle) * armLen)
    drawLine(Color(0xFF202030), pivot, tip, cell * 1.8f)
    drawLine(Color(0xFFC08848), pivot, tip, cell)
    // 大石頭：還沒丟出去時放在投臂尾端，0.35～0.75 秒沿著拋物線飛向怪物
    val fly = ((t - 0.35f) / 0.4f).coerceIn(0f, 1f)
    val rock = cell * 3.5f
    if (t < 0.75f) {
        val start = tip
        val x = start.x + (monster.x - start.x) * fly
        val arc = 4f * fly * (1f - fly) * 90.dp.toPx()
        val y = start.y + (monster.y - start.y) * fly - arc
        drawCircle(Color(0xFF202030), rock + cell * 0.6f, Offset(x, y))
        drawCircle(Color(0xFF8A8478), rock, Offset(x, y))
        drawRect(Color(0xFFB8B0A0), Offset(x - rock * 0.5f, y - rock * 0.5f), Size(cell, cell))
    }
    // 打中：碎石往四面飛
    if (t >= 0.75f) {
        val f = ((t - 0.75f) / 0.25f).coerceIn(0f, 1f)
        drawCircle(Color(0xFFF8E8A0).copy(alpha = (1f - f) * 0.7f), 40.dp.toPx() * f + 8.dp.toPx(), monster)
        for (k in 0 until 10) {
            val a = k * PI.toFloat() / 5 + 0.3f
            val r = 60.dp.toPx() * f
            val drop = f * f * 30.dp.toPx()
            drawRect(
                (if (k % 2 == 0) Color(0xFF8A8478) else Color(0xFFB8B0A0)).copy(alpha = 1f - f),
                Offset(monster.x + kotlin.math.cos(a) * r, monster.y + sin(a) * r + drop),
                Size(cell * 2, cell * 2),
            )
        }
    }
}

// 防護盾：人物身上一層淡藍色的光罩；擋住攻擊的瞬間（block 從 1 變 0）會亮一下並冒出六角形碎光
fun DrawScope.drawShieldBubble(hero: Offset, heroSize: Float, pulse: Float, block: Float) {
    val r = heroSize * 0.75f
    drawCircle(Color(0xFF90D0F8).copy(alpha = 0.18f + 0.1f * pulse + 0.5f * block), r, hero)
    drawCircle(Color(0xFFE8F8FF).copy(alpha = 0.55f + 0.4f * block), r, hero, style = Stroke(3.dp.toPx()))
    if (block > 0f) {
        val cell = 3.dp.toPx()
        for (k in 0 until 6) {
            val a = k * PI.toFloat() / 3
            val d = r + (1f - block) * 20.dp.toPx()
            drawRect(Color.White.copy(alpha = block), Offset(hero.x + kotlin.math.cos(a) * d, hero.y + sin(a) * d), Size(cell, cell))
        }
    }
}

// 混亂：怪物頭上有星星一格一格繞圈圈
fun DrawScope.drawConfusionStars(monster: Offset, monsterHeight: Float, t: Float) {
    val cell = 3.dp.toPx()
    val center = Offset(monster.x, monster.y - monsterHeight / 2 - cell * 3)
    for (k in 0 until 3) {
        // 每 1/8 圈跳一格，像像素遊戲
        val step = floor((t * 8f + k * 8f / 3f)) / 8f
        val a = step * 2 * PI.toFloat()
        val p = Offset(center.x + kotlin.math.cos(a) * cell * 6, center.y + sin(a) * cell * 2)
        val color = if (k == 1) Color(0xFFC8A0F0) else Color(0xFFF8D030)
        drawRect(color, Offset(p.x - cell / 2, p.y - cell * 1.5f), Size(cell, cell * 3))
        drawRect(color, Offset(p.x - cell * 1.5f, p.y - cell / 2), Size(cell * 3, cell))
    }
}
