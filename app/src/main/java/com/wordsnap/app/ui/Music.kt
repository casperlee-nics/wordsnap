package com.wordsnap.app.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val PREFS = "wordsnap"
private const val KEY_MUSIC = "music_on"

// 背景音樂開關存在手機裡，主畫面可以切換
fun isMusicOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MUSIC, true)

fun setMusicOn(context: Context, on: Boolean) =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_MUSIC, on).apply()

// 循環播放一段 8-bit 音樂；App 切到背景時暫停，離開畫面時停止
@Composable
fun LoopingMusic(@RawRes resId: Int, enabled: Boolean, volume: Float) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(resId, enabled, lifecycleOwner) {
        if (!enabled) return@DisposableEffect onDispose { }
        val created = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(volume, volume)
        }
        player = created
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> created?.start()
                Lifecycle.Event.ON_PAUSE -> created?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            created?.release()
            player = null
        }
    }
    // 音量改變時（例如念單字時調小）直接套用
    LaunchedEffect(player, volume) {
        player?.setVolume(volume, volume)
    }
}
