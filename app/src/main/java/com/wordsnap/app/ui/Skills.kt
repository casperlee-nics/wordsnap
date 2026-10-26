package com.wordsnap.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed

// 人物技能：每個職業在 LV10、20、30、40、50、60 各學會一招（技能表見專案的「全職業技能表」文件）
// 「1 格」= 怪物血條 1 個單字量；補血的 amount 是補回幾次答錯的血

// 一招可以同時有好幾個效果，例如聖光盾擊：這回合答錯不扣血，同時怪物扣 1 格
data class SkillEffect(
    // 怪物扣幾格血
    val damage: Int = 0,
    // 補回幾次答錯的血
    val heal: Int = 0,
    // 幾回合內答錯不扣血
    val shield: Int = 0,
    // 幾回合內怪物不能動，答錯不會被攻擊
    val stun: Int = 0,
    // 連續幾題直接給答案，算答對
    val reveal: Int = 0,
    // 跳過這題，移到最後再考
    val skip: Boolean = false,
    // 粉絲加油：幾回合內答對，怪物多扣 1 格
    val cheer: Int = 0,
    // 攻擊力加成幾回合；每次答對多打幾個半格（x1.5 = 1、x2 = 2）
    val boost: Int = 0,
    val boostHalves: Int = 0,
    // 幾回合內答錯，怪物反而打自己
    val confuse: Int = 0,
    // 幾回合內怪物攻擊力減半
    val weaken: Int = 0,
    // 幾回合內怪物被迷住，答錯反而補 1 次血
    val charm: Int = 0,
    // 幾回合內答錯，怪物打不到你
    val dodge: Int = 0,
    // 幾回合內題目先給前一半字母
    val hint: Int = 0,
    // 時間暫停幾回合：答錯不被攻擊，還能馬上重答一次
    val timeStop: Int = 0,
    // 到戰鬥結束，受到的傷害減少幾 %
    val guard: Int = 0,
    // 到戰鬥結束，答錯有一半機率不被攻擊
    val cute: Boolean = false,
    // 自己扣幾次答錯的血（戰艦打偏）
    val selfHit: Int = 0,
    // 背景換成這個場景，直到戰鬥結束
    val scene: ReviewScene? = null,
    // 背景音樂換掉，直到戰鬥結束
    val music: SkillMusic? = null,
    // 附身在別的職業身上（玩家自己選）
    val possess: Boolean = false,
)

// 持續到戰鬥結束的效果用這個當回合數
const val UNTIL_END = 999

// 技能換的背景音樂：古典名曲（隨機一首）、月光奏鳴曲第三樂章、酒醉的水手
enum class SkillMusic { Classical, Moonlight, Sailor }

