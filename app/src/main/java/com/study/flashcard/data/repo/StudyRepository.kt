package com.study.flashcard.data.repo

import com.study.flashcard.data.ai.GeminiClient
import com.study.flashcard.data.ai.GradingResult
import com.study.flashcard.data.local.AttemptDao
import com.study.flashcard.data.local.AttemptEntity
import com.study.flashcard.data.local.CardDao
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.study.Sm2Engine
import kotlinx.coroutines.flow.Flow

/**
 * Phiên học + thi thử: lấy thẻ đến hạn, chấm SM-2, lưu lịch sử, chấm tự luận.
 */
class StudyRepository(
    private val cardDao: CardDao,
    private val attemptDao: AttemptDao,
    private val gemini: GeminiClient
) {

    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cardDao.observeByDeck(deckId)

    suspend fun getDueCards(deckId: Long, now: Long = System.currentTimeMillis()): List<CardEntity> =
        cardDao.getDue(deckId, now)

    /** Chấm 1 thẻ theo SM-2, lưu ngay, trả về thẻ đã cập nhật. */
    suspend fun gradeCard(
        card: CardEntity,
        grade: Sm2Engine.Grade,
        now: Long = System.currentTimeMillis()
    ): CardEntity {
        val updated = Sm2Engine.review(card, grade, now)
        cardDao.update(updated)
        return updated
    }

    suspend fun recordAttempt(
        deckId: Long,
        startedAt: Long,
        finishedAt: Long,
        totalQuestions: Int,
        correctMcq: Int,
        essayScoreAvg: Float?,
        durationSec: Long
    ): Long = attemptDao.insert(
        AttemptEntity(
            deckId = deckId,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalQuestions = totalQuestions,
            correctMcq = correctMcq,
            essayScoreAvg = essayScoreAvg,
            durationSec = durationSec
        )
    )

    fun observeAttempts(deckId: Long): Flow<List<AttemptEntity>> =
        attemptDao.observeByDeck(deckId)

    /** AI chấm 1 câu tự luận (tham khảo — user tự chốt điểm cuối ở UI). */
    suspend fun gradeEssay(
        question: String,
        modelAnswer: String,
        userAnswer: String,
        apiKey: String
    ): GradingResult = gemini.gradeEssay(question, modelAnswer, userAnswer, apiKey)
}
