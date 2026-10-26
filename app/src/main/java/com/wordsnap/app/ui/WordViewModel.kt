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
import com.wordsnap.app.R
import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Book
import com.wordsnap.app.data.MonsterLog
import com.wordsnap.app.data.Profile
import com.wordsnap.app.data.Word
import com.wordsnap.app.i18n.appLanguage
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.translate.DictionaryPacks
import com.wordsnap.app.translate.ModelStatus
import com.wordsnap.app.translate.WordDictionary
import com.wordsnap.app.translate.WordTranslator
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DAY = 24 * 60 * 60 * 1000L

// 記得一次就升一格，每格下次複習的間隔：馬上、1、3、7、14、30 天
private val INTERVALS = longArrayOf(0, 1 * DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)

private const val PREFS = "wordsnap"
private const val KEY_PROFILE = "current_profile_id"
private const val KEY_BOOK = "last_book_"
private const val KEY_MEANING_CLEANUP = "meaning_cleanup_v1"
private const val KEY_TESTCOUNT_RESET = "testcount_reset_v1"
// 測試存檔的版本：改這裡會把測試冒險者整個重新產生
private const val KEY_TEST_SAVES = "test_saves_v2"
private const val KEY_REVIEW_SET = "review_set_"

// 複習一次最多 30 個字，記住 5 個才換新的 5 個進來
private const val REVIEW_SET_SIZE = 30
private const val REVIEW_SET_STEP = 5

// 記住 10 個字就能戰鬥；每場題數預設 10 題，可在遊戲設定改成 15 或 20 題
const val BATTLE_SIZE = 10
// 每場最少 5 題，能出的字不夠時用已經記住的字補
private const val MIN_BATTLE_SIZE = 5
private const val EXP_PER_WORD = 10
private const val EXP_PASS_BONUS = 50

// 答對七成算通過
fun passScore(total: Int): Int = (total * 7 + 9) / 10

// exhausted：記住的字都已經答對兩次了，只能進練習戰（經驗值 1/4、不掉道具）
data class BattleUnlock(val unlocked: Boolean, val remembered: Int, val total: Int, val exhausted: Boolean = false)

// 一個字在戰鬥中答對幾次後就不再出題
const val MAX_TESTS = 2

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
    // 打開寶箱拿到的道具，沒掉寶箱是 null
    val item: ItemType? = null,
    // 練習戰：經驗值只有 1/4、不會掉道具
    val practice: Boolean = false,
    // 把怪物打飛賺到的金幣
    val coins: Int = 0,
    // 這隻怪物之前已經打倒兩次，獎勵減半
    val halved: Boolean = false,
)

// 練習戰的經驗值除以幾
private const val PRACTICE_EXP_DIVISOR = 4

// 同一隻怪物打倒幾次之後，獎勵減半
private const val VETERAN_WINS = 2

// 打魔王的經驗值倍數：一般 1 倍、中魔王 2 倍、大魔王 3 倍
// 把怪物打飛得到的金幣
fun MonsterRank.coinReward(): Int = when (this) {
    MonsterRank.Normal -> 10
    MonsterRank.Mid -> 20
    MonsterRank.Big -> 30
}

private fun MonsterRank.expMultiplier(): Int = when (this) {
    MonsterRank.Normal -> 1
    MonsterRank.Mid -> 2
    MonsterRank.Big -> 3
}

