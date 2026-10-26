package com.wordsnap.app.ui

import android.content.Context
import com.wordsnap.app.data.gamePrefs

private const val KEY_BATTLE_SIZE = "battle_size"

// 遊戲設定：每場考試出幾題（10、15、20）
val BattleSizes = listOf(10, 15, 20)

fun loadBattleSize(context: Context): Int =
    context.gamePrefs().getInt(KEY_BATTLE_SIZE, BATTLE_SIZE)
        .takeIf { it in BattleSizes } ?: BATTLE_SIZE

fun saveBattleSize(context: Context, size: Int) {
    context.gamePrefs().edit().putInt(KEY_BATTLE_SIZE, size).apply()
}
