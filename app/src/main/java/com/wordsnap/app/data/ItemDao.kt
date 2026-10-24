package com.wordsnap.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ItemDao {
    @Query("SELECT * FROM items WHERE profileId = :profileId AND count > 0")
    abstract fun getForProfile(profileId: Long): Flow<List<Item>>

    @Query("INSERT OR IGNORE INTO items (profileId, type, count) VALUES (:profileId, :type, 0)")
    abstract suspend fun ensure(profileId: Long, type: String)

    @Query("UPDATE items SET count = count + 1 WHERE profileId = :profileId AND type = :type")
    abstract suspend fun increment(profileId: Long, type: String)

    // 得到一個道具
    @Transaction
    open suspend fun add(profileId: Long, type: String) {
        ensure(profileId, type)
        increment(profileId, type)
    }

    // 用掉一個道具
    @Query("UPDATE items SET count = count - 1 WHERE profileId = :profileId AND type = :type AND count > 0")
    abstract suspend fun use(profileId: Long, type: String)

    @Query("DELETE FROM items WHERE profileId = :profileId")
    abstract suspend fun deleteForProfile(profileId: Long)
}
