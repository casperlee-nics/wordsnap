package com.wordsnap.app.data

import androidx.room.Entity

// 冒險者道具袋裡的一種道具和數量；type 是 ui/Items.kt 裡 ItemType 的名稱
@Entity(tableName = "items", primaryKeys = ["profileId", "type"])
data class Item(
    val profileId: Long,
    val type: String,
    val count: Int,
)
