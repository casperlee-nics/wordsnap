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
// 效果還在的招式（光芒、醫生、樂團、跟班…）用 drawLasting 一直畫著；粉絲加油、昏迷、被迷住另外畫

// 每一招動畫播多久（毫秒）
fun skillDuration(skill: Skill): Int = when (skill) {
    Skill.AreWeElves -> 6000
    Skill.OdinWrath, Skill.MeteorCall, Skill.FireWhisper, Skill.Battleship, Skill.FullArsenal -> 3000
    Skill.Orichalcum, Skill.PlagueDoctor, Skill.ElvenUprising -> 2600
    Skill.RoyalSoldiers, Skill.GoblinKing, Skill.UltimateArmor -> 2400
    Skill.CatStampede, Skill.HotSpring -> 2200
    Skill.CatOrchestra, Skill.BombChain, Skill.RocketLaunch, Skill.DragonFlight, Skill.CutestCat, Skill.EnergySip, Skill.Possession -> 2000
    Skill.FanCheer, Skill.BoulderThrow, Skill.DragonRoar, Skill.DataSearch, Skill.DragonHowl, Skill.MushroomStew,
    Skill.CrossSlash, Skill.AncientTome, Skill.StolenWatch, Skill.PurrHeal, Skill.MeteorClaw -> 1800
    Skill.SneezeFire, Skill.BushHide, Skill.DarkMist, Skill.HolyShieldBash, Skill.ForestBlessing,
    Skill.GurgleCurse, Skill.HealBubble, Skill.CheatSheet, Skill.Overclock, Skill.DemonEye, Skill.ElfEye -> 1600
    else -> 1400
}

// 動畫裡打中怪物的時間（毫秒）：怪物在這時候晃一下、血條掉下去
fun skillImpact(skill: Skill): Int {
    val at = when (skill) {
        Skill.AreWeElves -> 0.3f
        Skill.RoyalSoldiers, Skill.EnergySip -> 0.4f
        Skill.HammerToss, Skill.CatStampede, Skill.CrossSlash, Skill.FireWhisper -> 0.45f
        Skill.HolyShieldBash, Skill.BombChain, Skill.DragonFlight, Skill.MeteorCall, Skill.Battleship -> 0.5f
        Skill.MeteorClaw, Skill.FullArsenal -> 0.55f
        Skill.SwishArrow, Skill.SneezeFire, Skill.RocketLaunch, Skill.OdinWrath -> 0.6f
        Skill.BoulderThrow, Skill.ForestBlessing -> 0.68f
        else -> 0.6f
    }
    return (skillDuration(skill) * at).toInt()
}

// 會換背景的招式在動畫的哪個時間換（被煙塵、火焰、閃光蓋住的時候）
fun skillSceneSwap(skill: Skill): Int {
    val at = when (skill) {
        Skill.Battleship -> 0.1f
        Skill.ElvenUprising -> 0.4f
        Skill.FireWhisper -> 0.6f
        Skill.MeteorCall -> 0.65f
        Skill.FullArsenal -> 0.7f
        else -> 0.5f
    }
    return (skillDuration(skill) * at).toInt()
}

// 「我們都是精靈?」：場記板「開拍！」顯示到哪裡、導演出場和字幕從哪裡開始
const val ELVES_ACTION_UNTIL = 0.1f
const val ELVES_CAPTION_FROM = 0.62f

// 惡魔的附身：動畫播到這裡時換成新的職業
const val POSSESS_SWAP_AT = 0.6f

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
        Skill.RoyalSoldiers -> drawRoyalSoldiers(t, hero, heroSize, monster, ground, cell)
        Skill.HolyShieldBash -> drawHolyShieldBash(t, hero, heroSize, monster, cell)
        Skill.GurgleCurse -> drawGurgle(t, hero, heroSize, monster, monsterHeight, cell)
        Skill.HealBubble -> drawHealBubbles(t, hero, heroSize, ground, cell)
        Skill.AreWeElves -> drawElves(t, monster, monsterHeight, ground, cell)
        Skill.ForestBlessing -> drawForestBlessing(t, hero, heroSize, monster, cell)
        Skill.CheatSheet -> drawCheatSheet(t, hero, heroSize, cell)
        Skill.BombChain -> drawBombChain(t, hero, monster, cell)
        Skill.Orichalcum -> drawOrichalcum(t, hero, heroSize, ground, cell)
        Skill.MushroomStew -> drawStew(t, hero, heroSize, ground, cell)
        Skill.CatStampede -> drawStampede(t, ground, cell)
        Skill.CatOrchestra -> drawOrchestraStart(t, hero, heroSize, monster, monsterHeight, ground, cell)
        Skill.DragonFlight -> drawDragonFlight(t, monster, cell)
        Skill.DragonHowl -> drawHowl(t, hero, heroSize, cell)
        Skill.RocketLaunch -> drawRockets(t, hero, heroSize, monster, cell)
        Skill.Overclock -> drawOverclock(t, hero, heroSize, cell)
        Skill.PlagueDoctor -> drawPlagueDoctor(t, monster, monsterHeight, ground, cell)
        Skill.DemonEye -> drawDemonEye(t, hero, heroSize, monster, monsterHeight, cell)
        Skill.CrossSlash -> drawCrossSlash(t, monster, monsterHeight, cell)
        Skill.OdinWrath -> drawOdinWrath(t, monster, cell)
        Skill.AncientTome -> drawAncientTome(t, hero, heroSize, cell)
        Skill.MeteorCall -> drawMeteorCall(t, monster, cell)
        Skill.ElfEye -> drawElfEye(t, hero, heroSize, cell)
        Skill.ElvenUprising -> drawElvenUprising(t, cell)
        Skill.StolenWatch -> drawStolenWatch(t, hero, heroSize, cell)
        Skill.GoblinKing -> drawGoblinKing(t, hero, heroSize, ground, cell)
        Skill.HotSpring -> drawHotSpring(t, hero, heroSize, cell)
        Skill.UltimateArmor -> drawUltimateArmor(t, hero, heroSize, ground, cell)
        Skill.PurrHeal -> drawPurr(t, hero, heroSize, cell)
        Skill.CutestCat -> drawCutestCat(t, hero, heroSize, monster, monsterHeight, ground, cell)
        Skill.MeteorClaw -> drawMeteorClaw(t, hero, monster, monsterHeight, cell)
        Skill.FireWhisper -> drawFireWhisper(t, hero, heroSize, monster, cell)
        Skill.Battleship -> drawBattleship(t, hero, monster, cell)
        Skill.FullArsenal -> drawFullArsenal(t, hero, heroSize, monster, cell)
        Skill.EnergySip -> drawEnergySip(t, hero, heroSize, monster, cell)
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

// 小惡魔的鬼臉：紫色像素臉、翻白眼、吐舌頭
private val BlehArt = listOf(
    "..KKKKKKKK..",
    ".KppppppppK.",
    "KppWWppWWppK",
    "KppKWppKWppK",
    "KppWWppWWppK",
    "KppppppppppK",
    "KpKKKKKKKKpK",
    "KppKPPPPKppK",
    ".KppKPPKppK.",
    "..KKKPPKKK..",
    "....KPPK....",
    ".....KK.....",
)

private fun DrawScope.drawBleh(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    // 大大的像素鬼臉：一格一格變大，左右晃
    val pop = stepped(seg(t, 0f, 0.2f), 4) * (1f - stepped(seg(t, 0.85f, 1f), 4))
    if (pop > 0f) {
        val wob = if ((t * 8).toInt() % 2 == 0) -cell * 2 else cell * 2
        val c = Offset(hero.x + heroSize * 0.6f + wob, hero.y - heroSize * 0.7f)
        spriteCentered(BlehArt, c, cell * 1.4f * pop)
    }
    // 怪物看傻了
    if (t > 0.3f) spriteCentered(QuestionArt, Offset(monster.x, monster.y - monsterHeight / 2 - cell * 5), cell * 1.3f, recolor = mapOf('Y' to 'q'))
    if (t > 0.3f && abs(sin(t * 40)) > 0.5f) spriteCentered(BangArt, Offset(monster.x + cell * 5, monster.y - monsterHeight / 2 - cell * 5), cell)
}

// ======== LV30、LV40 的招式 ========

private val SoldierArt = listOf(
    ".....Y...M.",
    "...KKKK..M.",
    "..KMMMMK.D.",
    "..KSKSKK.D.",
    "..KSSSSK.D.",
    "..KBBBBKKD.",
    ".KSBYYBSKD.",
    "..KBBBBK.D.",
    "..KmmmmK.D.",
    "..KDKKDK...",
    "..KK..KK...",
)

private val HolyShieldArt = listOf(
    "KKKKKKKKK",
    "KYYYYYYYK",
    "KYYYWYYYK",
    "KYYYWYYYK",
    "KYWWWWWYK",
    "KYYYWYYYK",
    "KYYYWYYYK",
    ".KYYWYYK.",
    ".KYYYYYK.",
    "..KYYYK..",
    "...KYK...",
    "....K....",
)

// 童話裡的小精靈：尖帽子、尖耳朵
private val ElfArt = listOf(
    "....RRW.",
    "...RRR..",
    "..RRRR..",
    ".WWWWWW.",
    "SSKSSKSS",
    ".SSSSSS.",
    ".GGYYGG.",
    ".GGGGGG.",
    ".KK..KK.",
)

// 片場導演：貝雷帽、墨鏡、大聲公
private val DirectorArt = listOf(
    "...KKKK....",
    "..KrrrrK...",
    ".KrrrrrrK..",
    "..KSSSSK...",
    "..KKKKKK...",
    "..KSSSSK...",
    "...KSSK..Y.",
    "..KyyyyK.YY",
    ".KSyyyySYYY",
    "..KyyyyK.YY",
    "..KDDDDK.Y.",
    "..KDKKDK...",
    "..KK..KK...",
)

private val GoldAxeArt = listOf(
    ".YYY..",
    "YWYYYD",
    "YWYYND",
    "YYYYND",
    ".YYN.D",
    ".....D",
    ".....D",
    ".....D",
    ".....D",
    ".....e",
)

private val MushroomArt = listOf(
    ".RRR.",
    "RWRWR",
    "RRRRR",
    ".WWW.",
    ".WWW.",
)

// 往右跑的貓（兩格腳步輪流）
private val CatRunA = listOf(
    "O......O.O",
    ".O.....OOO",
    "..OOOOOOKO",
    "..OOOOOOOP",
    "..OOOOOO..",
    "..O.O.O.O.",
)
private val CatRunB = CatRunA.dropLast(1) + "...O.O.O.."

// 正面坐著的貓（貓咪樂團）
private val CatSitArt = listOf(
    "O......O",
    "OO....OO",
    "OOOOOOOO",
    "OKOOOOKO",
    "OOOPPOOO",
    ".OOOOOO.",
    ".OOOOOO.",
    ".OO..OO.",
)

private val NoteArt = listOf(
    "..KK.",
    "..K.K",
    "..K..",
    "KKK..",
    "KKK..",
)

// 瘟疫醫生：黑帽子、白色鳥嘴面具、黑袍（從人物這邊走過去，面向右邊的怪物）
private val DoctorArt = listOf(
    "....KKKK...",
    "....KKKK...",
    ".KKKKKKKK..",
    "..KwwwK....",
    "..KwwKwwK..",
    "..KwwwwwwwK",
    "..KwwwwKK..",
    "...KKKK....",
    "..KxxxxK...",
    ".KxxKKxxK..",
    ".KxxxxxxK..",
    ".KxxxxxxK..",
    "..KK..KK...",
)

// 這幾種貓的毛色：橘貓、白貓、灰貓、黑貓、虎斑、奶茶
private val CatCoats = listOf(
    mapOf('O' to 'O'),
    mapOf('O' to 'w'),
    mapOf('O' to 's'),
    mapOf('O' to 'K', 'K' to 'Y'),
    mapOf('O' to 'Y'),
    mapOf('O' to 'd'),
)

private fun lerp(a: Offset, b: Offset, p: Float) = Offset(a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p)

private fun DrawScope.drawExplosion(c: Offset, p: Float, cell: Float) {
    if (p <= 0f || p >= 1f) return
    drawCircle(Color(0xFFF05020).copy(alpha = 1f - p), cell * (3 + p * 8), c)
    drawCircle(Color(0xFFF8D030).copy(alpha = 1f - p), cell * (2 + p * 5), c)
    for (k in 0 until 6) {
        val a = k * TAU / 6 + 0.3f
        val d = cell * (4 + p * 10)
        drawRect(Color(0xFFF09030).copy(alpha = 1f - p), Offset(c.x + cos(a) * d, c.y + sin(a) * d), Size(cell * 1.5f, cell * 1.5f))
    }
}

private fun DrawScope.drawHit(monster: Offset, p: Float, cell: Float) {
    if (p <= 0f || p >= 1f) return
    drawCircle(Color(0xFFF8E8A0).copy(alpha = (1f - p) * 0.7f), cell * (4 + p * 12), monster)
}

// 聖光盾擊：金色十字盾出現在人物前面，衝過去撞怪物再飛回來
private fun DrawScope.drawHolyShieldBash(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val c4 = cell * 1.4f
    val w = HolyShieldArt.first().length * c4
    val appear = seg(t, 0f, 0.2f)
    val go = seg(t, 0.25f, 0.5f)
    val back = seg(t, 0.55f, 0.85f)
    val start = Offset(hero.x + heroSize * 0.5f, hero.y)
    val end = Offset(monster.x - w, monster.y)
    val c = lerp(start, end, if (t < 0.55f) go * go else 1f - back)
    val alpha = appear * (1f - seg(t, 0.9f, 1f))
    // 盾牌背後的光芒
    for (k in 0 until 8) {
        val a = k * TAU / 8 + stepped(t, 12) * 2
        drawLine(Color(0xFFF8E8A0).copy(alpha = alpha * 0.8f), c, Offset(c.x + cos(a) * w, c.y + sin(a) * w), cell / 2)
    }
    spriteCentered(HolyShieldArt, c, c4, alpha = alpha)
    drawHit(monster, seg(t, 0.48f, 0.7f), cell)
}

