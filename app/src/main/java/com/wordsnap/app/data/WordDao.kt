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

    @Insert
    suspend fun insert(word: Word)

    @Insert
    suspend fun insertAll(words: List<Word>)

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)
}
