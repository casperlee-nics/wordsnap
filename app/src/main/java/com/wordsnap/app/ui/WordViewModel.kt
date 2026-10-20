package com.wordsnap.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Word
import com.wordsnap.app.translate.ModelStatus
import com.wordsnap.app.translate.WordTranslator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DAY = 24 * 60 * 60 * 1000L

// 記得一次就升一格，每格下次複習的間隔：馬上、1、3、7、14、30 天
private val INTERVALS = longArrayOf(0, 1 * DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)

// 畫面和資料庫之間的橋樑
class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).wordDao()

    val words: StateFlow<List<Word>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val translatorStatus: StateFlow<ModelStatus> = WordTranslator.status

    init {
        fillMissingMeanings()
    }

    // 下載失敗後讓使用者按「重試」
    fun retryTranslation() = fillMissingMeanings()

    fun add(text: String, meaning: String) {
        viewModelScope.launch {
            dao.insert(Word(text = text.trim(), meaning = meaning.trim()))
            fillMissingMeaningsNow()
        }
    }

    // 把拍照選到的單字一次加入，已經在單字本裡的會略過
    fun addAll(texts: Collection<String>) {
        viewModelScope.launch {
            val existing = dao.getAllTexts().map { it.lowercase() }.toSet()
            val newWords = texts.filter { it.lowercase() !in existing }
                .map { Word(text = it, meaning = "") }
            dao.insertAll(newWords)
            fillMissingMeaningsNow()
        }
    }

    fun update(word: Word, text: String, meaning: String) {
        viewModelScope.launch {
            dao.update(word.copy(text = text.trim(), meaning = meaning.trim()))
            fillMissingMeaningsNow()
        }
    }

    fun delete(word: Word) {
        viewModelScope.launch { dao.delete(word) }
    }

    private fun fillMissingMeanings() {
        viewModelScope.launch { fillMissingMeaningsNow() }
    }

    // 沒有中文意思的單字自動翻譯
    private suspend fun fillMissingMeaningsNow() {
        for (word in dao.getWithoutMeaning()) {
            val meaning = WordTranslator.translate(word.text) ?: return
            dao.fillMeaning(word.id, meaning)
        }
    }

    // ---- 翻卡複習 ----

    var reviewQueue by mutableStateOf<List<Word>>(emptyList())
        private set
    var reviewedCount by mutableIntStateOf(0)
        private set
    // 每答一題加一，讓畫面知道要把卡片翻回正面
    var reviewStep by mutableIntStateOf(0)
        private set

    fun startReview() {
        reviewedCount = 0
        viewModelScope.launch {
            reviewQueue = dao.getDue(System.currentTimeMillis())
        }
    }

    fun answer(word: Word, remembered: Boolean) {
        val box = if (remembered) (word.box + 1).coerceAtMost(INTERVALS.size - 1) else 0
        val updated = word.copy(box = box, dueAt = System.currentTimeMillis() + INTERVALS[box])
        viewModelScope.launch { dao.update(updated) }

        // 忘了的字放到最後，這次複習再考一次
        reviewQueue = reviewQueue.drop(1) + if (remembered) emptyList() else listOf(updated)
        if (remembered) reviewedCount++
        reviewStep++
    }
}
