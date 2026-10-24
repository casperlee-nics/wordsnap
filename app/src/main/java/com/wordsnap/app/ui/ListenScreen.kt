package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelGold
import kotlinx.coroutines.delay

// 朗讀模式：一個字接一個字自動念，先念英文，再念中文解釋，念完從頭再來
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenScreen(vm: WordViewModel, onBack: () -> Unit) {
    val words by vm.words.collectAsState()
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
            speaker.say(word.meaning, chinese = true)
        }
        delay(1200)
        index = (index + 1) % words.size
        round++
    }

    Scaffold(
        topBar = { PixelHeader("朗讀模式", onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PixelArt(scene.art, ScenePalette, Modifier.fillMaxWidth())
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                if (word == null) {
                    Text("這本單字簿還沒有單字")
                    Button(onClick = onBack) { Text("回單字本") }
                    return@Column
                }
                Text("第 ${index % words.size + 1} / ${words.size} 個", style = MaterialTheme.typography.bodyMedium)
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
                            word.meaning.ifBlank { "（還沒有中文意思）" },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (!playing) Text("已暫停", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PixelIconButton(IconPrev, "上一個", onClick = { index = (index - 1 + words.size) % words.size })
                    PixelIconButton(
                        if (playing) IconPause else IconPlay,
                        if (playing) "暫停" else "繼續",
                        onClick = { playing = !playing },
                        color = PixelGold,
                    )
                    PixelIconButton(IconNext, "下一個", onClick = { index = (index + 1) % words.size })
                }
                Text(
                    "關掉螢幕也會繼續念；若太久沒操作，手機可能會自動暫停",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
