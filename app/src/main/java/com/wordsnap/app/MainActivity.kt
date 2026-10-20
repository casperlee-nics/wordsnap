package com.wordsnap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordsnap.app.ui.ReviewScreen
import com.wordsnap.app.ui.ScanScreen
import com.wordsnap.app.ui.WordListScreen
import com.wordsnap.app.ui.WordViewModel

// App 的三個畫面
private enum class Screen { List, Scan, Review }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: WordViewModel = viewModel()
                var screen by rememberSaveable { mutableStateOf(Screen.List) }
                val backToList = { screen = Screen.List }

                if (screen != Screen.List) BackHandler(onBack = backToList)

                when (screen) {
                    Screen.List -> WordListScreen(
                        vm,
                        onScan = { screen = Screen.Scan },
                        onReview = { screen = Screen.Review },
                    )
                    Screen.Scan -> ScanScreen(
                        onAdd = { words ->
                            vm.addAll(words)
                            screen = Screen.List
                        },
                        onBack = backToList,
                    )
                    Screen.Review -> ReviewScreen(vm, onBack = backToList)
                }
            }
        }
    }
}
