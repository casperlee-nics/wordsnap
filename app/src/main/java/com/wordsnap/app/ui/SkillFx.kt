package com.wordsnap.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// 戰鬥畫面上的技能動畫：每一招都有自己的樣子，t 從 0 到 1
// 會持續幾回合的效果（粉絲加油、精神集中、怪物昏迷）另外用 drawCheerFans、drawFocusAura、drawStunned 一直畫著

// 每一招動畫播多久（毫秒）
fun skillDuration(skill: Skill): Int = when (skill) {
    Skill.FanCheer, Skill.BoulderThrow, Skill.DragonRoar, Skill.DataSearch -> 1800
    Skill.SneezeFire, Skill.BushHide, Skill.DarkMist -> 1600
    else -> 1400
}

private val TAU = 2 * PI.toFloat()

// 像素動畫一格一格跳：把 0..1 切成 steps 段
private fun stepped(t: Float, steps: Int) = floor(t * steps) / steps

private fun seg(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)

fun DrawScope.drawSkill(skill: Skill, t: Float, hero: Offset, monster: Offset, heroSize: Float, monsterHeight: Float, ground: Float) {
    val cell = 3.dp.toPx()
    when (skill) {
        Skill.FanCheer -> drawHeartBurst(t, hero, heroSize, cell)
        Skill.LoveBento -> drawBento(t, hero, heroSize, cell)
        Skill.MindFocus -> drawFocusStart(t, hero, heroSize, cell)
        Skill.CrystalPeek -> drawCrystalBall(t, hero, heroSize, cell)
        Skill.SwishArrow -> drawArrowShot(t, hero, monster, cell)
        Skill.ElfSpring -> drawSpring(t, hero, heroSize, ground, cell)
        Skill.BushHide -> drawBush(t, hero, heroSize, monster, monsterHeight, ground, cell, mist = false)
        Skill.BoulderThrow -> drawBoulder(t, hero, heroSize, monster, cell)
        Skill.PotLid -> drawPotLid(t, hero, heroSize, cell)
        Skill.HammerToss -> drawHammer(t, hero, monster, cell)
        Skill.TeaserWand -> drawTeaser(t, hero, monster, monsterHeight, cell)
        Skill.LickFur -> drawLick(t, hero, heroSize, cell)
        Skill.SneezeFire -> drawSneeze(t, hero, heroSize, monster, cell)
        Skill.DragonRoar -> drawRoar(t, hero, monster, monsterHeight, cell)
        Skill.BarrierOn -> drawBarrier(t, hero, heroSize, cell)
        Skill.DataSearch -> drawDataSearch(t, hero, heroSize, cell)
        Skill.Bleh -> drawBleh(t, hero, heroSize, monster, monsterHeight, cell)
        Skill.DarkMist -> drawBush(t, hero, heroSize, monster, monsterHeight, ground, cell, mist = true)
        else -> Unit
    }
}

// ---- 小像素圖 ----

private val FanArt = listOf(
    ".Y.KKK.Y.",
    "YYKHHHKYY",
    ".KHSSSHK.",
    ".KSKSKSK.",
    ".KSSPSSK.",
    "..KRRRK..",
    ".SKRRRKS.",
    "..KRRRK..",
    "..KPPPK..",
    "..KS.SK..",
    "..KK.KK..",
)

private val HeartArt = listOf(
    ".P.P.",
    "PPPPP",
    ".PPP.",
    "..P..",
)

private val BentoArt = listOf(
    "KWWWWYYgK",
    "KWRWWYYgK",
    "KWWWWDDgK",
    "KWWWWDDGK",
    "KKKKKKKKK",
)

private val BentoLid = listOf(
    ".KKKKKKK.",
    "KRRRRRRRK",
)

private val BushArt = listOf(
    "....GGGG......",
    "..GGgGGGGG....",
    ".GGGGGgGGGGG..",
    "GGgGGGGGGtGGG.",
    "GGGGGtGGGGGgGG",
    "GtGGGGGGgGGGGG",
    "GGGGgGGGGGGtGG",
    ".GGGGGGGtGGGG.",
)

private val QuestionArt = listOf(
    "YYY",
    "..Y",
    ".YY",
    "...",
    ".Y.",
)

private val BangArt = listOf(
    "R",
    "R",
    "R",
    ".",
    "R",
)

