package com.wordsnap.app.ui

import android.content.Context

private const val PREFS = "wordsnap"
private const val KEY_BATTLE_SIZE = "battle_size"

// 遊戲設定：每場考試出幾題（10、15、20）
val BattleSizes = listOf(10, 15, 20)

fun loadBattleSize(context: Context): Int =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_BATTLE_SIZE, BATTLE_SIZE)
        .takeIf { it in BattleSizes } ?: BATTLE_SIZE

fun saveBattleSize(context: Context, size: Int) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_BATTLE_SIZE, size).apply()
}
