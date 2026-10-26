package com.wordsnap.app.ui

import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Book
import com.wordsnap.app.data.Profile
import com.wordsnap.app.data.Word

// 測試用的存檔：9 個職業各一位 LV60 冒險者，道具每種 9 個，附一本已經記住 30 個字的單字簿（可以直接戰鬥），
// 冒險履歷裡 85 種怪物都遇過（舊的測試存檔沒有履歷時也會補上）
// 只建立一次（KEY_TEST_SAVES 換版本時整個重新產生）；刪掉了不會再出現。正式上架前把 ENABLED 改成 false（或整個檔案刪掉）
object TestSaves {
    const val ENABLED = true
    const val LEVEL = 60
    private const val ITEM_COUNT = 9
    private const val TEST_COINS = 5000

    private val WORDS = listOf(
        "apple", "banana", "castle", "dragon", "forest", "garden", "harbor", "island", "jungle", "knight",
        "ladder", "magic", "number", "ocean", "palace", "queen", "river", "silver", "tiger", "umbrella",
        "valley", "window", "yellow", "zebra", "bridge", "candle", "desert", "engine", "flower", "galaxy",
    )

    // 測試冒險者的暱稱，例如「LV60勇者」
    private fun nameOf(avatar: Avatar) = "LV$LEVEL${avatar.race.label}".take(12)

    // 重新產生測試存檔：先把舊的測試冒險者（單字、道具、冒險履歷、單字簿）整個刪掉，再重新建立
    suspend fun reset(db: AppDatabase) {
        Avatar.entries.forEach { avatar ->
            val profile = db.profileDao().findByName(nameOf(avatar)) ?: return@forEach
            db.wordDao().deleteForProfile(profile.id)
            db.itemDao().deleteForProfile(profile.id)
            db.monsterLogDao().deleteForProfile(profile.id)
            db.bookDao().deleteForProfile(profile.id)
            db.profileDao().delete(profile.id)
        }
        seed(db)
    }

    suspend fun seed(db: AppDatabase) {
        val profiles = db.profileDao()
        val now = System.currentTimeMillis()
        Avatar.entries.forEachIndexed { i, avatar ->
            val name = nameOf(avatar)
            if (profiles.findByName(name) != null) return@forEachIndexed
            // createdAt 依序排，存檔畫面照職業順序列出
            val id = profiles.insert(Profile(name = name, avatar = avatar.name, exp = totalExpForLevel(LEVEL), createdAt = now + i))
            val bookId = db.bookDao().insert(Book(profileId = id, name = "測試單字簿"))
            db.wordDao().insertAll(
                WORDS.map { Word(text = it, meaning = "", profileId = id, bookId = bookId, box = 1, dueAt = now + 30L * 24 * 60 * 60 * 1000) },
            )
            ItemType.entries.forEach { type -> repeat(ITEM_COUNT) { db.itemDao().add(id, type.name) } }
        }
    }

    // 測試存檔每次開 App 補上後來加的東西：金幣、新道具；冒險履歷還沒遇過的怪物都補成遇過、打倒一次（每次開 App 檢查，舊存檔也會補上）
    suspend fun topUp(db: AppDatabase) {
        val profiles = db.profileDao()
        val log = db.monsterLogDao()
        Avatar.entries.forEach { avatar ->
            val profile = profiles.findByName(nameOf(avatar)) ?: return@forEach
            // 道具屋測試：金幣 5000，後來新增的道具也各補 9 個
            if (profile.coins == 0) profiles.addCoins(profile.id, TEST_COINS)
            ItemType.entries.forEach { type ->
                if (db.itemDao().has(profile.id, type.name) == 0) repeat(ITEM_COUNT) { db.itemDao().add(profile.id, type.name) }
            }
            if (log.count(profile.id) >= AllMonsters.size) return@forEach
            AllMonsters.forEach { log.fill(profile.id, it.key) }
        }
    }
}
