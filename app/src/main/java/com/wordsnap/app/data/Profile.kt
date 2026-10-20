package com.wordsnap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 一個暱稱就是一位使用者，各自有自己的單字本和複習進度
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
