package com.wordsnap.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 單字簿：一個暱稱可以有好幾本，例如「課本」、「多益」
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
