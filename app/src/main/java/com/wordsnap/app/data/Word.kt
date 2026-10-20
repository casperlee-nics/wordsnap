package com.wordsnap.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 單字本裡的一個單字（資料庫中的一列）
@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val meaning: String,
    val createdAt: Long = System.currentTimeMillis(),
    // 複習用：box 越高代表越熟，dueAt 是下次該複習的時間
    @ColumnInfo(defaultValue = "0") val box: Int = 0,
    @ColumnInfo(defaultValue = "0") val dueAt: Long = 0,
    // 屬於哪個暱稱
    @ColumnInfo(defaultValue = "1") val profileId: Long = 1,
    // 放在哪一本單字簿
    val bookId: Long? = null,
)
