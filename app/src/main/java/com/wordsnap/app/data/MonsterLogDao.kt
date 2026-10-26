package com.wordsnap.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MonsterLogDao {
    @Query("SELECT * FROM monster_log WHERE profileId = :profileId")
    abstract fun getForProfile(profileId: Long): Flow<List<MonsterLog>>

    @Query("INSERT OR IGNORE INTO monster_log (profileId, `key`, met, wins) VALUES (:profileId, :key, 0, 0)")
    abstract suspend fun ensure(profileId: Long, key: String)

    @Query("UPDATE monster_log SET met = met + 1, wins = wins + :win WHERE profileId = :profileId AND `key` = :key")
    abstract suspend fun bump(profileId: Long, key: String, win: Int)

    // 打完一場：遇到次數加一，打倒了再加一次勝場
    @Transaction
    open suspend fun record(profileId: Long, key: String, won: Boolean) {
        ensure(profileId, key)
        bump(profileId, key, if (won) 1 else 0)
    }

    @Query("DELETE FROM monster_log WHERE profileId = :profileId")
    abstract suspend fun deleteForProfile(profileId: Long)
}
