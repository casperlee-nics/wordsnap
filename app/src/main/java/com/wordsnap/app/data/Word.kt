package com.wordsnap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 單字本裡的一個單字（資料庫中的一列）
@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val meaning: String,
    val createdAt: Long = System.currentTimeMillis(),
)
