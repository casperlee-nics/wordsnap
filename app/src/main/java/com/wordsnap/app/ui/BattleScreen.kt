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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.Word
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed
import com.wordsnap.app.ui.theme.pixelTopBarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 怪物每一格像素的大小
private val Cell = 4.dp

// 戰鬥模式：聽英文發音，看第一個和最後一個字母，填出中間的字母。
// 答對怪物扣血，答錯被怪物攻擊；全部答對才用種族招式把怪物打飛，錯超過一半就被打倒
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(vm: WordViewModel, bookId: Long, onBack: () -> Unit) {
    val profile by vm.currentProfile.collectAsState()
    val books by vm.books.collectAsState()
    val avatar = Avatar.from(profile?.avatar)
    val race = avatar.race
    val bookName = books.firstOrNull { it.id == bookId }?.name ?: ""
    val questions = remember { vm.pickBattleWords(bookId) }
    val monster = remember { BattleMonsters.random() }
    val scene = remember { ReviewScene.entries.random() }
    val speaker = rememberSpeaker()
    val context = LocalContext.current
    val musicOn = remember { isMusicOn(context) }
    // 念單字時音樂調到很小聲，避免蓋過發音
    LoopingMusic(R.raw.battle_bgm, enabled = musicOn, volume = if (speaker.speaking) 0.05f else 0.3f)

    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    val wrong = remember { mutableStateListOf<Word>() }
    var input by remember { mutableStateOf("") }
    // null：作答中；true：答對；false：答錯
    var result by remember { mutableStateOf<Boolean?>(null) }
    var finished by remember { mutableStateOf(false) }
    var died by remember { mutableStateOf(false) }
    // 結束動畫（打飛怪物或拖棺材）播完了，可以顯示結果
    var endShown by remember { mutableStateOf(false) }
    var reward by remember { mutableStateOf<BattleReward?>(null) }

    val total = questions.size
    val word = questions.getOrNull(index)
    val answer = word?.text?.trim().orEmpty()
    val heroMax = heroMaxHp(total)
    // 錯超過一半就被打倒
    val dying = wrong.size * 2 > total
    val perfect = finished && !died && wrong.isEmpty()

    fun submit() {
        if (result != null || input.isBlank() || answer.length < 3 || word == null) return
        val typed = input.trim()
        val ok = typed.equals(answer.substring(1, answer.length - 1), ignoreCase = true) ||
            typed.equals(answer, ignoreCase = true)
        result = ok
        if (ok) correct++ else wrong.add(word)
    }

    fun next() {
        input = ""
        result = null
        if (index + 1 >= total) {
            finished = true
            vm.finishBattle(bookId, questions, wrong.toList(), total, died = false) { reward = it }
        } else {
            index++
        }
    }

    // 每一題先念一次英文發音
    LaunchedEffect(index) {
        if (word != null && !finished) {
            delay(400)
            speaker.say(answer, rate = 0.85f)
        }
    }
    // 答對後稍等一下自動下一題；被打倒時等怪物攻擊完再倒下
    LaunchedEffect(result) {
        if (result == true) {
            delay(900)
            next()
        } else if (result == false && dying) {
            delay(1800)
            died = true
            finished = true
            vm.finishBattle(bookId, questions.take(index + 1), wrong.toList(), total, died = true) { reward = it }
        }
    }
    // 沒有全對：怪物留在原地，稍等一下就顯示結果
    LaunchedEffect(finished) {
        if (finished && !died && wrong.isNotEmpty()) {
            delay(900)
            endShown = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("戰鬥：$bookName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "逃跑")
                    }
                },
                colors = pixelTopBarColors(),
            )
        },
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
                    finisher = race.finisher,
                    hits = correct,
                    attacks = wrong.size,
                    knockout = perfect,
                    onKnockoutEnd = { endShown = true },
                )
            }
            HpBar(monster.name, hp = total - correct, max = total)
            HpBar(profile?.name ?: "", hp = (heroMax - wrong.size).coerceAtLeast(0), max = heroMax)

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    total == 0 -> {
                        Text("這本單字簿還沒有可以出題的單字（至少 3 個字母、已記住過）")
                        Button(onClick = onBack) { Text("回單字本") }
                    }
                    died -> {
                        Text("${profile?.name ?: ""} 被打倒了…", style = MaterialTheme.typography.headlineSmall)
                        Text("同伴把你拖回了教堂")
                    }
                    perfect -> {
                        Text("${race.finisher.label}！", style = MaterialTheme.typography.headlineSmall)
                        Text("擊倒了 ${monster.name}！")
                    }
                    finished -> {
                        Text("${monster.name} 還站著！", style = MaterialTheme.typography.headlineSmall)
                        Text("全部答對才能把牠打飛，下次再來")
                    }
                    word != null -> Question(
                        number = index + 1,
                        total = total,
                        correct = correct,
                        answer = answer,
                        meaning = word.meaning,
                        input = input,
                        onInput = { if (result == null) input = it.filterNot { c -> c == '\n' } },
                        result = result,
                        dying = dying,
                        onListen = { speaker.speak(answer) },
                        onSubmit = ::submit,
                        onNext = ::next,
                    )
                }
            }
        }
    }

    val done = reward
    if (endShown && done != null) {
        ResultDialog(done, monster.name, race, onClose = onBack)
    }
}