private val PaperArt = listOf(
    "WWWWWWW",
    "WKKKKWW",
    "WWWWWWW",
    "WKKKWKW",
    "WWWWWWW",
    "WKKWKKW",
    "WWWWWWW",
)

private fun DrawScope.spriteCentered(art: List<String>, center: Offset, cell: Float, alpha: Float = 1f, recolor: Map<Char, Char> = emptyMap()) {
    drawSprite(art, center.x - art.first().length * cell / 2, center.y - art.size * cell / 2, cell, alpha, recolor)
}

private fun DrawScope.pixelStar(c: Offset, cell: Float, color: Color) {
    drawRect(color, Offset(c.x - cell / 2, c.y - cell * 1.5f), Size(cell, cell * 3))
    drawRect(color, Offset(c.x - cell * 1.5f, c.y - cell / 2), Size(cell * 3, cell))
}

// ---- 一直畫著的效果 ----

// 女粉絲啦啦隊：3 位女勇者粉絲站在人物和怪物中間的後排，輪流跳起來揮彩球，頭上冒愛心
fun DrawScope.drawCheerFans(clock: Float, hero: Offset, heroSize: Float, ground: Float, appear: Float = 1f) {
    val cell = 3.dp.toPx()
    val hairs = listOf('H', 'Y', 'x')
    val shirts = listOf('R', 'B', 'p')
    val h = FanArt.size * cell
    val fanW = FanArt.first().length * cell
    // 畫面正中間偏人物這邊，後排站得比較高一點
    val middle = maxOf(size.width / 2 - fanW * 0.5f, hero.x + heroSize * 0.5f + fanW * 1.3f)
    for (i in 0 until 3) {
        val x = middle + (i - 1) * fanW * 1.2f
        val jump = if (((clock * 6).toInt() + i) % 3 == 0) cell * 2 else 0f
        val rise = (1f - appear) * h
        drawSprite(FanArt, x - fanW / 2, ground - h - cell * 4 - jump + rise, cell, recolor = mapOf('H' to hairs[i], 'R' to shirts[i]))
        // 愛心慢慢往上飄
        val p = (clock + i / 3f) % 1f
        spriteCentered(HeartArt, Offset(x + sin(p * TAU) * cell * 2, ground - h - cell * 7 - p * cell * 12), cell * 0.8f, alpha = 1f - p)
    }
}

// 精神力集中：人物身邊有一圈藍色光點慢慢轉
fun DrawScope.drawFocusAura(clock: Float, hero: Offset, heroSize: Float) {
    val cell = 3.dp.toPx()
    val r = heroSize * 0.62f
    for (k in 0 until 8) {
        val a = stepped(clock, 16) * TAU + k * TAU / 8
        val p = Offset(hero.x + cos(a) * r, hero.y + sin(a) * r * 0.35f + heroSize * 0.3f)
        drawRect(if (k % 2 == 0) Color(0xFF90D0F8) else Color(0xFF3A58C8), Offset(p.x - cell / 2, p.y - cell / 2), Size(cell, cell))
    }
}

// 怪物昏迷或嚇呆：頭上黃色星星一格一格繞圈，旁邊冒冷汗
fun DrawScope.drawStunned(clock: Float, monster: Offset, monsterHeight: Float) {
    val cell = 3.dp.toPx()
    val center = Offset(monster.x, monster.y - monsterHeight / 2 - cell * 2)
    for (k in 0 until 4) {
        val a = (stepped(clock, 8) + k / 4f) * TAU
        pixelStar(Offset(center.x + cos(a) * cell * 7, center.y + sin(a) * cell * 2), cell, if (k % 2 == 0) Color(0xFFF8D030) else Color.White)
    }
    val drop = (clock * 2) % 1f
    drawRect(Color(0xFF90D0F8), Offset(monster.x + monsterHeight * 0.45f, monster.y - monsterHeight * 0.3f + drop * cell * 6), Size(cell, cell * 2))
}

// ---- 各招的動畫 ----

private fun DrawScope.drawHeartBurst(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    for (k in 0 until 10) {
        val a = k * TAU / 10
        val d = heroSize * 0.3f + t * heroSize * 0.9f
        spriteCentered(HeartArt, Offset(hero.x + cos(a) * d, hero.y + sin(a) * d * 0.6f - t * heroSize * 0.3f), cell, alpha = 1f - t)
    }
}

