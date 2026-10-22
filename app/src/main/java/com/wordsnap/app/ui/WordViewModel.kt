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
private const val KEY_BOOK = "last_book_"

const val BATTLE_SIZE = 15
private const val EXP_PER_WORD = 10
private const val EXP_PASS_BONUS = 50

// 答對七成算通過
fun passScore(total: Int): Int = (total * 7 + 9) / 10

data class BattleUnlock(val unlocked: Boolean, val remembered: Int, val total: Int)

data class BattleReward(
    val passed: Boolean,
    val correct: Int,
    val total: Int,
    val exp: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    // 被怪物打倒
    val died: Boolean = false,
    // 這次錯滿兩次、變回沒記住的字
    val forgotten: List<String> = emptyList(),
    // 到目前為止總共被打倒幾次（含這次）
    val deaths: Int = 0,
)

// 答錯超過一半就被打倒，所以人物的 HP 是題數的一半再多 1
fun heroMaxHp(total: Int): Int = total / 2 + 1

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
        showAllBooks = false
        selectedBookId.value = null
    }

    private val profileId: Long? get() = currentProfile.value?.id

    // ---- 單字簿 ----

    val books: StateFlow<List<Book>> = currentProfile
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else bookDao.getForProfile(profile.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // null 代表「全部」
    val selectedBookId = MutableStateFlow<Long?>(null)

    // 使用者自己選了「全部」；沒選的話預設顯示第一本（我的單字簿）
    private var showAllBooks = false

    fun selectBook(id: Long?) {
        showAllBooks = id == null
        selectedBookId.value = id
        // 記住最後讀的那本，下次打開還停在這裡（0 代表「全部」）
        profileId?.let { pid -> prefs.edit().putLong(KEY_BOOK + pid, id ?: 0L).apply() }
    }

    init {
        // 單字簿讀進來（或換暱稱、刪掉目前那本）時，回到上次讀的那本；第一次就選第一本（我的冒險日誌）
        viewModelScope.launch {
            books.collect { list ->
                if (list.isEmpty()) return@collect
                val id = selectedBookId.value
                val missing = id != null && list.none { it.id == id }
                if ((id == null && !showAllBooks) || missing) {
                    val saved = profileId?.let { pid -> prefs.getLong(KEY_BOOK + pid, -1L) } ?: -1L
                    when {
                        saved == 0L && !missing -> showAllBooks = true
                        list.any { it.id == saved } -> selectedBookId.value = saved
                        else -> selectedBookId.value = list.first().id
                    }
                }
            }
        }
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

    // ---- 戰鬥模式 ----

    // 這本單字簿的戰鬥模式是否解鎖：記住 15 個字，或整本都記住過
    fun battleUnlock(bookId: Long?): BattleUnlock {
        if (bookId == null) return BattleUnlock(false, 0, 0)
        val inBook = allWords.value.filter { it.bookId == bookId }
        val remembered = inBook.count { it.box >= 1 }
        val unlocked = remembered >= BATTLE_SIZE || (inBook.isNotEmpty() && remembered == inBook.size)
        val pool = inBook.count { it.box >= 1 && it.text.trim().length >= 3 }
        return BattleUnlock(unlocked && pool > 0, remembered, inBook.size)
    }

    // 出題：從記住的字裡隨機挑最多 15 個，先挑沒考過的，不夠再隨機用考過的補
    fun pickBattleWords(bookId: Long): List<Word> {
        val pool = allWords.value.filter { it.bookId == bookId && it.box >= 1 && it.text.trim().length >= 3 }
        val fresh = pool.filter { it.testedAt == null }.shuffled()
        val tested = pool.filter { it.testedAt != null }.shuffled()
        val picked = fresh.take(BATTLE_SIZE)
        return (picked + tested.take(BATTLE_SIZE - picked.size)).shuffled()
    }

    // 戰鬥結束：記下考過的字、答錯的字加一次錯誤（錯滿兩次變回沒記住）、加經驗值、通過就標記這本單字簿已通關
    // asked 是這次真的考過的字（被打倒時只算已經作答的），wrong 是其中答錯的
    fun finishBattle(
        bookId: Long,
        asked: List<Word>,
        wrong: List<Word>,
        total: Int,
        died: Boolean,
        onDone: (BattleReward) -> Unit,
    ) {
        val profile = currentProfile.value ?: return
        val correct = asked.size - wrong.size
        val passed = !died && correct >= passScore(total)
        val gained = correct * EXP_PER_WORD + if (passed) EXP_PASS_BONUS else 0
        val before = levelInfo(profile.exp).level
        val after = levelInfo(profile.exp + gained).level
        val forgotten = wrong.filter { it.wrongCount + 1 >= 2 && it.box >= 1 }.map { it.text }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            db.withTransaction {
                dao.markTested(asked.map { it.id }, now)
                if (wrong.isNotEmpty()) {
                    val ids = wrong.map { it.id }
                    dao.addWrong(ids)
                    dao.forgetOftenWrong(ids, now)
                }
                profileDao.addExp(profile.id, gained)
                if (died) profileDao.addDeath(profile.id)
                if (passed) bookDao.markCleared(bookId, now)
            }
            val deaths = profile.deaths + if (died) 1 else 0
            onDone(BattleReward(passed, correct, total, gained, before, after, died, forgotten, deaths))
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

    // true：今天沒有該複習的字，改成隨機練習整本已記住的字
    var practiceMode by mutableStateOf(false)
        private set

    // 有該複習（沒記住）的字就只考那些；全部都記住了，就把整本隨機考一遍
    fun startReview() {
        val pid = profileId ?: return
        reviewedCount = 0
        viewModelScope.launch {
            val bookId = selectedBookId.value
            val due = dao.getDue(pid, System.currentTimeMillis())
                .filter { bookId == null || it.bookId == bookId }
            practiceMode = due.isEmpty()
            reviewQueue = if (due.isNotEmpty()) {
                due
            } else {
                allWords.value.filter { bookId == null || it.bookId == bookId }.shuffled()
            }
        }
    }

    fun answer(word: Word, remembered: Boolean) {
        // 練習模式按「記得」不改進度，避免沒到期的字被越排越遠
        val updated = when {
            practiceMode && remembered && word.box >= 1 -> word
            else -> {
                val box = if (remembered) (word.box + 1).coerceAtMost(INTERVALS.size - 1) else 0
                word.copy(
                    box = box,
                    dueAt = System.currentTimeMillis() + INTERVALS[box],
                    wrongCount = if (remembered) word.wrongCount else 0,
                )
            }
        }
        if (updated != word) viewModelScope.launch { dao.update(updated) }

        // 忘了的字放到最後，這次複習再考一次
        reviewQueue = reviewQueue.drop(1) + if (remembered) emptyList() else listOf(updated)
        if (remembered) reviewedCount++
        reviewStep++
    }
}
