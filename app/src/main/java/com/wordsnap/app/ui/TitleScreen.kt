package com.wordsnap.app.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelShape

private const val PREFS = "wordsnap"
private const val KEY_MUSIC = "music_on"

// 主畫面：上方是像素世界，中間是選單，下方是草地
@Composable
fun TitleScreen(
    hasSaves: Boolean,
    onNewGame: () -> Unit,
    onLoadGame: () -> Unit,
    onDeleteSave: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var musicOn by remember { mutableStateOf(prefs.getBoolean(KEY_MUSIC, true)) }
    TitleMusic(enabled = musicOn)

    Box(Modifier.fillMaxSize().background(SceneGrass)) {
        Column(Modifier.fillMaxSize()) {
            PixelArt(TitleSky, ScenePalette, Modifier.fillMaxWidth())
            Box(Modifier.weight(1f))
            PixelArt(TitleGround, ScenePalette, Modifier.fillMaxWidth())
        }

        // 選單放在草地中間，不會擋到上方的城堡
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Surface(
                shape = PixelShape,
                border = PixelBorder,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(bottom = 64.dp).width(240.dp),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("像素單字", style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) { Text("新遊戲") }
                    OutlinedButton(onClick = onLoadGame, enabled = hasSaves, modifier = Modifier.fillMaxWidth()) {
                        Text("讀取遊戲")
                    }
                    OutlinedButton(onClick = onDeleteSave, enabled = hasSaves, modifier = Modifier.fillMaxWidth()) {
                        Text("刪除進度")
                    }
                    TextButton(onClick = {
                        musicOn = !musicOn
                        prefs.edit().putBoolean(KEY_MUSIC, musicOn).apply()
                    }) { Text(if (musicOn) "♪ 音樂：開" else "♪ 音樂：關") }
                }
            }
        }
    }
}

// 主畫面的 8-bit 背景音樂，約 30 秒循環；App 切到背景時暫停
@Composable
private fun TitleMusic(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) return@DisposableEffect onDispose { }
        val player = MediaPlayer.create(context, R.raw.title_bgm)?.apply {
            isLooping = true
            setVolume(0.6f, 0.6f)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player?.start()
                Lifecycle.Event.ON_PAUSE -> player?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.release()
        }
    }
}
