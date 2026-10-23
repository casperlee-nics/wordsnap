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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.Job
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed
import com.wordsnap.app.ui.theme.PixelShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
@OptIn(ExperimentalMaterial3Api::class)
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
    // 魔王出場動畫播完了才開始出題
    var introDone by remember { mutableStateOf(!isBoss) }
    // 玩家被打掉的 HP、道具讓怪物多扣的血
    var damage by remember { mutableIntStateOf(0) }
    var bonus by remember { mutableIntStateOf(0) }
    var bonusHits by remember { mutableIntStateOf(0) }
    var itemUse by remember { mutableStateOf<ItemUse?>(null) }
    var showBag by remember { mutableStateOf(false) }
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

    LoopingMusic(
        when (monster.rank) {
            MonsterRank.Big -> R.raw.bigboss_bgm
            MonsterRank.Mid -> R.raw.midboss_bgm
            MonsterRank.Normal -> R.raw.battle_bgm
        },
        enabled = musicOn && introDone && !died && !showingLevelUp && !showingChest,
        volume = if (quiet) 0.05f else 0.3f,
    )
    OneShotSound(R.raw.warning_jingle, play = isBoss && !introDone)
    OneShotSound(R.raw.death_jingle, play = died)
    OneShotSound(R.raw.levelup_jingle, play = showingLevelUp)
    OneShotSound(R.raw.chest_jingle, play = showingChest)

    val total = questions.size
    val word = questions.getOrNull(index)
    val answer = word?.text?.trim().orEmpty()
    val heroHp = (HERO_MAX_HP - damage).coerceAtLeast(0)
    val monsterHp = (total - correct - bonus).coerceAtLeast(0)
    val knocked = finished && !died && total > 0 && monsterHp == 0
    val answering = introDone && !finished && word != null && result == null

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

    fun submit() {
        if (!answering || input.isBlank() || word == null) return
        // 要拼出整個單字
        val ok = input.trim().equals(answer, ignoreCase = true)
        result = ok
        if (ok) {
            correct++
        } else {
            wrong.add(word)
            damage += WRONG_DAMAGE
        }
    }

    fun next() {
        input = ""
        result = null
        if (index + 1 >= total) {
            finished = true
            vm.finishBattle(bookId, questions, wrong.toList(), total, died = false, monster, practice) { reward = it }
        } else {
            index++
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
            }
            else -> {
                bonus += type.monsterDamage(total)
                bonusHits++
            }
        }
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
            vm.finishBattle(bookId, questions.take(index + 1), wrong.toList(), total, died = true, monster, practice) { reward = it }
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

    Scaffold(
        topBar = { BattleHeader("戰鬥：$bookName", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            if (died) {
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
                    attacks = wrong.size,
                    monsterHp = monsterHp,
                    monsterMax = total,
                    heroHp = heroHp,
                    heroMax = HERO_MAX_HP,
                    knockout = knocked,
                    onKnockoutEnd = { endShown = true },
                    intro = !introDone,
                    onIntroEnd = { introDone = true },
                    itemUse = itemUse,
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (total > 0 && !finished && isBoss) {
                    Text(
                        "${if (monster.rank == MonsterRank.Big) "大魔王" else "中魔王"}「${monster.name}」出現了！",
                        style = MaterialTheme.typography.titleMedium,
                        color = PixelRed,
                    )
                }
                when {
                    total == 0 -> {
                        Text("這本單字簿還沒有可以出題的單字（至少 3 個字母、已記住）")
                        Button(onClick = onBack) { Text("回單字本") }
                    }
                    died -> {
                        Text("${profile?.name ?: ""} 被打倒了…", style = MaterialTheme.typography.headlineSmall)
                        Text("同伴把你拖回了教堂")
                        reward?.let { Text("這是你第 ${it.deaths} 次倒下", color = PixelRed) }
                    }
                    knocked -> {
                        Text("${race.finisher.label(tier)}！", style = MaterialTheme.typography.headlineSmall)
                        Text("擊倒了 ${monster.name}！")
                    }
                    finished -> {
                        Text("${monster.name} 還站著！", style = MaterialTheme.typography.headlineSmall)
                        Text("把怪物的血打光才能把牠打飛，下次再來")
                    }
                    !introDone -> Text("小心！", style = MaterialTheme.typography.titleMedium)
                    word != null -> Question(
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

    if (showBag) {
        ItemBagDialog(
            counts = items,
            onDismiss = { showBag = false },
            canUse = { type -> answering && (type.heal == 0 || damage > 0) },
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

// 戰鬥畫面上方的標題：深色底加上像素風的雙線金框，和下方的戰鬥場景連在一起
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BattleHeader(title: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(HeaderDark)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val px = 2.dp.toPx()
            // 深色底上的小點點花紋
            var y = px * 3
            while (y < size.height) {
                var x = px * 3 + if (((y / (px * 6)).toInt()) % 2 == 0) 0f else px * 3
                while (x < size.width) {
                    drawRect(HeaderDot, Offset(x, y), Size(px, px))
                    x += px * 6
                }
                y += px * 6
            }
            // 雙線外框：外面一圈金色、隔一格、裡面一圈淡金色
            drawRect(PixelGold, Offset(px, px), Size(size.width - px * 2, size.height - px * 2), style = Stroke(px))
            drawRect(HeaderGoldLight, Offset(px * 4, px * 4), Size(size.width - px * 8, size.height - px * 8), style = Stroke(px))
        }
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "逃跑", tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
            )
        }
    }
}

private val HeaderDark = Color(0xFF302848)
private val HeaderDot = Color(0xFF483868)
private val HeaderGoldLight = Color(0xFFF8F0A0)

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
    // 空白的輸入框，聽發音拼出整個單字；答錯時框裡直接換成正確答案（紅字），不會被鍵盤擋住
    val wrong = result == false
    OutlinedTextField(
        value = if (wrong) answer else input,
        onValueChange = onInput,
        readOnly = result != null,
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(
            textAlign = TextAlign.Center,
            color = if (wrong) PixelRed else MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (wrong) onNext() else onSubmit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    // 左下：大聲公再聽一次；中間：道具袋；右下：劍攻擊，答錯後換成向右箭頭「下一回」
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PixelIconButton(IconMegaphone, "再聽一次", onClick = onListen, size = 64.dp, palette = ColorIconPalette)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PixelIconButton(IconBag, "道具", onClick = onBag, size = 64.dp, palette = ItemPalette, enabled = bagEnabled)
        }
        if (wrong) {
            PixelIconButton(IconArrowRight, "下一回", onClick = onNext, size = 64.dp, color = PixelGold, palette = ColorIconPalette)
        } else {
            PixelIconButton(
                IconSword,
                "攻擊",
                onClick = onSubmit,
                size = 64.dp,
                color = PixelGold,
                palette = ColorIconPalette,
                enabled = result == null && input.isNotBlank(),
            )
        }
    }
    when (result) {
        true -> Text("命中！", style = MaterialTheme.typography.headlineSmall, color = PixelRed)
        false -> Text("被怪物攻擊了！", style = MaterialTheme.typography.titleMedium, color = PixelRed)
        null -> Unit
    }
    if (wrong && meaning.isNotBlank()) Text(meaning, style = MaterialTheme.typography.titleMedium)
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
    monsterHp: Int,
    monsterMax: Int,
    heroHp: Int,
    heroMax: Int,
    knockout: Boolean,
    onKnockoutEnd: () -> Unit,
    intro: Boolean,
    onIntroEnd: () -> Unit,
    itemUse: ItemUse?,
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
                else -> 1100
            }
            itemAnim.animateTo(1f, tween(duration, easing = LinearEasing))
            itemAnim.snapTo(0f)
        }

        val p = ko.value
        val effect = (p / 0.5f).coerceIn(0f, 1f)
        val fly = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val move = flight(finisher, fly)
        // 巨斧落下時怪物被壓扁
        val squash = if (finisher == Finisher.Smash && effect in 0.6f..1f && fly == 0f) 0.45f else 1f

        Box(Modifier.fillMaxWidth().graphicsLayer { translationX = landShake.dp.toPx() }) {
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
                                translationX = heroShake.value.dp.toPx() + heroLunge.value.dp.toPx()
                                // 被打中時一閃一閃
                                alpha = if (heroFlash.value > 0f && (heroFlash.value * 10).toInt() % 2 == 0) 0.25f else 1f
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
                                    rotationZ = move.rotation
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
                    }
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
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                when {
                    reward.died -> "被打倒了…"
                    perfect -> "完美勝利！"
                    reward.passed -> "勝利！"
                    else -> "還要再練練"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    reward.died -> Text("被 $monsterName 打倒了，在教堂休息一下再來吧（第 ${reward.deaths} 次倒下）")
                    knocked -> Text("把 $monsterName 打飛了！")
                    else -> Text("$monsterName 還站著，把牠的血打光才能打飛")
                }
                Text("答對 ${reward.correct} / ${reward.total}（${passScore(reward.total)} 題以上通過）")
                Text("得到經驗值 +${reward.exp}${if (monster.rank != MonsterRank.Normal) "（魔王加倍）" else ""}")
                if (reward.practice) Text("練習戰：經驗值只有 1/4，不會掉寶箱", style = MaterialTheme.typography.bodySmall)
                reward.item?.let { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelArt(item.icon, ItemPalette, Modifier.size(32.dp))
                        Text("打開寶箱，獲得「${item.label}」！", color = PixelRed, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (leveled) Text("升級！Lv ${reward.levelBefore} → Lv ${reward.levelAfter}", color = PixelRed)
                if (newTitle != oldTitle) Text("獲得新稱號「$newTitle」！", color = PixelRed)
                if (reward.passed && monster.rank != MonsterRank.Normal) {
                    Text("★ 打倒了${if (monster.rank == MonsterRank.Big) "大魔王" else "中魔王"}「$monsterName」！", color = PixelRed)
                } else if (monster.rank != MonsterRank.Normal) {
                    Text("要通過這場戰鬥才算打倒魔王，下次牠還會出現", style = MaterialTheme.typography.bodySmall)
                }
                if (reward.passed) Text("★ 這本單字簿戰鬥通過")
                if (reward.forgotten.isNotEmpty()) {
                    Text(
                        "這些字已經答錯兩次，改回「沒記住」，記得去複習：${reward.forgotten.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onClose) { Text("回單字本") } },
    )
}

// 升級畫面：整個畫面換成像素風景，人物跳一跳、LEVEL UP 閃爍，能力一項一項顯示提升了多少
// 稱號升階時，頭像換成新的樣子、學會新招式
@Composable
private fun LevelUpScreen(hero: Avatar, race: Race, before: Int, after: Int, onClose: () -> Unit) {
    val old = race.stats(before)
    val newStats = race.stats(after)
    val rows = listOf(
        Triple("力量", old.str, newStats.str),
        Triple("體力", old.vit, newStats.vit),
        Triple("智慧", old.intel, newStats.intel),
        Triple("運氣", old.luk, newStats.luk),
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
            Surface(
                shape = PixelShape,
                border = PixelBorder,
                color = MaterialTheme.colorScheme.surface,
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
                        Text("獲得新稱號「$newTitle」！", color = PixelRed)
                    }
                    if (shown > rows.size && newTier != oldTier) {
                        Text("外觀進化了！學會新招式「${race.finisher.label(newTier)}」！", color = PixelRed, textAlign = TextAlign.Center)
                    }
                    Button(onClick = onClose, enabled = shown > rows.size) { Text("太好了！") }
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

// 黑暗森林：怪物們嚇得往右邊逃跑
private fun DrawScope.drawFleeingMonsters(t: Float, cell: Float, monsters: List<Monster>) {
    val ground = size.height - cell
    monsters.forEachIndexed { i, monster ->
        val small = cell * 0.5f
        val w = monster.art.first().length * small
        val h = monster.art.size * small
        val span = size.width + w * 2
        val x = ((t + i * 0.2f) % 1f) * span - w
        val hop = if (((t * 24).toInt() + i) % 2 == 0) 0f else cell
        monster.art.forEachIndexed { y, row ->
            row.forEachIndexed { cx, ch ->
                val color = ScenePalette[ch] ?: return@forEachIndexed
                drawRect(color, Offset(x + cx * small, ground - h - hop + y * small), Size(small + 0.5f, small + 0.5f))
            }
        }
        // 逃跑時冒出的汗滴
        drawRect(Color(0xFF90D0F8), Offset(x - small * 2, ground - h - hop), Size(small * 2, small * 3))
    }
}

// 升級畫面天空上星星的位置（TitleSky 的格子）
private val LevelUpStars = listOf(
    3 to 2, 12 to 6, 20 to 1, 26 to 4, 40 to 2, 47 to 7, 58 to 1, 61 to 6, 8 to 9, 33 to 8, 52 to 10, 17 to 11,
)
