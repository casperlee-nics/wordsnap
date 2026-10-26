package com.wordsnap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wordsnap.app.R
import com.wordsnap.app.data.Word
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelPaper
import com.wordsnap.app.ui.theme.PixelRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 怪物每一格像素的大小
private val Cell = 4.dp
// 戰鬥畫面上玩家人物的大小
private val HeroSize = 56.dp
// 人物和怪物離畫面底部的距離
private val GroundPad = 8.dp

// 道具使用一次的紀錄；id 每次加一，讓動畫重新播放
private data class ItemUse(val type: ItemType, val id: Int)

// 戰鬥模式：聽英文發音，拼出整個單字。
// 答對時人物衝上去攻擊、怪物扣血；答錯被怪物攻擊，扣 1/5 的 HP，HP 歸零就被打倒
// 最後一題結束時怪物的血打光了，才用職業招式把怪物打飛（沒用道具就要全對）
// 招式和頭像隨稱號（每 10 級一階）變強；每記住 50 / 100 個字會遇到中魔王 / 大魔王
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BattleScreen(vm: WordViewModel, bookId: Long, onBack: () -> Unit) {
    val profile by vm.currentProfile.collectAsState()
    val books by vm.books.collectAsState()
    val items by vm.items.collectAsState()
    val avatar = Avatar.from(profile?.avatar)
    val race = avatar.race
    // 戰鬥開始時的稱號階級，決定頭像和招式
    val tier = remember { avatarTier(levelInfo(profile?.exp ?: 0).level) }
    val bookName = books.firstOrNull { it.id == bookId }?.name ?: ""
    // 這本的字都答對兩次了：練習戰，經驗值 1/4、不掉道具
    val practice = remember { vm.isPracticeBattle(bookId) }
    val questions = remember { vm.pickBattleWords(bookId, practice) }
    val monster = remember { vm.pickBattleMonster() }
    val isBoss = monster.rank != MonsterRank.Normal
    val scene = remember { ReviewScene.entries.random() }
    val speaker = rememberSpeaker()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val musicOn = remember { isMusicOn(context) }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    val wrong = remember { mutableStateListOf<Word>() }
    var input by remember { mutableStateOf("") }
    // null：作答中；true：答對；false：答錯
    var result by remember { mutableStateOf<Boolean?>(null) }
    var finished by remember { mutableStateOf(false) }
    var died by remember { mutableStateOf(false) }
    var fallen by remember { mutableStateOf(false) }
    // 魔王出場動畫播完了才開始出題
    var introDone by remember { mutableStateOf(!isBoss) }
    // 玩家被打掉的 HP、道具讓怪物多扣的血
    var damage by remember { mutableIntStateOf(0) }
    var bonus by remember { mutableIntStateOf(0) }
    var bonusHits by remember { mutableIntStateOf(0) }
    var itemUse by remember { mutableStateOf<ItemUse?>(null) }
    // 監獄彩蛋：點兩下鐵門，囚犯衝出來打怪物（每場一次）；值每次加一讓動畫播放
    var prisonBreak by remember { mutableIntStateOf(0) }
    var showBag by remember { mutableStateOf(false) }
    // 防護盾、混亂藥劑還剩幾回合
    var shieldTurns by remember { mutableIntStateOf(0) }
    var confuseTurns by remember { mutableIntStateOf(0) }
    // 怪物出手的次數（擋下的也算）、最後一次有沒有被盾擋下、怪物混亂打自己的次數
    var monsterAttacks by remember { mutableIntStateOf(0) }
    var lastBlocked by remember { mutableStateOf(false) }
    var selfHits by remember { mutableIntStateOf(0) }
    // 念單字的前後 0.5 秒把背景音樂調小聲
    var quiet by remember { mutableStateOf(false) }
    var sayJob by remember { mutableStateOf<Job?>(null) }
    // 結束動畫（打飛怪物或拖棺材）播完了，可以顯示結果
    var endShown by remember { mutableStateOf(false) }
    var reward by remember { mutableStateOf<BattleReward?>(null) }
    // 升級動畫看完了，才顯示戰鬥結果
    var levelUpDone by remember { mutableStateOf(false) }
    val leveledUp = reward?.let { it.levelAfter > it.levelBefore } == true
    val showingLevelUp = endShown && leveledUp && !levelUpDone
    // 升級之後（沒升級就直接）播放寶箱動畫，最後才顯示戰鬥結果
    var chestDone by remember { mutableStateOf(false) }
    val showingChest = endShown && !showingLevelUp && reward?.item != null && !chestDone

    val knockedOut = finished && !died && questions.isNotEmpty() && (questions.size - correct - bonus) <= 0
    LoopingMusic(
        when (monster.rank) {
            MonsterRank.Big -> R.raw.bigboss_bgm
            MonsterRank.Mid -> R.raw.midboss_bgm
            MonsterRank.Normal -> R.raw.battle_bgm
        },
        // 打倒怪物時先停掉戰鬥音樂，播勝利號角
        enabled = musicOn && introDone && !died && !knockedOut && !showingLevelUp && !showingChest,
        volume = if (quiet) 0.05f else 0.3f,
    )
    OneShotSound(R.raw.warning_jingle, play = isBoss && !introDone)
    OneShotSound(R.raw.death_jingle, play = died)
    OneShotSound(R.raw.levelup_jingle, play = showingLevelUp)
    OneShotSound(R.raw.chest_jingle, play = showingChest)
    OneShotSound(R.raw.victory_jingle, play = knockedOut)

    val total = questions.size
    val word = questions.getOrNull(index)
    val answer = word?.text?.trim().orEmpty()
    val heroHp = (HERO_MAX_HP - damage).coerceAtLeast(0)
    val monsterHp = (total - correct - bonus).coerceAtLeast(0)
    val knocked = knockedOut
    // 道具把怪物的血打光：等道具動畫打到怪物，再結束戰鬥
    var ending by remember { mutableStateOf(false) }
    val answering = introDone && !finished && !ending && word != null && result == null

    // 念一次英文：先把音樂調小聲 0.5 秒，念完再等 0.5 秒才調回來
    fun sayWord() {
        sayJob?.cancel()
        sayJob = scope.launch {
            try {
                quiet = true
                delay(500)
                speaker.say(answer, rate = 0.85f)
                delay(500)
            } finally {
                quiet = false
            }
        }
    }

    // 每答完一題，防護盾和混亂藥劑少一回合
    fun endRound() {
        if (shieldTurns > 0) shieldTurns--
        if (confuseTurns > 0) confuseTurns--
    }

    fun submit() {
        if (!answering || input.isBlank() || word == null) return
        // 要拼出整個單字
        val ok = input.trim().equals(answer, ignoreCase = true)
        result = ok
        if (ok) {
            correct++
        } else {
            wrong.add(word)
            when {
                // 混亂：怪物打到自己，自己扣血
                confuseTurns > 0 -> {
                    bonus++
                    selfHits++
                }
                // 防護盾：怪物照樣攻擊，但被盾擋下不扣血
                shieldTurns > 0 -> {
                    lastBlocked = true
                    monsterAttacks++
                }
                else -> {
                    lastBlocked = false
                    monsterAttacks++
                    damage += WRONG_DAMAGE
                }
            }
        }
        endRound()
    }

    // 怪物沒血了，或最後一題答完：結束戰鬥；asked 是真的作答過的題目
    fun win(asked: List<Word>) {
        finished = true
        val ko = total - correct - bonus <= 0
        vm.finishBattle(bookId, asked, wrong.toList(), total, died = false, monster, practice, knockedOut = ko) { reward = it }
    }

    fun next() {
        input = ""
        result = null
        // 道具或混亂讓怪物的血先打光了，就直接打倒，不用把題目答完
        if (index + 1 >= total || total - correct - bonus <= 0) {
            win(questions.take(index + 1))
        } else {
            index++
        }
    }

    // 道具或彩蛋額外打怪物；怪物被打到沒血：等打中（約 1 秒）就直接倒下，這題不用答
    fun hitMonster(amount: Int) {
        bonus += amount
        bonusHits++
        if (total - correct - bonus <= 0) {
            ending = true
            scope.launch {
                delay(1100)
                win(questions.take(index))
            }
        }
    }

    fun useItem(type: ItemType) {
        if (!answering) return
        showBag = false
        vm.useItem(type)
        itemUse = ItemUse(type, (itemUse?.id ?: 0) + 1)
        when {
            type.heal > 0 -> damage = (damage - type.heal).coerceAtLeast(0)
            type == ItemType.SmartPill -> {
                // 直接顯示答案，算答對
                input = answer
                result = true
                correct++
                endRound()
            }
            type == ItemType.Shield -> shieldTurns = ITEM_TURNS
            type == ItemType.Confusion -> confuseTurns = ITEM_TURNS
            else -> hitMonster(type.monsterDamage(total))
        }
    }

    // 監獄的鐵門點兩下：一群囚犯衝出來，怪物損血 1
    fun openPrisonDoor() {
        if (!answering || prisonBreak > 0) return
        prisonBreak++
        hitMonster(1)
    }

    // 每一題先念一次英文發音（魔王戰等出場動畫播完）
    LaunchedEffect(index, introDone) {
        if (word != null && !finished && introDone) {
            delay(300)
            sayWord()
        }
    }
    // 答對：停 1.5 秒看攻擊動畫，再自動下一題
    LaunchedEffect(result) {
        if (result == true) {
            delay(1500)
            next()
        }
    }
    // 答錯：看完正確答案，按向右箭頭才進下一回；HP 歸零就倒下
    fun nextRound() {
        if (result != false) return
        if (heroHp <= 0) {
            died = true
            finished = true
            vm.finishBattle(bookId, questions.take(index + 1), wrong.toList(), total, died = true, monster, practice, knockedOut = false) { reward = it }
        } else {
            next()
        }
    }
    // 怪物還有血：留在原地，稍等一下就顯示結果
    LaunchedEffect(finished) {
        if (finished && !died && !knocked) {
            delay(900)
            endShown = true
        }
    }

    // 鍵盤跳出來時，把戰場、輸入框和按鈕整個往上推（戰場上方的背景被推出畫面），按鈕不會被鍵盤擋住
    val scroll = rememberScrollState()
    val typing = WindowInsets.isImeVisible
    LaunchedEffect(typing, scroll.maxValue) {
        if (typing) scroll.animateScrollTo(scroll.maxValue)
    }

    Scaffold(
        topBar = { PixelHeader(tr(R.string.battle_title, bookName), onBack, backDescription = tr(R.string.battle_flee), theme = HeaderTheme.Stone) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(scroll),
        ) {
            // 被打倒：先在戰場上倒下，再接著被夥伴拖棺材去教堂
            if (died && fallen) {
                FuneralScene(onEnd = { endShown = true })
            } else {
                BattleField(
                    scene = scene,
                    monster = monster,
                    hero = avatar,
                    heroName = profile?.name.orEmpty(),
                    tier = tier,
                    finisher = race.finisher,
                    hits = correct,
                    bonusHits = bonusHits,
                    attacks = monsterAttacks,
                    lastBlocked = lastBlocked,
                    selfHits = selfHits,
                    shielded = shieldTurns > 0 || (lastBlocked && result == false),
                    confused = confuseTurns > 0,
                    monsterHp = monsterHp,
                    monsterMax = total,
                    heroHp = heroHp,
                    heroMax = HERO_MAX_HP,
                    knockout = knocked,
                    onKnockoutEnd = { endShown = true },
                    intro = !introDone,
                    onIntroEnd = { introDone = true },
                    itemUse = itemUse,
                    prisonBreak = prisonBreak,
                    onPrisonDoor = { openPrisonDoor() },
                    falling = died,
                    onFallEnd = { fallen = true },
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (total > 0 && !finished && isBoss) {
                    Text(
                        tr(R.string.battle_boss_appears, bossRank(monster), monster.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelRed,
                    )
                }
                when {
                    total == 0 -> {
                        Text(tr(R.string.battle_no_words), style = MaterialTheme.typography.bodyLarge.readable())
                        RetroButton(tr(R.string.back_to_book), onClick = onBack)
                    }
                    died -> {
                        Text(tr(R.string.battle_hero_down, profile?.name.orEmpty()), style = MaterialTheme.typography.headlineSmall)
                        Text(tr(R.string.battle_dragged))
                        reward?.let { Text(tr(R.string.battle_death_count, it.deaths), color = PixelRed) }
                    }
                    knocked -> {
                        Text(tr(R.string.battle_finisher, race.finisher.label(tier)), style = MaterialTheme.typography.headlineSmall)
                        Text(tr(R.string.battle_knocked, monster.name))
                    }
                    finished -> {
                        Text(tr(R.string.battle_still_standing, monster.name), style = MaterialTheme.typography.headlineSmall)
                        Text(tr(R.string.battle_still_standing_hint))
                    }
                    !introDone -> Text(tr(R.string.battle_watch_out), style = MaterialTheme.typography.titleMedium)
                    word != null -> {
                        Question(
                            answer = answer,
                            meaning = word.meaning,
                            input = input,
                            onInput = { if (result == null) input = it.filterNot { c -> c == '\n' } },
                            result = result,
                            onListen = ::sayWord,
                            onSubmit = ::submit,
                            onNext = ::nextRound,
                            onBag = { showBag = true },
                            bagEnabled = answering,
                        )
                    }
                }
            }
        }
    }

    if (showBag) {
        ItemBagDialog(
            counts = items,
            onDismiss = { showBag = false },
            canUse = { type ->
                answering && when (type) {
                    ItemType.Shield -> shieldTurns == 0
                    ItemType.Confusion -> confuseTurns == 0
                    else -> type.heal == 0 || damage > 0
                }
            },
            onUse = ::useItem,
        )
    }

    val done = reward
    if (endShown && done != null) {
        val chestItem = done.item
        if (showingLevelUp) {
            LevelUpScreen(avatar, race, done.levelBefore, done.levelAfter, onClose = { levelUpDone = true })
        } else if (showingChest && chestItem != null) {
            ChestScreen(chestItem, onClose = { chestDone = true })
        } else {
            ResultDialog(done, monster, race, knocked, onClose = onBack)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Question(
    answer: String,
    meaning: String,
    input: String,
    onInput: (String) -> Unit,
    result: Boolean?,
    onListen: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onBag: () -> Unit,
    bagEnabled: Boolean,
) {
    val wrong = result == false
    // 鍵盤跳出來時按鈕縮小一點，整個答題區比較矮
    val typing = WindowInsets.isImeVisible
    val buttonSize = if (typing) 52.dp else 64.dp
    // 由上往下：答對答錯的結果 → 按鈕 → 咒語輸入框
    // 輸入框放在最下面，畫面捲到底時它就緊貼在鍵盤上方，按鈕在它正上方，都不會被鍵盤蓋住
    when (result) {
        true -> Text(tr(R.string.battle_hit), style = MaterialTheme.typography.headlineSmall, color = PixelRed)
        false -> Text(tr(R.string.battle_hurt), style = MaterialTheme.typography.titleMedium, color = PixelRed)
        null -> Unit
    }
    if (wrong && meaning.isNotBlank()) Text(meaning, style = MaterialTheme.typography.titleMedium.readable())
    // 左邊：大聲公再聽一次；中間：道具袋；右邊：劍攻擊，答錯後換成向右箭頭「下一回」
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PixelIconButton(IconMegaphone, tr(R.string.speak_again), onClick = onListen, size = buttonSize, palette = ColorIconPalette)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PixelIconButton(IconBag, tr(R.string.battle_items), onClick = onBag, size = buttonSize, palette = ItemPalette, enabled = bagEnabled)
        }
        if (wrong) {
            PixelIconButton(IconArrowRight, tr(R.string.battle_next), onClick = onNext, size = buttonSize, color = PixelGold, palette = ColorIconPalette)
        } else {
            PixelIconButton(
                IconSword,
                tr(R.string.battle_attack),
                onClick = onSubmit,
                size = buttonSize,
                color = PixelGold,
                palette = ColorIconPalette,
                enabled = result == null && input.isNotBlank(),
            )
        }
    }
    // 空白的輸入框，聽發音拼出整個單字；答錯時框裡直接換成正確答案（紅字）
    // 鍵盤上的「完成」也能攻擊（答錯時是下一回），不用把手移到上面的按鈕
    SpellSlab(
        value = if (wrong) answer else input,
        onValueChange = onInput,
        result = result,
        onDone = { if (wrong) onNext() else onSubmit() },
    )
}

// 頭上的小血條，血少了會變黃、變紅
@Composable
private fun MiniHpBar(hp: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(if (max == 0) 0f else hp.toFloat() / max, tween(500), label = "hp")
    Box(modifier.height(8.dp).background(Color.White).border(2.dp, PixelInk).padding(2.dp)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction)
                .background(if (fraction > 0.5f) Color(0xFF40A048) else if (fraction > 0.2f) PixelGold else PixelRed),
        )
    }
}

// 頭上的名字牌：深色底白字，魔王是紅字
@Composable
private fun NameTag(name: String, color: Color = Color.White) {
    Text(
        name,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier.background(PixelInk.copy(alpha = 0.7f)).padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

// 戰鬥場景：背景、左半邊是自己的人物、右半邊是怪物（會呼吸、被打會晃、答錯會衝過來攻擊）、
// 招式和道具的特效、魔王出場動畫
@Composable
private fun BattleField(
    scene: ReviewScene,
    monster: Monster,
    hero: Avatar,
    heroName: String,
    tier: Int,
    finisher: Finisher,
    hits: Int,
    bonusHits: Int,
    attacks: Int,
    lastBlocked: Boolean,
    selfHits: Int,
    shielded: Boolean,
    confused: Boolean,
    monsterHp: Int,
    monsterMax: Int,
    heroHp: Int,
    heroMax: Int,
    knockout: Boolean,
    onKnockoutEnd: () -> Unit,
    intro: Boolean,
    onIntroEnd: () -> Unit,
    itemUse: ItemUse?,
    prisonBreak: Int = 0,
    onPrisonDoor: () -> Unit = {},
    falling: Boolean = false,
    onFallEnd: () -> Unit = {},
) {
    val artW = Cell * monster.art.first().length
    val artH = Cell * monster.art.size
    val endCallback by rememberUpdatedState(onKnockoutEnd)
    val introCallback by rememberUpdatedState(onIntroEnd)

    // 待機的小動作：上下彈跳、身體一伸一縮
    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse), label = "bob",
    )

    BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds()) {
        val half = maxWidth / 2
        // 衝過去的距離：差不多走到對方面前
        val reach = (half * 0.6f).value

        // 答對：人物衝上去出招；怪物被打中時左右晃、變透明一下
        val shake = remember { Animatable(0f) }
        val flash = remember { Animatable(0f) }
        val heroLunge = remember { Animatable(0f) }
        val strike = remember { Animatable(0f) }
        LaunchedEffect(hits) {
            if (hits == 0) return@LaunchedEffect
            launch {
                heroLunge.animateTo(0f, keyframes {
                    durationMillis = 700
                    -6f at 100
                    reach at 250
                    reach at 450
                    0f at 700
                })
            }
            launch {
                strike.snapTo(0f)
                strike.animateTo(1f, tween(700, easing = LinearEasing))
                strike.snapTo(0f)
            }
            delay(300)
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(450))
            }
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                10f at 60
                -10f at 120
                7f at 180
                -5f at 260
            })
        }
        // 道具（天使、村民）打中怪物
        LaunchedEffect(bonusHits) {
            if (bonusHits == 0) return@LaunchedEffect
            delay(900)
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(600))
            }
            shake.animateTo(0f, keyframes {
                durationMillis = 500
                14f at 80
                -14f at 160
                10f at 240
                -6f at 360
            })
        }
        // 怪物攻擊：往左衝向人物再退回來，人物被打到會晃、閃爍
        val lunge = remember { Animatable(0f) }
        val heroShake = remember { Animatable(0f) }
        val heroFlash = remember { Animatable(0f) }
        val blocked by rememberUpdatedState(lastBlocked)
        val block = remember { Animatable(0f) }
        LaunchedEffect(attacks) {
            if (attacks == 0) return@LaunchedEffect
            launch {
                lunge.animateTo(0f, keyframes {
                    durationMillis = 600
                    12f at 120
                    -reach at 260
                    -reach at 320
                    0f at 600
                })
            }
            delay(260)
            // 被防護盾擋下：光罩亮一下，人物不閃也不晃
            if (blocked) {
                block.snapTo(1f)
                block.animateTo(0f, tween(600))
                return@LaunchedEffect
            }
            launch {
                heroFlash.snapTo(1f)
                heroFlash.animateTo(0f, tween(600))
            }
            heroShake.animateTo(0f, keyframes {
                durationMillis = 400
                -8f at 60
                8f at 140
                -6f at 220
                4f at 300
            })
        }

        // 混亂打自己：怪物原地轉一圈，再被自己打到晃一下
        val selfSpin = remember { Animatable(0f) }
        LaunchedEffect(selfHits) {
            if (selfHits == 0) return@LaunchedEffect
            selfSpin.snapTo(0f)
            selfSpin.animateTo(1f, tween(500, easing = LinearEasing))
            selfSpin.snapTo(0f)
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(450))
            }
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                10f at 60
                -10f at 120
                7f at 180
                -5f at 260
            })
        }
        // 混亂星星一直繞圈
        val spinClock by idle.animateFloat(
            0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "stars",
        )

        // 擊倒：前半段是招式特效，後半段怪物飛走
        val ko = remember { Animatable(0f) }
        LaunchedEffect(knockout) {
            if (!knockout) return@LaunchedEffect
            delay(300)
            ko.animateTo(1f, tween(2200, easing = LinearEasing))
            endCallback()
        }

        // 魔王出場：WARNING 閃紅光 → 魔王從天而降 → 落地震動
        val introAnim = remember { Animatable(if (intro) 0f else 1f) }
        val bigBoss = monster.rank == MonsterRank.Big
        LaunchedEffect(Unit) {
            if (!intro) return@LaunchedEffect
            introAnim.animateTo(1f, tween(if (bigBoss) 2600 else 1800, easing = LinearEasing))
            introCallback()
        }
        val ip = introAnim.value
        val drop = ((ip - 0.45f) / 0.25f).coerceIn(0f, 1f)
        val landShake = if (ip in 0.7f..0.85f) sin(ip * 120f) * (if (bigBoss) 8f else 4f) else 0f

        // 道具動畫
        val itemAnim = remember { Animatable(0f) }
        LaunchedEffect(itemUse?.id) {
            if (itemUse == null) return@LaunchedEffect
            itemAnim.snapTo(0f)
            val duration = when (itemUse.type) {
                ItemType.Angel -> 1800
                ItemType.Riot -> 1600
                ItemType.Elixir -> 1600
                ItemType.SmartPill -> 1400
                ItemType.Catapult -> 1200
                else -> 1100
            }
            itemAnim.animateTo(1f, tween(duration, easing = LinearEasing))
            itemAnim.snapTo(0f)
        }

        // 監獄彩蛋：囚犯衝過怪物
        val prisonAnim = remember { Animatable(0f) }
        LaunchedEffect(prisonBreak) {
            if (prisonBreak == 0) return@LaunchedEffect
            prisonAnim.snapTo(0f)
            prisonAnim.animateTo(1f, tween(1600, easing = LinearEasing))
            prisonAnim.snapTo(0f)
        }
        val doorTap by rememberUpdatedState(onPrisonDoor)

        // 倒下：搖晃一下 → 一格一格往後倒 → 躺在地上變灰
        val fall = remember { Animatable(0f) }
        val fallCallback by rememberUpdatedState(onFallEnd)
        LaunchedEffect(falling) {
            if (!falling) return@LaunchedEffect
            fall.animateTo(1f, tween(1800, easing = LinearEasing))
            fallCallback()
        }
        val f = fall.value
        val fallWobble = if (f in 0.01f..0.3f) (if ((f * 30).toInt() % 2 == 0) -3f else 3f) else 0f
        val fallAngle = when {
            f < 0.3f -> 0f
            f < 0.7f -> -90f * floor((f - 0.3f) / 0.4f * 4f) / 4f
            else -> -90f
        }

        val p = ko.value
        val effect = (p / 0.5f).coerceIn(0f, 1f)
        val fly = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val move = flight(finisher, fly)
        // 巨斧落下時怪物被壓扁
        val squash = if (finisher == Finisher.Smash && effect in 0.6f..1f && fly == 0f) 0.45f else 1f

        val prisonScene = scene == ReviewScene.Prison && monster.rank == MonsterRank.Normal
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = landShake.dp.toPx() }
                .then(
                    if (!prisonScene) Modifier else Modifier.pointerInput(Unit) {
                        // 點兩下場景左邊的鐵門才算
                        detectTapGestures(onDoubleTap = { pos ->
                            val cell = size.width.toFloat() / scene.art.first().length
                            val x = (pos.x / cell).toInt()
                            val y = (pos.y / cell).toInt()
                            if (x in PRISON_DOOR_LEFT..PRISON_DOOR_RIGHT && y in PRISON_DOOR_TOP..PRISON_DOOR_BOTTOM) doorTap()
                        })
                    }
                ),
        ) {
            BattleBackdrop(scene, monster.rank)
            Row(Modifier.matchParentSize()) {
                // 左半邊：自己的人物
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier.padding(bottom = GroundPad),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (heroName.isNotBlank()) NameTag(heroName)
                        MiniHpBar(heroHp, heroMax, Modifier.padding(vertical = 2.dp).size(48.dp, 8.dp))
                        PixelAvatar(
                            hero,
                            size = HeroSize,
                            tier = tier,
                            modifier = Modifier.graphicsLayer {
                                translationX = heroShake.value.dp.toPx() + heroLunge.value.dp.toPx() + fallWobble.dp.toPx()
                                // 被打中時一閃一閃；倒下後變淡
                                alpha = when {
                                    f >= 0.7f -> 0.6f
                                    heroFlash.value > 0f && (heroFlash.value * 10).toInt() % 2 == 0 -> 0.25f
                                    else -> 1f
                                }
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationZ = fallAngle
                            },
                        )
                    }
                }
                // 右半邊：怪物
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier.padding(bottom = GroundPad),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val tagsVisible = !knockout && drop >= 1f
                        Box(Modifier.alpha(if (tagsVisible) 1f else 0f)) {
                            NameTag(monster.name, if (monster.rank == MonsterRank.Normal) Color.White else Color(0xFFFF7060))
                        }
                        MiniHpBar(
                            monsterHp,
                            monsterMax,
                            Modifier.padding(vertical = 2.dp).size(maxOf(artW, 48.dp), 8.dp).alpha(if (tagsVisible) 1f else 0f),
                        )
                        PixelArt(
                            monster.art,
                            ScenePalette,
                            Modifier
                                .size(artW, artH)
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                    translationX = shake.value.dp.toPx() + lunge.value.dp.toPx() + move.dx.dp.toPx()
                                    translationY = -bob * 4.dp.toPx() + move.dy.dp.toPx() -
                                        (1f - drop) * (artH + 160.dp).toPx()
                                    // 混亂打自己時一格一格轉一圈
                                    rotationZ = move.rotation + floor(selfSpin.value * 8f) * 45f
                                    scaleX = move.scale * (1f + 0.04f * bob)
                                    scaleY = move.scale * (1f - 0.04f * bob) * squash
                                    alpha = if (drop <= 0f) 0f else move.alpha * (1f - 0.7f * flash.value)
                                },
                        )
                    }
                }
            }

            // 特效都畫在這一層：位置用左右兩半的中心算
            Canvas(Modifier.matchParentSize()) {
                val ground = size.height - GroundPad.toPx()
                val heroCenter = Offset(size.width / 4 + heroLunge.value.dp.toPx(), ground - HeroSize.toPx() / 2)
                val monsterCenter = Offset(size.width * 3 / 4, ground - artH.toPx() / 2)
                val radius = maxOf(artW, artH).toPx() / 2
                // 打在人物身上的爪痕
                if (heroFlash.value > 0.4f) {
                    val px = 3.dp.toPx()
                    for (i in -1..1) {
                        val off = i * 8.dp.toPx()
                        drawLine(
                            Color.White,
                            Offset(heroCenter.x - 14.dp.toPx() + off, heroCenter.y - 14.dp.toPx()),
                            Offset(heroCenter.x + 10.dp.toPx() + off, heroCenter.y + 14.dp.toPx()),
                            px,
                        )
                    }
                }
                // 答對時的小招式（比擊倒的招式小一點）
                if (!knockout && strike.value > 0f) {
                    drawFinisher(finisher, strike.value, monsterCenter, radius * 0.7f, tier, heroCenter, small = true)
                }
                if (knockout && fly < 0.3f) {
                    drawFinisher(finisher, effect, monsterCenter, radius + 6.dp.toPx(), tier, heroCenter)
                }
                // 道具效果
                val t = itemAnim.value
                if (itemUse != null && t > 0f) {
                    when (itemUse.type) {
                        ItemType.SmallPotion, ItemType.BigPotion, ItemType.Elixir ->
                            drawHeal(itemUse.type, t, heroCenter, HeroSize.toPx())
                        ItemType.SmartPill -> drawBulb(t, heroCenter, HeroSize.toPx())
                        ItemType.Angel -> drawAngels(t, monsterCenter)
                        ItemType.Riot -> drawRiot(t, ground)
                        ItemType.Catapult -> drawCatapult(t, ground, monsterCenter)
                        ItemType.Shield, ItemType.Confusion -> Unit
                    }
                }
                if (prisonAnim.value > 0f) drawRiot(prisonAnim.value, ground, prisoners = true)
                // 防護盾的光罩、怪物混亂的星星
                if (shielded || block.value > 0f) {
                    drawShieldBubble(heroCenter, HeroSize.toPx(), bob, block.value)
                }
                if (confused && !knockout) {
                    drawConfusionStars(monsterCenter, artH.toPx(), spinClock)
                }
                // 魔王出場：畫面一閃一閃的紅光
                if (ip < 0.9f) {
                    val blink = if ((ip * 16).toInt() % 2 == 0) 1f else 0.3f
                    val tint = if (bigBoss) Color(0xFFE02020) else Color(0xFFF09030)
                    drawRect(tint.copy(alpha = 0.35f * blink * (1f - ip)))
                }
            }

            // 魔王出場：WARNING 橫幅
            if (ip < 0.9f) {
                val on = (ip * 12).toInt() % 2 == 0
                Box(
                    Modifier.align(Alignment.Center).fillMaxWidth().background(PixelInk.copy(alpha = 0.8f)).padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (bigBoss) "!! WARNING !!" else "! WARNING !",
                        color = if (on) PixelRed else PixelGold,
                        style = if (bigBoss) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

private class Flight(val dx: Float, val dy: Float, val rotation: Float, val scale: Float = 1f, val alpha: Float = 1f)

// 怪物飛走的方式（位移單位是 dp），t 從 0 到 1
private fun flight(finisher: Finisher, t: Float): Flight = when (finisher) {
    Finisher.Slash -> Flight(t * 320f, -t * 220f, t * 720f)
    Finisher.Magic -> Flight(0f, -t * 40f, t * 540f, scale = 1f - t, alpha = 1f - t)
    Finisher.Arrow -> Flight(t * 380f, -t * 140f, t * 480f)
    Finisher.Bomb -> Flight(t * 40f, -t * 360f, t * 200f)
    Finisher.Smash -> Flight(t * 340f, -sin(t * PI.toFloat()) * 90f, t * 540f)
    Finisher.Claw -> Flight(t * 340f, -t * 180f, -t * 540f)
    Finisher.Fire -> Flight(t * 260f, -t * 300f, t * 360f, alpha = 1f - 0.6f * t)
    Finisher.Laser -> Flight(t * 400f, -t * 60f, t * 1080f)
    Finisher.DarkFire -> Flight(t * 200f, -t * 280f, t * 720f, scale = 1f - 0.5f * t, alpha = 1f - 0.8f * t)
}

// 招式特效，e 從 0 到 1；稱號階級 tier 越高，特效越大、越華麗；small 是答對時的小招式
private fun DrawScope.drawFinisher(
    finisher: Finisher,
    e: Float,
    center: Offset,
    baseRadius: Float,
    tier: Int,
    from: Offset,
    small: Boolean = false,
) {
    val radius = baseRadius * (1f + 0.12f * tier)
    // 第 3 階以上：外圍多一圈金色光環
    if (tier >= 3 && !small) {
        drawCircle(
            PixelGold.copy(alpha = (1f - e).coerceIn(0f, 1f) * 0.8f),
            radius * (0.8f + e),
            center,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
    drawFinisherMove(finisher, e, center, radius, tier, small, from)
    // 第 2 階以上：斬擊、魔法、爪擊再轉個角度多出一次
    if (tier >= 2 && finisher in listOf(Finisher.Slash, Finisher.Magic, Finisher.Claw)) {
        rotate(90f, center) { drawFinisherMove(finisher, (e * 1.2f - 0.2f).coerceIn(0f, 1f), center, radius, tier, small, from) }
    }
    // 最高階：整個畫面閃一下白光
    if (tier >= 5 && !small && e in 0.4f..0.7f) {
        drawRect(Color.White.copy(alpha = 0.5f - kotlin.math.abs(e - 0.55f) * 3f))
    }
}

private fun DrawScope.drawFinisherMove(
    finisher: Finisher,
    e: Float,
    center: Offset,
    radius: Float,
    tier: Int,
    small: Boolean,
    from: Offset,
) {
    val px = 4.dp.toPx()
    when (finisher) {
        // 勇者：兩道交叉的劍光
        Finisher.Slash -> {
            val a = (e * 2).coerceIn(0f, 1f)
            val b = (e * 2 - 1).coerceIn(0f, 1f)
            val r = radius * 1.2f
            drawLine(Color.White, center + Offset(-r, -r), center + Offset(-r + 2 * r * a, -r + 2 * r * a), px * 1.5f)
            if (b > 0f) drawLine(Color.White, center + Offset(r, -r), center + Offset(r - 2 * r * b, -r + 2 * r * b), px * 1.5f)
        }
        // 魔法師：彩色星光從四周轉著聚過來
        Finisher.Magic -> {
            val colors = listOf(Color(0xFFF8D030), Color(0xFFC090E8), Color(0xFF90D0F8), Color.White)
            for (i in 0 until 10) {
                val angle = (i * 36f + e * 220f) * PI.toFloat() / 180f
                val d = radius * 2.2f * (1f - e) + radius * 0.2f
                val pos = center + Offset(cos(angle) * d, sin(angle) * d)
                drawRect(colors[i % colors.size], pos - Offset(px, px), Size(px * 2, px * 2))
            }
            if (e > 0.7f) drawCircle(Color.White.copy(alpha = (1f - e) * 3f), radius * (e - 0.4f) * 2f, center)
        }
        // 精靈弓箭手：箭從左邊飛過來射中怪物，稱號越高箭越多
        Finisher.Arrow -> {
            val count = if (small) 1 + tier / 2 else 1 + tier
            for (i in 0 until count) {
                val t = (e * 1.6f - i * 0.1f).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val start = from + Offset(0f, -(i * 5).dp.toPx())
                val target = center + Offset(((i % 3) - 1) * px * 2, ((i % 2) * 2 - 1) * px * (i / 2))
                val pos = Offset(start.x + (target.x - start.x) * t, start.y + (target.y - start.y) * t - sin(t * PI.toFloat()) * 30.dp.toPx())
                val dx = target.x - start.x
                val dy = target.y - start.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val dir = Offset(dx / len, dy / len)
                val tail = pos - dir * (px * 5)
                drawLine(Color(0xFF805030), tail, pos, px * 0.7f)
                drawRect(Color.White, pos - Offset(px * 0.7f, px * 0.7f), Size(px * 1.4f, px * 1.4f))
                drawRect(Color(0xFF40A048), tail - Offset(px * 0.8f, px * 0.8f), Size(px * 1.6f, px * 1.6f))
            }
            // 射中時的光點
            if (e > 0.7f) {
                val glow = if (tier >= 3) Color(0xFFF8F0A0) else Color.White
                drawCircle(glow.copy(alpha = (1f - e) * 3f), radius * 0.6f, center)
            }
        }
        // 哥布林：丟出炸彈，碰到就爆炸
        Finisher.Bomb -> {
            if (e < 0.5f) {
                val t = e / 0.5f
                val start = from
                val pos = Offset(start.x + (center.x - start.x) * t, start.y + (center.y - start.y) * t - sin(t * PI.toFloat()) * 60.dp.toPx())
                drawCircle(Color(0xFF202030), px * 2.5f, pos)
                drawRect(Color(0xFFF09030), pos + Offset(px, -px * 4), Size(px, px))
            } else {
                val t = (e - 0.5f) / 0.5f
                drawCircle(Color(0xFFE04030), radius * 1.4f * t, center)
                drawCircle(Color(0xFFF09030), radius * 1.0f * t, center)
                drawCircle(Color(0xFFF8D030), radius * 0.6f * t, center)
            }
        }
        // 矮人：巨斧從天而降
        Finisher.Smash -> {
            val t = (e / 0.6f).coerceIn(0f, 1f)
            val top = center.y - radius
            val y = -radius + (top + radius) * t
            drawRect(Color(0xFF805030), Offset(center.x - px / 2, y - radius * 1.6f), Size(px, radius * 1.6f))
            drawRect(Color(0xFFC0C8D8), Offset(center.x - radius * 0.7f, y - px * 5), Size(radius * 1.4f, px * 5))
            if (e >= 0.6f) {
                for (i in -2..2) {
                    drawLine(Color.White, Offset(center.x + i * radius * 0.5f, top), Offset(center.x + i * radius * 0.8f, top - px * 4), px)
                }
            }
        }
        // 貓族：三道爪痕
        Finisher.Claw -> {
            val r = radius * 1.1f
            for (i in 0 until 3) {
                val t = (e * 3 - i).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val off = (i - 1) * px * 4
                val s = center + Offset(-r + off, -r)
                drawLine(Color(0xFFE04030), s, s + Offset(2 * r * t, 2 * r * t), px * 1.2f)
            }
        }
        // 龍族：從人物口中噴出火焰
        Finisher.Fire -> {
            val start = from
            val colors = listOf(Color(0xFFE04030), Color(0xFFF09030), Color(0xFFF8D030))
            for (i in 0 until 18) {
                val t = (e * 1.6f - i * 0.05f).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val wobble = sin((i * 1.7f + e * 12f)) * px * 2
                val pos = Offset(start.x + (center.x - start.x) * t, start.y + (center.y - start.y) * t + wobble)
                val s = px * (1.5f + t * 2f)
                drawRect(colors[i % 3], pos - Offset(s / 2, s / 2), Size(s, s))
            }
        }
        // 機械族：雷射光束
        Finisher.Laser -> {
            val t = (e / 0.4f).coerceIn(0f, 1f)
            val thick = px * (2f + 2f * sin(e * 20f).coerceAtLeast(0f))
            val endX = from.x + (center.x + radius - from.x) * t
            drawRect(Color(0xFF90D0F8), Offset(from.x, center.y - thick), Size(endX - from.x, thick * 2))
            drawRect(Color.White, Offset(from.x, center.y - thick / 3), Size(endX - from.x, thick * 2 / 3))
        }
        // 魔族：從手上丟出暗影球，打中後紫黑色的火焰從怪物腳下竄起
        Finisher.DarkFire -> {
            val dark = Color(0xFF302848)
            val purple = Color(0xFF9060C0)
            val pink = Color(0xFFC090E8)
            if (e < 0.4f) {
                val t = e / 0.4f
                val pos = Offset(from.x + (center.x - from.x) * t, from.y + (center.y - from.y) * t)
                drawCircle(purple, px * 3f, pos)
                drawCircle(dark, px * 2f, pos)
            } else {
                val t = (e - 0.4f) / 0.6f
                val flames = if (small) 5 else 9
                for (i in 0 until flames) {
                    val x = center.x + (i - flames / 2) * radius * 0.28f
                    val h = radius * (0.8f + 0.6f * sin(i * 1.3f + e * 18f).coerceAtLeast(0f)) * sin(t * PI.toFloat())
                    val bottom = center.y + radius * 0.8f
                    drawRect(dark, Offset(x - px, bottom - h), Size(px * 2.2f, h))
                    drawRect(purple, Offset(x - px * 0.6f, bottom - h * 0.75f), Size(px * 1.4f, h * 0.75f))
                    drawRect(pink, Offset(x - px * 0.3f, bottom - h * 0.35f), Size(px * 0.8f, h * 0.35f))
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(reward: BattleReward, monster: Monster, race: Race, knocked: Boolean, onClose: () -> Unit) {
    val monsterName = monster.name
    val leveled = reward.levelAfter > reward.levelBefore
    val oldTitle = race.title(reward.levelBefore)
    val newTitle = race.title(reward.levelAfter)
    val perfect = !reward.died && reward.correct == reward.total
    PixelAlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                when {
                    reward.died -> tr(R.string.result_down)
                    perfect -> tr(R.string.result_perfect)
                    reward.passed -> tr(R.string.result_win)
                    else -> tr(R.string.result_lose)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    reward.died -> Text(tr(R.string.result_down_message, monsterName, reward.deaths))
                    knocked -> Text(tr(R.string.result_knocked, monsterName))
                    else -> Text(tr(R.string.result_standing, monsterName))
                }
                Text(tr(R.string.result_score, reward.correct, reward.total, passScore(reward.total)))
                Text(tr(if (monster.rank != MonsterRank.Normal) R.string.result_exp_boss else R.string.result_exp, reward.exp))
                if (reward.practice) Text(tr(R.string.result_practice), style = MaterialTheme.typography.bodySmall)
                reward.item?.let { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelArt(item.icon, ItemPalette, Modifier.size(32.dp))
                        Text(tr(R.string.result_chest, item.label), color = PixelRed, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (leveled) Text(tr(R.string.result_level_up, reward.levelBefore, reward.levelAfter), color = PixelRed)
                if (newTitle != oldTitle) Text(tr(R.string.new_title, newTitle), color = PixelRed)
                if (reward.passed && monster.rank != MonsterRank.Normal) {
                    Text(tr(R.string.result_boss_down, bossRank(monster), monsterName), color = PixelRed)
                } else if (monster.rank != MonsterRank.Normal) {
                    Text(tr(R.string.result_boss_hint), style = MaterialTheme.typography.bodySmall)
                }
                if (reward.passed) Text(tr(R.string.result_book_cleared))
                if (reward.forgotten.isNotEmpty()) {
                    Text(
                        tr(R.string.result_forgotten, reward.forgotten.joinToString(tr(R.string.list_separator))),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { RetroButton(tr(R.string.back_to_book), onClick = onClose) },
    )
}

// 升級畫面：整個畫面換成像素風景，人物跳一跳、LEVEL UP 閃爍，能力一項一項顯示提升了多少
// 稱號升階時，頭像換成新的樣子、學會新招式
@Composable
private fun LevelUpScreen(hero: Avatar, race: Race, before: Int, after: Int, onClose: () -> Unit) {
    val old = race.stats(before)
    val newStats = race.stats(after)
    val rows = listOf(
        Triple(tr(R.string.stat_str), old.str, newStats.str),
        Triple(tr(R.string.stat_vit), old.vit, newStats.vit),
        Triple(tr(R.string.stat_int), old.intel, newStats.intel),
        Triple(tr(R.string.stat_luk), old.luk, newStats.luk),
    )
    val oldTitle = race.title(before)
    val newTitle = race.title(after)
    val oldTier = avatarTier(before)
    // 背景隨機：城堡、歡呼的城鎮、怪物逃竄的黑暗森林
    val backdrop = remember { LevelUpBackdrop.entries.random() }
    val fleeing = remember { NormalMonsters.shuffled().take(5) }
    val newTier = avatarTier(after)
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(600)
        while (shown < rows.size + 1) {
            shown++
            delay(350)
        }
    }
    val anim = rememberInfiniteTransition(label = "levelup")
    val hop by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "hop",
    )
    val blink by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Restart), label = "blink",
    )
    // 怪物慢慢跑過整個畫面
    val roam by anim.animateFloat(
        0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "roam",
    )
    // 閃閃發亮的星星
    val twinkle by anim.animateFloat(
        0f, 8f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "twinkle",
    )

    Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(backdrop.ground)) {
            Box(Modifier.fillMaxWidth()) {
                PixelArt(backdrop.art, ScenePalette, Modifier.fillMaxWidth())
                Canvas(Modifier.matchParentSize()) {
                    val cell = size.width / backdrop.art.first().length
                    when (backdrop) {
                        // 城堡：天空閃閃發亮的星星
                        LevelUpBackdrop.Castle -> {
                            val step = twinkle.toInt()
                            LevelUpStars.forEachIndexed { i, (x, y) ->
                                if ((i + step) % 3 != 0) {
                                    val c = if ((i + step) % 2 == 0) PixelGold else Color.White
                                    drawRect(c, Offset(x * cell, y * cell), Size(cell, cell))
                                    drawRect(c.copy(alpha = 0.6f), Offset((x - 1) * cell, y * cell), Size(cell * 3, cell * 0.5f))
                                }
                            }
                        }
                        LevelUpBackdrop.Town -> drawCheeringTown(twinkle / 8f, cell)
                        LevelUpBackdrop.Forest -> drawFleeingMonsters(roam, cell, fleeing)
                    }
                }
            }
            // 和人物能力同一種羊皮紙底的視窗
            PixelSurface(
                color = PixelPaper,
                modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "LEVEL UP!",
                        color = if (blink < 0.5f) PixelRed else PixelGold,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    PixelAvatar(
                        hero,
                        size = 72.dp,
                        tier = if (shown > rows.size) newTier else oldTier,
                        modifier = Modifier.graphicsLayer { translationY = -(hop * 3).toInt() * 4.dp.toPx() },
                    )
                    Text("Lv $before → Lv $after", style = MaterialTheme.typography.titleLarge)
                    rows.forEachIndexed { i, (label, from, to) ->
                        if (i < shown) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f))
                                Text("$from → $to", Modifier.weight(1f))
                                Text("+${to - from}", color = PixelRed)
                            }
                        }
                    }
                    if (shown > rows.size && newTitle != oldTitle) {
                        Text(tr(R.string.new_title, newTitle), color = PixelRed)
                    }
                    if (shown > rows.size && newTier != oldTier) {
                        Text(tr(R.string.levelup_evolve, race.finisher.label(newTier)), color = PixelRed, textAlign = TextAlign.Center)
                    }
                    Button(onClick = onClose, enabled = shown > rows.size) { Text(tr(R.string.great)) }
                }
            }
        }
    }
}

// 升級畫面的背景；ground 是背景圖下方空白處的顏色
private enum class LevelUpBackdrop(val art: List<String>, val ground: Color) {
    Castle(TitleSky, SceneGrass),
    Town(ReviewScene.TownInside.art, ScenePalette.getValue('M')),
    Forest(DarkForest, ScenePalette.getValue('i')),
}

// 城鎮：村民們一跳一跳地歡呼，彩色紙花往下飄（t 從 0 到 1 一直循環）
private fun DrawScope.drawCheeringTown(t: Float, cell: Float) {
    val ground = size.height - cell
    val shirts = listOf('G', 'R', 'B', 'O', 'p', 'y', 'k')
    for (i in 0 until 7) {
        val x = (3 + i * 9) * cell
        // 每個人錯開時間跳
        val jump = if (((t * 16).toInt() + i) % 2 == 0) 0f else cell * 2
        drawVillager(x, ground - jump, cell * 0.8f, shirts[i % shirts.size], cheering = jump > 0f)
    }
    val colors = listOf(Color(0xFFE04030), Color(0xFFF8D030), Color(0xFF3A58C8), Color(0xFF40A048), Color.White)
    for (k in 0 until 30) {
        val x = ((k * 37) % 64) * cell
        val y = (((k * 13) % 28) + t * 28 * 2) % 28 * cell
        drawRect(colors[k % colors.size], Offset(x, y), Size(cell * 0.8f, cell * 0.8f))
    }
}

// 黑暗森林：怪物們嚇得往森林深處逃跑，越跑越遠、越來越小，最後消失在樹林裡
private fun DrawScope.drawFleeingMonsters(t: Float, cell: Float, monsters: List<Monster>) {
    val ground = size.height - cell
    val horizon = size.height * 0.45f
    val vanishX = size.width / 2
    // 遠的先畫，近的怪物才會擋在前面
    monsters.withIndex().sortedByDescending { (t + it.index * 0.2f) % 1f }.forEach { (i, monster) ->
        val p = (t + i * 0.2f) % 1f
        // 每隻從前面不同的位置出發，往畫面中間的遠方跑
        val startX = size.width * (0.1f + 0.8f * ((i * 37) % 100) / 100f)
        val small = cell * (0.9f - 0.75f * p)
        val w = monster.art.first().length * small
        val h = monster.art.size * small
        val footX = startX + (vanishX - startX) * p
        val footY = ground - (ground - horizon) * p
        val hop = if (((t * 24).toInt() + i) % 2 == 0) 0f else small * 1.5f
        val alpha = if (p > 0.8f) (1f - p) / 0.2f else 1f
        val left = footX - w / 2
        val top = footY - h - hop
        monster.art.forEachIndexed { y, row ->
            row.forEachIndexed { cx, ch ->
                val color = ScenePalette[ch] ?: return@forEachIndexed
                drawRect(color.copy(alpha = alpha), Offset(left + cx * small, top + y * small), Size(small + 0.5f, small + 0.5f))
            }
        }
        // 逃跑時往後甩的汗滴
        drawRect(Color(0xFF90D0F8).copy(alpha = alpha), Offset(left + w, top + h * 0.2f), Size(small * 2, small * 3))
    }
}

// 升級畫面天空上星星的位置（TitleSky 的格子）
private val LevelUpStars = listOf(
    3 to 2, 12 to 6, 20 to 1, 26 to 4, 40 to 2, 47 to 7, 58 to 1, 61 to 6, 8 to 9, 33 to 8, 52 to 10, 17 to 11,
)

// 戰鬥的輸入框：一塊刻著金框的黑石板，左上角掛著「咒語」小牌，四個角鑲著寶石；
// 打字時字會發出藍光，答對變金色、答錯變紅色，石板外圈的光跟著一閃一閃
@Composable
private fun SpellSlab(value: String, onValueChange: (String) -> Unit, result: Boolean?, onDone: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "slab").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "glow",
    )
    val tint = when (result) {
        true -> SpellGold
        false -> SpellRed
        null -> SpellBlue
    }
    Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .drawBehind { drawSpellSlab(tint, if (result == null && value.isEmpty()) glow else 1f) }
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (value.isEmpty()) {
                Text(tr(R.string.spell_hint), color = SpellHint, style = MaterialTheme.typography.titleMedium.readable())
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = result != null,
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.readable().copy(
                    textAlign = TextAlign.Center,
                    color = tint,
                    shadow = Shadow(tint.copy(alpha = 0.8f), blurRadius = 12f),
                ),
                cursorBrush = SolidColor(SpellBlue),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 左上角的小木牌
        Text(
            tr(R.string.spell_label),
            color = Color(0xFFFFE8A8),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .offset(x = 16.dp, y = (-10).dp)
                .background(Color(0xFF5A381C))
                .border(2.dp, PixelInk)
                .padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}

private fun DrawScope.drawSpellSlab(tint: Color, glow: Float) {
    val px = 3.dp.toPx()
    val w = size.width
    val h = size.height
    // 外圈發光
    drawRect(tint.copy(alpha = 0.15f + 0.25f * glow), Offset(-px, -px), Size(w + px * 2, h + px * 2))
    // 缺角的黑框
    drawRect(PixelInk, Offset(px, 0f), Size(w - px * 2, h))
    drawRect(PixelInk, Offset(0f, px), Size(w, h - px * 2))
    // 金框：左上亮、右下暗
    drawRect(SlabGoldDark, Offset(px, px), Size(w - px * 2, h - px * 2))
    drawRect(SlabGold, Offset(px, px), Size(w - px * 3, h - px * 3))
    drawRect(SlabGoldLight, Offset(px, px), Size(w - px * 3, px))
    drawRect(SlabGoldLight, Offset(px, px), Size(px, h - px * 3))
    // 黑石板本體，有一塊一塊的石紋
    val inset = px * 3
    drawRect(PixelInk, Offset(inset - px, inset - px), Size(w - inset * 2 + px * 2, h - inset * 2 + px * 2))
    drawRect(SlabStone, Offset(inset, inset), Size(w - inset * 2, h - inset * 2))
    val random = java.util.Random(7)
    repeat(((w / px) / 3).toInt()) {
        val x = inset + random.nextFloat() * (w - inset * 2 - px * 2)
        val y = inset + random.nextFloat() * (h - inset * 2 - px)
        drawRect(if (random.nextBoolean()) SlabStoneLight else SlabStoneDark, Offset(x, y), Size(px * (1 + random.nextInt(3)), px * 0.7f))
    }
    // 寫咒語的發光底線
    drawRect(tint.copy(alpha = 0.35f + 0.4f * glow), Offset(inset + px * 4, h - inset - px * 3), Size(w - inset * 2 - px * 8, px * 0.7f))
    // 四個角的寶石
    for ((cx, cy) in listOf(px * 1.5f to px * 1.5f, w - px * 4.5f to px * 1.5f, px * 1.5f to h - px * 4.5f, w - px * 4.5f to h - px * 4.5f)) {
        drawRect(PixelInk, Offset(cx, cy), Size(px * 3, px * 3))
        drawRect(tint, Offset(cx + px * 0.5f, cy + px * 0.5f), Size(px * 2, px * 2))
        drawRect(Color.White.copy(alpha = 0.8f), Offset(cx + px * 0.5f, cy + px * 0.5f), Size(px * 0.8f, px * 0.8f))
    }
}

private val SpellBlue = Color(0xFF8CE8FF)
private val SpellGold = Color(0xFFF8D030)
private val SpellRed = Color(0xFFFF6A5A)
private val SpellHint = Color(0xFF7A86A0)
private val SlabStone = Color(0xFF262838)
private val SlabStoneLight = Color(0xFF363A50)
private val SlabStoneDark = Color(0xFF1A1C28)
private val SlabGold = Color(0xFFC89A3A)
private val SlabGoldLight = Color(0xFFF0D080)
private val SlabGoldDark = Color(0xFF7A5418)

// 魔王的等級名稱：大魔王、中魔王
private fun bossRank(monster: Monster): String =
    tr(if (monster.rank == MonsterRank.Big) R.string.boss_big else R.string.boss_mid)
