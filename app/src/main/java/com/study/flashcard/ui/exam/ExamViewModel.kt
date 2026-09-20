package com.study.flashcard.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.study.flashcard.data.ai.GradingResult
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.repo.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ExamPhase {
    /** Chọn số câu + thời gian trước khi vào thi. */
    data object Setup : ExamPhase
    data object Doing : ExamPhase
    data object Result : ExamPhase
}

data class ExamAnswer(
    /** MCQ: đáp án đã chọn. ESSAY: câu trả lời đã viết. */
    val text: String = "",
    val aiGrading: GradingResult? = null,
    val grading: Boolean = false,
    /** Điểm user tự chốt cho tự luận (mặc định = điểm AI). */
    val essayScore: Float? = null
)

data class ExamUiState(
    val phase: ExamPhase = ExamPhase.Setup,
    val deckName: String = "",
    val questions: List<CardEntity> = emptyList(),
    val index: Int = 0,
    val answers: Map<Long, ExamAnswer> = emptyMap(),
    val secondsLeft: Long = 0,
    val totalSeconds: Long = 0,
    val questionCount: Int = 15,
    val minutes: Int = 20,
    val starting: Boolean = false,
    val error: String? = null,
    val correctMcq: Int = 0,
    val totalMcq: Int = 0,
    val essayAvg: Float? = null
) {
    val current: CardEntity? get() = questions.getOrNull(index)
}

/**
 * Thi thử bấm giờ: MCQ chấm tự động, tự luận AI chấm tham khảo + user tự chốt điểm.
 */
class ExamViewModel(
    private val deckId: Long,
    private val repo: StudyRepository,
    private val apiKeyProvider: () -> String
) : ViewModel() {

    private val _ui = MutableStateFlow(ExamUiState())
    val ui: StateFlow<ExamUiState> = _ui

    private var timer: Job? = null

    fun setCount(n: Int) {
        _ui.value = _ui.value.copy(questionCount = n.coerceIn(5, 50))
    }

    fun setMinutes(m: Int) {
        _ui.value = _ui.value.copy(minutes = m.coerceIn(5, 120))
    }

    fun start() {
        val s = _ui.value
        if (s.starting) return
        viewModelScope.launch {
            _ui.value = s.copy(starting = true, error = null)
            try {
                val all = repo.observeCardsOnce(deckId)
                if (all.isEmpty()) {
                    _ui.value = s.copy(starting = false, error = "Bộ đề chưa có thẻ nào.")
                    return@launch
                }
                val picked = all.shuffled().take(s.questionCount)
                val totalSec = s.minutes * 60L
                _ui.value = s.copy(
                    phase = ExamPhase.Doing,
                    questions = picked,
                    index = 0,
                    answers = emptyMap(),
                    secondsLeft = totalSec,
                    totalSeconds = totalSec,
                    starting = false
                )
                startTimer(totalSec)
            } catch (e: Exception) {
                _ui.value = s.copy(starting = false, error = e.message ?: "Không bắt đầu được.")
            }
        }
    }

    fun answerCurrent(text: String) {
        val s = _ui.value
        val q = s.current ?: return
        _ui.value = s.copy(
            answers = s.answers + (q.id to (s.answers[q.id]?.copy(text = text) ?: ExamAnswer(text)))
        )
    }

    fun next() {
        val s = _ui.value
        if (s.index < s.questions.lastIndex) _ui.value = s.copy(index = s.index + 1)
    }

    fun prev() {
        val s = _ui.value
        if (s.index > 0) _ui.value = s.copy(index = s.index - 1)
    }

    fun jump(i: Int) {
        val s = _ui.value
        if (i in s.questions.indices) _ui.value = s.copy(index = i)
    }

    fun gradeCurrentWithAi() {
        val s = _ui.value
        val q = s.current ?: return
        if (q.type.uppercase() != "ESSAY") return
        val ans = s.answers[q.id] ?: return
        if (ans.text.isBlank() || ans.grading) return
        val key = apiKeyProvider()
        if (key.isBlank()) {
            _ui.value = s.copy(error = "Chưa có API key. Hãy vào Cài đặt để nhập key.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                answers = _ui.value.answers + (q.id to ans.copy(grading = true))
            )
            try {
                val g = repo.gradeEssay(q.question, q.answer, ans.text, key)
                _ui.value = _ui.value.copy(
                    answers = _ui.value.answers + (
                        q.id to ans.copy(grading = false, aiGrading = g, essayScore = g.score)
                        )
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    answers = _ui.value.answers + (q.id to ans.copy(grading = false)),
                    error = e.message ?: "AI chấm thất bại."
                )
            }
        }
    }

    fun setEssayScore(score: Float) {
        val s = _ui.value
        val q = s.current ?: return
        val ans = s.answers[q.id] ?: return
        _ui.value = s.copy(
            answers = s.answers + (q.id to ans.copy(essayScore = score.coerceIn(0f, 10f)))
        )
    }

    fun submit() {
        timer?.cancel()
        val s = _ui.value
        var correct = 0
        var mcqTotal = 0
        val essayScores = mutableListOf<Float>()
        for (q in s.questions) {
            val ans = s.answers[q.id]?.text.orEmpty()
            if (q.type.uppercase() == "MCQ") {
                mcqTotal++
                if (ans == q.answer) correct++
            } else {
                s.answers[q.id]?.essayScore?.let { essayScores += it }
            }
        }
        val avg = essayScores.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val startedAt = System.currentTimeMillis() - (s.totalSeconds - s.secondsLeft) * 1000
        viewModelScope.launch {
            try {
                repo.recordAttempt(
                    deckId = deckId,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    totalQuestions = s.questions.size,
                    correctMcq = correct,
                    essayScoreAvg = avg,
                    durationSec = s.totalSeconds - s.secondsLeft
                )
            } catch (_: Exception) { }
        }
        _ui.value = s.copy(
            phase = ExamPhase.Result,
            correctMcq = correct,
            totalMcq = mcqTotal,
            essayAvg = avg
        )
    }

    private fun startTimer(totalSec: Long) {
        timer?.cancel()
        timer = viewModelScope.launch {
            var left = totalSec
            while (left > 0) {
                delay(1000)
                left--
                _ui.value = _ui.value.copy(secondsLeft = left)
                if (_ui.value.phase != ExamPhase.Doing) return@launch
            }
            // Hết giờ tự nộp.
            if (_ui.value.phase == ExamPhase.Doing) submit()
        }
    }

    override fun onCleared() {
        timer?.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val deckId: Long,
        private val repo: StudyRepository,
        private val apiKeyProvider: () -> String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExamViewModel(deckId, repo, apiKeyProvider) as T
    }
}

/** Lấy snapshot 1 lần (không observe) để bốc đề thi. */
private suspend fun StudyRepository.observeCardsOnce(deckId: Long): List<CardEntity> {
    return observeCards(deckId).first()
}