// 咕嚕咕嚕混亂咒：紫色泡泡咕嚕咕嚕飄到怪物頭上，繞成漩渦
private fun DrawScope.drawGurgle(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    val head = Offset(monster.x, monster.y - monsterHeight / 2 - cell * 3)
    val from = Offset(hero.x + heroSize * 0.3f, hero.y - heroSize * 0.4f)
    for (k in 0 until 10) {
        val p = seg(t, k * 0.05f, 0.45f + k * 0.05f)
        if (p <= 0f || p >= 1f) continue
        val c = lerp(from, head, p) + Offset(0f, sin(p * TAU * 2 + k) * cell * 4 - sin(p * PI.toFloat()) * cell * 10)
        val r = cell * (1.5f + k % 3)
        drawCircle(Color(0xFF9060C0), r, c)
        drawCircle(Color(0xFF202030), r, c, style = Stroke(cell / 2))
        drawRect(Color.White, Offset(c.x - r * 0.5f, c.y - r * 0.5f), Size(cell, cell))
    }
    if (t > 0.5f) {
        val p = seg(t, 0.5f, 1f)
        for (k in 0 until 8) {
            val a = (stepped(p, 16) * 2 + k / 8f) * TAU
            val r = cell * (3 + k) * (1f - p * 0.3f)
            drawRect(Color(0xFFC090E8).copy(alpha = 1f - p * 0.6f), Offset(head.x + cos(a) * r, head.y + sin(a) * r * 0.4f), Size(cell, cell))
        }
    }
    if (t in 0.45f..0.6f) drawRect(Color(0xFF9060C0).copy(alpha = 0.12f))
}

// 治癒泡泡：腳邊冒出一顆顆泡泡往上飄，破掉變成亮晶晶
private fun DrawScope.drawHealBubbles(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    for (k in 0 until 12) {
        val p = seg(t, k * 0.04f, 0.5f + k * 0.04f)
        if (p <= 0f || p >= 1f) continue
        val x = hero.x + ((k * 5) % 9 - 4) * heroSize * 0.12f + sin(p * TAU + k) * cell * 2
        val y = ground - p * heroSize * 1.3f
        val r = cell * (1.5f + k % 3)
        if (p < 0.85f) {
            drawCircle(Color(0xFF90D0F8).copy(alpha = 0.35f), r, Offset(x, y))
            drawCircle(Color(0xFF90D0F8), r, Offset(x, y), style = Stroke(cell / 2))
            drawRect(Color.White, Offset(x - r * 0.5f, y - r * 0.5f), Size(cell, cell))
        } else {
            pixelStar(Offset(x, y), cell, Color.White)
        }
    }
    if (t > 0.4f) drawHeal(ItemType.SmallPotion, seg(t, 0.4f, 1f), hero, heroSize)
}

// 偷看小抄：從袖子抽出小紙條，眼睛左看右看，打兩個勾（連續兩題）
private fun DrawScope.drawCheatSheet(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val out = seg(t, 0f, 0.25f)
    val hide = seg(t, 0.8f, 1f)
    val c4 = cell * (0.6f + out * 0.9f)
    val c = Offset(hero.x + heroSize * 0.35f, hero.y - out * heroSize * 0.85f + hide * heroSize * 0.6f)
    spriteCentered(PaperArt, c, c4, alpha = 1f - hide)
    // 從紙條上方偷看的眼睛
    if (t in 0.25f..0.8f) {
        val look = if ((t * 5).toInt() % 2 == 0) -cell else cell
        for (side in listOf(-1, 1)) {
            val e = Offset(c.x + side * cell * 3 + look, c.y - PaperArt.size * c4 / 2 - cell * 2)
            drawRect(Color.White, Offset(e.x - cell * 1.5f, e.y - cell), Size(cell * 3, cell * 2))
            drawRect(Color(0xFF202030), Offset(e.x - cell / 2 + look / 2, e.y - cell / 2), Size(cell, cell))
        }
        drawRect(Color(0xFF90D0F8), Offset(c.x + cell * 7, c.y - cell * 6 + ((t * 3) % 1f) * cell * 4), Size(cell, cell * 2))
    }
    // 打勾
    for (k in 0 until 2) {
        val p = seg(t, 0.45f + k * 0.12f, 0.55f + k * 0.12f)
        if (p <= 0f) continue
        val o = Offset(c.x - cell * 4 + k * cell * 7, c.y + PaperArt.size * c4 / 2 + cell * 3)
        val g = Color(0xFF40A048).copy(alpha = 1f - hide)
        drawLine(g, o, Offset(o.x + cell * 2 * p, o.y + cell * 2 * p), cell)
        if (p >= 1f) drawLine(g, Offset(o.x + cell * 2, o.y + cell * 2), Offset(o.x + cell * 5, o.y - cell * 2), cell)
    }
}

// 炸彈連環爆：丟出三顆冒火花的炸彈，一顆接一顆爆炸
private fun DrawScope.drawBombChain(t: Float, hero: Offset, monster: Offset, cell: Float) {
    for (k in 0 until 3) {
        val s = k * 0.12f
        val p = seg(t, s, s + 0.35f)
        val target = monster + Offset((k - 1) * cell * 7, (k % 2) * cell * 5 - cell * 3)
        if (p > 0f && p < 1f) {
            val c = lerp(hero, target, p) - Offset(0f, sin(p * PI.toFloat()) * cell * 18)
            drawCircle(Color(0xFF202030), cell * 3, c)
            drawRect(Color(0xFF707890), Offset(c.x - cell * 1.5f, c.y - cell * 1.5f), Size(cell, cell))
            drawLine(Color(0xFFC08848), Offset(c.x + cell * 2, c.y - cell * 2), Offset(c.x + cell * 3, c.y - cell * 4), cell / 2)
            if ((t * 20).toInt() % 2 == 0) pixelStar(Offset(c.x + cell * 3, c.y - cell * 5), cell * 0.7f, Color(0xFFF8D030))
        }
        drawExplosion(target, seg(t, s + 0.35f, s + 0.6f), cell)
    }
    if (t in 0.45f..0.75f && (t * 20).toInt() % 2 == 0) drawRect(Color.White.copy(alpha = 0.12f))
}

// 奧利哈鋼發現：十字鎬挖地 → 挖到發光的礦石 → 放上鐵砧敲三下 → 打出金色巨斧交給人物
private fun DrawScope.drawOrichalcum(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    val spot = Offset(hero.x + heroSize * 0.8f, ground)
    if (t < 0.32f) {
        // 十字鎬一下一下挖，土塊往上噴
        val swing = if ((t * 10).toInt() % 2 == 0) -40f else 15f
        val pivot = Offset(spot.x - cell * 4, spot.y - cell * 8)
        rotate(swing, pivot) {
            drawRect(Color(0xFF805030), Offset(pivot.x - cell / 2, pivot.y - cell * 8), Size(cell, cell * 9))
            drawRect(Color(0xFF707890), Offset(pivot.x - cell * 4, pivot.y - cell * 9), Size(cell * 8, cell * 2))
        }
        for (k in 0 until 5) {
            val p = (t * 6 + k * 0.2f) % 1f
            drawRect(Color(0xFF986030), Offset(spot.x + (k - 2) * cell * 2 * p, spot.y - sin(p * PI.toFloat()) * cell * 10), Size(cell, cell))
        }
    }
    // 挖到的礦石：金色帶一點藍綠的光
    val ore = seg(t, 0.2f, 0.32f)
    if (t in 0.2f..0.64f) {
        val c = Offset(spot.x, spot.y - cell * 2 - ore * cell * (if (t < 0.32f) 6 else 7))
        val glow = if ((t * 12).toInt() % 2 == 0) 0.6f else 0.3f
        drawCircle(Color(0xFF90F0D0).copy(alpha = glow), cell * 5, c)
        drawRect(Color(0xFFF8D030), Offset(c.x - cell * 2, c.y - cell * 1.5f), Size(cell * 4, cell * 3))
        drawRect(Color(0xFF70E0C0), Offset(c.x - cell, c.y - cell * 1.5f), Size(cell, cell))
        drawRect(Color.White, Offset(c.x + cell, c.y - cell / 2), Size(cell, cell))
    }
    // 鐵砧和鐵鎚：敲三下冒火花
    if (t in 0.32f..0.66f) {
        val top = spot.y - cell * 5
        drawRect(Color(0xFF202030), Offset(spot.x - cell * 6, top - cell / 2), Size(cell * 12, cell * 3))
        drawRect(Color(0xFF707890), Offset(spot.x - cell * 5.5f, top), Size(cell * 11, cell * 2))
        drawRect(Color(0xFF707890), Offset(spot.x - cell * 2, top + cell * 2), Size(cell * 4, cell * 3))
        val beat = seg(t, 0.34f, 0.64f) * 3 % 1f
        val lift = abs(cos(beat * PI.toFloat())) * cell * 8
        val hx = spot.x + cell * 3
        drawRect(Color(0xFF805030), Offset(hx, top - cell * 9 - lift), Size(cell, cell * 7))
        drawRect(Color(0xFF707890), Offset(hx - cell * 2, top - cell * 11 - lift), Size(cell * 5, cell * 3))
        if (beat < 0.25f) {
            for (k in 0 until 6) {
                val a = -PI.toFloat() * (0.1f + 0.8f * k / 5f)
                val d = cell * (3 + beat * 30)
                drawRect(Color(0xFFF8D030), Offset(spot.x + cos(a) * d, top - cell * 3 + sin(a) * d), Size(cell, cell))
            }
        }
    }
    // 金色巨斧升起、轉一圈發光，再飛到人物手上
    if (t > 0.64f) {
        val rise = seg(t, 0.64f, 0.8f)
        val give = seg(t, 0.84f, 1f)
        val c = lerp(Offset(spot.x, spot.y - cell * 6 - rise * heroSize * 0.6f), Offset(hero.x + heroSize * 0.45f, hero.y), give)
        for (k in 0 until 8) {
            val a = k * TAU / 8 + t * 4
            drawLine(Color(0xFFF8E8A0).copy(alpha = 0.7f * (1f - give)), c, Offset(c.x + cos(a) * cell * 12, c.y + sin(a) * cell * 12), cell / 2)
        }
        rotate(stepped(rise, 8) * 360f, c) {
            spriteCentered(GoldAxeArt, c, cell * 1.3f)
        }
    }
}

// 矮人蘑菇燉湯：蘑菇跳進鍋子，咕嘟咕嘟冒煙，喝一口補血
private fun DrawScope.drawStew(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    val pot = Offset(hero.x + heroSize * 0.75f, ground - cell * 5)
    // 鍋底的小火
    for (k in 0 until 3) {
        val flick = if (((t * 12).toInt() + k) % 2 == 0) cell else 0f
        drawRect(Color(0xFFF05020), Offset(pot.x - cell * 4 + k * cell * 3, ground - cell * 2 - flick), Size(cell * 2, cell * 2 + flick))
    }
    drawRect(Color(0xFF202030), Offset(pot.x - cell * 7, pot.y - cell * 5), Size(cell * 14, cell * 8))
    drawRect(Color(0xFF707890), Offset(pot.x - cell * 6, pot.y - cell * 4), Size(cell * 12, cell * 6))
    drawRect(Color(0xFFB85030), Offset(pot.x - cell * 6, pot.y - cell * 4), Size(cell * 12, cell * 2))
    drawRect(Color(0xFF202030), Offset(pot.x - cell * 8, pot.y - cell * 4), Size(cell * 2, cell))
    drawRect(Color(0xFF202030), Offset(pot.x + cell * 6, pot.y - cell * 4), Size(cell * 2, cell))
    for (k in 0 until 3) {
        val p = seg(t, k * 0.1f, 0.3f + k * 0.1f)
        if (p <= 0f || p >= 1f) continue
        val from = Offset(pot.x - cell * 18 + k * cell * 4, pot.y - cell * 14)
        val c = lerp(from, Offset(pot.x + (k - 1) * cell * 3, pot.y - cell * 4), p) - Offset(0f, sin(p * PI.toFloat()) * cell * 8)
        spriteCentered(MushroomArt, c, cell)
    }
    // 咕嘟咕嘟：泡泡和蒸氣
    if (t > 0.3f) {
        for (k in 0 until 4) {
            val p = ((t - 0.3f) * 2.5f + k * 0.25f) % 1f
            drawCircle(Color.White.copy(alpha = 0.7f * (1f - p)), cell * (1.5f + p * 2), Offset(pot.x + (k - 1.5f) * cell * 3 + sin(p * TAU) * cell, pot.y - cell * 5 - p * cell * 14))
        }
    }
    if (t > 0.55f) drawHeal(ItemType.SmallPotion, seg(t, 0.55f, 1f), hero, heroSize)
}

// 進擊的貓貓們～喵：一大群貓從左邊衝過整個畫面，踩過怪物，揚起灰塵
private fun DrawScope.drawStampede(t: Float, ground: Float, cell: Float) {
    val w = CatRunA.first().length * cell * 1.2f
    val h = CatRunA.size * cell * 1.2f
    for (i in 0 until 18) {
        val s = (i % 6) * 0.04f + (i / 6) * 0.1f
        val p = seg(t, s, s + 0.55f)
        if (p <= 0f || p >= 1f) continue
        val x = -w + p * (size.width + w * 2)
        val lane = (i * 5) % 3
        val y = ground - h - lane * cell * 3
        val frame = if (((t * 20).toInt() + i) % 2 == 0) CatRunA else CatRunB
        drawSprite(frame, x, y - (if (frame === CatRunB) cell else 0f), cell * 1.2f, recolor = CatCoats[i % CatCoats.size])
        // 揚起的灰塵
        drawCircle(Color(0xFFB8B0A0).copy(alpha = 0.6f), cell * 2, Offset(x - cell * 2, ground - cell - lane * cell * 3))
    }
}

