package com.wordsnap.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wordsnap.app.R
import com.wordsnap.app.i18n.tr

// 遊戲說明的內容：一段一個主題，最後一段是公司介紹
private class HelpSection(@StringRes val title: Int, @StringRes val body: Int)

private val HelpSections = listOf(
    HelpSection(
        R.string.help_start_title,
        R.string.help_start,
    ),
    HelpSection(
        R.string.help_collect_title,
        R.string.help_collect,
    ),
    HelpSection(
        R.string.help_review_title,
        R.string.help_review,
    ),
    HelpSection(
        R.string.help_battle_title,
        R.string.help_battle,
    ),
    HelpSection(
        R.string.help_level_title,
        R.string.help_level,
    ),
    HelpSection(
        R.string.help_boss_title,
        R.string.help_boss,
    ),
    HelpSection(
        R.string.help_tips_title,
        R.string.help_tips,
    ),
    HelpSection(
        R.string.help_free_title,
        R.string.help_free,
    ),
    HelpSection(
        R.string.help_about_title,
        R.string.help_about,
    ),
)

// 主畫面右下角問號打開的說明
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    PixelWindowDialog(title = tr(R.string.help_title), onDismiss = onDismiss) {
        HelpSections.forEach { section ->
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("▶ ${tr(section.title)}", style = MaterialTheme.typography.titleMedium)
                Text(tr(section.body), style = MaterialTheme.typography.bodyMedium.readable())
            }
        }
        // 最後面寫上目前的軟體版本
        val context = LocalContext.current
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        }
        Text(tr(R.string.help_version, version), style = MaterialTheme.typography.bodySmall)
    }
}
