package com.wordsnap.app.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

// 用手機內建的文字轉語音念英文單字，也可以念中文解釋
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val ready = CompletableDeferred<Boolean>()
    @Volatile private var isReady = false
    private val main = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val nextId = AtomicInteger()

    // 正在說話時為 true，戰鬥音樂會跟著調小聲
    var speaking by mutableStateOf(false)
        private set

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                main.post { speaking = true }
            }

            override fun onDone(utteranceId: String) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId)

            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String) {
        pending.remove(id)?.complete(Unit)
        main.post { speaking = false }
    }

    // 手機上品質最好的英文、中文聲音（找不到就用預設的）
    private var englishVoice: Voice? = null
    private var chineseVoice: Voice? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            val voices = runCatching { tts.voices }.getOrNull().orEmpty()
                .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            englishVoice = bestVoice(voices.filter { it.locale.language == "en" && it.locale.country == "US" })
            chineseVoice = bestVoice(voices.filter { it.locale.language == "zh" && it.locale.country == "TW" })
                ?: bestVoice(voices.filter { it.locale.language == "zh" })
            useEnglish()
        }
        isReady = status == TextToSpeech.SUCCESS
        ready.complete(status == TextToSpeech.SUCCESS)
    }

    // 不需要網路的聲音裡品質最高的（沒網路也念得出來）；沒有才用需要網路的
    private fun bestVoice(voices: List<Voice>): Voice? =
        voices.sortedWith(compareBy<Voice> { it.isNetworkConnectionRequired }.thenByDescending { it.quality }).firstOrNull()

    private fun useEnglish() {
        val voice = englishVoice
        if (voice == null || tts.setVoice(voice) != TextToSpeech.SUCCESS) tts.setLanguage(Locale.US)
    }

    private fun useChinese() {
        val voice = chineseVoice
        if (voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.TRADITIONAL_CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.CHINESE)
        }
    }

    // 直接念一個英文單字（會打斷正在念的）
    fun speak(text: String) {
        if (!isReady) return
        useEnglish()
        tts.setSpeechRate(1f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak-${nextId.incrementAndGet()}")
    }

    // 念完才回來，用在朗讀模式一句接一句
    suspend fun say(text: String, chinese: Boolean = false, rate: Float = 1f) {
        if (!ready.await() || text.isBlank()) return
        if (chinese) useChinese() else useEnglish()
        tts.setSpeechRate(rate)
        val id = "say-${nextId.incrementAndGet()}"
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        if (tts.speak(text, TextToSpeech.QUEUE_ADD, null, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return
        }
        try {
            done.await()
        } catch (e: CancellationException) {
            tts.stop()
            throw e
        }
    }

    fun stop() = tts.stop()

    fun shutdown() = tts.shutdown()
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
