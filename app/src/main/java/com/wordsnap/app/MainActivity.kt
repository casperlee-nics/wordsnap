package com.wordsnap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordsnap.app.reminder.ReviewReminder
import com.wordsnap.app.ui.NicknameScreen
import com.wordsnap.app.ui.ReviewScreen
import com.wordsnap.app.ui.ScanScreen
import com.wordsnap.app.ui.WordListScreen
import com.wordsnap.app.ui.WordViewModel
import com.wordsnap.app.ui.theme.PixelTheme

// App 的三個畫面
private enum class Screen { List, Scan, Review }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReviewReminder.schedule(this)

        setContent {
            PixelTheme {
                val vm: WordViewModel = viewModel()
                val profiles by vm.profiles.collectAsState()

                when {
                    // 還在讀資料：只畫背景，避免閃一下暱稱畫面
                    profiles == null -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                    profiles!!.isEmpty() -> NicknameScreen(onStart = vm::createProfile)
                    else -> {
                        AskNotificationPermission()
                        MainScreens(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreens(vm: WordViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.List) }
    val backToList = { screen = Screen.List }
    val words by vm.words.collectAsState()

    if (screen != Screen.List) BackHandler(onBack = backToList)

    when (screen) {
        Screen.List -> WordListScreen(
            vm,
            onScan = { screen = Screen.Scan },
            onReview = { screen = Screen.Review },
        )
        Screen.Scan -> ScanScreen(
            existing = words.map { it.text.lowercase() }.toSet(),
            onAdd = { picked ->
                vm.addAll(picked)
                screen = Screen.List
            },
            onBack = backToList,
        )
        Screen.Review -> ReviewScreen(vm, onBack = backToList)
    }
}

// Android 13 以上要先問使用者能不能發通知（拒絕也沒關係，只是不會收到提醒）
@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