// effect 是 null 的招式還沒做好，技能表裡看得到但不能用
enum class Skill(
    val race: Race,
    val level: Int,
    @StringRes private val nameRes: Int,
    @StringRes private val descRes: Int,
    val effect: SkillEffect?,
) {
    FanCheer(Race.Hero, 10, R.string.skill_fan_cheer, R.string.skill_fan_cheer_desc, SkillEffect(cheer = 2)),
    LoveBento(Race.Hero, 20, R.string.skill_love_bento, R.string.skill_love_bento_desc, SkillEffect(heal = 1)),
    HolyShieldBash(Race.Hero, 30, R.string.skill_holy_shield_bash, R.string.skill_holy_shield_bash_desc, SkillEffect(shield = 1, damage = 1)),
    RoyalSoldiers(Race.Hero, 40, R.string.skill_royal_soldiers, R.string.skill_royal_soldiers_desc, SkillEffect(damage = 1, reveal = 1, heal = 1)),
    CrossSlash(Race.Hero, 50, R.string.skill_cross_slash, R.string.skill_cross_slash_desc, SkillEffect(damage = 3)),
    OdinWrath(Race.Hero, 60, R.string.skill_odin_wrath, R.string.skill_odin_wrath_desc, SkillEffect(damage = 5, music = SkillMusic.Moonlight)),
    MindFocus(Race.Mage, 10, R.string.skill_mind_focus, R.string.skill_mind_focus_desc, SkillEffect(boost = 3, boostHalves = 1)),
    CrystalPeek(Race.Mage, 20, R.string.skill_crystal_peek, R.string.skill_crystal_peek_desc, SkillEffect(reveal = 1)),
    GurgleCurse(Race.Mage, 30, R.string.skill_gurgle_curse, R.string.skill_gurgle_curse_desc, SkillEffect(confuse = 2)),
    HealBubble(Race.Mage, 40, R.string.skill_heal_bubble, R.string.skill_heal_bubble_desc, SkillEffect(heal = 2)),
    AncientTome(Race.Mage, 50, R.string.skill_ancient_tome, R.string.skill_ancient_tome_desc, SkillEffect(hint = 4)),
    MeteorCall(Race.Mage, 60, R.string.skill_meteor_call, R.string.skill_meteor_call_desc, SkillEffect(damage = 5, scene = ReviewScene.Moon)),
    SwishArrow(Race.Archer, 10, R.string.skill_swish_arrow, R.string.skill_swish_arrow_desc, SkillEffect(damage = 1)),
    ElfSpring(Race.Archer, 20, R.string.skill_elf_spring, R.string.skill_elf_spring_desc, SkillEffect(heal = 1)),
    AreWeElves(Race.Archer, 30, R.string.skill_are_we_elves, R.string.skill_are_we_elves_desc, SkillEffect(damage = 2, stun = 1)),
    ForestBlessing(Race.Archer, 40, R.string.skill_forest_blessing, R.string.skill_forest_blessing_desc, SkillEffect(heal = 2, damage = 1)),
    ElfEye(Race.Archer, 50, R.string.skill_elf_eye, R.string.skill_elf_eye_desc, SkillEffect(hint = 4)),
    ElvenUprising(Race.Archer, 60, R.string.skill_elven_uprising, R.string.skill_elven_uprising_desc, SkillEffect(boost = UNTIL_END, boostHalves = 1, scene = ReviewScene.DeepForest)),
    BushHide(Race.Goblin, 10, R.string.skill_bush_hide, R.string.skill_bush_hide_desc, SkillEffect(skip = true)),
    BoulderThrow(Race.Goblin, 20, R.string.skill_boulder_throw, R.string.skill_boulder_throw_desc, SkillEffect(damage = 1, stun = 2)),
    CheatSheet(Race.Goblin, 30, R.string.skill_cheat_sheet, R.string.skill_cheat_sheet_desc, SkillEffect(reveal = 2)),
    BombChain(Race.Goblin, 40, R.string.skill_bomb_chain, R.string.skill_bomb_chain_desc, SkillEffect(damage = 2)),
    StolenWatch(Race.Goblin, 50, R.string.skill_stolen_watch, R.string.skill_stolen_watch_desc, SkillEffect(timeStop = 3)),
    GoblinKing(Race.Goblin, 60, R.string.skill_goblin_king, R.string.skill_goblin_king_desc, SkillEffect(guard = 25, boost = UNTIL_END, boostHalves = 1)),
    PotLid(Race.Dwarf, 10, R.string.skill_pot_lid, R.string.skill_pot_lid_desc, SkillEffect(shield = 1)),
    HammerToss(Race.Dwarf, 20, R.string.skill_hammer_toss, R.string.skill_hammer_toss_desc, SkillEffect(damage = 1)),
    Orichalcum(Race.Dwarf, 30, R.string.skill_orichalcum, R.string.skill_orichalcum_desc, SkillEffect(boost = 5, boostHalves = 1)),
    MushroomStew(Race.Dwarf, 40, R.string.skill_mushroom_stew, R.string.skill_mushroom_stew_desc, SkillEffect(heal = 2)),
    HotSpring(Race.Dwarf, 50, R.string.skill_hot_spring, R.string.skill_hot_spring_desc, SkillEffect(heal = 2, boost = 2, boostHalves = 1)),
    UltimateArmor(Race.Dwarf, 60, R.string.skill_ultimate_armor, R.string.skill_ultimate_armor_desc, SkillEffect(guard = 50)),
    TeaserWand(Race.Cat, 10, R.string.skill_teaser_wand, R.string.skill_teaser_wand_desc, SkillEffect(stun = 1)),
    LickFur(Race.Cat, 20, R.string.skill_lick_fur, R.string.skill_lick_fur_desc, SkillEffect(heal = 1)),
    CatStampede(Race.Cat, 30, R.string.skill_cat_stampede, R.string.skill_cat_stampede_desc, SkillEffect(damage = 2)),
    CatOrchestra(Race.Cat, 40, R.string.skill_cat_orchestra, R.string.skill_cat_orchestra_desc, SkillEffect(charm = 3, music = SkillMusic.Classical)),
    PurrHeal(Race.Cat, 50, R.string.skill_purr_heal, R.string.skill_purr_heal_desc, SkillEffect(heal = 3)),
    CutestCat(Race.Cat, 60, R.string.skill_cutest_cat, R.string.skill_cutest_cat_desc, SkillEffect(cute = true)),
    SneezeFire(Race.Dragon, 10, R.string.skill_sneeze_fire, R.string.skill_sneeze_fire_desc, SkillEffect(damage = 1)),
    DragonRoar(Race.Dragon, 20, R.string.skill_dragon_roar, R.string.skill_dragon_roar_desc, SkillEffect(stun = 2)),
    DragonFlight(Race.Dragon, 30, R.string.skill_dragon_flight, R.string.skill_dragon_flight_desc, SkillEffect(damage = 2)),
    DragonHowl(Race.Dragon, 40, R.string.skill_dragon_howl, R.string.skill_dragon_howl_desc, SkillEffect(boost = 3, boostHalves = 2)),
    MeteorClaw(Race.Dragon, 50, R.string.skill_meteor_claw, R.string.skill_meteor_claw_desc, SkillEffect(damage = 3)),
    FireWhisper(Race.Dragon, 60, R.string.skill_fire_whisper, R.string.skill_fire_whisper_desc, SkillEffect(damage = 5, scene = ReviewScene.Lava)),
    BarrierOn(Race.Machine, 10, R.string.skill_barrier_on, R.string.skill_barrier_on_desc, SkillEffect(shield = 1)),
    DataSearch(Race.Machine, 20, R.string.skill_data_search, R.string.skill_data_search_desc, SkillEffect(reveal = 1)),
    RocketLaunch(Race.Machine, 30, R.string.skill_rocket_launch, R.string.skill_rocket_launch_desc, SkillEffect(damage = 2)),
    Overclock(Race.Machine, 40, R.string.skill_overclock, R.string.skill_overclock_desc, SkillEffect(boost = 3, boostHalves = 2)),
    Battleship(Race.Machine, 50, R.string.skill_battleship, R.string.skill_battleship_desc, SkillEffect(selfHit = 1, damage = 3, scene = ReviewScene.Ship, music = SkillMusic.Sailor)),
    FullArsenal(Race.Machine, 60, R.string.skill_full_arsenal, R.string.skill_full_arsenal_desc, SkillEffect(damage = 5, scene = ReviewScene.RuinedCity)),
    Bleh(Race.Demon, 10, R.string.skill_bleh, R.string.skill_bleh_desc, SkillEffect(stun = 1)),
    DarkMist(Race.Demon, 20, R.string.skill_dark_mist, R.string.skill_dark_mist_desc, SkillEffect(dodge = 2)),
    PlagueDoctor(Race.Demon, 30, R.string.skill_plague_doctor, R.string.skill_plague_doctor_desc, SkillEffect(weaken = 5)),
    DemonEye(Race.Demon, 40, R.string.skill_demon_eye, R.string.skill_demon_eye_desc, SkillEffect(confuse = 2)),
    EnergySip(Race.Demon, 50, R.string.skill_energy_sip, R.string.skill_energy_sip_desc, SkillEffect(damage = 2, heal = 2)),
    Possession(Race.Demon, 60, R.string.skill_possession, R.string.skill_possession_desc, SkillEffect(possess = true));
    val label: String get() = tr(nameRes)
    val description: String get() = tr(descRes)
    val ready: Boolean get() = effect != null

    companion object {
        fun of(race: Race): List<Skill> = entries.filter { it.race == race }
    }
}

