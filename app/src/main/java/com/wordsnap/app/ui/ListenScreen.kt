package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelGold
import kotlinx.coroutines.delay

// 朗讀模式：隨機順序一個字接一個字自動念，先念英文，再念中文解釋，念完從頭再來
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenScreen(vm: WordViewModel, onBack: () -> Unit) {
    val bookWords by vm.words.collectAsState()
    // 隨機順序念；單字有增減（例如刪掉一個）時才重新打亂
    val words = remember(bookWords.map { it.id }.toSet()) { bookWords.shuffled() }
    val speaker = rememberSpeaker()
    val scene = remember { ReviewScene.entries.random() }
    var index by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    // 每念完一個字加一，只有一個字時也會重新開始念
    var round by remember { mutableIntStateOf(0) }

    val word = words.getOrNull(index % words.size.coerceAtLeast(1))

    // 換字或暫停時重新開始念；離開畫面時自動停止
    LaunchedEffect(index, round, playing, words.size) {
        if (!playing || word == null) {
            speaker.stop()
            return@LaunchedEffect
        }
        val text = word.text.trim()
        speaker.say(text, rate = 0.85f)
        delay(400)
        if (word.meaning.isNotBlank()) {
            speaker.say(word.meaning, meaning = true)
        }
        delay(1200)
        index = (index + 1) % words.size
        round++
    }

    Scaffold(
        topBar = { PixelHeader(tr(R.string.listen_title), onBack, theme = HeaderTheme.Tapestry) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PixelArt(scene.art, ScenePalette, Modifier.fillMaxWidth())
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                // 比正中間再偏上一點
                verticalArrangement = Arrangement.spacedBy(20.dp, BiasAlignment.Vertical(-0.45f)),
            ) {
                if (word == null) {
                    Text(tr(R.string.listen_empty))
                    RetroButton(tr(R.string.back_to_book), onClick = onBack)
                    return@Column
                }
                Text(tr(R.string.listen_progress, index % words.size + 1, words.size), style = MaterialTheme.typography.bodyMedium)
                PixelSurface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(vertical = 24.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(word.text, style = wordStyle(word.text), textAlign = TextAlign.Center)
                        Text(
                            word.meaning.ifBlank { tr(R.string.no_meaning) },
                            style = MaterialTheme.typography.headlineSmall.readable(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (!playing) Text(tr(R.string.listen_paused), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PixelIconButton(IconPrev, tr(R.string.listen_prev), onClick = { index = (index - 1 + words.size) % words.size })
                    PixelIconButton(
                        if (playing) IconPause else IconPlay,
                        tr(if (playing) R.string.listen_pause else R.string.listen_resume),
                        onClick = { playing = !playing },
                        color = PixelGold,
                    )
                    PixelIconButton(IconNext, tr(R.string.listen_next), onClick = { index = (index + 1) % words.size })
                }
                Text(
                    tr(R.string.listen_hint),
                    style = MaterialTheme.typography.bodySmall.readable(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
