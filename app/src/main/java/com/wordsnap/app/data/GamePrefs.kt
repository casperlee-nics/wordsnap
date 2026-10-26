package com.wordsnap.app.data

import android.content.Context
import android.content.SharedPreferences

// 遊戲設定和小記錄（語言、音量、字體大小、每場題數、上次選的暱稱等）都存在同一個檔案
fun Context.gamePrefs(): SharedPreferences = getSharedPreferences("wordsnap", Context.MODE_PRIVATE)
