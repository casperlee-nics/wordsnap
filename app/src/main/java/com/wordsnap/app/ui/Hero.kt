package com.wordsnap.app.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.i18n.trArray

// 人物的職業（種族）、等級、能力、稱號和招式

// 擊倒怪物的招式，每個職業不同；稱號每升一階，招式就換成更強的一招
enum class Finisher(@ArrayRes private val movesRes: Int) {
    Slash(R.array.moves_slash),
    Magic(R.array.moves_magic),
    Arrow(R.array.moves_arrow),
    Bomb(R.array.moves_bomb),
    Smash(R.array.moves_smash),
    Claw(R.array.moves_claw),
    Fire(R.array.moves_fire),
    Laser(R.array.moves_laser),
    DarkFire(R.array.moves_darkfire);

    // 第 tier 階的招式名稱（tier 見 avatarTier）
    fun label(tier: Int): String {
        val moves = trArray(movesRes)
        return moves[tier.coerceIn(0, moves.size - 1)]
    }
}

// 能力：力量、體力、智慧、運氣 = 基礎值 + 每升一級的成長值 x (等級 - 1)
enum class Race(
    @StringRes private val labelRes: Int,
    val base: Stats,
    val growth: Stats,
    val finisher: Finisher,
    // Lv 1、10、20、30、40、50 時的稱號
    @ArrayRes private val titlesRes: Int,
) {
    Hero(R.string.race_hero, Stats(6, 6, 4, 5), Stats(3, 2, 1, 2), Finisher.Slash, R.array.titles_hero),
    Mage(R.string.race_mage, Stats(3, 4, 9, 5), Stats(1, 2, 3, 2), Finisher.Magic, R.array.titles_mage),
    Archer(R.string.race_archer, Stats(5, 4, 5, 7), Stats(2, 1, 2, 3), Finisher.Arrow, R.array.titles_archer),
    Goblin(R.string.race_goblin, Stats(4, 4, 3, 9), Stats(2, 1, 1, 3), Finisher.Bomb, R.array.titles_goblin),
    Dwarf(R.string.race_dwarf, Stats(8, 8, 3, 3), Stats(3, 3, 1, 1), Finisher.Smash, R.array.titles_dwarf),
    Cat(R.string.race_cat, Stats(4, 4, 5, 7), Stats(2, 1, 2, 3), Finisher.Claw, R.array.titles_cat),
    Dragon(R.string.race_dragon, Stats(9, 7, 4, 2), Stats(3, 2, 2, 1), Finisher.Fire, R.array.titles_dragon),
    Machine(R.string.race_machine, Stats(6, 9, 6, 1), Stats(2, 3, 2, 1), Finisher.Laser, R.array.titles_machine),
    Demon(R.string.race_demon, Stats(7, 5, 7, 3), Stats(3, 1, 3, 1), Finisher.DarkFire, R.array.titles_demon);

    val label: String get() = tr(labelRes)
    val titles: List<String> get() = trArray(titlesRes).toList()
}

data class Stats(val str: Int, val vit: Int, val intel: Int, val luk: Int)

val Avatar.race: Race
    get() = when (this) {
        Avatar.Hero -> Race.Hero
        Avatar.Mage -> Race.Mage
        Avatar.ElfArcher -> Race.Archer
        Avatar.Goblin -> Race.Goblin
        Avatar.Dwarf -> Race.Dwarf
        Avatar.Cat -> Race.Cat
        Avatar.Dragon -> Race.Dragon
        Avatar.Robot -> Race.Machine
        Avatar.Demon -> Race.Demon
    }

const val MAX_LEVEL = 99

// 從 Lv n 升到 n+1 需要的經驗值，越高級需要越多
// 每記住一個字（答對兩次）大約 30 經驗值：記住約 800 個字到 LV60（主打輕鬆背單字，職業多可以多練幾個）
fun expToNext(level: Int): Int = 15 + 3 * level + level * level / 4

// 升到這一級為止總共需要的經驗值
fun totalExpForLevel(level: Int): Int = (1 until level).sumOf { expToNext(it) }

// 轉職：超過 10 級才能轉，轉了會降 10 級
const val CLASS_CHANGE_MIN_LEVEL = 11
const val CLASS_CHANGE_PENALTY = 10

// 轉職後的總經驗值：降 10 級，這一級已經累積的經驗值保留（不超過新等級的上限）
fun expAfterClassChange(totalExp: Int): Int {
    val info = levelInfo(totalExp)
    val level = (info.level - CLASS_CHANGE_PENALTY).coerceAtLeast(1)
    return totalExpForLevel(level) + info.current.coerceAtMost(expToNext(level) - 1)
}

// 用總經驗值算出等級，以及這一級已經累積多少、還要多少
data class LevelInfo(val level: Int, val current: Int, val needed: Int)

fun levelInfo(totalExp: Int): LevelInfo {
    var level = 1
    var left = totalExp
    while (level < MAX_LEVEL && left >= expToNext(level)) {
        left -= expToNext(level)
        level++
    }
    return LevelInfo(level, left, expToNext(level))
}

fun Race.stats(level: Int) = Stats(
    base.str + growth.str * (level - 1),
    base.vit + growth.vit * (level - 1),
    base.intel + growth.intel * (level - 1),
    base.luk + growth.luk * (level - 1),
)

// 每 10 級換一個稱號
fun Race.title(level: Int): String = titles[(level / 10).coerceAtMost(titles.size - 1)]

// 下一個稱號在幾級，已經是最後一個就回傳 null
fun Race.nextTitleLevel(level: Int): Int? {
    val next = (level / 10 + 1) * 10
    return if (level / 10 + 1 < titles.size) next else null
}
