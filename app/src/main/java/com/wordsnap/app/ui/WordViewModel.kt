package com.wordsnap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordsnap.app.data.AppDatabase
import com.wordsnap.app.data.Word
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 畫面和資料庫之間的橋樑
class WordViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).wordDao()

    val words: StateFlow<List<Word>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(text: String, meaning: String) {
        viewModelScope.launch {
            dao.insert(Word(text = text.trim(), meaning = meaning.trim()))
        }
    }

    fun delete(word: Word) {
        viewModelScope.launch { dao.delete(word) }
    }
}