// 每場戰鬥能用幾次技能：LV10–29 一次、LV30–49 兩次、LV50 以上三次；同一招每場只能用一次
fun skillUsesPerBattle(level: Int): Int = when {
    level >= 50 -> 3
    level >= 30 -> 2
    level >= 10 -> 1
    else -> 0
}

// 人物技能視窗：列出這個職業已經學會的招式（學會的等級、名稱、效果），還沒學到的不顯示
// 戰鬥中傳 onUse，點一下就使用；單字簿的「人物技能」只能看不能用
@Composable
fun SkillDialog(
    skills: List<Skill>,
    level: Int,
    onDismiss: () -> Unit,
    used: List<Skill> = emptyList(),
    usesLeft: Int = 0,
    canUse: (Skill) -> Boolean = { false },
    onUse: ((Skill) -> Unit)? = null,
) {
    PixelWindowDialog(title = tr(R.string.skill_title), onDismiss = onDismiss) {
        when {
            level < skills.first().level -> Text(tr(R.string.skill_none_yet), style = MaterialTheme.typography.bodyMedium.readable())
            onUse != null -> Text(tr(R.string.skill_uses_left, usesLeft), style = MaterialTheme.typography.titleSmall, color = PixelRed)
        }
        skills.filter { it.level <= level }.forEachIndexed { index, skill ->
            val learned = true
            val enabled = if (onUse != null) canUse(skill) else learned && skill.ready
            if (index > 0) Box(Modifier.fillMaxWidth().height(2.dp).background(SkillDivider))
            Row(
                Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f)
                    .clickable(enabled = enabled && onUse != null) { onUse?.invoke(skill) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 左邊的小格子寫學會的等級
                Box(
                    Modifier.size(48.dp).border(2.dp, PixelInk).background(if (learned) SkillSlot else SkillSlotLocked),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("LV${skill.level}", style = MaterialTheme.typography.labelMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(skill.label, style = MaterialTheme.typography.titleMedium)
                    Text(skill.description, style = MaterialTheme.typography.bodySmall.readable())
                    val status = when {
                        !skill.ready -> tr(R.string.skill_coming)
                        skill in used -> tr(R.string.skill_used)
                        else -> null
                    }
                    if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = PixelRed)
                }
            }
        }
        Text(tr(R.string.skill_rule), style = MaterialTheme.typography.bodySmall.readable())
    }
}