// 畫面和資料庫之間的橋樑
@OptIn(ExperimentalCoroutinesApi::class)
class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.get(application)
    private val dao = db.wordDao()
    private val profileDao = db.profileDao()
    private val bookDao = db.bookDao()
    private val itemDao = db.itemDao()
    private val monsterLogDao = db.monsterLogDao()
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
                    bookDao.insert(Book(profileId = newId, name = tr(R.string.default_book)))
                }
            selectProfile(id)
            onReady()
        }
    }

    // 轉職：換成別的職業（種族），等級降 10 級
    fun changeClass(profile: Profile, avatar: Avatar) {
        if (levelInfo(profile.exp).level < CLASS_CHANGE_MIN_LEVEL) return
        // 不能轉成和現在一樣的職業
        if (Avatar.from(profile.avatar) == avatar) return
        viewModelScope.launch { profileDao.changeClass(profile.id, avatar.name, expAfterClassChange(profile.exp)) }
    }

    // 記錄管理：改名字，不能和別的冒險者同名；失敗時回傳原因
    fun renameProfile(profile: Profile, name: String, onDone: (String?) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return onDone(tr(R.string.name_blank))
        viewModelScope.launch {
            val other = profileDao.findByName(trimmed)
            if (other != null && other.id != profile.id) {
                onDone(tr(R.string.name_taken, trimmed))
            } else {
                profileDao.rename(profile.id, trimmed)
                onDone(null)
            }
        }
    }

    // 刪除冒險者：這個冒險者的單字、單字簿、道具和暱稱一起刪掉
    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            db.withTransaction {
                dao.deleteForProfile(profile.id)
                itemDao.deleteForProfile(profile.id)
                monsterLogDao.deleteForProfile(profile.id)
                bookDao.deleteForProfile(profile.id)
                profileDao.delete(profile.id)
            }
        }
    }

    fun selectProfile(id: Long) {
        prefs.edit().putLong(KEY_PROFILE, id).apply()
        // 讀取的就是目前的冒險者：單字簿清單不會重新送來，保留目前選的那本（上次讀的）
        val same = currentProfile.value?.id == id
        selectedProfileId.value = id
        if (same) {
            if (selectedBookId.value == null && !showAllBooks) restoreBook(books.value)
            return
        }
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
            books.collect { list -> restoreBook(list) }
        }
    }

    private fun restoreBook(list: List<Book>) {
        if (list.isEmpty()) return
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

    // 刪除一本單字簿和裡面的單字；至少要留一本
    fun deleteBook(book: Book) {
        if (books.value.size <= 1) return
        viewModelScope.launch {
            db.withTransaction {
                profileDao.addKeptRemembered(book.profileId, dao.countRememberedInBook(book.id))
                dao.deleteForBook(book.id)
                bookDao.delete(book.id)
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
        viewModelScope.launch {
            if (!prefs.getBoolean(KEY_TESTCOUNT_RESET, false)) {
                dao.resetTestCounts()
                prefs.edit().putBoolean(KEY_TESTCOUNT_RESET, true).apply()
            }
            cleanupMeaningsOnce()
            // 測試存檔（9 職業 LV60、全道具），只建立一次
            if (TestSaves.ENABLED && !prefs.getBoolean(KEY_TEST_SAVES, false)) {
                TestSaves.reset(db)
                prefs.edit().putBoolean(KEY_TEST_SAVES, true).apply()
            }
            if (TestSaves.ENABLED) TestSaves.topUp(db)
            // 目前的語言是日文，字典包有新版（或還沒裝）就先裝好，才查得到解釋
            DictionaryPacks.install(getApplication(), appLanguage(getApplication()))
            fillMissingMeaningsNow()
        }
    }

    // v0.71：之前從字典查到的長解釋，精簡成最多 2 個中文意思（只做一次）
    private suspend fun cleanupMeaningsOnce() {
        if (prefs.getBoolean(KEY_MEANING_CLEANUP, false)) return
        for (word in dao.getWithMeaning()) {
            if (!WordDictionary.looksLikeDictionary(word.meaning)) continue
            val simple = WordDictionary.simplify(word.meaning)
            if (simple.isNotEmpty() && simple != word.meaning) dao.setMeaning(word.id, simple)
        }
        prefs.edit().putBoolean(KEY_MEANING_CLEANUP, true).apply()
    }

    // ---- 道具 ----

    // 目前冒險者的道具和數量
    val items: StateFlow<Map<ItemType, Int>> = currentProfile
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else itemDao.getForProfile(profile.id) }
        .map { list -> list.mapNotNull { item -> ItemType.from(item.type)?.let { it to item.count } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // 冒險履歷：遇過的怪物 key → 紀錄
    val monsterLog: StateFlow<Map<String, MonsterLog>> = currentProfile
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else monsterLogDao.getForProfile(profile.id) }
        .map { list -> list.associateBy { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun useItem(type: ItemType) {
        val pid = profileId ?: return
        viewModelScope.launch { itemDao.use(pid, type.name) }
    }

    // 道具屋：用金幣買一個道具（金幣不夠就不會買）
    fun buyItem(type: ItemType) {
        val pid = profileId ?: return
        viewModelScope.launch {
            db.withTransaction {
                if (profileDao.spendCoins(pid, type.price) == 1) itemDao.add(pid, type.name)
            }
        }
    }

    // 道具屋：半價賣掉一個道具
    fun sellItem(type: ItemType) {
        val pid = profileId ?: return
        viewModelScope.launch {
            db.withTransaction {
                if (itemDao.use(pid, type.name) == 1) profileDao.addCoins(pid, type.sellPrice)
            }
        }
    }

    fun add(text: String, meaning: String) {
        val pid = profileId ?: return
        viewModelScope.launch {
            dao.insert(Word(text = text.trim(), meaning = meaning.trim(), profileId = pid, bookId = bookForNewWords))
            fillMissingMeaningsNow()
        }
    }

    // 把拍照選到的單字一次加入指定的單字簿，這本已經有的字會略過（別本有的字可以重複加）
    fun addAll(texts: Collection<String>, bookId: Long?) {
        val pid = profileId ?: return
        viewModelScope.launch {
            val existing = allWords.value.filter { it.bookId == bookId }.map { it.text.trim().lowercase() }.toSet()
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

    // 這個字已經在哪些單字簿裡（不分大小寫）；編輯時不算自己
    fun booksContaining(text: String, except: Word? = null): List<Book> {
        val key = text.trim().lowercase()
        if (key.isEmpty()) return emptyList()
        val ids = allWords.value.filter { it.id != except?.id && it.text.trim().lowercase() == key }.map { it.bookId }.toSet()
        return books.value.filter { it.id in ids }
    }

    // 刪掉已經記住的字，記住的字數照樣保留
    fun delete(word: Word) {
        viewModelScope.launch {
            db.withTransaction {
                if (word.box >= 1) profileDao.addKeptRemembered(word.profileId, 1)
                dao.delete(word)
            }
        }
    }

    // 這個冒險者總共記住幾個字：現在單字簿裡記住的，加上刪掉的單字簿裡記住過的
    fun rememberedTotal(): Int =
        allWords.value.count { it.box >= 1 } + (currentProfile.value?.keptRemembered ?: 0)

    // 下載失敗後讓使用者按「重試」
    fun retryTranslation() = fillMissingMeanings()

    private fun fillMissingMeanings() {
        viewModelScope.launch { fillMissingMeaningsNow() }
    }

    // 沒有中文意思的單字自動填上：先查離線字典，查不到才用 ML Kit 翻譯
    private suspend fun fillMissingMeaningsNow() {
        var canTranslate = true
        for (word in dao.getWithoutMeaning()) {
            val meaning = WordDictionary.lookup(getApplication(), word.text)
                ?: if (canTranslate) WordTranslator.translate(getApplication(), word.text) else null
            if (meaning == null) {
                // 翻譯檔還沒下載好，剩下的字只查字典，下次開 App 再試
                canTranslate = false
                continue
            }
            dao.fillMeaning(word.id, meaning)
        }
    }

    // ---- 戰鬥模式 ----

    // 這本單字簿的戰鬥模式是否解鎖：記住 10 個字，或整本都記住過
    fun battleUnlock(bookId: Long?): BattleUnlock {
        if (bookId == null) return BattleUnlock(false, 0, 0)
        val inBook = allWords.value.filter { it.bookId == bookId }
        val remembered = inBook.count { it.box >= 1 }
        val unlocked = remembered >= BATTLE_SIZE || (inBook.isNotEmpty() && remembered == inBook.size)
        val ready = inBook.count { it.isBattleReady() }
        val any = inBook.count { it.canPractice() }
        return BattleUnlock(unlocked && any > 0, remembered, inBook.size, exhausted = unlocked && any > 0 && ready == 0)
    }

    // 練習戰也能出的字：記住了、至少 3 個字母
    private fun Word.canPractice() = box >= 1 && text.trim().length >= 3

    // 一般戰鬥出題的字：再加上答對不到兩次
    private fun Word.isBattleReady() = canPractice() && testCount < MAX_TESTS

    // 這本的字都答對兩次了：進戰鬥就是練習戰
    fun isPracticeBattle(bookId: Long): Boolean {
        val inBook = allWords.value.filter { it.bookId == bookId }
        return inBook.none { it.isBattleReady() } && inBook.any { it.canPractice() }
    }

    // 出題：從記住的字裡隨機挑最多 10 個，先挑沒考過的，不夠再隨機用考過的補；練習戰從全部記住的字挑
    // 少於 5 題時，隨機用已經記住的字補到 5 題
    fun pickBattleWords(bookId: Long, practice: Boolean = false): List<Word> {
        val pool = allWords.value.filter { it.bookId == bookId && if (practice) it.canPractice() else it.isBattleReady() }
        val fresh = pool.filter { it.testedAt == null }.shuffled()
        val tested = pool.filter { it.testedAt != null }.shuffled()
        // 每場題數在遊戲設定裡選（10、15、20）
        val size = loadBattleSize(getApplication<Application>())
        val picked = fresh.take(size)
        val chosen = picked + tested.take(size - picked.size)
        if (chosen.size >= MIN_BATTLE_SIZE) return chosen.shuffled()
        // 不到 5 題：只用這本其他記住的字補，不會拿別本單字簿的字
        val ids = chosen.map { it.id }.toSet()
        val extra = allWords.value.filter { it.id !in ids && it.bookId == bookId && it.canPractice() }.shuffled()
        return (chosen + extra.take(MIN_BATTLE_SIZE - chosen.size)).shuffled()
    }

    // 這隻怪物已經打倒兩次以上
    fun isVeteran(monster: Monster): Boolean = (monsterLog.value[monster.key]?.wins ?: 0) >= VETERAN_WINS

    // 以前遇過這隻怪物（戰鬥中可以點牠看資料）
    fun hasMet(monster: Monster): Boolean = (monsterLog.value[monster.key]?.met ?: 0) >= 1

    // 這次要打的怪物：每記住 100 個字出現一隻大魔王、每 50 個字一隻中魔王，其他時候隨機一般怪物
    fun pickBattleMonster(): Monster {
        val profile = currentProfile.value ?: return NormalMonsters.random()
        val remembered = rememberedTotal()
        return when {
            remembered / 100 > profile.bigBossSeen -> BigBosses[profile.bigBossSeen % BigBosses.size]
            remembered / 50 > profile.midBossSeen -> MidBosses[profile.midBossSeen % MidBosses.size]
            else -> NormalMonsters.random()
        }
    }

    // 戰鬥結束：記下考過的字、答錯的字加一次錯誤（錯滿兩次變回沒記住）、加經驗值、通過就標記這本單字簿已通關
    // asked 是這次真的考過的字（被打倒時只算已經作答的），wrong 是其中答錯的
    fun finishBattle(
        bookId: Long,
        asked: List<Word>,
        wrong: List<Word>,
        total: Int,
        died: Boolean,
        monster: Monster,
        practice: Boolean,
        // 有沒有把怪物打飛：只有打飛才會掉寶箱
        knockedOut: Boolean,
        // 哥布林的掠奪：金幣倍數
        coinMultiplier: Int = 1,
        onDone: (BattleReward) -> Unit,
    ) {
        val profile = currentProfile.value ?: return
        val correct = asked.size - wrong.size
        // 用道具提早把怪物打倒也算通過
        val passed = !died && (knockedOut || correct >= passScore(total))
        // 這隻怪物已經打倒兩次以上：經驗值、金幣、寶箱機率都減半
        val veteran = isVeteran(monster)
        val divisor = if (veteran) 2 else 1
        val full = (correct * EXP_PER_WORD + if (passed) EXP_PASS_BONUS else 0) * monster.rank.expMultiplier() / divisor
        val gained = if (practice) full / PRACTICE_EXP_DIVISOR else full
        // 把怪物打飛時有一半機率掉寶箱；練習戰不會掉，避免一直刷道具
        val item = if (knockedOut && !practice && Random.nextFloat() < CHEST_CHANCE / divisor) ItemType.roll() else null
        // 打飛怪物賺金幣；練習戰沒有
        val coins = if (knockedOut && !practice) monster.rank.coinReward() * coinMultiplier / divisor else 0
        val before = levelInfo(profile.exp).level
        val after = levelInfo(profile.exp + gained).level
        val forgotten = wrong.filter { it.wrongCount + 1 >= 2 && it.box >= 1 }.map { it.text }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            db.withTransaction {
                dao.markTested(asked.map { it.id }, now)
                val wrongIds = wrong.map { it.id }.toSet()
                dao.addCorrect(asked.map { it.id }.filter { it !in wrongIds })
                if (wrong.isNotEmpty()) {
                    val ids = wrong.map { it.id }
                    dao.addWrong(ids)
                    dao.forgetOftenWrong(ids, now)
                }
                profileDao.addExp(profile.id, gained)
                if (died) profileDao.addDeath(profile.id)
                if (passed) bookDao.markCleared(bookId, now)
                if (item != null) itemDao.add(profile.id, item.name)
                if (coins > 0) profileDao.addCoins(profile.id, coins)
                // 冒險履歷：遇到這隻怪物一次，通過就算打倒
                monsterLogDao.record(profile.id, monster.key, passed)
                // 魔王要通過這場戰鬥才算打倒，不然下次還會再出現
                if (passed && monster.rank != MonsterRank.Normal) {
                    val remembered = rememberedTotal()
                    if (monster.rank == MonsterRank.Big) {
                        // 打倒大魔王時，同一段的中魔王也一起跳過
                        profileDao.setBossSeen(profile.id, maxOf(profile.midBossSeen, remembered / 50), profile.bigBossSeen + 1)
                    } else {
                        profileDao.setBossSeen(profile.id, profile.midBossSeen + 1, profile.bigBossSeen)
                    }
                }
            }
            val deaths = profile.deaths + if (died) 1 else 0
            onDone(BattleReward(passed, correct, total, gained, before, after, died, forgotten, deaths, item, practice, coins, veteran))
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

    // 正在準備這次要複習的字（還沒準備好時畫面先空著，不會顯示上一次剩下的字）
    var reviewLoading by mutableStateOf(false)
        private set

    // 有該複習（沒記住）的字就只考那些；全部都記住了，就把整本隨機考一遍
    fun startReview() {
        val pid = profileId ?: return
        reviewedCount = 0
        // 先清掉上一次複習剩下的字，免得一進來念到上一次的最後一個字
        reviewQueue = emptyList()
        reviewLoading = true
        viewModelScope.launch {
            val bookId = selectedBookId.value
            val due = dao.getDue(pid, System.currentTimeMillis())
                .filter { bookId == null || it.bookId == bookId }
            practiceMode = due.isEmpty()
            // 到期的字也打亂順序出現；太多時只複習固定的 30 個
            reviewQueue = if (due.isNotEmpty()) {
                reviewSet(bookId, due).shuffled()
            } else {
                allWords.value.filter { bookId == null || it.bookId == bookId }.shuffled()
            }
            reviewLoading = false
        }
    }

    // 到期的字超過 30 個時，每次都複習同一組 30 個（第一次隨機挑），
    // 這組裡記住 5 個（就不再到期）才補進新的 5 個，不會每次換一批
    private fun reviewSet(bookId: Long?, due: List<Word>): List<Word> {
        if (due.size <= REVIEW_SET_SIZE) return due
        val key = KEY_REVIEW_SET + (bookId ?: "all")
        val dueIds = due.map { it.id }.toSet()
        val saved = prefs.getString(key, null).orEmpty().split(',').mapNotNull { it.toLongOrNull() }
        val kept = saved.filter { it in dueIds }
        val room = (REVIEW_SET_SIZE - kept.size) / REVIEW_SET_STEP * REVIEW_SET_STEP
        val added = due.filter { it.id !in kept }.shuffled().take(room).map { it.id }
        val set = (kept + added).toSet()
        prefs.edit().putString(key, set.joinToString(",")).apply()
        return due.filter { it.id in set }
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
                    // 忘記了就重新算，記住後可以再上戰場
                    testCount = if (remembered) word.testCount else 0,
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
