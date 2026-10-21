package com.wordsnap.app.ui

// 人物的種族、等級、能力和稱號

// 擊倒怪物的招式，每個種族不同
enum class Finisher(val label: String) {
    Slash("揮劍斬擊"),
    Magic("星光魔法"),
    Bomb("偷丟炸彈"),
    Smash("巨斧重擊"),
    Claw("貓爪連擊"),
    Fire("火焰吐息"),
    Laser("雷射光束"),
}

// 能力：力量、體力、智慧、運氣 = 基礎值 + 每升一級的成長值 x (等級 - 1)
enum class Race(
    val label: String,
    val base: Stats,
    val growth: Stats,
    val finisher: Finisher,
    // Lv 1、10、20、30、40、50 時的稱號
    val titles: List<String>,
) {
    Human("人類", Stats(5, 5, 5, 5), Stats(2, 2, 2, 2), Finisher.Slash,
        listOf("見習劍士", "劍士", "騎士", "聖騎士", "劍聖", "傳說勇者")),
    Elf("精靈", Stats(3, 4, 8, 5), Stats(1, 2, 3, 2), Finisher.Magic,
        listOf("森林學徒", "精靈法師", "元素術士", "大魔導師", "賢者", "星辰守護者")),
    Goblin("哥布林", Stats(4, 4, 3, 9), Stats(2, 1, 1, 3), Finisher.Bomb,
        listOf("小嘍囉", "盜賊", "突襲隊長", "哥布林頭目", "哥布林王", "混沌之王")),
    Dwarf("矮人", Stats(8, 8, 3, 3), Stats(3, 3, 1, 1), Finisher.Smash,
        listOf("礦工", "斧手", "鐵匠戰士", "重裝衛士", "山之領主", "大地之王")),
    Cat("貓族", Stats(4, 4, 5, 7), Stats(2, 1, 2, 3), Finisher.Claw,
        listOf("流浪貓", "獵人", "弓箭手", "神射手", "暗影獵手", "貓神")),
    Dragon("龍族", Stats(9, 7, 4, 2), Stats(3, 2, 2, 1), Finisher.Fire,
        listOf("幼龍", "飛龍", "火焰龍", "古龍", "龍王", "神龍")),
    Machine("機械族", Stats(6, 9, 6, 1), Stats(2, 3, 2, 1), Finisher.Laser,
        listOf("試作機", "戰鬥機兵", "強化機兵", "鋼鐵守衛", "超級機甲", "終極機神")),
}

data class Stats(val str: Int, val vit: Int, val intel: Int, val luk: Int)

val Avatar.race: Race
    get() = when (this) {
        Avatar.Man, Avatar.Woman -> Race.Human
        Avatar.ElfMan, Avatar.ElfWoman -> Race.Elf
        Avatar.Goblin -> Race.Goblin
        Avatar.Dwarf -> Race.Dwarf
        Avatar.Cat -> Race.Cat
        Avatar.Dragon -> Race.Dragon
        Avatar.Robot -> Race.Machine
    }

const val MAX_LEVEL = 99

// 從 Lv n 升到 n+1 需要的經驗值，越高級需要越多
fun expToNext(level: Int): Int = 100 + 20 * (level - 1)

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
