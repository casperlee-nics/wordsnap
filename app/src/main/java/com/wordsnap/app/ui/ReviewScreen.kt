package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordsnap.app.speech.rememberSpeaker
import com.wordsnap.app.ui.theme.PixelGold

// 單字卡複習：卡片出現就念英文、同時顯示中文，按「不記得」或「記得」直接換下一張
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: WordViewModel, onBack: () -> Unit) {
    val speaker = rememberSpeaker()
    LaunchedEffect(Unit) { vm.startReview() }
    // 每次進來隨機一個場景，複習途中不換
    val scene = remember { ReviewScene.entries.random() }

    Scaffold(
        topBar = { PixelHeader("${if (vm.practiceMode) "練習" else "複習"}（剩 ${vm.reviewQueue.size} 張）", onBack, theme = HeaderTheme.Banner) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PixelArt(scene.art, ScenePalette, Modifier.fillMaxWidth())
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val word = vm.reviewQueue.firstOrNull()
                if (vm.reviewLoading) {
                    // 還在準備這次的字：先空著
                } else if (word == null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (vm.practiceMode) "整本都練習完了！" else "今天的單字都複習完了！",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (vm.reviewedCount > 0) Text("這次記住了 ${vm.reviewedCount} 個字")
                    RetroButton("回單字本", onClick = onBack)
                    Spacer(Modifier.weight(1f))
                } else {
                    // 卡片一出現就念英文
                    // 換到新的一張（或換了一個字）就念眼前這個字
                    LaunchedEffect(vm.reviewStep, word.id) { speaker.speak(word.text) }
                    // 卡片和按鈕放在偏上面，下面留空間
                    Spacer(Modifier.weight(0.3f))

                    // 像遊戲對話框的卡片：英文和中文意思一起顯示
                    PixelSurface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 長的單字自動縮小、必要時換行，不會超出外框
                                Text(
                                    word.text,
                                    style = wordStyle(word.text),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                IconButton(onClick = { speaker.speak(word.text) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "再念一次")
                                }
                            }
                            Text(
                                word.meaning.ifBlank { "（還沒有中文意思）" },
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // 按了就直接換下一張：左邊不記得（人物加問號），右邊記得（人物加燈泡）
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        PixelIconButton(
                            IconForgot,
                            "不記得",
                            onClick = { vm.answer(word, remembered = false) },
                            size = 88.dp,
                            palette = ColorIconPalette,
                        )
                        PixelIconButton(
                            IconRemember,
                            "記得",
                            onClick = { vm.answer(word, remembered = true) },
                            size = 88.dp,
                            color = PixelGold,
                            palette = ColorIconPalette,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // 隨機練習的提示放在最下面
                    if (vm.practiceMode) {
                        Text("今天的字都記住了，隨機再練一次", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// 依單字長度選字體大小：短的用大字，長的縮小，避免超出卡片
@Composable
fun wordStyle(text: String): TextStyle = when {
    text.length <= 8 -> MaterialTheme.typography.displaySmall
    text.length <= 12 -> MaterialTheme.typography.headlineLarge
    else -> MaterialTheme.typography.headlineSmall
}