@Composable
private fun Question(
    number: Int,
    total: Int,
    correct: Int,
    answer: String,
    meaning: String,
    input: String,
    onInput: (String) -> Unit,
    result: Boolean?,
    dying: Boolean,
    onListen: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("第 $number / $total 題　命中 $correct", Modifier.weight(1f))
        OutlinedButton(onClick = onListen) { Text("♪ 再聽一次") }
    }
    // 中間輸入的字和左右兩邊的字母一樣大
    val big = MaterialTheme.typography.displaySmall
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(answer.first().toString(), style = big)
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            singleLine = true,
            textStyle = big.copy(textAlign = TextAlign.Center),
            placeholder = {
                Text(
                    "_".repeat((answer.length - 2).coerceAtMost(8)),
                    style = big,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.weight(1f),
        )
        Text(answer.last().toString(), style = big)
    }
    Text("共 ${answer.length} 個字母，填中間的 ${answer.length - 2} 個", style = MaterialTheme.typography.bodySmall)
    when (result) {
        null -> Button(onClick = onSubmit, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("攻擊！")
        }
        true -> Text("命中！", style = MaterialTheme.typography.headlineSmall, color = PixelRed)
        false -> {
            Text("沒打中，被怪物攻擊了！答案是 $answer", style = MaterialTheme.typography.titleMedium, color = PixelRed)
            if (meaning.isNotBlank()) Text(meaning)
            if (dying) {
                Text("受到致命一擊…", style = MaterialTheme.typography.titleMedium)
            } else {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("下一題 →") }
            }
        }
    }
}

@Composable
private fun HpBar(name: String, hp: Int, max: Int) {
    val fraction by animateFloatAsState(if (max == 0) 0f else hp.toFloat() / max, tween(500), label = "hp")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(name)
        Box(
            Modifier.weight(1f).height(12.dp).border(2.dp, PixelInk).padding(2.dp),
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fraction)
                    .background(if (fraction > 0.5f) Color(0xFF40A048) else if (fraction > 0.2f) PixelGold else PixelRed),
            )
        }
        Text("HP $hp/$max")
    }
}

// 戰鬥場景：背景、怪物（會呼吸、被打會晃、答錯會衝過來攻擊）、自己的人物、擊倒特效
@Composable
private fun BattleField(
    scene: ReviewScene,
    monster: Monster,
    hero: Avatar,
    finisher: Finisher,
    hits: Int,
    attacks: Int,
    knockout: Boolean,
    onKnockoutEnd: () -> Unit,
) {
    val width = Cell * monster.art.first().length
    val height = Cell * monster.art.size
    val endCallback by rememberUpdatedState(onKnockoutEnd)

    // 待機的小動作：上下彈跳、身體一伸一縮
    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(
        0f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse), label = "bob",
    )
    // 被打中：左右晃、變透明一下
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(hits) {
        if (hits == 0) return@LaunchedEffect
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
                -110f at 260
                -110f at 320
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

    Box(Modifier.fillMaxWidth().clipToBounds()) {
        PixelArt(scene.art, ScenePalette, Modifier.fillMaxWidth())
        PixelAvatar(
            hero,
            size = 36.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 6.dp)
                .graphicsLayer {
                    translationX = heroShake.value.dp.toPx()
                    // 被打中時一閃一閃
                    alpha = if (heroFlash.value > 0f && (heroFlash.value * 10).toInt() % 2 == 0) 0.25f else 1f
                },
        )
        // 打在人物身上的爪痕
        if (heroFlash.value > 0.4f) {
            Canvas(Modifier.matchParentSize()) {
                val px = 3.dp.toPx()
                val cx = (12 + 18).dp.toPx()
                val cy = size.height - (6 + 18).dp.toPx()
                for (i in -1..1) {
                    val off = i * 8.dp.toPx()
                    drawLine(Color.White, Offset(cx - 14.dp.toPx() + off, cy - 14.dp.toPx()), Offset(cx + 10.dp.toPx() + off, cy + 14.dp.toPx()), px)
                }
            }
        }

        val p = ko.value
        val effect = (p / 0.5f).coerceIn(0f, 1f)
        val fly = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val move = flight(finisher, fly)
        // 巨斧落下時怪物被壓扁
        val squash = if (finisher == Finisher.Smash && effect in 0.6f..1f && fly == 0f) 0.45f else 1f
        PixelArt(
            monster.art,
            ScenePalette,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(width, height)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationX = shake.value.dp.toPx() + lunge.value.dp.toPx() + move.dx.dp.toPx()
                    translationY = -bob * 4.dp.toPx() + move.dy.dp.toPx()
                    rotationZ = move.rotation
                    scaleX = move.scale * (1f + 0.04f * bob)
                    scaleY = move.scale * (1f - 0.04f * bob) * squash
                    alpha = move.alpha * (1f - 0.7f * flash.value)
                },
        )
        if (knockout && fly < 0.3f) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width / 2, size.height - 8.dp.toPx() - height.toPx() / 2)
                val radius = maxOf(width, height).toPx() / 2 + 6.dp.toPx()
                drawFinisher(finisher, effect, center, radius)
            }
        }
    }
}

