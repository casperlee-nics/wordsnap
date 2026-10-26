package com.wordsnap.app.data

import androidx.room.Entity

// 冒險履歷：冒險者遇過的每一種怪物，遇到幾次、打倒幾次；key 是 ui/MonsterArt.kt 裡怪物的 key
@Entity(tableName = "monster_log", primaryKeys = ["profileId", "key"])
data class MonsterLog(
    val profileId: Long,
    val key: String,
    val met: Int,
    val wins: Int,
)