private fun DrawScope.drawBento(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val c = Offset(hero.x, hero.y - heroSize * 0.75f)
    val box = Offset(c.x - BentoArt.first().length * cell / 2, c.y)
    drawSprite(BentoArt, box.x, box.y, cell)
    // 蓋子打開往上跳
    val open = seg(t, 0.1f, 0.3f)
    drawSprite(BentoLid, box.x, box.y - BentoLid.size * cell - open * cell * 6, cell, alpha = 1f - seg(t, 0.6f, 0.9f))
    // 吃一口：飯粒飛向人物，冒愛心和綠色亮光
    if (t > 0.35f) {
        for (k in 0 until 3) {
            val p = seg(t, 0.35f + k * 0.08f, 0.6f + k * 0.08f)
            spriteCentered(HeartArt, Offset(c.x + (k - 1) * cell * 5, c.y - p * heroSize * 0.6f), cell, alpha = 1f - p)
        }
        drawHeal(ItemType.SmallPotion, seg(t, 0.4f, 1f), hero, heroSize)
    }
}

private fun DrawScope.drawFocusStart(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    // 藍色光圈從大到小收進人物身上，越轉越快
    for (ring in 0 until 3) {
        val p = seg(t, ring * 0.15f, 0.7f + ring * 0.1f)
        val r = heroSize * (1.4f - p * 0.8f)
        val spin = p * p * 6f
        for (k in 0 until 12) {
            val a = spin * TAU + k * TAU / 12 + ring
            drawRect(Color(0xFF90D0F8).copy(alpha = 1f - seg(t, 0.85f, 1f)), Offset(hero.x + cos(a) * r, hero.y + sin(a) * r * 0.5f), Size(cell, cell))
        }
    }
    if (t > 0.7f) drawCircle(Color(0xFF90D0F8).copy(alpha = (1f - t) * 1.5f), heroSize * 0.7f, hero)
}

private fun DrawScope.drawCrystalBall(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val c = Offset(hero.x, hero.y - heroSize * 0.85f - sin(t * TAU * 2) * cell)
    val r = cell * 5
    val glow = if ((t * 10).toInt() % 2 == 0) 0.5f else 0.25f
    drawCircle(Color(0xFFC090E8).copy(alpha = glow), r * 1.8f, c)
    drawCircle(Color(0xFF202030), r + cell, c)
    drawCircle(Color(0xFF9060C0), r, c)
    drawRect(Color.White, Offset(c.x - r * 0.5f, c.y - r * 0.6f), Size(cell, cell * 2))
    drawRect(Color(0xFFC08848), Offset(c.x - r, c.y + r * 0.8f), Size(r * 2, cell * 2))
    // 球裡跑出一個個字母（小方塊），往人物飛去
    for (k in 0 until 5) {
        val p = seg(t, 0.3f + k * 0.1f, 0.6f + k * 0.1f)
        if (p <= 0f || p >= 1f) continue
        val x = c.x + (k - 2) * cell * 3 * p
        val y = c.y + p * heroSize * 0.6f
        drawRect(Color(0xFFF8D030), Offset(x, y), Size(cell * 1.5f, cell * 2))
    }
}

private fun DrawScope.drawArrowShot(t: Float, hero: Offset, monster: Offset, cell: Float) {
    val p = seg(t, 0.15f, 0.6f)
    if (t < 0.15f) {
        // 拉弓：弓弦往後拉
        drawLine(Color(0xFFC08848), Offset(hero.x + cell * 4, hero.y - cell * 6), Offset(hero.x + cell * 4, hero.y + cell * 6), cell)
        drawLine(Color.White, Offset(hero.x + cell * 4, hero.y - cell * 6), Offset(hero.x + cell * (4 - t * 20), hero.y), cell / 2)
    }
    if (p in 0.01f..0.99f) {
        val x = hero.x + (monster.x - hero.x) * p
        val y = hero.y + (monster.y - hero.y) * p - sin(p * PI.toFloat()) * cell * 4
        // 綠色光尾
        for (k in 1..5) drawRect(Color(0xFF70C058).copy(alpha = 0.8f - k * 0.14f), Offset(x - k * cell * 3, y - cell / 2), Size(cell * 2, cell))
        drawLine(Color(0xFF805030), Offset(x - cell * 6, y), Offset(x, y), cell)
        drawRect(Color(0xFFC0C8D8), Offset(x, y - cell), Size(cell * 2, cell * 2))
        drawRect(Color.White, Offset(x - cell * 7, y - cell * 1.5f), Size(cell, cell))
        drawRect(Color.White, Offset(x - cell * 7, y + cell * 0.5f), Size(cell, cell))
    }
    if (t >= 0.6f) {
        val f = seg(t, 0.6f, 1f)
        drawCircle(Color(0xFFA0F8A0).copy(alpha = (1f - f) * 0.7f), cell * (4 + f * 10), monster)
    }
}

