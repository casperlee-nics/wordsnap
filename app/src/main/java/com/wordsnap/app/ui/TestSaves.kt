package com.wordsnap.app.ui

import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Book
import com.wordsnap.app.data.Profile
import com.wordsnap.app.data.Word

// 測試用的存檔：9 個職業各一位 LV60 冒險者，道具每種 9 個，附一本已經記住 30 個字的單字簿（可以直接戰鬥）
// 只建立一次；刪掉了不會再出現。正式上架前把 ENABLED 改成 false（或整個檔案刪掉）
object TestSaves {
    const val ENABLED = true
    const val LEVEL = 60
    private const val ITEM_COUNT = 9

    private val WORDS = listOf(
        "apple", "banana", "castle", "dragon", "forest", "garden", "harbor", "island", "jungle", "knight",
        "ladder", "magic", "number", "ocean", "palace", "queen", "river", "silver", "tiger", "umbrella",
        "valley", "window", "yellow", "zebra", "bridge", "candle", "desert", "engine", "flower", "galaxy",
    )

    suspend fun seed(db: AppDatabase) {
        val profiles = db.profileDao()
        val now = System.currentTimeMillis()
        Avatar.entries.forEachIndexed { i, avatar ->
            val name = "LV$LEVEL${avatar.race.label}".take(12)
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
}
