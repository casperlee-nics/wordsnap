package com.wordsnap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

// 遊戲說明的內容：一段一個主題，之後要加公司介紹，在清單最後加一段就好
private class HelpSection(val title: String, val body: String)

private val HelpSections = listOf(
    HelpSection(
        "開始冒險",
        "按「新遊戲」輸入暱稱、選一個職業。每個冒險者有自己的單字簿、等級和道具，" +
            "之後用「讀取遊戲」接著玩；「記錄管理」可以改名字或刪除紀錄。",
    ),
    HelpSection(
        "收集單字怪物",
        "在單字簿按「加單字」：手動輸入、相機找出隱藏怪物、相簿新增怪物，或讀取古書（PDF）上的怪物。" +
            "辨識出來的單字和片語點一下就能收進單字簿，中文解釋會自動填好。",
    ),
    HelpSection(
        "複習與朗讀",
        "「複習」用翻卡的方式練習，記得就按燈泡、不記得按問號，記住的字會隔幾天再出現。" +
            "「朗讀」會自動依序念出英文和中文解釋。每天晚上 8 點會提醒你複習。",
    ),
    HelpSection(
        "戰鬥",
        "一本單字簿記住 15 個字（或整本都記住）就能戰鬥。聽發音拼出單字，答對攻擊怪物、答錯被打掉 HP。" +
            "最後把怪物的血打光就能打倒牠；HP 歸零會被拖回教堂。每場 5～15 題，答對 2 次的字就不再出題。",
    ),
    HelpSection(
        "等級、轉職與道具",
        "戰鬥拿到經驗值會升級，每 10 級換一個稱號，頭像和必殺技也會變強。" +
            "超過 10 級可以在「人物能力」轉職成別的職業，但會降 10 級。" +
            "戰鬥勝利有機會開到寶箱，道具在戰鬥中從道具袋使用。",
    ),
    HelpSection(
        "魔王",
        "每記住 50 個字會出現一隻中魔王，每記住 100 個字會出現一隻大魔王，打倒魔王可以拿到更多經驗值。",
    ),
    HelpSection(
        "小技巧",
        "主畫面的「♪ 音樂」可以調整背景音樂（關／小／大），不影響念單字的聲音。" +
            "主畫面的城堡藏著小彩蛋，白天和晚上都點點看吧！",
    ),
)

// 主畫面右下角問號打開的說明
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    PixelWindowDialog(title = "遊戲說明", onDismiss = onDismiss) {
        HelpSections.forEach { section ->
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("▶ ${section.title}", style = MaterialTheme.typography.titleMedium)
                Text(section.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
