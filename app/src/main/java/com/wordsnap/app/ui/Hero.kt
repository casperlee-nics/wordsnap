package com.wordsnap.app.ui

// 人物的職業（種族）、等級、能力、稱號和招式

// 擊倒怪物的招式，每個職業不同；稱號每升一階，招式就換成更強的一招
enum class Finisher(val moves: List<String>) {
    Slash(listOf("斬擊", "十字斬", "旋風斬", "聖光斬", "天空裂斬", "勇者之劍")),
    Magic(listOf("火花術", "冰霜術", "雷電術", "流星術", "星辰爆裂", "終焉魔法")),
    Arrow(listOf("單發箭", "二連箭", "疾風箭", "月光箭雨", "森林之怒", "精靈王之箭")),
    Bomb(listOf("小炸彈", "連環炸彈", "火藥桶", "大爆炸", "毒霧炸彈", "混沌爆破")),
    Smash(listOf("斧頭劈", "重斧擊", "大地裂", "岩崩斧", "山崩地裂", "大地之怒")),
    Claw(listOf("貓貓拳", "貓爪擊", "三連爪", "疾風爪", "貓咪亂舞", "萌萌貓娘拳")),
    Fire(listOf("火花", "火焰吐息", "烈焰", "灼熱風暴", "地獄火", "神龍烈焰")),
    Laser(listOf("電擊", "雷射光束", "雙重雷射", "電漿砲", "超級光束", "終極機神砲")),
    DarkFire(listOf("暗影球", "黑炎彈", "冥獄火", "魔劍閃", "黑暗風暴", "魔界審判"));

    // 第 tier 階的招式名稱（tier 見 avatarTier）
    fun label(tier: Int): String = moves[tier.coerceIn(0, moves.size - 1)]
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
    Hero("人類・勇者", Stats(6, 6, 4, 5), Stats(3, 2, 1, 2), Finisher.Slash,
        listOf("見習勇者", "勇者", "王國勇者", "聖光勇者", "傳說勇者", "救世勇者")),
    Mage("人類・魔法師", Stats(3, 4, 9, 5), Stats(1, 2, 3, 2), Finisher.Magic,
        listOf("魔法學徒", "見習魔法師", "魔法師", "大魔法師", "魔導賢者", "星辰魔女")),
    Archer("精靈・弓手", Stats(5, 4, 5, 7), Stats(2, 1, 2, 3), Finisher.Arrow,
        listOf("森林見習弓手", "精靈弓手", "疾風射手", "月光神射手", "森林守護者", "精靈王")),
    Goblin("哥布林", Stats(4, 4, 3, 9), Stats(2, 1, 1, 3), Finisher.Bomb,
        listOf("小嘍囉", "盜賊", "突襲隊長", "哥布林頭目", "哥布林王", "混沌之王")),
    Dwarf("矮人", Stats(8, 8, 3, 3), Stats(3, 3, 1, 1), Finisher.Smash,
        listOf("礦工", "斧手", "鐵匠戰士", "重裝衛士", "山之領主", "大地之王")),
    Cat("貓族", Stats(4, 4, 5, 7), Stats(2, 1, 2, 3), Finisher.Claw,
        listOf("幼仔貓", "成年貓", "貓族老大", "貓守護神", "傲嬌的家貓", "萌萌貓娘")),
    Dragon("龍族", Stats(9, 7, 4, 2), Stats(3, 2, 2, 1), Finisher.Fire,
        listOf("幼龍", "飛龍", "火焰龍", "古龍", "龍王", "神龍")),
    Machine("機械族", Stats(6, 9, 6, 1), Stats(2, 3, 2, 1), Finisher.Laser,
        listOf("試作機", "戰鬥機兵", "強化機兵", "鋼鐵守衛", "超級機甲", "終極機神")),
    Demon("魔族", Stats(7, 5, 7, 3), Stats(3, 1, 3, 1), Finisher.DarkFire,
        listOf("小惡魔", "魔族戰士", "魔劍士", "魔將軍", "魔王子", "魔界之王")),
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
fun expToNext(level: Int): Int = 80 + 20 * level + 3 * level * level / 2

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
