package com.wordsnap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordsnap.app.data.Profile
import com.wordsnap.app.reminder.ReviewReminder
import com.wordsnap.app.ui.BattleScreen
import com.wordsnap.app.ui.GameFrame
import com.wordsnap.app.ui.ListenScreen
import com.wordsnap.app.ui.NicknameScreen
import com.wordsnap.app.ui.ReviewScreen
import com.wordsnap.app.ui.SaveSlotScreen
import com.wordsnap.app.ui.ScanScreen
import com.wordsnap.app.ui.TitleScreen
import com.wordsnap.app.ui.WordListScreen
import com.wordsnap.app.ui.WordViewModel
import com.wordsnap.app.ui.theme.PixelTheme

// App 的畫面：主畫面、新遊戲、讀取、刪除，以及進入遊戲後的單字本、拍照、複習、朗讀、戰鬥
private enum class Screen { Title, NewGame, Load, Delete, List, Scan, Review, Listen, Battle }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 讓遊戲機外框鋪到狀態列和導覽列後面
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ReviewReminder.schedule(this)

        setContent {
            PixelTheme {
                GameFrame {
                    val vm: WordViewModel = viewModel()
                    val profiles by vm.profiles.collectAsState()
                    // 還在讀資料：只畫背景，避免閃一下
                    if (profiles == null) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                    } else {
                        MainScreens(vm, profiles!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreens(vm: WordViewModel, profiles: List<Profile>) {
    var screen by rememberSaveable { mutableStateOf(Screen.Title) }
    var battleBookId by rememberSaveable { mutableLongStateOf(-1L) }
    val backToList = { screen = Screen.List }
    val backToTitle = { screen = Screen.Title }
    val allWords by vm.allWords.collectAsState()
    val books by vm.books.collectAsState()
    val selectedBookId by vm.selectedBookId.collectAsState()

    val inGame = screen >= Screen.List

    when (screen) {
        Screen.Title -> Unit
        Screen.NewGame, Screen.Load, Screen.Delete, Screen.List -> BackHandler(onBack = backToTitle)
        Screen.Scan, Screen.Review, Screen.Listen, Screen.Battle -> BackHandler(onBack = backToList)
    }
    if (inGame) AskNotificationPermission()

    when (screen) {
        Screen.Title -> TitleScreen(
            hasSaves = profiles.isNotEmpty(),
            onNewGame = { screen = Screen.NewGame },
            onLoadGame = { screen = Screen.Load },
            onDeleteSave = { screen = Screen.Delete },
        )
        Screen.NewGame -> NicknameScreen(onStart = { name, avatar ->
            vm.createProfile(name, avatar) { screen = Screen.List }
        })
        Screen.Load -> SaveSlotScreen(
            deleting = false,
            profiles = profiles,
            onPick = { profile ->
                vm.selectProfile(profile.id)
                screen = Screen.List
            },
            onBack = backToTitle,
        )
        Screen.Delete -> SaveSlotScreen(
            deleting = true,
            profiles = profiles,
            onPick = vm::deleteProfile,
            onBack = backToTitle,
        )
        Screen.List -> WordListScreen(
            vm,
            onScan = { screen = Screen.Scan },
            onReview = { screen = Screen.Review },
            onListen = { screen = Screen.Listen },
            onBattle = { bookId ->
                battleBookId = bookId
                screen = Screen.Battle
            },
            onHome = backToTitle,
        )
        Screen.Scan -> ScanScreen(
            existing = allWords.map { it.text.lowercase() }.toSet(),
            books = books,
            defaultBookId = selectedBookId,
            onCreateBook = vm::createBook,
            onAdd = { picked, bookId ->
                vm.addAll(picked, bookId)
                screen = Screen.List
            },
            onBack = backToList,
        )
        Screen.Review -> ReviewScreen(vm, onListen = { screen = Screen.Listen }, onBack = backToList)
        Screen.Listen -> ListenScreen(vm, onBack = backToList)
        Screen.Battle -> BattleScreen(vm, battleBookId, onBack = backToList)
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
