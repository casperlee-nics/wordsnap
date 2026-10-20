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
import com.wordsnap.app.ui.ScanScreen
import com.wordsnap.app.ui.WordListScreen
import com.wordsnap.app.ui.WordViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: WordViewModel = viewModel()
                var scanning by rememberSaveable { mutableStateOf(false) }

                if (scanning) {
                    BackHandler { scanning = false }
                    ScanScreen(
                        onAdd = { words ->
                            vm.addAll(words)
                            scanning = false
                        },
                        onBack = { scanning = false },
                    )
                } else {
                    WordListScreen(vm, onScan = { scanning = true })
                }
            }
        }
    }
}