// 貓咪交響樂團～喵（剛出場）：音符從人物身上冒出來，樂團升上來，愛心飄向怪物
private fun DrawScope.drawOrchestraStart(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, ground: Float, cell: Float) {
    drawCatBand(t * 2, hero, heroSize, ground, appear = seg(t, 0f, 0.4f))
    for (k in 0 until 6) {
        val p = seg(t, k * 0.06f, 0.5f + k * 0.06f)
        if (p <= 0f || p >= 1f) continue
        val c = Offset(hero.x + p * heroSize * (0.6f + k * 0.15f), hero.y - p * heroSize * 0.9f + sin(p * TAU + k) * cell * 2)
        spriteCentered(NoteArt, c, cell, alpha = 1f - p, recolor = mapOf('K' to if (k % 2 == 0) 'Y' else 'P'))
    }
    for (k in 0 until 4) {
        val p = seg(t, 0.4f + k * 0.08f, 0.8f + k * 0.08f)
        if (p <= 0f || p >= 1f) continue
        val c = lerp(Offset(size.width / 2, hero.y - heroSize * 0.6f), Offset(monster.x + (k - 1.5f) * cell * 4, monster.y - monsterHeight / 2), p)
        spriteCentered(HeartArt, c, cell)
    }
}

// 一團往上竄的像素火焰（龍之咆哮）
private fun DrawScope.drawFlames(clock: Float, hero: Offset, heroSize: Float, power: Float, cell: Float) {
    for (k in 0 until 7) {
        val x = hero.x + (k - 3) * heroSize * 0.16f
        val tall = heroSize * (0.25f + 0.3f * abs(sin(stepped(clock, 12) * TAU * 2 + k * 1.7f))) * power
        val base = hero.y + heroSize * 0.5f
        drawRect(Color(0xFFE04030).copy(alpha = 0.8f), Offset(x - cell * 1.5f, base - tall), Size(cell * 3, tall))
        drawRect(Color(0xFFF09030).copy(alpha = 0.85f), Offset(x - cell, base - tall * 0.7f), Size(cell * 2, tall * 0.7f))
        drawRect(Color(0xFFF8D030).copy(alpha = 0.9f), Offset(x - cell / 2, base - tall * 0.4f), Size(cell, tall * 0.4f))
    }
}

// 龍之咆哮：仰天大吼，紅色火焰從腳下竄起，畫面閃紅光
private fun DrawScope.drawHowl(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    drawFlames(t * 3, hero, heroSize, 0.4f + seg(t, 0f, 0.4f) * 1.2f - seg(t, 0.7f, 1f) * 0.9f, cell)
    for (ring in 0 until 3) {
        val p = seg(t, 0.1f + ring * 0.15f, 0.6f + ring * 0.15f)
        if (p <= 0f || p >= 1f) continue
        val c = Offset(hero.x, hero.y - heroSize * 0.4f - p * heroSize)
        drawOval(Color(0xFFF09030).copy(alpha = 1f - p), Offset(c.x - cell * (4 + p * 14), c.y - cell * (1 + p * 3)), Size(cell * (8 + p * 28), cell * (2 + p * 6)), style = Stroke(cell))
    }
    if ((t in 0.15f..0.3f) || (t in 0.45f..0.55f)) drawRect(Color(0xFFE02020).copy(alpha = 0.15f))
}

// 火箭發射：身上的艙門打開，四支小火箭往上飛再轉彎打中怪物
private fun DrawScope.drawRockets(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    for (k in 0 until 4) {
        val s = 0.1f + k * 0.07f
        val p = seg(t, s, s + 0.4f)
        val start = Offset(hero.x + (k - 1.5f) * cell * 3, hero.y - heroSize * 0.4f)
        val peak = Offset((hero.x + monster.x) / 2 + (k - 1.5f) * cell * 5, hero.y - heroSize * 1.6f)
        val end = monster + Offset((k - 1.5f) * cell * 3, (k % 2) * cell * 4)
        fun at(q: Float): Offset {
            val a = lerp(start, peak, q)
            val b = lerp(peak, end, q)
            return lerp(a, b, q)
        }
        if (t < s) {
            // 準備發射：火箭頭從身上冒出來
            val peek = seg(t, 0f, s)
            drawRect(Color(0xFFE04030), Offset(start.x - cell / 2, start.y - cell * 2 * peek), Size(cell, cell * 2 * peek))
            continue
        }
        if (p < 1f) {
            for (j in 1..5) {
                val q = (p - j * 0.04f).coerceAtLeast(0f)
                drawCircle(Color(0xFFE8E8F0).copy(alpha = 0.6f - j * 0.1f), cell * (1f + j * 0.4f), at(q))
            }
            val c = at(p)
            val d = at((p + 0.02f).coerceAtMost(1f)) - at((p - 0.02f).coerceAtLeast(0f))
            val deg = (kotlin.math.atan2(d.y, d.x) * 180f / PI.toFloat())
            rotate(deg, c) {
                drawRect(Color(0xFF202030), Offset(c.x - cell * 3, c.y - cell * 1.5f), Size(cell * 6, cell * 3))
                drawRect(Color(0xFFC0C8D8), Offset(c.x - cell * 2.5f, c.y - cell), Size(cell * 4.5f, cell * 2))
                drawRect(Color(0xFFE04030), Offset(c.x + cell * 2, c.y - cell), Size(cell, cell * 2))
                drawRect(Color(0xFFE04030), Offset(c.x - cell * 3, c.y - cell * 2), Size(cell * 1.5f, cell * 4))
                if ((t * 30).toInt() % 2 == 0) drawRect(Color(0xFFF8D030), Offset(c.x - cell * 4.5f, c.y - cell / 2), Size(cell * 1.5f, cell))
            }
        }
        drawExplosion(end, seg(t, s + 0.4f, s + 0.62f), cell)
    }
}

