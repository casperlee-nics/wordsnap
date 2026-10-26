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

enum class SkillKind {
    // 怪物扣 amount 格血（stunTurns > 0 時順便讓怪物昏迷）
    Damage,
    // 補回 amount 次答錯的血
    Heal,
    // turns 回合內答錯不扣血
    Shield,
    // turns 回合內怪物不能動，答錯不會被攻擊
    Stun,
    // 直接顯示這題答案，算答對
    Reveal,
    // 跳過這題，移到最後再考
    Skip,
    // 粉絲加油：turns 回合內答對，怪物多扣 1 格
    Cheer,
    // 集中：turns 回合內答對，攻擊力 x1.5
    Focus,
}

data class SkillEffect(val kind: SkillKind, val amount: Int = 0, val turns: Int = 0, val stunTurns: Int = 0)

// effect 是 null 的招式還沒做好（v0.96 以後開放），技能表裡看得到但不能用
enum class Skill(
    val race: Race,
    val level: Int,
    @StringRes private val nameRes: Int,
    @StringRes private val descRes: Int,
    val effect: SkillEffect?,
) {
    FanCheer(Race.Hero, 10, R.string.skill_fan_cheer, R.string.skill_fan_cheer_desc, SkillEffect(SkillKind.Cheer, turns = 2)),
    LoveBento(Race.Hero, 20, R.string.skill_love_bento, R.string.skill_love_bento_desc, SkillEffect(SkillKind.Heal, amount = 1)),
    RoyalSoldiers(Race.Hero, 30, R.string.skill_royal_soldiers, R.string.skill_royal_soldiers_desc, null),
    HolyShieldBash(Race.Hero, 40, R.string.skill_holy_shield_bash, R.string.skill_holy_shield_bash_desc, null),
    CrossSlash(Race.Hero, 50, R.string.skill_cross_slash, R.string.skill_cross_slash_desc, null),
    OdinWrath(Race.Hero, 60, R.string.skill_odin_wrath, R.string.skill_odin_wrath_desc, null),
    MindFocus(Race.Mage, 10, R.string.skill_mind_focus, R.string.skill_mind_focus_desc, SkillEffect(SkillKind.Focus, turns = 3)),
    CrystalPeek(Race.Mage, 20, R.string.skill_crystal_peek, R.string.skill_crystal_peek_desc, SkillEffect(SkillKind.Reveal)),
    GurgleCurse(Race.Mage, 30, R.string.skill_gurgle_curse, R.string.skill_gurgle_curse_desc, null),
    HealBubble(Race.Mage, 40, R.string.skill_heal_bubble, R.string.skill_heal_bubble_desc, null),
    AncientTome(Race.Mage, 50, R.string.skill_ancient_tome, R.string.skill_ancient_tome_desc, null),
    MeteorCall(Race.Mage, 60, R.string.skill_meteor_call, R.string.skill_meteor_call_desc, null),
    SwishArrow(Race.Archer, 10, R.string.skill_swish_arrow, R.string.skill_swish_arrow_desc, SkillEffect(SkillKind.Damage, amount = 1)),
    ElfSpring(Race.Archer, 20, R.string.skill_elf_spring, R.string.skill_elf_spring_desc, SkillEffect(SkillKind.Heal, amount = 1)),
    AreWeElves(Race.Archer, 30, R.string.skill_are_we_elves, R.string.skill_are_we_elves_desc, null),
    LeafShieldArrow(Race.Archer, 40, R.string.skill_leaf_shield_arrow, R.string.skill_leaf_shield_arrow_desc, null),
    ElfEye(Race.Archer, 50, R.string.skill_elf_eye, R.string.skill_elf_eye_desc, null),
    ElvenUprising(Race.Archer, 60, R.string.skill_elven_uprising, R.string.skill_elven_uprising_desc, null),
    BushHide(Race.Goblin, 10, R.string.skill_bush_hide, R.string.skill_bush_hide_desc, SkillEffect(SkillKind.Skip)),
    BoulderThrow(Race.Goblin, 20, R.string.skill_boulder_throw, R.string.skill_boulder_throw_desc, SkillEffect(SkillKind.Damage, amount = 1, stunTurns = 2)),
    CheatSheet(Race.Goblin, 30, R.string.skill_cheat_sheet, R.string.skill_cheat_sheet_desc, null),
    BombChain(Race.Goblin, 40, R.string.skill_bomb_chain, R.string.skill_bomb_chain_desc, null),
    StolenWatch(Race.Goblin, 50, R.string.skill_stolen_watch, R.string.skill_stolen_watch_desc, null),
    GoblinKing(Race.Goblin, 60, R.string.skill_goblin_king, R.string.skill_goblin_king_desc, null),
    PotLid(Race.Dwarf, 10, R.string.skill_pot_lid, R.string.skill_pot_lid_desc, SkillEffect(SkillKind.Shield, turns = 1)),
    HammerToss(Race.Dwarf, 20, R.string.skill_hammer_toss, R.string.skill_hammer_toss_desc, SkillEffect(SkillKind.Damage, amount = 1)),
    Orichalcum(Race.Dwarf, 30, R.string.skill_orichalcum, R.string.skill_orichalcum_desc, null),
    MushroomStew(Race.Dwarf, 40, R.string.skill_mushroom_stew, R.string.skill_mushroom_stew_desc, null),
    HotSpring(Race.Dwarf, 50, R.string.skill_hot_spring, R.string.skill_hot_spring_desc, null),
    UltimateArmor(Race.Dwarf, 60, R.string.skill_ultimate_armor, R.string.skill_ultimate_armor_desc, null),
    TeaserWand(Race.Cat, 10, R.string.skill_teaser_wand, R.string.skill_teaser_wand_desc, SkillEffect(SkillKind.Stun, turns = 1)),
    LickFur(Race.Cat, 20, R.string.skill_lick_fur, R.string.skill_lick_fur_desc, SkillEffect(SkillKind.Heal, amount = 1)),
    CatStampede(Race.Cat, 30, R.string.skill_cat_stampede, R.string.skill_cat_stampede_desc, null),
    CatOrchestra(Race.Cat, 40, R.string.skill_cat_orchestra, R.string.skill_cat_orchestra_desc, null),
    PurrHeal(Race.Cat, 50, R.string.skill_purr_heal, R.string.skill_purr_heal_desc, null),
    CutestCat(Race.Cat, 60, R.string.skill_cutest_cat, R.string.skill_cutest_cat_desc, null),
    SneezeFire(Race.Dragon, 10, R.string.skill_sneeze_fire, R.string.skill_sneeze_fire_desc, SkillEffect(SkillKind.Damage, amount = 1)),
    DragonRoar(Race.Dragon, 20, R.string.skill_dragon_roar, R.string.skill_dragon_roar_desc, SkillEffect(SkillKind.Stun, turns = 2)),
    TailSwing(Race.Dragon, 30, R.string.skill_tail_swing, R.string.skill_tail_swing_desc, null),
    DragonHowl(Race.Dragon, 40, R.string.skill_dragon_howl, R.string.skill_dragon_howl_desc, null),
    MeteorClaw(Race.Dragon, 50, R.string.skill_meteor_claw, R.string.skill_meteor_claw_desc, null),
    FireWhisper(Race.Dragon, 60, R.string.skill_fire_whisper, R.string.skill_fire_whisper_desc, null),
    BarrierOn(Race.Machine, 10, R.string.skill_barrier_on, R.string.skill_barrier_on_desc, SkillEffect(SkillKind.Shield, turns = 1)),
    DataSearch(Race.Machine, 20, R.string.skill_data_search, R.string.skill_data_search_desc, SkillEffect(SkillKind.Reveal)),
    RocketLaunch(Race.Machine, 30, R.string.skill_rocket_launch, R.string.skill_rocket_launch_desc, null),
    Overclock(Race.Machine, 40, R.string.skill_overclock, R.string.skill_overclock_desc, null),
    Battleship(Race.Machine, 50, R.string.skill_battleship, R.string.skill_battleship_desc, null),
    FullArsenal(Race.Machine, 60, R.string.skill_full_arsenal, R.string.skill_full_arsenal_desc, null),
    Bleh(Race.Demon, 10, R.string.skill_bleh, R.string.skill_bleh_desc, SkillEffect(SkillKind.Stun, turns = 1)),
    DarkMist(Race.Demon, 20, R.string.skill_dark_mist, R.string.skill_dark_mist_desc, SkillEffect(SkillKind.Skip)),
    PlagueDoctor(Race.Demon, 30, R.string.skill_plague_doctor, R.string.skill_plague_doctor_desc, null),
    DemonEye(Race.Demon, 40, R.string.skill_demon_eye, R.string.skill_demon_eye_desc, null),
    EnergySip(Race.Demon, 50, R.string.skill_energy_sip, R.string.skill_energy_sip_desc, null),
    Possession(Race.Demon, 60, R.string.skill_possession, R.string.skill_possession_desc, null);
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

// 人物技能視窗：列出這個職業的 6 招（學會的等級、名稱、效果）
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
        skills.forEachIndexed { index, skill ->
            val learned = skill.level <= level
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
                        !learned -> tr(R.string.skill_learn_at, skill.level)
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
