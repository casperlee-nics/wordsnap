package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelWhite

// 第一次開啟：先輸入暱稱，再選一個像素人物
@Composable
fun NicknameScreen(onStart: (String, Avatar) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pickingAvatar by remember { mutableStateOf(false) }
    var avatar by remember { mutableStateOf(Avatar.Hero) }
    // 輸入暱稱和選角色時播放有希望感的音樂
    val context = LocalContext.current
    val musicOn = remember { isMusicOn(context) }
    LoopingMusic(R.raw.hope_bgm, enabled = musicOn, volume = 0.5f)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            // 可以上下捲動，鍵盤跳出來時輸入框會自動捲到看得見的位置
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // 從上面開始排，輸入框和說明文字比較靠上
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        ) {
            if (!pickingAvatar) {
                // 上面是纏著藤蔓的木牌標題，下面是攤開的魔法書
                WoodSignTitle("像素單字")
                MagicBook(Modifier.width(100.dp))
                // 在羊皮書卷上寫暱稱，右邊蓋章確認（按鈕在旁邊，鍵盤跳出來也不會被擋住）
                ScrollNameInput(
                    value = name,
                    onValueChange = { name = it.take(12) },
                    placeholder = "寫下你的暱稱",
                    onConfirm = { pickingAvatar = true },
                )
                Text("冒險者，請輸入你的暱稱", style = MaterialTheme.typography.titleMedium)
            } else {
                Text("${name.trim()}，選一個你的角色", style = MaterialTheme.typography.titleMedium)
                // 左邊是選中的人物，右邊的古書卷寫著這個職業的故事
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.pixelFrame(PixelWhite, unit = 2.dp).padding(14.dp)) {
                        PixelAvatar(avatar, size = 96.dp)
                    }
                    StoryScroll(
                        title = avatar.race.label,
                        story = avatar.story,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
                AvatarPicker(selected = avatar, onSelect = { avatar = it })
                AdventureButton("開始冒險", onClick = { onStart(name, avatar) })
            }
        }
    }
}

// 像素人物，一排放不下就往下排，選中的有粗框
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPicker(selected: Avatar, onSelect: (Avatar) -> Unit, showLabels: Boolean = true, exclude: Avatar? = null) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar.entries.filter { it != exclude }.forEach { item ->
            val isSelected = item == selected
            Column(
                Modifier
                    .width(72.dp)
                    .then(
                        if (isSelected) {
                            Modifier.pixelFrame(PixelWhite, unit = 2.dp)
                        } else {
                            Modifier.pixelFrame(MaterialTheme.colorScheme.background, accent = PixelGray, unit = 2.dp)
                        },
                    )
                    .clickable { onSelect(item) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelAvatar(item, size = 48.dp)
                if (showLabels) Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private val PixelGray = Color(0xFFB8A888)
