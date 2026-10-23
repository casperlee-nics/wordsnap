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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.data.Profile
import com.wordsnap.app.ui.theme.PixelBorder
import com.wordsnap.app.ui.theme.PixelRed
import com.wordsnap.app.ui.theme.PixelShape
import com.wordsnap.app.ui.theme.pixelTopBarColors

// 讀取遊戲（點存檔進入）或刪除冒險者（點存檔刪除）共用的存檔清單
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSlotScreen(
    deleting: Boolean,
    profiles: List<Profile>,
    onPick: (Profile) -> Unit,
    onBack: () -> Unit,
) {
    var confirm by remember { mutableStateOf<Profile?>(null) }
    val context = LocalContext.current
    val musicOn = remember { isMusicOn(context) }
    // 讀取遊戲時播放專屬音樂
    LoopingMusic(R.raw.load_bgm, enabled = musicOn && !deleting, volume = 0.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (deleting) "刪除冒險者" else "讀取遊戲") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = pixelTopBarColors(),
            )
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("還沒有存檔，請先開始新遊戲")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    Surface(
                        shape = PixelShape,
                        border = PixelBorder,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (deleting) confirm = profile else onPick(profile)
                        },
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PixelAvatar(Avatar.from(profile.avatar), size = 56.dp, tier = avatarTier(levelInfo(profile.exp).level))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                                Text(Avatar.from(profile.avatar).label, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(if (deleting) "刪除" else "→", color = if (deleting) PixelRed else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    confirm?.let { profile ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("刪除冒險者 ${profile.name}？") },
            text = { Text("${profile.name} 的所有單字簿、單字、道具和複習進度都會刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    onPick(profile)
                    confirm = null
                }) { Text("刪除", color = PixelRed) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("取消") } },
        )
    }
}
