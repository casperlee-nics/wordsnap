package com.wordsnap.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.ui.theme.PixelGold
import com.wordsnap.app.ui.theme.PixelInk
import com.wordsnap.app.ui.theme.PixelShape

// 第一次開啟：輸入暱稱
@Composable
fun NicknameScreen(onStart: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
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
                onClick = { onStart(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("開始冒險 →") }
        }
    }
}
