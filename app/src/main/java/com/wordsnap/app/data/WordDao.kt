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
    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Word>>

    @Query("SELECT text FROM words")
    suspend fun getAllTexts(): List<String>

    @Query("SELECT * FROM words WHERE meaning = ''")
    suspend fun getWithoutMeaning(): List<Word>

    @Query("SELECT * FROM words WHERE dueAt <= :now ORDER BY dueAt")
    suspend fun getDue(now: Long): List<Word>

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
}