private class Flight(val dx: Float, val dy: Float, val rotation: Float, val scale: Float = 1f, val alpha: Float = 1f)

// 怪物飛走的方式（位移單位是 dp），t 從 0 到 1
private fun flight(finisher: Finisher, t: Float): Flight = when (finisher) {
    Finisher.Slash -> Flight(t * 320f, -t * 220f, t * 720f)
    Finisher.Magic -> Flight(0f, -t * 40f, t * 540f, scale = 1f - t, alpha = 1f - t)
    Finisher.Bomb -> Flight(t * 40f, -t * 360f, t * 200f)
    Finisher.Smash -> Flight(t * 340f, -sin(t * PI.toFloat()) * 90f, t * 540f)
    Finisher.Claw -> Flight(-t * 340f, -t * 180f, -t * 540f)
    Finisher.Fire -> Flight(t * 260f, -t * 300f, t * 360f, alpha = 1f - 0.6f * t)
    Finisher.Laser -> Flight(t * 400f, -t * 60f, t * 1080f)
}

// 各種族的擊倒招式特效，e 從 0 到 1
private fun DrawScope.drawFinisher(finisher: Finisher, e: Float, center: Offset, radius: Float) {
    val px = 4.dp.toPx()
    when (finisher) {
        // 人類：兩道交叉的劍光
        Finisher.Slash -> {
            val a = (e * 2).coerceIn(0f, 1f)
            val b = (e * 2 - 1).coerceIn(0f, 1f)
            val r = radius * 1.2f
            drawLine(Color.White, center + Offset(-r, -r), center + Offset(-r + 2 * r * a, -r + 2 * r * a), px * 1.5f)
            if (b > 0f) drawLine(Color.White, center + Offset(r, -r), center + Offset(r - 2 * r * b, -r + 2 * r * b), px * 1.5f)
        }
        // 精靈：彩色星光從四周轉著聚過來
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
        // 哥布林：丟出炸彈，碰到就爆炸
        Finisher.Bomb -> {
            if (e < 0.5f) {
                val t = e / 0.5f
                val start = Offset(24.dp.toPx(), size.height - 30.dp.toPx())
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
        // 龍族：從左下角噴出火焰
        Finisher.Fire -> {
            val start = Offset(30.dp.toPx(), size.height - 24.dp.toPx())
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
            val endX = center.x * t + radius * t
            drawRect(Color(0xFF90D0F8), Offset(0f, center.y - thick), Size(endX, thick * 2))
            drawRect(Color.White, Offset(0f, center.y - thick / 3), Size(endX, thick * 2 / 3))
        }
    }
}

@Composable
private fun ResultDialog(reward: BattleReward, monsterName: String, race: Race, onClose: () -> Unit) {
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
                    reward.died -> Text("被 $monsterName 打倒了，在教堂休息一下再來吧")
                    perfect -> Text("把 $monsterName 打飛了！")
                    else -> Text("$monsterName 還站著，全部答對才能把牠打飛")
                }
                Text("答對 ${reward.correct} / ${reward.total}（${passScore(reward.total)} 題以上通過）")
                Text("得到經驗值 +${reward.exp}")
                if (leveled) Text("升級！Lv ${reward.levelBefore} → Lv ${reward.levelAfter}", color = PixelRed)
                if (newTitle != oldTitle) Text("獲得新稱號「$newTitle」！", color = PixelRed)
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
