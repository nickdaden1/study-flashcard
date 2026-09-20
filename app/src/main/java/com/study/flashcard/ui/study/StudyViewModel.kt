package com.study.flashcard.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.repo.StudyRepository
import com.study.flashcard.data.study.Sm2Engine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StudyUiState(
    val loading: Boolean = true,
    /** Hàng đợi thẻ trong buổi học (AGAIN quay lại cuối hàng). */
    val queue: List<CardEntity> = emptyList(),
    val index: Int = 0,
    val flipped: Boolean = false,
    val againCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val finished: Boolean = false
) {
    val current: CardEntity? get() = queue.getOrNull(index)
}

/** Phiên học flashcard: lật thẻ → tự đánh → SM-2 xếp lại. */
class StudyViewModel(
    private val deckId: Long,
    private val repo: StudyRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(StudyUiState())
    val ui: StateFlow<StudyUiState> = _ui

    init {
        viewModelScope.launch {
            val due = repo.getDueCards(deckId).shuffled()
            _ui.value = StudyUiState(loading = false, queue = due, finished = due.isEmpty())
        }
    }

    fun flip() {
        _ui.value = _ui.value.copy(flipped = true)
    }

    fun grade(grade: Sm2Engine.Grade) {
        val s = _ui.value
        val card = s.current ?: return
        viewModelScope.launch {
            val updated = repo.gradeCard(card, grade)
            val again = s.againCount + if (grade == Sm2Engine.Grade.AGAIN) 1 else 0
            val good = s.goodCount + if (grade == Sm2Engine.Grade.GOOD) 1 else 0
            val easy = s.easyCount + if (grade == Sm2Engine.Grade.EASY) 1 else 0
            if (grade == Sm2Engine.Grade.AGAIN) {
                // Quay lại cuối hàng để ôn lại ngay trong buổi.
                val rest = s.queue.filterIndexed { i, _ -> i != s.index } + updated
                _ui.value = s.copy(
                    queue = rest,
                    flipped = false,
                    againCount = again,
                    goodCount = good,
                    easyCount = easy
                )
            } else {
                val next = s.index + 1
                _ui.value = s.copy(
                    index = next,
                    flipped = false,
                    againCount = again,
                    goodCount = good,
                    easyCount = easy,
                    finished = next >= s.queue.size
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val deckId: Long, private val repo: StudyRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StudyViewModel(deckId, repo) as T
    }
}