// 惡魔的附身：選一個別的職業（不能選自己），附身到戰鬥結束
@Composable
fun PossessionDialog(self: Race, onPick: (Race) -> Unit, onDismiss: () -> Unit) {
    PixelWindowDialog(title = tr(R.string.possession_title), onDismiss = onDismiss) {
        Text(tr(R.string.possession_hint), style = MaterialTheme.typography.bodySmall.readable())
        Race.entries.filter { it != self }.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { race ->
                    Column(
                        Modifier.weight(1f).border(2.dp, PixelInk).background(SkillSlot)
                            .clickable { onPick(race) }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PixelAvatar(avatarOf(race), size = 44.dp)
                        Text(avatarOf(race).label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

// 職業的預設頭像（附身時換成這個樣子）
fun avatarOf(race: Race): Avatar = Avatar.entries.first { it.race == race }

private val SkillSlot = Color(0xFFF8E0A0)
private val SkillSlotLocked = Color(0xFFC8C0B0)
private val SkillDivider = Color(0x40202030)

// 戰鬥的技能按鈕：閃亮的星星
val IconSkill = listOf(
    "................",
    ".......KK.......",
    "......KYYK......",
    "......KYYK..K...",
    ".KKKKKKYYKKKKKK.",
    ".KYYYYYYYYYYYYK.",
    "..KYYYYWYYYYYK..",
    "...KYYYWWYYYK...",
    "....KYYYYYYK....",
    "....KYYYYYYK....",
    "...KYYYKKYYYK...",
    "...KYYK..KYYK...",
    "..KYYK....KYYK..",
    "..KYK......KYK..",
    "..KK........KK..",
    "................",
)
