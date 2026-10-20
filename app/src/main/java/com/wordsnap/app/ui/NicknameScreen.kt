package com.wordsnap.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelShape

// 第一次開啟：先輸入暱稱，再選一個像素人物
@Composable
fun NicknameScreen(onStart: (String, Avatar) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pickingAvatar by remember { mutableStateOf(false) }
    var avatar by remember { mutableStateOf(Avatar.Man) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            if (!pickingAvatar) {
                Box(
                    Modifier.size(144.dp).border(3.dp, PixelInk, PixelShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = PixelGold, shape = PixelShape, modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null)
                    }
                }
                Text("像素單字", style = MaterialTheme.typography.displaySmall)
                Text("冒險者，請輸入你的暱稱", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(12) },
                    label = { Text("暱稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { pickingAvatar = true },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("下一步 →") }
            } else {
                PixelAvatar(avatar, size = 120.dp)
                Text("${name.trim()}，選一個你的角色", style = MaterialTheme.typography.titleMedium)
                AvatarPicker(selected = avatar, onSelect = { avatar = it })
                Button(onClick = { onStart(name, avatar) }, modifier = Modifier.fillMaxWidth()) {
                    Text("開始冒險 →")
                }
                TextButton(onClick = { pickingAvatar = false }) { Text("← 改暱稱") }
            }
        }
    }
}

// 8 個像素人物，排成 4 x 2，選中的有粗框
@Composable
fun AvatarPicker(selected: Avatar, onSelect: (Avatar) -> Unit, showLabels: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Avatar.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val isSelected = item == selected
                    Column(
                        Modifier
                            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) PixelInk else PixelGray, PixelShape)
                            .clickable { onSelect(item) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PixelAvatar(item, size = 48.dp)
                        if (showLabels) Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

private val PixelGray = Color(0xFFB8A888)
