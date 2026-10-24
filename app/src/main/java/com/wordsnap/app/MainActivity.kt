package com.wordsnap.app

import android.Manifest
import android.content.Context
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
import com.wordsnap.app.i18n.Strings
import com.wordsnap.app.i18n.withAppLanguage
import com.wordsnap.app.reminder.ReviewReminder
import com.wordsnap.app.ui.BattleScreen
import com.wordsnap.app.ui.CompanySplash
import com.wordsnap.app.ui.FitToScreen
import com.wordsnap.app.ui.GameFrame
import com.wordsnap.app.ui.ListenScreen
import com.wordsnap.app.ui.NicknameScreen
import com.wordsnap.app.ui.ReviewScreen
import com.wordsnap.app.ui.SaveSlotScreen
import com.wordsnap.app.ui.ScanScreen
import com.wordsnap.app.ui.ScanSource
import com.wordsnap.app.ui.TitleScreen
import com.wordsnap.app.ui.WordListScreen
import com.wordsnap.app.ui.WordViewModel
import com.wordsnap.app.ui.loadTextSize
import com.wordsnap.app.ui.theme.PixelTheme

// App 的畫面：主畫面、新遊戲、讀取、刪除，以及進入遊戲後的單字本、拍照、複習、朗讀、戰鬥
private enum class Screen { Title, NewGame, Load, Delete, List, Scan, Review, Listen, Battle }

class MainActivity : ComponentActivity() {
    // 依遊戲設定的語言（還沒設定就跟著手機系統）顯示所有文字
    override fun attachBaseContext(newBase: Context) {
        Strings.init(newBase)
        super.attachBaseContext(newBase.withAppLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 讓遊戲機外框鋪到狀態列和導覽列後面
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ReviewReminder.schedule(this)
        loadTextSize(this)

        setContent {
            // 小螢幕的手機把整個畫面等比例縮小
            PixelTheme {
                FitToScreen {
                    // 先播公司開場動畫，跑完才進遊戲（轉螢幕不會重播）
                    var splashDone by rememberSaveable { mutableStateOf(false) }
                    if (!splashDone) {
                        CompanySplash(onDone = { splashDone = true })
                    } else GameFrame {
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
}

@Composable
private fun MainScreens(vm: WordViewModel, profiles: List<Profile>) {
    var screen by rememberSaveable { mutableStateOf(Screen.Title) }
    var battleBookId by rememberSaveable { mutableLongStateOf(-1L) }
    var scanSource by rememberSaveable { mutableStateOf(ScanSource.Camera) }
    val backToList = { screen = Screen.List }
    // 從拍照／相簿／古書返回時，回到單字簿並重新打開加單字的四個選項
    var reopenAddChoice by rememberSaveable { mutableStateOf(false) }
    val backFromScan = {
        reopenAddChoice = true
        screen = Screen.List
    }
    val backToTitle = { screen = Screen.Title }
    val allWords by vm.allWords.collectAsState()
    val books by vm.books.collectAsState()
    val selectedBookId by vm.selectedBookId.collectAsState()

    val inGame = screen >= Screen.List

    when (screen) {
        Screen.Title -> Unit
        Screen.NewGame, Screen.Load, Screen.Delete, Screen.List -> BackHandler(onBack = backToTitle)
        Screen.Scan -> BackHandler(onBack = backFromScan)
        Screen.Review, Screen.Listen, Screen.Battle -> BackHandler(onBack = backToList)
    }
    if (inGame) AskNotificationPermission()

    when (screen) {
        Screen.Title -> TitleScreen(
            hasSaves = profiles.isNotEmpty(),
            onNewGame = { screen = Screen.NewGame },
            onLoadGame = { screen = Screen.Load },
            onManageSaves = { screen = Screen.Delete },
        )
        Screen.NewGame -> NicknameScreen(onStart = { name, avatar ->
            vm.createProfile(name, avatar) { screen = Screen.List }
        })
        Screen.Load -> SaveSlotScreen(
            managing = false,
            profiles = profiles,
            onPick = { profile ->
                vm.selectProfile(profile.id)
                screen = Screen.List
            },
            onBack = backToTitle,
        )
        Screen.Delete -> SaveSlotScreen(
            managing = true,
            profiles = profiles,
            onPick = {},
            onBack = backToTitle,
            onRename = vm::renameProfile,
            onDelete = vm::deleteProfile,
        )
        Screen.List -> WordListScreen(
            vm,
            onScan = { source ->
                scanSource = source
                screen = Screen.Scan
            },
            onReview = { screen = Screen.Review },
            onListen = { screen = Screen.Listen },
            onBattle = { bookId ->
                battleBookId = bookId
                screen = Screen.Battle
            },
            onHome = backToTitle,
            startWithAddChoice = reopenAddChoice,
            onAddChoiceShown = { reopenAddChoice = false },
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
            onBack = backFromScan,
            source = scanSource,
        )
        Screen.Review -> ReviewScreen(vm, onBack = backToList)
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
