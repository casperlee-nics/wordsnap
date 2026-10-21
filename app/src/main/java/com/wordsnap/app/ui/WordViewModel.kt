package com.wordsnap.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Book
import com.wordsnap.app.data.Profile
import com.wordsnap.app.data.Word
import com.wordsnap.app.translate.ModelStatus
import com.wordsnap.app.translate.WordTranslator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DAY = 24 * 60 * 60 * 1000L

// 記得一次就升一格，每格下次複習的間隔：馬上、1、3、7、14、30 天
private val INTERVALS = longArrayOf(0, 1 * DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)

private const val PREFS = "wordsnap"
private const val KEY_PROFILE = "current_profile_id"

// 畫面和資料庫之間的橋樑
@OptIn(ExperimentalCoroutinesApi::class)
class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val dao = db.wordDao()
    private val profileDao = db.profileDao()
    private val bookDao = db.bookDao()
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- 暱稱 ----

    // null 代表還在讀取，空清單代表第一次開啟、還沒有暱稱
    val profiles: StateFlow<List<Profile>?> = profileDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val selectedProfileId = MutableStateFlow(prefs.getLong(KEY_PROFILE, -1L))

    val currentProfile: StateFlow<Profile?> = combine(profiles, selectedProfileId) { list, id ->
        list?.firstOrNull { it.id == id } ?: list?.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 新暱稱會順便建立第一本單字簿；已有同名的暱稱就直接切換過去
    fun createProfile(name: String, avatar: Avatar, onReady: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = profileDao.findByName(trimmed)?.id
                ?: profileDao.insert(Profile(name = trimmed, avatar = avatar.name)).also { newId ->
                    bookDao.insert(Book(profileId = newId, name = AppDatabase.DEFAULT_BOOK))
                }
            selectProfile(id)
            onReady()
        }
    }

    // 刪除進度：這個冒險者的單字、單字簿和暱稱一起刪掉
    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            db.withTransaction {
                dao.deleteForProfile(profile.id)
                bookDao.deleteForProfile(profile.id)
                profileDao.delete(profile.id)
            }
        }
    }

    fun selectProfile(id: Long) {
        prefs.edit().putLong(KEY_PROFILE, id).apply()
        selectedProfileId.value = id
        selectedBookId.value = null
    }

    private val profileId: Long? get() = currentProfile.value?.id

    // ---- 單字簿 ----

    val books: StateFlow<List<Book>> = currentProfile
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else bookDao.getForProfile(profile.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // null 代表「全部」
    val selectedBookId = MutableStateFlow<Long?>(null)

    fun selectBook(id: Long?) {
        selectedBookId.value = id
    }

    fun createBook(name: String, onCreated: (Long) -> Unit = {}) {
        val pid = profileId ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            onCreated(bookDao.insert(Book(profileId = pid, name = trimmed)))
        }
    }

    // 新增單字時放進目前選的單字簿，選「全部」時放第一本
    private val bookForNewWords: Long? get() = selectedBookId.value ?: books.value.firstOrNull()?.id

    // ---- 單字本 ----

    // 這個暱稱的所有單字（拍照時用來判斷「已有」）
    val allWords: StateFlow<List<Word>> = currentProfile
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else dao.getAll(profile.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 目前單字簿裡的單字
    val words: StateFlow<List<Word>> = combine(allWords, selectedBookId) { list, bookId ->
        if (bookId == null) list else list.filter { it.bookId == bookId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val translatorStatus: StateFlow<ModelStatus> = WordTranslator.status

    init {
        fillMissingMeanings()
    }

    fun add(text: String, meaning: String) {
        val pid = profileId ?: return
        viewModelScope.launch {
            dao.insert(Word(text = text.trim(), meaning = meaning.trim(), profileId = pid, bookId = bookForNewWords))
            fillMissingMeaningsNow()
        }
    }

    // 把拍照選到的單字一次加入指定的單字簿，這個暱稱已經有的字會略過
    fun addAll(texts: Collection<String>, bookId: Long?) {
        val pid = profileId ?: return
        viewModelScope.launch {
            val existing = dao.getAllTexts(pid).map { it.lowercase() }.toSet()
            val newWords = texts.filter { it.lowercase() !in existing }
                .map { Word(text = it, meaning = "", profileId = pid, bookId = bookId) }
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

    // 下載失敗後讓使用者按「重試」
    fun retryTranslation() = fillMissingMeanings()

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
        val pid = profileId ?: return
        reviewedCount = 0
        viewModelScope.launch {
            val bookId = selectedBookId.value
            reviewQueue = dao.getDue(pid, System.currentTimeMillis())
                .filter { bookId == null || it.bookId == bookId }
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
