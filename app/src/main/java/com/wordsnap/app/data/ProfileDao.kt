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

    @Query("UPDATE profiles SET exp = exp + :amount WHERE id = :id")
    suspend fun addExp(id: Long, amount: Int)

    @Query("UPDATE profiles SET deaths = deaths + 1 WHERE id = :id")
    suspend fun addDeath(id: Long)

    @Query("UPDATE profiles SET midBossSeen = :mid, bigBossSeen = :big WHERE id = :id")
    suspend fun setBossSeen(id: Long, mid: Int, big: Int)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
