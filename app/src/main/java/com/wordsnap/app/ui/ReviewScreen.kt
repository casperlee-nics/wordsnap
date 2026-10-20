package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordsnap.app.speech.rememberSpeaker

// 翻卡複習：先看英文，按「看答案」翻面，再選記得或忘了
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: WordViewModel, onBack: () -> Unit) {
    val speaker = rememberSpeaker()
    LaunchedEffect(Unit) { vm.startReview() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("複習（剩 ${vm.reviewQueue.size} 張）") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            val word = vm.reviewQueue.firstOrNull()
            if (word == null) {
                Text("今天的單字都複習完了！", style = MaterialTheme.typography.headlineSmall)
                if (vm.reviewedCount > 0) Text("這次記住了 ${vm.reviewedCount} 個字")
                Button(onClick = onBack) { Text("回單字本") }
            } else {
                var revealed by remember(vm.reviewStep) { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.text, style = MaterialTheme.typography.displayMedium)
                    IconButton(onClick = { speaker.speak(word.text) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "發音")
                    }
                }

                if (!revealed) {
                    Button(onClick = { revealed = true }, Modifier.fillMaxWidth()) { Text("看答案") }
                } else {
                    Text(
                        word.meaning.ifBlank { "（還沒有中文意思）" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { vm.answer(word, remembered = false) }, Modifier.weight(1f)) {
                            Text("忘了")
                        }
                        Button(onClick = { vm.answer(word, remembered = true) }, Modifier.weight(1f)) {
                            Text("記得")
                        }
                    }
                }
            }
        }
    }
}
