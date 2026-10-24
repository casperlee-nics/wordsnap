package com.wordsnap.app.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wordsnap.app.R

private const val PREFS = "wordsnap"
private const val KEY_MUSIC = "music_on"
private const val KEY_LEVEL = "music_level"

// 背景音樂音量：關、小、大（只調整音樂和音效，不影響念單字的聲音）
enum class MusicLevel(@StringRes val labelRes: Int, val scale: Float) {
    Off(R.string.settings_volume_off, 0f),
    Low(R.string.settings_volume_low, 0.4f),
    High(R.string.settings_volume_high, 1f),
}

// 目前的音量設定，主畫面一切換，正在播的音樂馬上跟著變
private var currentLevel by mutableStateOf<MusicLevel?>(null)

fun musicLevel(context: Context): MusicLevel {
    currentLevel?.let { return it }
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_LEVEL, null)?.let { name -> MusicLevel.entries.find { it.name == name } }
    // 還沒調過音量就用小聲；舊版只有開/關：關 → 關
    val level = saved ?: if (prefs.getBoolean(KEY_MUSIC, true)) MusicLevel.Low else MusicLevel.Off
    currentLevel = level
    return level
}

fun setMusicLevel(context: Context, level: MusicLevel) {
    currentLevel = level
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LEVEL, level.name).apply()
}

fun isMusicOn(context: Context): Boolean = musicLevel(context) != MusicLevel.Off

// 循環播放一段 8-bit 音樂；App 切到背景時暫停，離開畫面時停止
// paused 為 true 時暫時停住（例如戰鬥念單字時），變回 false 從停住的地方接著播
@Composable
fun LoopingMusic(@RawRes resId: Int, enabled: Boolean, volume: Float, paused: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val level = musicLevel(context)
    val on = enabled && level != MusicLevel.Off
    val scaled = volume * level.scale
    val pausedNow by rememberUpdatedState(paused)
    DisposableEffect(resId, on, lifecycleOwner) {
        if (!on) return@DisposableEffect onDispose { }
        val created = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(scaled, scaled)
        }
        player = created
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!pausedNow) created?.start()
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
    LaunchedEffect(player, scaled) {
        player?.setVolume(scaled, scaled)
    }
    LaunchedEffect(player, paused) {
        val p = player ?: return@LaunchedEffect
        val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (paused) {
            if (p.isPlaying) p.pause()
        } else if (resumed && !p.isPlaying) {
            p.start()
        }
    }
}

// 播放一次的短音效（升級、被打倒），play 變成 true 時開始，離開畫面時停止
@Composable
fun OneShotSound(@RawRes resId: Int, play: Boolean, volume: Float = 0.6f) {
    val context = LocalContext.current
    DisposableEffect(resId, play) {
        val scaled = volume * musicLevel(context).scale
        if (!play || scaled <= 0f) return@DisposableEffect onDispose { }
        val player = MediaPlayer.create(context, resId)?.apply {
            setVolume(scaled, scaled)
            start()
        }
        onDispose { player?.release() }
    }
}
