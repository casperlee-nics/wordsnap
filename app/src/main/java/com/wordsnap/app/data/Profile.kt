package com.wordsnap.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 一個暱稱就是一位使用者，各自有自己的單字本和複習進度
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    // 像素人物的名稱，見 ui/Avatar.kt
    val avatar: String? = null,
    // 戰鬥得到的經驗值，等級由它算出來，見 ui/Hero.kt
    @ColumnInfo(defaultValue = "0") val exp: Int = 0,
    // 戰鬥中被打倒的次數
    @ColumnInfo(defaultValue = "0") val deaths: Int = 0,
    // 已經打倒幾隻中魔王（每記住 50 個字一隻）、大魔王（每記住 100 個字一隻）
    @ColumnInfo(defaultValue = "0") val midBossSeen: Int = 0,
    @ColumnInfo(defaultValue = "0") val bigBossSeen: Int = 0,
    // 刪掉的單字簿或單字裡已經記住的字數；記住的字數照樣算進去，不會因為刪書被扣掉
    @ColumnInfo(defaultValue = "0") val keptRemembered: Int = 0,
)