// 超頻模式：頭上的儀表指針衝到紅色，身體噴出蒸氣
private fun DrawScope.drawOverclock(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val c = Offset(hero.x, hero.y - heroSize * 0.75f)
    val r = cell * 7
    val show = seg(t, 0f, 0.1f) * (1f - seg(t, 0.85f, 1f))
    if (show > 0f) {
        val box = Offset(c.x - r - cell, c.y - r - cell)
        drawRect(Color(0xFF202030).copy(alpha = show), box, Size(r * 2 + cell * 2, r + cell * 3))
        val arcTop = Offset(c.x - r, c.y - r)
        drawArc(Color(0xFF70E070).copy(alpha = show), 180f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
        drawArc(Color(0xFFF8D030).copy(alpha = show), 240f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
        drawArc(Color(0xFFE04030).copy(alpha = show), 300f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
        // 指針一格一格往右衝，到紅色區抖動
        val sweep = stepped(seg(t, 0.1f, 0.45f), 8)
        val jitter = if (t > 0.45f && (t * 30).toInt() % 2 == 0) 0.08f else 0f
        val a = PI.toFloat() * (1f + sweep * 0.92f + jitter)
        drawLine(Color.White.copy(alpha = show), c, Offset(c.x + cos(a) * r * 0.9f, c.y + sin(a) * r * 0.9f), cell / 1.5f)
    }
    if (t > 0.4f) {
        drawSteam(t * 3, hero, heroSize, cell, seg(t, 0.4f, 0.5f) * (1f - seg(t, 0.85f, 1f)) * 2f)
        if ((t * 10).toInt() % 2 == 0) drawCircle(Color(0xFFF09030).copy(alpha = 0.25f), heroSize * 0.7f, hero)
    }
}

// 兩邊噴出的白色蒸氣
private fun DrawScope.drawSteam(clock: Float, hero: Offset, heroSize: Float, cell: Float, power: Float = 1f) {
    for (side in listOf(-1, 1)) {
        for (k in 0 until 3) {
            val p = (clock + k / 3f) % 1f
            val c = Offset(hero.x + side * (heroSize * 0.4f + p * cell * 8), hero.y - heroSize * 0.3f - p * cell * 10)
            drawCircle(Color.White.copy(alpha = (0.7f * (1f - p) * power).coerceIn(0f, 1f)), cell * (1.5f + p * 3), c)
        }
    }
}

// 魔眼混亂：人物頭上睜開一隻大眼睛，放出一圈圈催眠波
private fun DrawScope.drawDemonEye(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    val c = Offset(hero.x, hero.y - heroSize * 0.85f)
    val open = seg(t, 0f, 0.25f) * (1f - seg(t, 0.85f, 1f))
    val w = cell * 14
    val h = cell * 8 * open
    drawOval(Color(0xFF202030), Offset(c.x - w / 2 - cell, c.y - h / 2 - cell), Size(w + cell * 2, h + cell * 2))
    if (h > cell) {
        drawOval(Color(0xFFE8E8F0), Offset(c.x - w / 2, c.y - h / 2), Size(w, h))
        drawCircle(Color(0xFFE04030), minOf(h / 2, cell * 3.5f), c)
        drawRect(Color(0xFF202030), Offset(c.x - cell / 2, c.y - minOf(h / 2, cell * 3)), Size(cell, minOf(h, cell * 6)))
    }
    for (ring in 0 until 4) {
        val p = seg(t, 0.3f + ring * 0.1f, 0.65f + ring * 0.1f)
        if (p <= 0f || p >= 1f) continue
        val o = lerp(c, Offset(monster.x, monster.y - monsterHeight * 0.3f), p)
        drawCircle((if (ring % 2 == 0) Color(0xFF9060C0) else Color(0xFFE04030)).copy(alpha = 1f - p * 0.5f), cell * (3 + p * 5), o, style = Stroke(cell))
    }
    if (t > 0.65f) {
        val head = Offset(monster.x, monster.y - monsterHeight / 2 - cell * 3)
        for (k in 0 until 10) {
            val a = (stepped(t, 20) * 3 + k / 10f) * TAU
            val r = cell * (1 + k * 0.7f)
            drawRect(Color(0xFFC090E8), Offset(head.x + cos(a) * r, head.y + sin(a) * r * 0.5f), Size(cell, cell))
        }
    }
}

// ---- LV30 以上一直畫著的效果 ----

// 怪物生病了（瘟疫）：頭上冒綠色泡泡、流綠色冷汗
fun DrawScope.drawSick(clock: Float, monster: Offset, monsterHeight: Float) {
    val cell = 3.dp.toPx()
    for (k in 0 until 3) {
        val p = (clock + k / 3f) % 1f
        val c = Offset(monster.x + (k - 1) * cell * 5 + sin(p * TAU) * cell, monster.y - monsterHeight / 2 - p * cell * 10)
        drawCircle(Color(0xFF70C058).copy(alpha = 1f - p), cell * (1.2f + p), c)
    }
    val drop = (clock * 1.5f) % 1f
    drawRect(Color(0xFF70C058), Offset(monster.x - monsterHeight * 0.45f, monster.y - monsterHeight * 0.25f + drop * cell * 6), Size(cell, cell * 2))
}

// 怪物被貓咪樂團迷住了：頭上愛心繞圈
fun DrawScope.drawCharmed(clock: Float, monster: Offset, monsterHeight: Float) {
    val cell = 3.dp.toPx()
    val center = Offset(monster.x, monster.y - monsterHeight / 2 - cell * 3)
    for (k in 0 until 3) {
        val a = (stepped(clock, 8) + k / 3f) * TAU
        spriteCentered(HeartArt, Offset(center.x + cos(a) * cell * 7, center.y + sin(a) * cell * 2), cell * 0.9f)
    }
}

// 貓咪樂團：三隻貓在畫面中間後面拉小提琴、吹喇叭、打鼓，音符飄起來（直到戰鬥結束）
private fun DrawScope.drawCatBand(clock: Float, hero: Offset, heroSize: Float, ground: Float, appear: Float = 1f) {
    val cell = 2.dp.toPx()
    val w = CatSitArt.first().length * cell
    val h = CatSitArt.size * cell
    val middle = maxOf(size.width / 2, hero.x + heroSize * 0.5f + w * 3)
    val top = ground - heroSize * 1.55f + (1f - appear) * heroSize
    if (appear <= 0f) return
    for (i in 0 until 3) {
        val x = middle + (i - 1) * w * 2f - w / 2
        val beat = ((clock * 8).toInt() + i) % 2 == 0
        val y = top - (if (beat) cell else 0f)
        drawSprite(CatSitArt, x, y, cell, alpha = appear, recolor = CatCoats[(i * 2 + 1) % CatCoats.size])
        when (i) {
            0 -> {
                // 小提琴和琴弓
                drawOval(Color(0xFFA06030).copy(alpha = appear), Offset(x + w - cell, y + h * 0.4f), Size(cell * 3, cell * 4))
                val bow = if (beat) cell * 2 else -cell * 2
                drawLine(Color(0xFFE8E8F0).copy(alpha = appear), Offset(x + w - cell * 2 + bow, y + h * 0.3f), Offset(x + w + cell * 3 + bow, y + h * 0.75f), cell / 2)
            }
            1 -> {
                // 喇叭
                drawRect(Color(0xFFF8D030).copy(alpha = appear), Offset(x + w * 0.5f, y + h * 0.55f), Size(w * 0.7f, cell))
                drawRect(Color(0xFFF8D030).copy(alpha = appear), Offset(x + w * 1.1f, y + h * 0.45f), Size(cell * 1.5f, cell * 3))
            }
            else -> {
                // 小鼓和鼓棒
                drawRect(Color(0xFFE04030).copy(alpha = appear), Offset(x, y + h), Size(w, cell * 3))
                drawRect(Color(0xFFE8E8F0).copy(alpha = appear), Offset(x, y + h), Size(w, cell))
                val stick = if (beat) 0f else cell * 2
                drawLine(Color(0xFFC08848).copy(alpha = appear), Offset(x + cell, y + h * 0.6f), Offset(x + cell * 3, y + h - stick), cell / 2)
            }
        }
        // 飄起來的音符
        val p = (clock + i / 3f) % 1f
        spriteCentered(NoteArt, Offset(x + w / 2 + sin(p * TAU) * cell * 3, top - cell * 3 - p * cell * 12), cell, alpha = (1f - p) * appear, recolor = mapOf('K' to if (i == 1) 'P' else 'Y'))
    }
}

// ======== v0.98 改過的招式 ========

// 學者：方帽、眼鏡、咖啡色長袍
private val ScholarArt = listOf(
    "..KKKKKKK..",
    "...KKKK..Y.",
    "..KSSSSK.Y.",
    "..KWKKWK...",
    "..KSSSSK...",
    "..KeeeeK...",
    ".KSeeeeSK..",
    "..KeeeeK...",
    "..KeeeeK...",
    "..KDKKDK...",
    "..KK..KK...",
)

// 白魔法師：白色連帽長袍、紅色三角花紋、法杖
private val WhiteMageArt = listOf(
    "...KKKK..Y.",
    "..KWWWWK.D.",
    ".KWWSSWWKD.",
    ".KWKSSKWKD.",
    ".KWSSSSWKD.",
    "..KWWWWKKD.",
    ".KSWWWWSKD.",
    "..KWWWWK.D.",
    "..KWRWRK...",
    "..KWWWWK...",
    "..KK..KK...",
)

// 彼得潘故事裡那種會發光的小仙子（自己畫的，不是電影裡的角色）
private val FairyArt = listOf(
    "LL...LL",
    "LLL.LLL",
    ".LLYLL.",
    "..YSY..",
    "...Y...",
    "..Y.Y..",
)

// 神燈精靈：頭巾、淡紫色皮膚、下半身是一縷煙
private val GenieArt = listOf(
    "....YRRY....",
    "...RRRRRR...",
    "...KqqqqK...",
    "...qKqqKq...",
    "...qqqqqq...",
    "....qKKq....",
    "..qqqqqqqq..",
    ".qqKqqqqKqq.",
    ".qq.YYYY.qq.",
    "....wwww....",
    ".....ww.....",
    "......w.....",
)

// 往右飛的小飛龍
private val FlyDragonArt = listOf(
    "..KK........",
    ".KGGK...KKK.",
    "KGGGGKKKGGYK",
    ".KGGGGGGGGGK",
    "..KGGGGGGKK.",
    "...KGK.KGK..",
)

// 哥布林小跟班
private val MinionArt = listOf(
    "G.KKK.G",
    "GKGGGKG",
    ".KYGYK.",
    ".KGGGK.",
    "..KDK..",
    ".KDDDK.",
    ".KDDDK.",
    ".KK.KK.",
)

private val CrownArt = listOf(
    "Y..Y..Y",
    "YY.Y.YY",
    "YYRYBYY",
    "YYYYYYY",
)

private val DrumstickArt = listOf(
    "..dddd",
    ".dddddd",
    ".ddddd.",
    "W.ddd..",
    ".W.....",
    "W.W....",
).map { it.padEnd(7, '.') }

// 招喚王國的士兵：士兵、學者、白魔法師走進來，士兵戳怪物、學者舉黑板寫答案、白魔法師灑光補血
private fun DrawScope.drawRoyalSoldiers(t: Float, hero: Offset, heroSize: Float, monster: Offset, ground: Float, cell: Float) {
    val w = SoldierArt.first().length * cell
    val h = SoldierArt.size * cell
    val march = seg(t, 0f, 0.3f)
    val leave = seg(t, 0.86f, 1f)
    val poke = sin(seg(t, 0.3f, 0.5f) * TAU).coerceAtLeast(0f) * cell * 4
    val arts = listOf(SoldierArt, ScholarArt, WhiteMageArt)
    val xs = FloatArray(3)
    for (i in 0 until 3) {
        val target = monster.x - w * (1.4f + i * 1.1f)
        val start = -w * (1.5f + i)
        var x = start + (target - start) * march
        x += (start - x) * leave
        xs[i] = x
        val walking = (march in 0.01f..0.99f) || leave > 0f
        val step = if (walking && ((t * 18).toInt() + i) % 2 == 0) cell else 0f
        drawSprite(arts[i], x - w / 2 + (if (i == 0) poke else 0f), ground - h - step, cell)
    }
    drawHit(monster, seg(t, 0.36f, 0.6f), cell)
    // 學者舉起黑板，上面寫著答案
    val board = seg(t, 0.45f, 0.52f) * (1f - seg(t, 0.8f, 0.86f))
    if (board > 0f) {
        val c = Offset(xs[1], ground - h - cell * 7 - board * cell * 2)
        drawRect(Color(0xFF805030), Offset(c.x - cell * 7, c.y - cell * 5), Size(cell * 14, cell * 9))
        drawRect(Color(0xFF2C3C28), Offset(c.x - cell * 6, c.y - cell * 4), Size(cell * 12, cell * 7))
        val write = stepped(seg(t, 0.5f, 0.7f), 6)
        for (k in 0 until 3) drawRect(Color.White, Offset(c.x - cell * 5, c.y - cell * 3 + k * cell * 2), Size(cell * (10 - k * 3) * write, cell))
        drawLine(Color(0xFF805030), Offset(c.x, c.y + cell * 4), Offset(c.x, ground - h + cell * 3), cell)
    }
    // 白魔法師揮法杖，光點灑到人物身上
    if (t in 0.55f..0.9f) {
        val p = seg(t, 0.55f, 0.9f)
        for (k in 0 until 8) {
            val q = (p * 1.5f - k * 0.06f).coerceIn(0f, 1f)
            if (q <= 0f || q >= 1f) continue
            val from = Offset(xs[2] + w * 0.3f, ground - h)
            val c = lerp(from, hero + Offset((k - 4) * cell * 2f, -heroSize * 0.2f), q) - Offset(0f, sin(q * PI.toFloat()) * cell * 10)
            pixelStar(c, cell * 0.7f, if (k % 2 == 0) Color.White else Color(0xFFF8D030))
        }
    }
    if (t > 0.65f) drawHeal(ItemType.SmallPotion, seg(t, 0.65f, 1f), hero, heroSize)
}

// 我們都是精靈?：場記板「開拍！」→ 小精靈跑來敲敲打打、小仙子灑亮粉、神燈精靈從燈裡冒出來揍怪物
// → 導演拿大聲公跑進來把大家拖走（字幕另外顯示）
private fun DrawScope.drawElves(t: Float, monster: Offset, monsterHeight: Float, ground: Float, cell: Float) {
    // 場記板：板子一拍「啪！」
    if (t < ELVES_ACTION_UNTIL + 0.02f) {
        val c = Offset(size.width / 2, size.height * 0.35f)
        val shut = seg(t, 0.03f, 0.07f)
        drawRect(Color(0xFF202030), Offset(c.x - cell * 9, c.y - cell * 2), Size(cell * 18, cell * 11))
        for (k in 0 until 3) drawRect(Color.White, Offset(c.x - cell * 7, c.y + cell * (1 + k * 2.5f)), Size(cell * 14, cell / 2))
        rotate(-30f * (1f - shut), Offset(c.x - cell * 9, c.y - cell * 2)) {
            drawRect(Color(0xFF202030), Offset(c.x - cell * 9, c.y - cell * 5), Size(cell * 18, cell * 3))
            for (k in 0 until 4) drawRect(Color.White, Offset(c.x - cell * 8 + k * cell * 4.5f, c.y - cell * 5), Size(cell * 2, cell * 3))
        }
        if (shut >= 1f && t < 0.09f) drawRect(Color.White.copy(alpha = 0.4f))
    }
    if (t < ELVES_ACTION_UNTIL) return
    val w = ElfArt.first().length * cell
    val h = ElfArt.size * cell
    val run = seg(t, 0.1f, 0.2f)
    // 被導演拖走：大家一起往左滑出畫面
    val drag = seg(t, 0.7f, 0.97f)
    val dragDx = -drag * drag * (monster.x + w * 10)
    val elfX = List(3) { i -> monster.x + (i - 1) * w * 1.3f - w / 2 }
    for (i in 0 until 3) {
        var x = -w * (2 + i) + (elfX[i] + w * (2 + i)) * run
        var y = ground - h
        if (t in 0.2f..0.45f) {
            val hop = abs(sin((t - 0.2f) * TAU * 4 + i))
            y -= hop * monsterHeight * 0.5f
        }
        if (drag > 0f) {
            x = elfX[i] + dragDx
            y = ground - h + if (((t * 30).toInt() + i) % 2 == 0) -cell else 0f
        }
        val colors = if (i == 1) mapOf('R' to 'G', 'G' to 'R') else emptyMap()
        drawSprite(ElfArt, x, y, cell, recolor = colors)
        if (t in 0.22f..0.45f && ((t * 16).toInt() + i) % 3 == 0) pixelStar(Offset(x + w, y), cell, Color(0xFFF8D030))
    }
    drawHit(monster, seg(t, 0.28f, 0.4f), cell)
    // 小仙子：從左上飛進來，繞著怪物轉圈灑亮粉，最後跟著大家飛走
    if (t in 0.15f..0.97f) {
        val fly = seg(t, 0.15f, 0.25f)
        val around = (t - 0.25f) * TAU * 3
        val orbit = Offset(monster.x + cos(around) * monsterHeight * 0.7f, monster.y - monsterHeight * 0.4f + sin(around) * cell * 6)
        var c = if (t < 0.25f) lerp(Offset(0f, size.height * 0.1f), orbit, fly) else orbit
        if (drag > 0f) c = Offset(c.x + dragDx * 1.1f, c.y - drag * cell * 10)
        drawCircle(Color(0xFFF8F0A0).copy(alpha = 0.45f), cell * 4, c)
        spriteCentered(FairyArt, c, cell * 0.8f, recolor = if ((t * 20).toInt() % 2 == 0) emptyMap() else mapOf('L' to 'W'))
        for (k in 1..5) {
            val trail = Offset(c.x - cos(around) * k * cell * 1.5f, c.y + k * cell * 1.5f)
            drawRect(Color(0xFFF8D030).copy(alpha = 1f - k / 6f), trail, Size(cell / 1.5f, cell / 1.5f))
        }
    }
    // 神燈精靈：神燈出現 → 冒煙 → 精靈長出來 → 大拳頭揍下去；被拖走時縮回燈裡
    if (t in 0.3f..0.97f) {
        val lamp = Offset(monster.x - monsterHeight * 0.5f - cell * 12 + (if (drag > 0f) dragDx else 0f), ground - cell * 2)
        drawRect(Color(0xFF202030), Offset(lamp.x - cell * 4, lamp.y - cell * 3), Size(cell * 9, cell * 4))
        drawRect(Color(0xFFF8D030), Offset(lamp.x - cell * 3.5f, lamp.y - cell * 2.5f), Size(cell * 8, cell * 3))
        drawRect(Color(0xFFF8D030), Offset(lamp.x + cell * 4.5f, lamp.y - cell * 3.5f), Size(cell * 3, cell))
        drawRect(Color(0xFFD0A858), Offset(lamp.x - cell, lamp.y - cell * 4), Size(cell * 3, cell * 1.5f))
        val smoke = seg(t, 0.3f, 0.38f)
        for (k in 0 until 5) {
            val p = (smoke * 1.4f - k * 0.08f).coerceIn(0f, 1f)
            if (p <= 0f || t > 0.5f) continue
            drawCircle(Color(0xFFC090E8).copy(alpha = 0.7f * (1f - p * 0.5f)), cell * (2 + k), Offset(lamp.x + sin(p * TAU) * cell * 2, lamp.y - cell * 4 - p * cell * 18))
        }
        val grow = seg(t, 0.36f, 0.44f) * (1f - seg(t, 0.72f, 0.8f))
        if (grow > 0f) {
            val gc = cell * 1.3f * grow
            val gh = GenieArt.size * gc
            val top = Offset(lamp.x, lamp.y - cell * 4 - gh / 2)
            spriteCentered(GenieArt, top, gc)
            // 大拳頭揍怪物
            val punch = seg(t, 0.44f, 0.5f) * (1f - seg(t, 0.52f, 0.58f))
            if (punch > 0f) {
                val fist = lerp(top + Offset(gh * 0.3f, 0f), monster, punch * 0.8f)
                drawCircle(Color(0xFF202030), cell * 3.5f, fist)
                drawCircle(Color(0xFFC090E8), cell * 3f, fist)
            }
        }
        drawHit(monster, seg(t, 0.48f, 0.58f), cell)
    }
    // 擺姿勢，怪物看傻了
    if (t in 0.52f..0.7f) spriteCentered(QuestionArt, Offset(monster.x + w * 1.8f, monster.y - monsterHeight / 2 - cell * 4), cell * 1.3f)
    // 導演從左邊跑過來
    if (t > ELVES_CAPTION_FROM) {
        val dw = DirectorArt.first().length * cell
        val dh = DirectorArt.size * cell
        val come = seg(t, ELVES_CAPTION_FROM, 0.7f)
        val target = monster.x - monsterHeight * 0.5f - cell * 18 - dw
        val x = -dw + (target + dw) * come + dragDx
        val step = if (come in 0.01f..0.99f && (t * 24).toInt() % 2 == 0) cell else 0f
        drawSprite(DirectorArt, x, ground - dh - step, cell)
        if (t in 0.68f..0.74f) spriteCentered(BangArt, Offset(x + dw / 2, ground - dh - cell * 4), cell)
    }
}

// 森林的庇護：樹葉繞著人物轉、補血，再射出一支綠色的箭
private fun DrawScope.drawForestBlessing(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val gather = seg(t, 0f, 0.35f)
    val fade = 1f - seg(t, 0.85f, 1f)
    for (k in 0 until 10) {
        val a = k * TAU / 10 + (1f - gather) * TAU + t * 3
        val r = heroSize * (1.2f - 0.55f * gather)
        val c = Offset(hero.x + cos(a) * r, hero.y + sin(a) * r * 0.7f)
        val color = if (k % 2 == 0) Color(0xFF40A048) else Color(0xFF70C058)
        drawRect(color.copy(alpha = fade), Offset(c.x - cell, c.y - cell / 2), Size(cell * 2, cell))
        drawRect(color.copy(alpha = fade), Offset(c.x - cell / 2, c.y - cell), Size(cell, cell * 2))
    }
    if (t > 0.15f) drawHeal(ItemType.SmallPotion, seg(t, 0.15f, 0.7f), hero, heroSize)
    if (t > 0.3f) drawArrowShot(seg(t, 0.3f, 1f), hero, monster, cell)
}

// 群龍飛舞：兩隻小飛龍從左邊飛過天空，經過怪物上方時往下吐火
private fun DrawScope.drawDragonFlight(t: Float, monster: Offset, cell: Float) {
    val c4 = cell * 1.3f
    val w = FlyDragonArt.first().length * c4
    val h = FlyDragonArt.size * c4
    for (i in 0 until 2) {
        val p = seg(t, i * 0.08f, 0.85f + i * 0.08f)
        if (p <= 0f || p >= 1f) continue
        val x = -w + p * (size.width + w * 2)
        val y = size.height * (0.08f + i * 0.14f) + sin(p * TAU * 2 + i) * cell * 2
        // 翅膀上下拍
        val up = ((t * 14).toInt() + i) % 2 == 0
        val wingY = if (up) y - c4 * 3 else y + c4
        drawRect(Color(0xFF202030), Offset(x + w * 0.3f, wingY), Size(w * 0.35f, c4 * 2.5f))
        drawRect(if (i == 0) Color(0xFF70C058) else Color(0xFFF09030), Offset(x + w * 0.3f + c4 / 2, wingY + c4 / 2), Size(w * 0.35f - c4, c4 * 1.5f))
        drawSprite(FlyDragonArt, x, y, c4, recolor = if (i == 1) mapOf('G' to 'r', 'Y' to 'Y') else emptyMap())
        // 飛到怪物上方附近就往下吐火
        val mouth = Offset(x + w, y + h * 0.5f)
        if (abs(mouth.x - monster.x) < w * 1.8f) {
            for (k in 0 until 7) {
                val q = ((t * 8 + k / 7f) % 1f)
                val c = lerp(mouth, monster + Offset((k - 3) * cell, 0f), q)
                drawCircle(if (k % 2 == 0) Color(0xFFF05020) else Color(0xFFF8D030), cell * (1.2f + q * 3), c)
            }
        }
    }
    drawHit(monster, seg(t, 0.45f, 0.7f), cell)
}

private fun doctorLeft(monster: Offset, monsterHeight: Float, cell: Float) =
    monster.x - monsterHeight * 0.5f - DoctorArt.first().length * cell - cell * 2

// 瘟疫降臨：瘟疫醫生從人物這邊走到怪物旁邊，拐杖一揮，綠色瘴氣飄到怪物身上（之後一直站在旁邊）
private fun DrawScope.drawPlagueDoctor(t: Float, monster: Offset, monsterHeight: Float, ground: Float, cell: Float) {
    val w = DoctorArt.first().length * cell
    val h = DoctorArt.size * cell
    val stand = doctorLeft(monster, monsterHeight, cell)
    val come = seg(t, 0f, 0.35f)
    val x = -w + (stand + w) * come
    val step = if (come in 0.01f..0.99f && (t * 16).toInt() % 2 == 0) cell else 0f
    drawSprite(DoctorArt, x, ground - h - step, cell)
    // 拐杖：揮瘴氣時舉起來
    val raise = seg(t, 0.35f, 0.42f) * (1f - seg(t, 0.85f, 0.95f))
    val hand = Offset(x + w - cell * 2, ground - h * 0.4f)
    val tip = Offset(hand.x + cell * 3 + raise * cell * 4, hand.y + cell * 7 - raise * cell * 14)
    drawLine(Color(0xFF805030), hand, tip, cell)
    if (t in 0.38f..0.9f) {
        val p = seg(t, 0.38f, 0.9f)
        for (k in 0 until 7) {
            val q = (p * 1.6f - k * 0.1f).coerceIn(0f, 1f)
            if (q <= 0f || q >= 1f) continue
            val c = lerp(tip, monster + Offset((k - 3) * cell * 3, (k % 3 - 1) * cell * 4), q)
            drawCircle(Color(0xFF70C058).copy(alpha = 0.7f * (1f - q * 0.5f)), cell * (2 + k % 3), c)
        }
        drawCircle(Color(0xFF70C058).copy(alpha = 0.25f * seg(t, 0.5f, 0.65f)), monsterHeight * 0.55f, monster)
    }
    if (t > 0.6f) drawSick(t * 2, monster, monsterHeight)
}

// 瘟疫結束：醫生鞠個躬，走回人物那邊離開
fun DrawScope.drawDoctorLeave(t: Float, monster: Offset, monsterHeight: Float, ground: Float) {
    val cell = 3.dp.toPx()
    val w = DoctorArt.first().length * cell
    val h = DoctorArt.size * cell
    val stand = doctorLeft(monster, monsterHeight, cell)
    val bow = if (t < 0.25f) cell * 2 else 0f
    val go = seg(t, 0.25f, 1f)
    val x = stand - go * (stand + w)
    val step = if (go > 0f && (t * 16).toInt() % 2 == 0) cell else 0f
    drawSprite(DoctorArt.map { it.reversed() }.takeIf { go > 0f } ?: DoctorArt, x, ground - h - step + bow, cell)
}

// ======== LV50、LV60 的招式 ========

// 聖劍十字斬：劍光先直劈再橫斬，在怪物身上畫出大十字，畫面閃白
private fun DrawScope.drawCrossSlash(t: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    val len = monsterHeight * 0.9f + cell * 10
    val v = seg(t, 0.1f, 0.25f)
    val hz = seg(t, 0.28f, 0.42f)
    val fade = 1f - seg(t, 0.65f, 0.95f)
    if (v > 0f) {
        drawLine(Color(0xFFF8E8A0).copy(alpha = 0.6f * fade), Offset(monster.x, monster.y - len), Offset(monster.x, monster.y - len + len * 2 * v), cell * 4)
        drawLine(Color.White.copy(alpha = fade), Offset(monster.x, monster.y - len), Offset(monster.x, monster.y - len + len * 2 * v), cell * 1.5f)
    }
    if (hz > 0f) {
        drawLine(Color(0xFFF8E8A0).copy(alpha = 0.6f * fade), Offset(monster.x - len, monster.y - cell * 3), Offset(monster.x - len + len * 2 * hz, monster.y - cell * 3), cell * 4)
        drawLine(Color.White.copy(alpha = fade), Offset(monster.x - len, monster.y - cell * 3), Offset(monster.x - len + len * 2 * hz, monster.y - cell * 3), cell * 1.5f)
    }
    if (t in 0.42f..0.6f) drawRect(Color.White.copy(alpha = 0.8f * (1f - seg(t, 0.42f, 0.6f))))
    if (t > 0.45f) {
        for (k in 0 until 8) {
            val p = seg(t, 0.45f, 0.9f)
            val a = k * TAU / 8
            pixelStar(Offset(monster.x + cos(a) * cell * (4 + p * 16), monster.y + sin(a) * cell * (4 + p * 16)), cell, Color(0xFFF8D030).copy(alpha = 1f - p))
        }
    }
}

// 勇者奧丁的憤怒：天空變暗、烏雲滾滾，一道大閃電劈在怪物身上
private fun DrawScope.drawOdinWrath(t: Float, monster: Offset, cell: Float) {
    val dark = seg(t, 0f, 0.25f) * (1f - seg(t, 0.85f, 1f))
    drawRect(Color(0xFF140C18).copy(alpha = 0.55f * dark))
    // 烏雲從兩邊滾進來
    for (k in 0 until 9) {
        val side = if (k % 2 == 0) -1f else 1f
        val roll = seg(t, 0f, 0.35f)
        val x = size.width / 2 + side * (size.width * 0.7f - roll * size.width * (0.2f + k * 0.06f))
        val y = cell * (4 + (k % 3) * 4)
        drawCircle(Color(0xFF483868).copy(alpha = dark), cell * (6 + k % 3 * 2), Offset(x, y))
        drawCircle(Color(0xFF302848).copy(alpha = dark), cell * (4 + k % 2 * 2), Offset(x + cell * 4, y + cell * 2))
    }
    // 雲裡先閃兩下，再劈下來
    if (t in 0.35f..0.4f || t in 0.45f..0.48f) drawRect(Color.White.copy(alpha = 0.25f))
    if (t in 0.55f..0.75f) {
        val pts = mutableListOf(Offset(monster.x + cell * 6, 0f))
        var y = 0f
        var k = 0
        while (y < monster.y) {
            y += cell * 6
            k++
            pts.add(Offset(monster.x + (if (k % 2 == 0) cell * 5 else -cell * 4), minOf(y, monster.y)))
        }
        for (i in 1 until pts.size) {
            drawLine(Color(0xFFF8E8A0), pts[i - 1], pts[i], cell * 4)
            drawLine(Color.White, pts[i - 1], pts[i], cell * 1.5f)
        }
        if (t < 0.62f) drawRect(Color.White.copy(alpha = 0.7f))
    }
    drawExplosion(monster, seg(t, 0.6f, 0.9f), cell * 1.5f)
}

// 古代魔導書：厚書翻開，發光字母一個個飛到下面的輸入框
private fun DrawScope.drawAncientTome(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val c = Offset(hero.x + heroSize * 0.3f, hero.y - heroSize * 0.85f + sin(t * TAU) * cell)
    val open = seg(t, 0f, 0.25f)
    val fade = 1f - seg(t, 0.85f, 1f)
    drawCircle(Color(0xFFF8E8A0).copy(alpha = 0.4f * open * fade), cell * 10, c)
    // 封面（咖啡色）往兩邊打開，露出白色書頁
    drawRect(Color(0xFF805030).copy(alpha = fade), Offset(c.x - cell * 8 * open - cell, c.y - cell * 4), Size(cell * 8 * open + cell * 2 + cell * 8 * open, cell * 9))
    drawRect(Color(0xFFF0E0C0).copy(alpha = fade), Offset(c.x - cell * 7 * open, c.y - cell * 3), Size(cell * 14 * open, cell * 7))
    drawRect(Color(0xFF202030).copy(alpha = fade), Offset(c.x - cell / 4, c.y - cell * 3), Size(cell / 2, cell * 7))
    for (k in 0 until 6) {
        val p = seg(t, 0.25f + k * 0.08f, 0.55f + k * 0.08f)
        if (p <= 0f || p >= 1f) continue
        val target = Offset(size.width / 2 + (k - 2.5f) * cell * 5, size.height + cell * 4)
        val l = lerp(c, target, p)
        drawRect(Color(0xFFF8D030), Offset(l.x - cell, l.y - cell * 1.5f), Size(cell * 2, cell * 3))
        drawRect(Color.White, Offset(l.x - cell / 2, l.y - cell), Size(cell, cell))
    }
}

// 隕石招喚：天空裂開，大隕石砸下來，煙塵散去變成月球表面
private fun DrawScope.drawMeteorCall(t: Float, monster: Offset, cell: Float) {
    // 天空的裂縫
    val crack = seg(t, 0.05f, 0.2f) * (1f - seg(t, 0.45f, 0.5f))
    if (crack > 0f) {
        var p = Offset(size.width * 0.75f, 0f)
        for (k in 0 until 6) {
            val n = Offset(p.x + (if (k % 2 == 0) cell * 5 else -cell * 4), p.y + cell * 3 * crack)
            drawLine(Color.White, p, n, cell)
            p = n
        }
    }
    // 2～3 顆小隕石拖著火焰接連掉下來
    for (k in 0 until 3) {
        val s0 = 0.2f + k * 0.07f
        val target = monster + Offset((k - 1) * cell * 7, (k % 2) * cell * 3 - cell)
        val fall = seg(t, s0, s0 + 0.16f)
        if (fall > 0f && fall < 1f) {
            val start = Offset(size.width * (0.85f + k * 0.07f), -cell * 8)
            val c = lerp(start, target, fall * fall)
            for (j in 1..5) drawCircle(Color(0xFFF09030).copy(alpha = 0.7f - j * 0.12f), cell * (4 - j * 0.4f), lerp(c, start, j * 0.06f))
            drawCircle(Color(0xFF202030), cell * 4.5f, c)
            drawCircle(Color(0xFF805030), cell * 3.8f, c)
            drawCircle(Color(0xFF986030), cell * 1.5f, c + Offset(-cell * 1.5f, -cell))
        }
        if (k < 2) drawExplosion(target, seg(t, s0 + 0.16f, s0 + 0.3f), cell * 1.2f)
    }
    drawExplosion(monster, seg(t, 0.5f, 0.7f), cell * 2.5f)
    // 煙塵蓋住整個畫面，散開後已經在月球上
    val dust = seg(t, 0.52f, 0.65f) * (1f - seg(t, 0.72f, 0.95f))
    if (dust > 0f) {
        for (k in 0 until 18) {
            val x = (k % 6 + 0.5f) * size.width / 6
            val y = (k / 6 + 0.5f) * size.height / 3
            drawCircle(Color(0xFFB8B0A0).copy(alpha = 0.9f * dust), size.width / 6, Offset(x + sin(t * 9 + k) * cell * 3, y))
        }
    }
}

// 精靈之眼：眼睛發出綠光，下面一排字母一個個亮起來
private fun DrawScope.drawElfEye(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val glow = seg(t, 0f, 0.2f) * (1f - seg(t, 0.85f, 1f))
    val eyeY = hero.y - heroSize * 0.12f
    for (side in listOf(-1, 1)) {
        val e = Offset(hero.x + side * heroSize * 0.12f, eyeY)
        drawCircle(Color(0xFF70E070).copy(alpha = 0.5f * glow), cell * 3, e)
        drawRect(Color(0xFFA0F8A0).copy(alpha = glow), Offset(e.x - cell, e.y - cell / 2), Size(cell * 2, cell))
    }
    // 綠色的光掃過畫面
    if (t in 0.15f..0.5f) {
        val p = seg(t, 0.15f, 0.5f)
        val x = hero.x + p * (size.width - hero.x)
        drawLine(Color(0xFF70E070).copy(alpha = 0.5f), Offset(hero.x, eyeY), Offset(x, size.height), cell * 2)
    }
    val lit = stepped(seg(t, 0.35f, 0.8f), 6)
    for (k in 0 until 6) {
        val c = Offset(size.width / 2 + (k - 2.5f) * cell * 6, size.height - cell * 4)
        val on = k < lit * 6
        drawRect(Color(0xFF202030).copy(alpha = glow), Offset(c.x - cell * 2.5f, c.y - cell * 3), Size(cell * 5, cell * 6))
        drawRect((if (on && k < 3) Color(0xFFA0F8A0) else Color(0xFF465A3A)).copy(alpha = glow), Offset(c.x - cell * 2, c.y - cell * 2.5f), Size(cell * 4, cell * 5))
    }
}

private val ArcherOnTreeArt = listOf(
    "..GGG..",
    ".GSSSG.",
    "..SKS.d",
    ".GGGGd.",
    "SGGGGd.",
    ".GGGG.d",
)

private fun elfTreeSpots(width: Float, height: Float) = List(5) { i -> Offset(width * (0.38f + i * 0.13f), height * (0.1f + (i % 2) * 0.08f)) }

// 精靈族的逆襲：樹葉旋風捲過畫面（背景換成森林深處），樹上冒出一排精靈弓手
private fun DrawScope.drawElvenUprising(t: Float, cell: Float) {
    val storm = seg(t, 0f, 0.3f) * (1f - seg(t, 0.45f, 0.6f))
    if (storm > 0f) {
        drawRect(Color(0xFF286838).copy(alpha = 0.5f * storm))
        for (k in 0 until 40) {
            val a = k * 2.4f + t * 12
            val r = (k % 10 + 1) * size.width / 18
            val c = Offset(size.width / 2 + cos(a) * r, size.height / 2 + sin(a) * r * 0.45f)
            drawRect((if (k % 2 == 0) Color(0xFF40A048) else Color(0xFF70C058)).copy(alpha = storm), c, Size(cell * 2, cell))
        }
    }
    val pop = seg(t, 0.5f, 0.8f)
    elfTreeSpots(size.width, size.height).forEachIndexed { i, spot ->
        val p = (pop * 5 - i).coerceIn(0f, 1f)
        if (p > 0f) spriteCentered(ArcherOnTreeArt, Offset(spot.x, spot.y + (1f - p) * cell * 6), cell, alpha = p)
    }
}

// 答對時，樹上的精靈一起放箭
fun DrawScope.drawElfVolley(t: Float, monster: Offset) {
    val cell = 3.dp.toPx()
    elfTreeSpots(size.width, size.height).forEachIndexed { i, spot ->
        val p = seg(t, i * 0.05f, 0.6f + i * 0.05f)
        if (p <= 0f || p >= 1f) return@forEachIndexed
        val c = lerp(spot, monster, p)
        val d = monster - spot
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y)
        val u = Offset(d.x / len, d.y / len)
        drawLine(Color(0xFF805030), c - u * (cell * 6), c, cell)
        drawRect(Color(0xFFC0C8D8), Offset(c.x - cell, c.y - cell), Size(cell * 2, cell * 2))
    }
}

private fun DrawScope.drawPocketWatch(c: Offset, cell: Float, hands: Float, alpha: Float = 1f) {
    drawCircle(Color(0xFF202030).copy(alpha = alpha), cell * 5.5f, c)
    drawCircle(Color(0xFFF8D030).copy(alpha = alpha), cell * 5f, c)
    drawCircle(Color(0xFFF8F0E0).copy(alpha = alpha), cell * 3.8f, c)
    drawRect(Color(0xFFF8D030).copy(alpha = alpha), Offset(c.x - cell, c.y - cell * 7.5f), Size(cell * 2, cell * 2))
    val a1 = hands * TAU - PI.toFloat() / 2
    val a2 = hands * TAU * 12 - PI.toFloat() / 2
    drawLine(Color(0xFF202030).copy(alpha = alpha), c, Offset(c.x + cos(a1) * cell * 2, c.y + sin(a1) * cell * 2), cell / 1.5f)
    drawLine(Color(0xFF202030).copy(alpha = alpha), c, Offset(c.x + cos(a2) * cell * 3.2f, c.y + sin(a2) * cell * 3.2f), cell / 2)
}

// 偷來的懷錶：拿出亮晶晶的懷錶晃一晃，「喀」一聲，畫面變灰、怪物定格
private fun DrawScope.drawStolenWatch(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val swing = sin(t * TAU * 2) * 30f * (1f - seg(t, 0.4f, 0.45f))
    val pivot = Offset(hero.x + heroSize * 0.4f, hero.y - heroSize * 0.6f)
    rotate(swing, pivot) {
        val c = Offset(pivot.x, pivot.y + cell * 12)
        drawLine(Color(0xFFD0A858), pivot, c, cell / 2)
        drawPocketWatch(c, cell, if (t < 0.45f) t * 3 else 0.45f * 3)
        if ((t * 10).toInt() % 3 == 0) pixelStar(Offset(c.x + cell * 6, c.y - cell * 4), cell, Color.White)
    }
    val gray = seg(t, 0.45f, 0.6f)
    if (gray > 0f) drawRect(Color(0xFF707890).copy(alpha = 0.3f * gray))
    if (t in 0.45f..0.5f) drawRect(Color.White.copy(alpha = 0.4f))
}

// 我是哥布林大王：雞腿一支支飛進嘴裡，變巨大、戴上王冠，兩隻小跟班跳出來
private fun DrawScope.drawGoblinKing(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    val mouth = Offset(hero.x, hero.y - heroSize * 0.1f)
    for (k in 0 until 4) {
        val p = seg(t, k * 0.1f, 0.15f + k * 0.1f)
        if (p <= 0f || p >= 1f) continue
        val from = Offset(hero.x + heroSize * (1.2f + k * 0.2f), hero.y - heroSize * 0.9f)
        val c = lerp(from, mouth, p) - Offset(0f, sin(p * PI.toFloat()) * cell * 8)
        spriteCentered(DrumstickArt, c, cell)
        if (p > 0.85f) pixelStar(mouth + Offset(cell * 4, -cell * 3), cell, Color.White)
    }
    if (t > 0.45f) {
        val drop = seg(t, 0.45f, 0.6f)
        spriteCentered(CrownArt, Offset(hero.x, hero.y - heroSize * 0.95f - (1f - drop) * cell * 15), cell * 1.3f)
    }
    val jump = seg(t, 0.6f, 0.85f)
    if (jump > 0f) drawMinions(t, hero, heroSize, ground, cell, jump)
}

private fun DrawScope.drawMinions(clock: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float, appear: Float = 1f) {
    val w = MinionArt.first().length * cell
    val h = MinionArt.size * cell
    for (side in listOf(-1, 1)) {
        val x = hero.x + side * heroSize * 0.95f - w / 2
        val hop = if (((clock * 6).toInt() + side) % 2 == 0) cell else 0f
        val y = ground - h - hop - sin(appear * PI.toFloat()) * cell * 8
        drawSprite(MinionArt, x + side * (1f - appear) * cell * 20, y, cell)
    }
}

// 矮人秘境溫泉：腳下冒出溫泉，頭頂毛巾，四處冒蒸氣
private fun DrawScope.drawHotSpring(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    val fill = seg(t, 0f, 0.25f) * (1f - seg(t, 0.85f, 1f))
    val pool = Offset(hero.x, hero.y + heroSize * 0.3f)
    drawOval(Color(0xFF8A8478).copy(alpha = fill), Offset(pool.x - heroSize * 0.75f, pool.y - cell * 4), Size(heroSize * 1.5f, cell * 10))
    drawOval(Color(0xFF90D0F8).copy(alpha = fill), Offset(pool.x - heroSize * 0.65f, pool.y - cell * 3), Size(heroSize * 1.3f, cell * 8))
    drawTowel(hero, heroSize, cell, fill)
    for (k in 0 until 8) {
        val p = ((t * 2.5f) + k / 8f) % 1f
        val x = hero.x + (k - 3.5f) * heroSize * 0.2f + sin(p * TAU + k) * cell * 2
        drawCircle(Color.White.copy(alpha = 0.6f * (1f - p) * fill), cell * (2 + p * 3), Offset(x, pool.y - p * heroSize))
    }
    if (t > 0.3f) drawHeal(ItemType.SmallPotion, seg(t, 0.3f, 0.9f), hero, heroSize)
}

private fun DrawScope.drawTowel(hero: Offset, heroSize: Float, cell: Float, alpha: Float = 1f) {
    val top = hero.y - heroSize * 0.55f
    drawRect(Color(0xFF202030).copy(alpha = alpha), Offset(hero.x - cell * 5, top - cell * 2.5f), Size(cell * 10, cell * 3))
    drawRect(Color.White.copy(alpha = alpha), Offset(hero.x - cell * 4.5f, top - cell * 2), Size(cell * 9, cell * 2))
    drawRect(Color(0xFFE04030).copy(alpha = alpha), Offset(hero.x - cell * 4.5f, top - cell), Size(cell * 9, cell / 2))
}

private val ArmorArt = listOf(
    "KK.....KK",
    "KMKKKKKMK",
    "KMMMMMMMK",
    ".KMMWMMK.",
    ".KMMMMMK.",
    ".KMMMMMK.",
    "..KKKKK..",
)

// 防具終極強化：在鐵砧上敲打盔甲，盔甲飛到身上，整個人變成發光的金色
private fun DrawScope.drawUltimateArmor(t: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float) {
    val spot = Offset(hero.x + heroSize * 0.8f, ground)
    if (t < 0.5f) {
        val top = spot.y - cell * 5
        drawRect(Color(0xFF202030), Offset(spot.x - cell * 6, top - cell / 2), Size(cell * 12, cell * 3))
        drawRect(Color(0xFF707890), Offset(spot.x - cell * 5.5f, top), Size(cell * 11, cell * 2))
        drawRect(Color(0xFF707890), Offset(spot.x - cell * 2, top + cell * 2), Size(cell * 4, cell * 3))
        spriteCentered(ArmorArt, Offset(spot.x, top - cell * 4), cell)
        val beat = seg(t, 0f, 0.48f) * 4 % 1f
        val lift = abs(cos(beat * PI.toFloat())) * cell * 8
        drawRect(Color(0xFF805030), Offset(spot.x + cell * 4, top - cell * 12 - lift), Size(cell, cell * 7))
        drawRect(Color(0xFF707890), Offset(spot.x + cell * 2, top - cell * 14 - lift), Size(cell * 5, cell * 3))
        if (beat < 0.25f) for (k in 0 until 5) drawRect(Color(0xFFF8D030), Offset(spot.x + (k - 2) * cell * 3 * (1 + beat * 4), top - cell * 6 - beat * cell * 10), Size(cell, cell))
    } else {
        val fly = seg(t, 0.5f, 0.65f)
        val c = lerp(Offset(spot.x, spot.y - cell * 9), hero + Offset(0f, heroSize * 0.15f), fly)
        if (fly < 1f) spriteCentered(ArmorArt, c, cell * (1f + fly * 0.4f))
        val shine = seg(t, 0.65f, 0.8f)
        if (shine > 0f) {
            drawGoldGlow(t * 3, hero, heroSize, cell, shine)
            if (t < 0.75f) drawRect(Color(0xFFF8E8A0).copy(alpha = 0.3f))
        }
    }
}

private fun DrawScope.drawGoldGlow(clock: Float, hero: Offset, heroSize: Float, cell: Float, power: Float = 1f) {
    val pulse = 0.5f + 0.5f * sin(clock * TAU)
    val half = heroSize * 0.55f
    drawRect(Color(0xFFF8D030).copy(alpha = (0.18f + 0.15f * pulse) * power), Offset(hero.x - half, hero.y - half), Size(half * 2, half * 2))
    drawRect(Color(0xFFF8D030).copy(alpha = 0.8f * power), Offset(hero.x - half, hero.y - half), Size(half * 2, half * 2), style = Stroke(cell / 1.5f))
    if ((clock * 3).toInt() % 2 == 0) pixelStar(Offset(hero.x + half, hero.y - half), cell, Color.White.copy(alpha = power))
}

// 呼嚕呼嚕治癒～喵：縮成一團呼嚕呼嚕，冒出音符和愛心
private fun DrawScope.drawPurr(t: Float, hero: Offset, heroSize: Float, cell: Float) {
    // 呼嚕的震動線
    if ((t * 12).toInt() % 2 == 0 && t < 0.85f) {
        for (side in listOf(-1, 1)) {
            drawLine(Color.White, Offset(hero.x + side * heroSize * 0.55f, hero.y - cell * 2), Offset(hero.x + side * heroSize * 0.7f, hero.y - cell * 4), cell / 2)
            drawLine(Color.White, Offset(hero.x + side * heroSize * 0.55f, hero.y + cell * 2), Offset(hero.x + side * heroSize * 0.7f, hero.y + cell * 4), cell / 2)
        }
    }
    for (k in 0 until 8) {
        val p = seg(t, k * 0.07f, 0.45f + k * 0.07f)
        if (p <= 0f || p >= 1f) continue
        val c = Offset(hero.x + (k - 3.5f) * cell * 3 + sin(p * TAU + k) * cell * 2, hero.y - heroSize * 0.4f - p * heroSize * 0.8f)
        if (k % 2 == 0) spriteCentered(HeartArt, c, cell, alpha = 1f - p)
        else spriteCentered(NoteArt, c, cell, alpha = 1f - p, recolor = mapOf('K' to 'P'))
    }
    if (t > 0.3f) drawHeal(ItemType.SmallPotion, seg(t, 0.3f, 1f), hero, heroSize)
}

// 貓咪是地表最可愛的生物～喵～：像素大貓眼眨呀眨，兩隻小貓跑出來，怪物頭上一直冒愛心
private val CatEyeArt = listOf(
    "..KKKKKKK..",
    ".KgggggggK.",
    "KggWWKggggK",
    "KgWWWKKgggK",
    "KggWKKKgggK",
    "KgggKKKgggK",
    "KgggKKKgWgK",
    ".KgggKgggK.",
    "..KKKKKKK..",
)

private val CatEyeClosedArt = listOf(
    "...........",
    "...........",
    "...........",
    "...........",
    "KKKKKKKKKKK",
    ".K.......K.",
    "...........",
    "...........",
    "...........",
)

private val KittenRecolors = listOf(emptyMap(), mapOf('O' to 'M'))

private fun DrawScope.drawKittens(clock: Float, hero: Offset, heroSize: Float, ground: Float, cell: Float, appear: Float = 1f) {
    val k = cell * 0.8f
    val h = CatSitArt.size * k
    for ((i, side) in listOf(-1, 1).withIndex()) {
        val hop = if (((clock * 4).toInt() + i) % 2 == 0) 0f else k
        val x = hero.x + side * heroSize * 0.75f + side * (1f - appear) * cell * 20
        spriteCentered(CatSitArt, Offset(x, ground - h / 2 - hop), k, recolor = KittenRecolors[i])
    }
}

private fun DrawScope.drawRisingHearts(clock: Float, monster: Offset, monsterHeight: Float, cell: Float) {
    for (k in 0 until 4) {
        val q = (clock * 0.6f + k / 4f) % 1f
        val x = monster.x + (k - 1.5f) * cell * 4 + sin(q * TAU + k) * cell * 1.5f
        spriteCentered(HeartArt, Offset(x, monster.y - monsterHeight / 2 - q * cell * 14), cell, alpha = 1f - q)
    }
}

private fun DrawScope.drawCutestCat(t: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, ground: Float, cell: Float) {
    val show = stepped(seg(t, 0.05f, 0.2f), 3) * (1f - stepped(seg(t, 0.55f, 0.7f), 3))
    if (show > 0f) {
        drawRect(Color(0xFFF08090).copy(alpha = 0.2f * show))
        val eyeCell = size.height * 0.4f / CatEyeArt.size * show
        // 眨兩次眼
        val blink = t in 0.28f..0.32f || t in 0.44f..0.48f
        for (side in listOf(-1, 1)) {
            val c = Offset(size.width / 2 + side * eyeCell * 8f, size.height * 0.42f)
            spriteCentered(if (blink) CatEyeClosedArt else CatEyeArt, c, eyeCell)
            if (!blink && (t * 12).toInt() % 2 == 0) pixelStar(c + Offset(eyeCell * 6, -eyeCell * 5), cell, Color.White)
        }
    }
    if (t > 0.55f) drawKittens(t, hero, heroSize, ground, cell, seg(t, 0.55f, 0.8f))
    if (t > 0.5f) {
        val p = seg(t, 0.5f, 0.8f)
        drawHeartEyes(monster, monsterHeight, cell * (0.5f + p * 0.7f))
        if (t > 0.6f) drawRisingHearts(t * 2, monster, monsterHeight, cell)
    }
}

private fun DrawScope.drawHeartEyes(monster: Offset, monsterHeight: Float, cell: Float) {
    for (side in listOf(-1, 1)) spriteCentered(HeartArt, Offset(monster.x + side * monsterHeight * 0.17f, monster.y - monsterHeight * 0.18f), cell)
}

// 流星龍爪：飛上天，再拖著火焰俯衝下來，留下三道爪痕
private fun DrawScope.drawMeteorClaw(t: Float, hero: Offset, monster: Offset, monsterHeight: Float, cell: Float) {
    val up = seg(t, 0f, 0.25f)
    if (up in 0.01f..0.99f) {
        val c = Offset(hero.x, hero.y - up * hero.y * 1.2f)
        drawLine(Color.White.copy(alpha = 0.7f), Offset(c.x, c.y + cell * 20), c, cell * 2)
    }
    val dive = seg(t, 0.3f, 0.5f)
    if (dive in 0.01f..0.99f) {
        val start = Offset(monster.x - monsterHeight, -cell * 10)
        val c = lerp(start, monster, dive)
        for (k in 1..6) drawCircle(Color(0xFFF09030).copy(alpha = 0.7f - k * 0.1f), cell * (5 - k * 0.5f), lerp(c, start, k * 0.07f))
        drawCircle(Color(0xFFF8D030), cell * 4, c)
    }
    if (t > 0.5f) {
        val p = seg(t, 0.5f, 0.6f)
        val fade = 1f - seg(t, 0.75f, 1f)
        for (k in 0 until 3) {
            val off = (k - 1) * cell * 5
            val a = Offset(monster.x - cell * 8 + off, monster.y - monsterHeight * 0.4f)
            val b = Offset(monster.x + cell * 4 + off, monster.y + monsterHeight * 0.3f)
            drawLine(Color(0xFFE04030).copy(alpha = fade), a, lerp(a, b, p), cell * 2.5f)
            drawLine(Color.White.copy(alpha = fade), a, lerp(a, b, p), cell)
        }
        if (t < 0.58f) drawRect(Color.White.copy(alpha = 0.35f))
    }
}

// 火龍的呢喃：從火龍嘴裡噴出一長串火焰燒向怪物，噴了好一陣子，最後火焰燒滿畫面，退去後變成岩漿火海
private fun DrawScope.drawFireWhisper(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val mouth = Offset(hero.x + heroSize * 0.45f, hero.y - heroSize * 0.1f)
    if (t < 0.12f) drawCircle(Color(0xFFF09030).copy(alpha = 0.8f), cell * (1 + seg(t, 0f, 0.12f) * 3), mouth)
    // 火焰柱：一團團方塊火從嘴巴往右邊噴，越遠越大
    val reach = seg(t, 0.08f, 0.2f)
    val stop = seg(t, 0.62f, 0.72f)
    if (reach > 0f && stop < 1f) {
        val end = Offset(size.width + cell * 6, monster.y)
        val clock = stepped(t, 40)
        for (k in 0 until 26) {
            val p = (k / 26f + clock * 3f) % 1f
            if (p > reach || p < stop) continue
            val c = lerp(mouth, end, p) + Offset(0f, sin(p * 30 + k) * cell * (1 + p * 3))
            val r = cell * (1.5f + p * 5)
            val color = when (k % 3) { 0 -> Color(0xFFE04030); 1 -> Color(0xFFF09030); else -> Color(0xFFF8D030) }
            drawRect(color.copy(alpha = 0.9f), Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
            drawRect(Color(0xFFF8E8A0).copy(alpha = 0.7f), Offset(c.x - r * 0.4f, c.y - r * 0.4f), Size(r * 0.8f, r * 0.8f))
        }
        drawCircle(Color(0xFFF8D030), cell * 2.5f, mouth)
    }
    // 最後火焰燒滿整個畫面，蓋住換背景的那一下
    val spread = seg(t, 0.5f, 0.6f)
    val fade = 1f - seg(t, 0.66f, 0.9f)
    if (spread > 0f && fade > 0f) {
        val cols = 24
        val colW = size.width / cols
        for (i in 0 until cols) {
            val x = i * colW
            val tall = size.height * (0.75f + 0.35f * abs(sin(stepped(t, 20) * 9 + i * 1.3f))) * spread
            drawRect(Color(0xFFE04030).copy(alpha = 0.9f * fade), Offset(x, size.height - tall), Size(colW + 1, tall))
            drawRect(Color(0xFFF09030).copy(alpha = 0.9f * fade), Offset(x + colW * 0.15f, size.height - tall * 0.7f), Size(colW * 0.7f, tall * 0.7f))
            drawRect(Color(0xFFF8D030).copy(alpha = 0.9f * fade), Offset(x + colW * 0.3f, size.height - tall * 0.4f), Size(colW * 0.4f, tall * 0.4f))
        }
        if (t in 0.55f..0.68f) drawRect(Color(0xFFF05020).copy(alpha = 0.6f * fade))
    }
}

// 戰艦：灰色船身、兩座砲塔、煙囪
private fun DrawScope.drawShip(left: Float, water: Float, cell: Float, recoil: Float) {
    val w = cell * 34
    drawRect(Color(0xFF202030), Offset(left, water - cell * 6), Size(w, cell * 6))
    drawRect(Color(0xFF707890), Offset(left + cell, water - cell * 5), Size(w - cell * 2, cell * 4))
    drawRect(Color(0xFF202030), Offset(left + cell * 10, water - cell * 12), Size(cell * 12, cell * 6))
    drawRect(Color(0xFFC0C8D8), Offset(left + cell * 11, water - cell * 11), Size(cell * 10, cell * 5))
    drawRect(Color(0xFF202030), Offset(left + cell * 14, water - cell * 17), Size(cell * 4, cell * 5))
    for (k in 0 until 2) {
        val tx = left + cell * (4 + k * 22)
        drawRect(Color(0xFF202030), Offset(tx, water - cell * 9), Size(cell * 6, cell * 3))
        drawRect(Color(0xFF202030), Offset(tx + cell * 5 - recoil, water - cell * 8.5f), Size(cell * 6, cell))
    }
}

// 戰艦招喚：背景換成海上，戰艦開進來狂轟，有一發打偏打到自己
private fun DrawScope.drawBattleship(t: Float, hero: Offset, monster: Offset, cell: Float) {
    val water = size.height * 0.45f
    val come = seg(t, 0.05f, 0.3f)
    val leave = seg(t, 0.82f, 1f)
    val left = -cell * 40 + come * cell * 44 - leave * cell * 50
    val recoil = if (t in 0.3f..0.75f && (t * 16).toInt() % 2 == 0) cell * 2 else 0f
    drawShip(left, water, cell, recoil)
    val muzzle = Offset(left + cell * 32, water - cell * 8)
    for (k in 0 until 4) {
        val s = 0.3f + k * 0.08f
        val p = seg(t, s, s + 0.15f)
        val self = k == 2
        val target = if (self) hero else monster + Offset((k - 1) * cell * 4, 0f)
        if (p > 0f && p < 1f) {
            val c = lerp(muzzle, target, p) - Offset(0f, sin(p * PI.toFloat()) * cell * (if (self) 30 else 14))
            drawCircle(Color(0xFF202030), cell * 2, c)
        }
        drawExplosion(target, seg(t, s + 0.15f, s + 0.3f), cell * (if (self) 0.8f else 1.3f))
        if (t in s..(s + 0.03f)) drawCircle(Color(0xFFF8D030), cell * 3, muzzle)
    }
    // 打到自己：頭上冒「!」
    if (t in 0.6f..0.8f) spriteCentered(BangArt, Offset(hero.x, hero.y - cell * 16), cell * 1.3f)
}

// 任務～殲滅～逼～全武器發射!：背上一顆大火箭升空再砸下來，前面兩挺機槍狂掃，飛機慢慢飛過丟炸彈，最後一片白光
private fun DrawScope.drawBigRocket(c: Offset, cell: Float, down: Boolean) {
    rotate(if (down) 180f else 0f, c) {
        drawRect(Color(0xFF202030), Offset(c.x - cell * 3, c.y - cell * 8), Size(cell * 6, cell * 14))
        drawRect(Color(0xFFC0C8D8), Offset(c.x - cell * 2, c.y - cell * 6), Size(cell * 4, cell * 11))
        drawRect(Color(0xFFE04030), Offset(c.x - cell * 2, c.y - cell * 9), Size(cell * 4, cell * 3))
        drawRect(Color(0xFFE04030), Offset(c.x - cell, c.y - cell * 10), Size(cell * 2, cell))
        drawRect(Color(0xFF202030), Offset(c.x - cell * 5, c.y + cell * 2), Size(cell * 10, cell * 4))
        drawRect(Color(0xFF707890), Offset(c.x - cell * 4, c.y + cell * 3), Size(cell * 8, cell * 2))
        val flame = if ((c.y / cell).toInt() % 2 == 0) cell * 5 else cell * 3
        drawRect(Color(0xFFF09030), Offset(c.x - cell * 2, c.y + cell * 6), Size(cell * 4, flame))
        drawRect(Color(0xFFF8D030), Offset(c.x - cell, c.y + cell * 6), Size(cell * 2, flame * 0.7f))
    }
}

private fun DrawScope.drawFullArsenal(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val deploy = seg(t, 0f, 0.12f)
    // 兩挺機槍
    val guns = listOf(Offset(0.45f, -0.15f), Offset(0.5f, 0.2f))
    guns.forEach { g ->
        val base = Offset(hero.x + g.x * heroSize, hero.y + g.y * heroSize)
        drawRect(Color(0xFF202030), Offset(base.x - cell * 2, base.y - cell * 1.5f), Size(cell * 8 * deploy, cell * 3))
        drawRect(Color(0xFF707890), Offset(base.x - cell * 1.5f, base.y - cell), Size(cell * 7 * deploy, cell * 2))
    }
    if (t in 0.15f..0.68f) {
        guns.forEachIndexed { i, g ->
            val muzzle = Offset(hero.x + g.x * heroSize + cell * 7, hero.y + g.y * heroSize)
            if (((t * 30).toInt() + i) % 2 == 0) drawRect(Color(0xFFF8D030), Offset(muzzle.x, muzzle.y - cell * 1.5f), Size(cell * 3, cell * 3))
            for (k in 0 until 6) {
                val p = ((t - 0.15f) * 9f + k / 6f + i * 0.08f) % 1f
                val c = lerp(muzzle, monster + Offset(0f, (k % 3 - 1) * cell * 3), p)
                drawRect(Color(0xFFF8D030), Offset(c.x - cell * 2, c.y - cell / 2), Size(cell * 3, cell))
            }
        }
        if ((t * 20).toInt() % 2 == 0) drawHit(monster, 0.3f, cell)
    }
    // 大火箭：從背上升空飛出畫面，再從天上砸到怪物
    val up = seg(t, 0.1f, 0.3f)
    if (up > 0f && up < 1f) drawBigRocket(Offset(hero.x - heroSize * 0.3f, hero.y - up * up * (hero.y + cell * 30)), cell, down = false)
    val down = seg(t, 0.36f, 0.55f)
    if (down > 0f && down < 1f) drawBigRocket(Offset(monster.x, -cell * 15 + down * down * (monster.y + cell * 15)), cell, down = true)
    for (k in 0 until 3) drawExplosion(monster + Offset((k - 1) * cell * 6, (k % 2) * cell * 4 - cell * 2), seg(t, 0.55f + k * 0.04f, 0.75f + k * 0.04f), cell * 2f)
    // 飛機慢慢飛過，經過怪物上方時丟炸彈
    val span = size.width + cell * 20
    for (k in 0 until 2) {
        val s0 = 0.12f + k * 0.15f
        val dur = 0.7f
        val p = seg(t, s0, s0 + dur)
        val y = cell * (6 + k * 6)
        if (p > 0f && p < 1f) {
            val c = Offset(-cell * 10 + p * span, y)
            drawRect(Color(0xFF202030), Offset(c.x - cell * 6, c.y - cell), Size(cell * 12, cell * 3))
            drawRect(Color(0xFF465A3A), Offset(c.x - cell * 5.5f, c.y - cell / 2), Size(cell * 11, cell * 2))
            drawRect(Color(0xFF202030), Offset(c.x - cell, c.y - cell * 4), Size(cell * 4, cell * 9))
            drawRect(Color(0xFF202030), Offset(c.x - cell * 6, c.y - cell * 3), Size(cell * 2, cell * 3))
        }
        for (j in 0 until 3) {
            val bx = monster.x + (j - 1) * cell * 7
            val dropT = s0 + dur * (bx + cell * 10) / span
            val fall = seg(t, dropT, dropT + 0.07f)
            if (fall > 0f && fall < 1f) {
                val bc = Offset(bx, y + fall * fall * (monster.y - y))
                drawRect(Color(0xFF202030), Offset(bc.x - cell, bc.y - cell * 2), Size(cell * 2, cell * 4))
            }
            drawExplosion(Offset(bx, monster.y), seg(t, dropT + 0.07f, dropT + 0.2f), cell * 1.2f)
        }
    }
    if (t in 0.66f..0.82f) drawRect(Color.White.copy(alpha = 0.9f * (1f - seg(t, 0.72f, 0.82f))))
}

// 吸吸能量：伸出長吸管插到怪物身上，紫色光球一顆顆被吸過來
private fun DrawScope.drawEnergySip(t: Float, hero: Offset, heroSize: Float, monster: Offset, cell: Float) {
    val reach = seg(t, 0f, 0.2f)
    val fade = 1f - seg(t, 0.85f, 1f)
    val mouth = Offset(hero.x + heroSize * 0.3f, hero.y - heroSize * 0.05f)
    val tip = lerp(mouth, monster, reach)
    drawLine(Color(0xFF202030).copy(alpha = fade), mouth, tip, cell * 2.5f)
    drawLine(Color(0xFFF08090).copy(alpha = fade), mouth, tip, cell * 1.5f)
    for (k in 0 until 6) {
        val p = seg(t, 0.2f + k * 0.07f, 0.45f + k * 0.07f)
        if (p <= 0f || p >= 1f) continue
        val c = lerp(monster, mouth, p)
        drawCircle(Color(0xFFC090E8), cell * 2.2f, c)
        drawCircle(Color(0xFF9060C0), cell * 1.4f, c)
    }
    if (t > 0.55f) drawHeal(ItemType.SmallPotion, seg(t, 0.55f, 1f), hero, heroSize)
}

// 用自己的顏色表畫人物頭像（人物和道具用的顏色不一樣）
private fun DrawScope.drawArt(art: List<String>, colors: Map<Char, Color>, left: Float, top: Float, cell: Float, alpha: Float = 1f) {
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val color = colors[c] ?: return@forEachIndexed
            drawRect(color.copy(alpha = color.alpha * alpha), Offset(left + x * cell, top + y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

// 惡魔的附身：那個職業的人從畫面左邊走進來，魔族化成黑影捲成一團鑽進她身體，眼睛變紅
fun DrawScope.drawPossession(t: Float, hero: Offset, heroSize: Float, art: List<String>, colors: Map<Char, Color>, ground: Float) {
    val cell = 3.dp.toPx()
    val artCell = heroSize / art.first().length
    // 走進來，停在魔族旁邊，再被黑影拉到魔族的位置
    val walk = seg(t, 0f, 0.3f)
    val pull = seg(t, 0.45f, POSSESS_SWAP_AT)
    val stand = hero.x - heroSize * 0.9f
    val x = -heroSize + walk * (stand + heroSize) + pull * (hero.x - stand)
    val bob = if (walk < 1f && (t * 16).toInt() % 2 == 0) -cell else 0f
    if (t < POSSESS_SWAP_AT) drawArt(art, colors, x - heroSize / 2, ground - heroSize + bob, artCell)
    // 魔族化成黑影，捲成一團
    val swirl = seg(t, 0.25f, 0.55f)
    if (swirl > 0f && t < POSSESS_SWAP_AT + 0.05f) {
        val c = Offset(lerp(hero, Offset(x, hero.y), seg(t, 0.4f, POSSESS_SWAP_AT)).x, hero.y)
        for (k in 0 until 18) {
            val a = k * TAU / 18 + swirl * TAU * 2
            val r = heroSize * (0.3f + 0.4f * sin(swirl * PI.toFloat()))
            drawCircle((if (k % 2 == 0) Color(0xFF140C18) else Color(0xFF483868)).copy(alpha = 0.85f), cell * (2 + k % 3), Offset(c.x + cos(a) * r, c.y + sin(a) * r * 0.6f))
        }
        drawCircle(Color(0xFF140C18).copy(alpha = 0.85f * seg(t, 0.25f, 0.4f)), heroSize * 0.5f, hero)
    }
    if (t in (POSSESS_SWAP_AT - 0.05f)..(POSSESS_SWAP_AT + 0.05f)) drawRect(Color(0xFF140C18).copy(alpha = 0.5f))
    // 附身完成：眼睛發紅光
    if (t > POSSESS_SWAP_AT) {
        val glow = if ((t * 10).toInt() % 2 == 0) 1f else 0.5f
        val fade = 1f - seg(t, 0.88f, 1f)
        for (side in listOf(-1, 1)) {
            val e = Offset(hero.x + side * heroSize * 0.12f, hero.y - heroSize * 0.12f)
            drawCircle(Color(0xFFE04030).copy(alpha = 0.5f * glow * fade), cell * 3, e)
            drawRect(Color(0xFFE04030).copy(alpha = fade), Offset(e.x - cell, e.y - cell / 2), Size(cell * 2, cell))
        }
        drawCircle(Color(0xFF9060C0).copy(alpha = 0.6f * fade), heroSize * (0.5f + seg(t, POSSESS_SWAP_AT, 0.9f) * 0.4f), hero, style = Stroke(cell))
    }
}

// 偷來的懷錶：時間暫停的最後一回合，偷偷在怪物身上劃一刀
private val DaggerArt = listOf(
    "....M",
    "...MW",
    "..MW.",
    ".MW..",
    "dK...",
    "Kd...",
)

fun DrawScope.drawSneakStab(t: Float, monster: Offset, monsterHeight: Float) {
    val cell = 3.dp.toPx()
    val a = Offset(monster.x - monsterHeight * 0.4f, monster.y - monsterHeight * 0.4f)
    val b = Offset(monster.x + monsterHeight * 0.4f, monster.y + monsterHeight * 0.3f)
    val slash = seg(t, 0f, 0.35f)
    if (t < 0.4f) spriteCentered(DaggerArt, lerp(a, b, slash), cell * 1.4f)
    if (t > 0.1f) {
        val fade = 1f - seg(t, 0.5f, 1f)
        drawLine(Color.White.copy(alpha = fade), a, lerp(a, b, slash), cell * 1.5f)
        drawLine(Color(0xFFC090E8).copy(alpha = fade * 0.7f), a + Offset(cell, cell), lerp(a, b, slash) + Offset(cell, cell), cell)
    }
    drawHit(monster, seg(t, 0.3f, 0.8f), cell)
}

// ---- 效果還在時一直畫著的東西 ----

fun DrawScope.drawLasting(skill: Skill, clock: Float, hero: Offset, heroSize: Float, monster: Offset, monsterHeight: Float, ground: Float) {
    val cell = 3.dp.toPx()
    when (skill) {
        Skill.Orichalcum -> {
            val c = Offset(hero.x + heroSize * 0.5f, hero.y + sin(stepped(clock, 8) * TAU) * cell)
            spriteCentered(GoldAxeArt, c, cell * 1.1f)
            if ((clock * 4).toInt() % 2 == 0) pixelStar(Offset(c.x - cell * 2, c.y - cell * 5), cell, Color.White)
        }
        Skill.DragonHowl -> drawFlames(clock, hero, heroSize, 0.45f, cell)
        // 超頻：頭上的儀表一直在紅色區抖，身體冒蒸氣
        Skill.Overclock -> {
            drawSteam(clock, hero, heroSize, cell, 0.7f)
            drawGauge(Offset(hero.x, hero.y - heroSize * 0.75f), cell, 0.92f + if ((clock * 20).toInt() % 2 == 0) 0.05f else 0f)
        }
        Skill.HotSpring -> {
            drawTowel(hero, heroSize, cell)
            val p = (clock * 2) % 1f
            drawCircle(Color.White.copy(alpha = 0.6f * (1f - p)), cell * (1.5f + p * 2), Offset(hero.x + cell * 3, hero.y - heroSize * 0.6f - p * cell * 8))
        }
        Skill.ElvenUprising -> elfTreeSpots(size.width, size.height).forEachIndexed { i, spot ->
            val bob = if (((clock * 4).toInt() + i) % 2 == 0) 0f else cell
            spriteCentered(ArcherOnTreeArt, Offset(spot.x, spot.y + bob), cell)
        }
        Skill.GoblinKing -> {
            spriteCentered(CrownArt, Offset(hero.x, hero.y - heroSize * 0.95f), cell * 1.3f)
            drawMinions(clock, hero, heroSize, ground, cell)
        }
        Skill.PlagueDoctor -> {
            val bow = if ((clock * 2).toInt() % 2 == 0) 0f else cell / 2
            drawSprite(DoctorArt, doctorLeft(monster, monsterHeight, cell), ground - DoctorArt.size * cell + bow, cell)
            drawSick(clock, monster, monsterHeight)
        }
        Skill.CatOrchestra -> drawCatBand(clock, hero, heroSize, ground)
        Skill.UltimateArmor -> drawGoldGlow(clock, hero, heroSize, cell)
        Skill.CutestCat -> {
            drawHeartEyes(monster, monsterHeight, cell * 1.2f)
            drawRisingHearts(clock, monster, monsterHeight, cell)
            drawKittens(clock, hero, heroSize, ground, cell)
        }
        // 黑霧遁形：身體周圍一直飄著黑霧
        Skill.DarkMist -> for (k in 0 until 8) {
            val a = k * TAU / 8 + clock * TAU
            drawCircle(Color(0xFF302848).copy(alpha = 0.55f), cell * (2 + k % 3), Offset(hero.x + cos(a) * heroSize * 0.45f, hero.y + sin(a) * heroSize * 0.35f))
        }
        // 時間暫停：畫面灰灰的，人物頭上掛著停住的懷錶
        Skill.StolenWatch -> {
            drawRect(Color(0xFF707890).copy(alpha = 0.25f))
            drawPocketWatch(Offset(hero.x + heroSize * 0.55f, hero.y - heroSize * 0.7f), cell * 0.8f, 1.35f)
        }
        // 先給前一半字母：書（或發光的眼睛）一直在
        Skill.AncientTome -> {
            val c = Offset(hero.x + heroSize * 0.5f, hero.y - heroSize * 0.7f + sin(clock * TAU) * cell)
            drawRect(Color(0xFF805030), Offset(c.x - cell * 4, c.y - cell * 3), Size(cell * 8, cell * 6))
            drawRect(Color(0xFFF0E0C0), Offset(c.x - cell * 3.5f, c.y - cell * 2.5f), Size(cell * 7, cell * 5))
            drawRect(Color(0xFF202030), Offset(c.x - cell / 4, c.y - cell * 2.5f), Size(cell / 2, cell * 5))
        }
        Skill.ElfEye -> for (side in listOf(-1, 1)) {
            val e = Offset(hero.x + side * heroSize * 0.12f, hero.y - heroSize * 0.12f)
            drawCircle(Color(0xFF70E070).copy(alpha = 0.35f), cell * 2.5f, e)
        }
        Skill.MindFocus -> drawFocusAura(clock, hero, heroSize)
        else -> Unit
    }
}

// 超頻的儀表：綠黃紅三段，指針 0..1
private fun DrawScope.drawGauge(c: Offset, cell: Float, needle: Float, alpha: Float = 1f) {
    val r = cell * 7
    val box = Offset(c.x - r - cell, c.y - r - cell)
    drawRect(Color(0xFF202030).copy(alpha = alpha), box, Size(r * 2 + cell * 2, r + cell * 3))
    val arcTop = Offset(c.x - r, c.y - r)
    drawArc(Color(0xFF70E070).copy(alpha = alpha), 180f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
    drawArc(Color(0xFFF8D030).copy(alpha = alpha), 240f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
    drawArc(Color(0xFFE04030).copy(alpha = alpha), 300f, 60f, false, arcTop, Size(r * 2, r * 2), style = Stroke(cell * 1.5f))
    val a = PI.toFloat() * (1f + needle)
    drawLine(Color.White.copy(alpha = alpha), c, Offset(c.x + cos(a) * r * 0.9f, c.y + sin(a) * r * 0.9f), cell / 1.5f)
}
