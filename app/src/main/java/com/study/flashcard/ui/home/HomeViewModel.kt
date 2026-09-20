package com.study.flashcard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.study.flashcard.data.model.DeckWithProgress
import com.study.flashcard.data.repo.DeckRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: DeckRepository) : ViewModel() {

    val decks: StateFlow<List<DeckWithProgress>> =
        repo.observeDecksWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDeck(id: Long) {
        viewModelScope.launch {
            try { repo.deleteDeck(id) } catch (_: Exception) { }
        }
    }

    fun renameDeck(id: Long, name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.renameDeck(id, name) }
            catch (e: Exception) { onError(e.message ?: "Không đổi tên được.") }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val repo: DeckRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repo) as T
    }
}