private fun DrawScope.drawSpring(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    // 腳邊冒出泉水，水珠一顆顆往上噴再落下
    for (k in 0 until 14) {
        val p = (t * 2.2f - k * 0.08f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val dx = (k % 7 - 3) * cell * 1.5f * p
        val dy = sin(p * PI.toFloat()) * heroSize * 0.9f
        drawRect(if (k % 2 == 0) Color(0xFF90D0F8) else Color.White, Offset(hero.x + dx, ground - dy), Size(cell, cell * 1.5f))
    }
    // 腳邊開出小花
    val bloom = seg(t, 0.4f, 0.7f)
    for (k in 0 until 4) {
        val x = hero.x + (k - 1.5f) * heroSize * 0.3f
        val s = cell * bloom
        drawRect(Color(0xFF40A048), Offset(x, ground - cell * 2 * bloom), Size(cell / 2, cell * 2 * bloom))
        drawRect(if (k % 2 == 0) Color(0xFFF08090) else Color(0xFFF8D030), Offset(x - s / 2, ground - cell * 3 * bloom), Size(s * 1.5f, s * 1.5f))
    }
    if (t > 0.5f) drawHeal(ItemType.SmallPotion, seg(t, 0.5f, 1f), hero, heroSize)
}

private fun DrawScope.drawBush(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, ground: Float, cell: Float, mist: Boolean) {
    val show = seg(t, 0f, 0.15f) * (1f - seg(t, 0.85f, 1f))
    if (mist) {
        // 黑霧：一團團紫黑色的霧把人物包起來
        for (k in 0 until 16) {
            val a = k * TAU / 16 + t * TAU
            val r = heroSize * (0.2f + 0.35f * ((k * 7) % 5) / 5f)
            val c = Offset(hero.x + cos(a) * r, hero.y + sin(a) * r * 0.7f)
            drawCircle((if (k % 2 == 0) Color(0xFF302848) else Color(0xFF140C18)).copy(alpha = 0.85f * show), cell * (3 + k % 3), c)
        }
    } else {
        // 草叢蓋住人物，左右偷偷移動
        val sway = if ((t * 8).toInt() % 2 == 0) -cell else cell
        val bushCell = heroSize / BushArt.first().length * 1.3f
        drawSprite(BushArt, hero.x - BushArt.first().length * bushCell / 2 + sway, ground - BushArt.size * bushCell, bushCell, alpha = show)
    }
    // 怪物東張西望，頭上冒問號
    if (t in 0.2f..0.9f) {
        val bob = if ((t * 6).toInt() % 2 == 0) 0f else cell
        spriteCentered(QuestionArt, Offset(monster.x - cell * 3, monster.y - monsterHeight / 2 - cell * 4 - bob), cell)
        spriteCentered(QuestionArt, Offset(monster.x + cell * 3, monster.y - monsterHeight / 2 - cell * 6 + bob), cell)
    }
}

private fun DrawScope.drawBoulder(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val r = cell * 6
    val lift = seg(t, 0f, 0.3f)
    val fly = seg(t, 0.3f, 0.65f)
    val start = Offset(hero.x, hero.y - heroSize * 0.5f - r - lift * cell * 4)
    if (t < 0.65f) {
        val x = start.x + (monster.x - start.x) * fly
        val y = start.y + (monster.y - start.y) * fly - 4f * fly * (1f - fly) * 80.dp.toPx()
        // 舉起來時手臂發抖
        val shake = if (fly == 0f && (t * 30).toInt() % 2 == 0) cell / 2 else 0f
        drawCircle(Color(0xFF202030), r + cell, Offset(x + shake, y))
        drawCircle(Color(0xFF8A8478), r, Offset(x + shake, y))
        drawRect(Color(0xFFB8B0A0), Offset(x + shake - r * 0.5f, y - r * 0.5f), Size(cell * 2, cell * 2))
        drawRect(Color(0xFF707890), Offset(x + shake + r * 0.2f, y + r * 0.1f), Size(cell * 2, cell))
    } else {
        val f = seg(t, 0.65f, 1f)
        drawCircle(Color(0xFFF8E8A0).copy(alpha = (1f - f) * 0.7f), cell * 4 + f * cell * 14, monster)
        for (k in 0 until 8) {
            val a = k * TAU / 8 + 0.4f
            val d = f * cell * 18
            drawRect(Color(0xFF8A8478), Offset(monster.x + cos(a) * d, monster.y + sin(a) * d + f * f * cell * 8), Size(cell * 2, cell * 2))
        }
    }
}

private fun DrawScope.drawPotLid(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val appear = seg(t, 0f, 0.25f)
    val c = Offset(hero.x + heroSize * 0.45f, hero.y + (1f - appear) * heroSize)
    // 大鍋蓋：灰色圓盤和把手
    drawOval(Color(0xFF202030), Offset(c.x - cell * 3, c.y - cell * 8), Size(cell * 6, cell * 16))
    drawOval(Color(0xFFC0C8D8), Offset(c.x - cell * 2, c.y - cell * 7), Size(cell * 4, cell * 14))
    drawRect(Color(0xFF707890), Offset(c.x + cell * 2, c.y - cell), Size(cell * 2, cell * 2))
    drawRect(Color.White, Offset(c.x - cell, c.y - cell * 5), Size(cell, cell * 3))
    // 噹！亮光閃一下
    if (t > 0.5f && (t * 12).toInt() % 2 == 0) {
        for (k in 0 until 4) {
            val a = k * TAU / 4 + PI.toFloat() / 4
            drawLine(Color(0xFFF8D030), Offset(c.x + cos(a) * cell * 9, c.y + sin(a) * cell * 9), Offset(c.x + cos(a) * cell * 13, c.y + sin(a) * cell * 13), cell)
        }
    }
}

private fun DrawScope.drawHammer(t: Float, hero: Offset, monster: Offset, cell: Float) {
    // 0～0.45 飛過去、0.45～0.9 飛回來，一路旋轉
    val out = seg(t, 0f, 0.45f)
    val back = seg(t, 0.5f, 0.95f)
    val p = if (t < 0.5f) out else 1f - back
    if (t >= 0.95f) return
    val c = Offset(hero.x + (monster.x - hero.x) * p, hero.y - sin(p * PI.toFloat()) * cell * 16)
    rotate(stepped(t, 16) * 1440f, c) {
        drawRect(Color(0xFF805030), Offset(c.x - cell / 2, c.y - cell * 5), Size(cell, cell * 8))
        drawRect(Color(0xFF202030), Offset(c.x - cell * 3.5f, c.y - cell * 6.5f), Size(cell * 7, cell * 4))
        drawRect(Color(0xFF707890), Offset(c.x - cell * 3, c.y - cell * 6), Size(cell * 6, cell * 3))
    }
    if (t in 0.42f..0.6f) drawCircle(Color(0xFFF8E8A0).copy(alpha = 0.6f), cell * 8, monster)
}

private fun DrawScope.drawTeaser(t: Float, hero: Offset, monster: Offset, monsterHeight: Float, cell: Float) {
    // 逗貓棒從人物伸向怪物，羽毛左右搖
    val reach = seg(t, 0f, 0.25f)
    val tip = Offset(hero.x + (monster.x - hero.x) * 0.55f * reach, monster.y - monsterHeight * 0.6f)
    val swing = sin(t * TAU * 4) * cell * 6
    val feather = Offset(tip.x + swing, tip.y - cell * 2)
    drawLine(Color(0xFFC08848), Offset(hero.x + cell * 3, hero.y), tip, cell)
    drawLine(Color(0xFFE8E8F0), tip, feather, cell / 2)
    drawCircle(Color(0xFFF08090), cell * 2.5f, feather)
    drawCircle(Color(0xFFF8D030), cell * 1.5f, Offset(feather.x + cell, feather.y + cell * 2))
    // 怪物的眼睛跟著轉：頭上兩顆小眼珠左右跑
    if (reach >= 1f) {
        val look = swing / 3
        for (side in listOf(-1, 1)) {
            val e = Offset(monster.x + side * cell * 4 + look, monster.y - monsterHeight / 2 - cell * 3)
            drawRect(Color.White, Offset(e.x - cell * 1.5f, e.y - cell), Size(cell * 3, cell * 2))
            drawRect(Color(0xFF202030), Offset(e.x - cell / 2 + look / 2, e.y - cell / 2), Size(cell, cell))
        }
    }
}

private fun DrawScope.drawLick(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    // 舔舔：粉紅小舌頭一伸一縮，身邊冒閃亮亮
    if ((t * 8).toInt() % 2 == 0 && t < 0.7f) {
        drawRect(Color(0xFFF08090), Offset(hero.x + heroSize * 0.15f, hero.y - heroSize * 0.05f), Size(cell * 2, cell * 2))
    }
    for (k in 0 until 6) {
        val p = seg(t, k * 0.1f, k * 0.1f + 0.4f)
        if (p <= 0f || p >= 1f) continue
        val a = k * TAU / 6
        val c = Offset(hero.x + cos(a) * heroSize * 0.55f, hero.y + sin(a) * heroSize * 0.45f)
        pixelStar(c, cell * (0.5f + p), Color.White.copy(alpha = 1f - p))
    }
    if (t > 0.4f) drawHeal(ItemType.SmallPotion, seg(t, 0.4f, 1f), hero, heroSize)
}

private fun DrawScope.drawSneeze(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val mouth = Offset(hero.x + heroSize * 0.4f, hero.y - heroSize * 0.1f)
    // 先「哈…」吸氣（頭上一點點白煙），再「啾！」噴出一團白煙
    if (t < 0.3f) {
        val puff = seg(t, 0.15f, 0.3f)
        for (k in 0 until 4) drawCircle(Color.White.copy(alpha = 0.8f * puff), cell * (2 + k), Offset(mouth.x + k * cell * 2 * puff, mouth.y - k * cell))
    }
    // 三顆小火球依序飛向怪物
    for (k in 0 until 3) {
        val p = seg(t, 0.25f + k * 0.12f, 0.6f + k * 0.12f)
        if (p <= 0f || p >= 1f) continue
        val c = Offset(mouth.x + (monster.x - mouth.x) * p, mouth.y + (monster.y - mouth.y) * p - sin(p * PI.toFloat()) * cell * (4 + k * 3))
        drawCircle(Color(0xFFF05020), cell * 2.5f, c)
        drawCircle(Color(0xFFF8D030), cell * 1.3f, c)
        drawRect(Color(0xFFF09030).copy(alpha = 0.6f), Offset(c.x - cell * 4, c.y - cell / 2), Size(cell * 2, cell))
    }
    if (t > 0.6f) {
        val f = seg(t, 0.6f, 1f)
        drawCircle(Color(0xFFF09030).copy(alpha = (1f - f) * 0.6f), cell * (5 + f * 8), monster)
    }
}

private fun DrawScope.drawRoar(t: Float, hero: Offset, monster: Offset, monsterHeight: Float, cell: Float) {
    // 一圈圈震波從龍嘴往外擴散
    for (ring in 0 until 4) {
        val p = seg(t, ring * 0.12f, 0.5f + ring * 0.12f)
        if (p <= 0f || p >= 1f) continue
        drawCircle(
            (if (ring % 2 == 0) Color(0xFFF09030) else Color.White).copy(alpha = 1f - p),
            cell * 4 + p * (monster.x - hero.x),
            hero,
            style = Stroke(cell),
        )
    }
    // 怪物嚇到：頭上「!」，冒冷汗、發抖
    if (t > 0.45f) {
        val shake = if ((t * 20).toInt() % 2 == 0) -cell else cell
        spriteCentered(BangArt, Offset(monster.x + shake, monster.y - monsterHeight / 2 - cell * 5), cell * 1.5f)
        for (k in 0 until 3) {
            val d = ((t - 0.45f) * 3 + k * 0.3f) % 1f
            drawRect(Color(0xFF90D0F8), Offset(monster.x - monsterHeight * 0.4f + k * cell * 4, monster.y - monsterHeight * 0.3f + d * cell * 8), Size(cell, cell * 2))
        }
    }
    // 整個畫面閃一下紅光
    if (t in 0.1f..0.3f) drawRect(Color(0xFFE02020).copy(alpha = 0.15f))
}

private fun DrawScope.drawBarrier(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    // 格子防護罩從身體往外展開
    val p = seg(t, 0f, 0.5f)
    val half = heroSize * 0.65f * p
    val color = Color(0xFF90D0F8).copy(alpha = 0.9f - seg(t, 0.7f, 1f) * 0.6f)
    var x = -half
    while (x <= half) {
        drawLine(color, Offset(hero.x + x, hero.y - half), Offset(hero.x + x, hero.y + half), cell / 2)
        x += cell * 4
    }
    var y = -half
    while (y <= half) {
        drawLine(color, Offset(hero.x - half, hero.y + y), Offset(hero.x + half, hero.y + y), cell / 2)
        y += cell * 4
    }
    drawRect(Color(0xFF3A58C8).copy(alpha = color.alpha), Offset(hero.x - half, hero.y - half), Size(half * 2, half * 2), style = Stroke(cell))
}

private fun DrawScope.drawDataSearch(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    // 頭上跑進度條，跑滿「逼」一聲印出小紙條
    val top = hero.y - heroSize * 0.8f
    val w = heroSize * 0.9f
    val left = hero.x - w / 2
    val fill = stepped(seg(t, 0f, 0.55f), 10)
    drawRect(Color(0xFF202030), Offset(left - cell, top - cell), Size(w + cell * 2, cell * 4))
    drawRect(Color(0xFF302848), Offset(left, top), Size(w, cell * 2))
    drawRect(Color(0xFF70E070), Offset(left, top), Size(w * fill, cell * 2))
    if (t > 0.55f) {
        val print = seg(t, 0.6f, 0.9f)
        val h = PaperArt.size * cell * print
        drawRect(Color(0xFF202030), Offset(hero.x - cell * 4.5f, top - cell * 3), Size(cell * 9, cell * 2))
        drawSprite(PaperArt.takeLast((PaperArt.size * print).toInt().coerceAtLeast(1)), hero.x - cell * 3.5f, top - cell * 3 - h, cell)
        if ((t * 10).toInt() % 2 == 0) pixelStar(Offset(hero.x + w / 2 + cell * 2, top - cell * 2), cell, Color(0xFFF8D030))
    }
}

private fun DrawScope.drawBleh(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    // 大大的鬼臉泡泡：紫色臉、翻白眼、吐舌頭，左右晃
    val pop = seg(t, 0f, 0.2f) * (1f - seg(t, 0.85f, 1f))
    val wob = sin(t * TAU * 3) * cell * 2
    val c = Offset(hero.x + heroSize * 0.6f + wob, hero.y - heroSize * 0.7f)
    val r = cell * 8 * pop
    if (r > 0f) {
        drawCircle(Color(0xFF202030), r + cell, c)
        drawCircle(Color(0xFF9060C0), r, c)
        drawRect(Color.White, Offset(c.x - r * 0.6f, c.y - r * 0.4f), Size(r * 0.4f, r * 0.3f))
        drawRect(Color.White, Offset(c.x + r * 0.2f, c.y - r * 0.4f), Size(r * 0.4f, r * 0.3f))
        drawRect(Color(0xFF202030), Offset(c.x - r * 0.5f, c.y + r * 0.15f), Size(r, r * 0.15f))
        drawRect(Color(0xFFF08090), Offset(c.x - r * 0.15f, c.y + r * 0.3f), Size(r * 0.35f, r * 0.45f))
    }
    // 怪物看傻了
    if (t > 0.3f) spriteCentered(QuestionArt, Offset(monster.x, monster.y - monsterHeight / 2 - cell * 5), cell * 1.3f, recolor = mapOf('Y' to 'q'))
    if (t > 0.3f && abs(sin(t * 40)) > 0.5f) spriteCentered(BangArt, Offset(monster.x + cell * 5, monster.y - monsterHeight / 2 - cell * 5), cell)
}
