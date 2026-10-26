package com.wordsnap.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.AppLanguage
import com.wordsnap.app.i18n.appLanguage
import com.wordsnap.app.i18n.saveAppLanguage
import com.wordsnap.app.i18n.tr
import com.wordsnap.app.translate.DictionaryPacks
import com.wordsnap.app.ui.theme.PixelInk
import kotlinx.coroutines.launch

// 主畫面的「遊戲設定」：語言、音量、文字大小、每場考試的題數
// 換語言時先安裝那個語言的字典，再重新開啟畫面，所有文字都換成新的語言
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf(appLanguage(context)) }
    var volume by remember { mutableStateOf(musicLevel(context)) }
    var textSize by remember { mutableStateOf(loadTextSize(context)) }
    var battleSize by remember { mutableStateOf(loadBattleSize(context)) }
    // 正在安裝字典時的進度（null 代表沒有在安裝）
    var installing by remember { mutableStateOf<Float?>(null) }
    // 字典包找不到時，先問一聲再切換
    var missingPack by remember { mutableStateOf<AppLanguage?>(null) }

    fun switchTo(target: AppLanguage) {
        saveAppLanguage(context, target)
        context.findActivity()?.recreate()
    }

    PixelWindowDialog(title = tr(R.string.settings_title), onDismiss = { if (installing == null) onDismiss() }) {
        SettingLabel(tr(R.string.settings_language))
        OptionRow(
            options = AppLanguage.entries,
            chosen = language,
            label = { it.nativeName },
            enabled = installing == null,
            onPick = { target ->
                if (target == language) return@OptionRow
                language = target
                if (DictionaryPacks.isInstalled(context, target)) {
                    switchTo(target)
                } else {
                    installing = 0f
                    scope.launch {
                        val ok = DictionaryPacks.install(context, target) { installing = it }
                        installing = null
                        if (ok) switchTo(target) else missingPack = target
                    }
                }
            },
        )
        installing?.let { progress ->
            Text(tr(R.string.settings_installing, (progress * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }

        SettingLabel(tr(R.string.settings_volume))
        OptionRow(
            options = MusicLevel.entries,
            chosen = volume,
            label = { tr(it.labelRes) },
            onPick = {
                volume = it
                setMusicLevel(context, it)
            },
        )
        Text(tr(R.string.settings_volume_hint), style = MaterialTheme.typography.bodySmall)

        SettingLabel(tr(R.string.settings_text_size))
        OptionRow(
            options = TextSize.entries,
            chosen = textSize,
            label = { tr(it.labelRes) },
            onPick = {
                textSize = it
                setTextSize(context, it)
            },
        )
        Text(tr(R.string.settings_text_hint), style = MaterialTheme.typography.bodySmall.readable())

        SettingLabel(tr(R.string.settings_battle_size))
        OptionRow(
            options = BattleSizes,
            chosen = battleSize,
            label = { tr(R.string.settings_battle_size_option, it) },
            onPick = {
                battleSize = it
                saveBattleSize(context, it)
            },
        )
        Text(tr(R.string.settings_battle_size_hint), style = MaterialTheme.typography.bodySmall.readable())
    }

    missingPack?.let { target ->
        PixelAlertDialog(
            onDismissRequest = { },
            title = { Text(target.nativeName) },
            text = { Text(tr(R.string.settings_pack_missing)) },
            confirmButton = { TextButton(onClick = { missingPack = null; switchTo(target) }) { Text(tr(R.string.confirm)) } },
            dismissButton = {
                TextButton(onClick = {
                    missingPack = null
                    language = appLanguage(context)
                }) { Text(tr(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text("▶ $text", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

// 一排選項格子，選中的那格是淺色木頭底加黑框（和原本主畫面的音樂選項一樣）
@Composable
private fun <T> OptionRow(
    options: List<T>,
    chosen: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    enabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val isChosen = option == chosen
            Box(
                Modifier
                    .weight(1f)
                    .border(2.dp, if (isChosen) PixelInk else OptionLine)
                    .background(if (isChosen) OptionChosen else Color.Transparent)
                    .clickable(enabled = enabled) { onPick(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                FitText(label(option), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

private val OptionChosen = Color(0xFFE8C894)
private val OptionLine = Color(0x40202030)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
