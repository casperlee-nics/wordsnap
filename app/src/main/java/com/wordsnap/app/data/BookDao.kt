package com.wordsnap.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE profileId = :profileId ORDER BY createdAt")
    fun getForProfile(profileId: Long): Flow<List<Book>>

    @Insert
    suspend fun insert(book: Book): Long

    @Query("DELETE FROM books WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}
