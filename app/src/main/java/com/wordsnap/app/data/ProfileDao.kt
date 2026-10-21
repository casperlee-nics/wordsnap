package com.wordsnap.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt")
    fun getAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Profile?

    @Insert
    suspend fun insert(profile: Profile): Long

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
