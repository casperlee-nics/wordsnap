package com.wordsnap.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 讀寫單字的方法
@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAll(profileId: Long): Flow<List<Word>>

    @Query("SELECT text FROM words WHERE profileId = :profileId")
    suspend fun getAllTexts(profileId: Long): List<String>

    @Query("SELECT * FROM words WHERE meaning = ''")
    suspend fun getWithoutMeaning(): List<Word>

    @Query("SELECT * FROM words WHERE profileId = :profileId AND dueAt <= :now ORDER BY dueAt")
    suspend fun getDue(profileId: Long, now: Long): List<Word>

    // 每日提醒用：所有暱稱加起來今天要複習幾個字
    @Query("SELECT COUNT(*) FROM words WHERE dueAt <= :now")
    suspend fun countDue(now: Long): Int

    @Insert
    suspend fun insert(word: Word)

    @Insert
    suspend fun insertAll(words: List<Word>)

    // 只在還沒有意思時才填，避免蓋掉使用者自己寫的
    @Query("UPDATE words SET meaning = :meaning WHERE id = :id AND meaning = ''")
    suspend fun fillMeaning(id: Long, meaning: String)

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    // 戰鬥模式考過的單字記下時間，下次優先出沒考過的
    @Query("UPDATE words SET testedAt = :time, testCount = testCount + 1 WHERE id IN (:ids)")
    suspend fun markTested(ids: List<Long>, time: Long)

    // 戰鬥模式答錯的字加一次錯誤
    @Query("UPDATE words SET wrongCount = wrongCount + 1 WHERE id IN (:ids)")
    suspend fun addWrong(ids: List<Long>)

    // 錯滿兩次的字變回「沒記住」，馬上要複習
    @Query("UPDATE words SET box = 0, dueAt = :now, wrongCount = 0, testCount = 0 WHERE wrongCount >= 2 AND id IN (:ids)")
    suspend fun forgetOftenWrong(ids: List<Long>, now: Long)

    // v0.71 精簡舊的字典解釋用
    @Query("SELECT * FROM words WHERE meaning != ''")
    suspend fun getWithMeaning(): List<Word>

    @Query("UPDATE words SET meaning = :meaning WHERE id = :id")
    suspend fun setMeaning(id: Long, meaning: String)

    @Query("DELETE FROM words WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)

    @Query("DELETE FROM words WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}
