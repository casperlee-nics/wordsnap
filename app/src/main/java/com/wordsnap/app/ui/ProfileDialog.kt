package com.wordsnap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.wordsnap.app.data.Profile

// 切換或新增暱稱
@Composable
fun ProfileDialog(
    profiles: List<Profile>,
    currentId: Long?,
    onSelect: (Long) -> Unit,
    onCreate: (String, Avatar) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var newAvatar by remember { mutableStateOf(Avatar.Man) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切換冒險者") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                profiles.forEach { profile ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(profile.id) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = profile.id == currentId, onClick = { onSelect(profile.id) })
                        PixelAvatar(Avatar.from(profile.avatar), size = 32.dp)
                        Text(profile.name, Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(12) },
                    label = { Text("新增暱稱") },
                    singleLine = true,
                )
                if (newName.isNotBlank()) AvatarPicker(selected = newAvatar, onSelect = { newAvatar = it }, showLabels = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(newName, newAvatar) }, enabled = newName.isNotBlank()) { Text("新增") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉") } },
    )
}
