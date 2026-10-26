package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.Profile
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelRed

// 讀取遊戲（點存檔進入）或記錄管理（右邊有改名字、刪除紀錄的按鈕）共用的存檔清單
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSlotScreen(
    managing: Boolean,
    profiles: List<Profile>,
    onPick: (Profile) -> Unit,
    onBack: () -> Unit,
    onRename: (Profile, String, (String?) -> Unit) -> Unit = { _, _, _ -> },
    onDelete: (Profile) -> Unit = {},
) {
    var confirm by remember { mutableStateOf<Profile?>(null) }
    var renaming by remember { mutableStateOf<Profile?>(null) }
    // 讀取遊戲和記錄管理各有自己的音樂
    LoopingMusic(if (managing) R.raw.manage_bgm else R.raw.load_bgm, volume = 0.5f)

    Scaffold(
        topBar = {
            PixelHeader(
                tr(if (managing) R.string.menu_manage else R.string.menu_continue),
                onBack,
                theme = HeaderTheme.Leather,
            )
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(tr(R.string.saves_empty))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    // 和人物能力標題同一種羊皮紙紋路，外面木紋細框
                    Box(
                        Modifier.fillMaxWidth().parchmentCard().clickable(enabled = !managing) { onPick(profile) },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            // 左邊：人物、名字和職業
                            PixelAvatar(Avatar.from(profile.avatar), size = 56.dp, tier = avatarTier(levelInfo(profile.exp).level))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(profile.name, color = PixelInk, style = MaterialTheme.typography.titleLarge)
                                Text(Avatar.from(profile.avatar).label, color = PixelInk, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (managing) {
                                // 右邊：改名字（鉛筆）、刪除紀錄（垃圾桶）
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PixelIconPlain(IconPencil, tr(R.string.rename), onClick = { renaming = profile })
                                    PixelIconPlain(IconTrash, tr(R.string.saves_delete), onClick = { confirm = profile })
                                }
                            } else {
                                // 和新冒險一樣的蓋章按鈕
                                SealButton(enabled = true, onClick = { onPick(profile) }, label = tr(R.string.saves_go))
                            }
                        }
                    }
                }
            }
        }
    }

    renaming?.let { profile ->
        RenameDialog(
            profile = profile,
            onDismiss = { renaming = null },
            onRename = { name, onError ->
                onRename(profile, name) { error -> if (error == null) renaming = null else onError(error) }
            },
        )
    }

    confirm?.let { profile ->
        PixelAlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(tr(R.string.saves_delete_title, profile.name)) },
            text = { Text(tr(R.string.saves_delete_message, profile.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(profile)
                    confirm = null
                }) { Text(tr(R.string.delete), color = PixelRed) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RenameDialog(profile: Profile, onDismiss: () -> Unit, onRename: (String, (String) -> Unit) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var error by remember { mutableStateOf<String?>(null) }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.rename)) },
        text = {
            // 和新冒險一樣：寫在羊皮書卷上，右邊蓋章確認
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScrollNameInput(
                    value = name,
                    onValueChange = {
                        name = it.take(12)
                        error = null
                    },
                    placeholder = tr(R.string.rename_hint),
                    onConfirm = { onRename(name.trim()) { error = it } },
                )
                error?.let { Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr(R.string.cancel)) } },
    )
}
