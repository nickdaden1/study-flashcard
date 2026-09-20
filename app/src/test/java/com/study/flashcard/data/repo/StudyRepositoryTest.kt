package com.study.flashcard.data.repo

import com.study.flashcard.data.ai.GeminiApi
import com.study.flashcard.data.ai.GeminiClient
import com.study.flashcard.data.ai.GeminiRequest
import com.study.flashcard.data.ai.GeminiResponse
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.study.Sm2Engine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyRepositoryTest {

    private fun repo(cardDao: FakeCardDao = FakeCardDao(), attemptDao: FakeAttemptDao = FakeAttemptDao()) =
        StudyRepository(
            cardDao,
            attemptDao,
            GeminiClient(
                api = object : GeminiApi {
                    override suspend fun generateContent(
                        model: String,
                        apiKey: String,
                        body: GeminiRequest
                    ): GeminiResponse = GeminiResponse(emptyList())
                },
                delayBetweenChunksMs = 0
            )
        ) to attemptDao

    private fun card(id: Long = 1, dueAt: Long = 0) = CardEntity(
        id = id, deckId = 1, type = "MCQ",
        question = "Q", answer = "A",
        choicesJson = """["A","B","C","D"]""",
        dueAt = dueAt
    )

    @Test
    fun `cham the ap dung SM2 va luu ngay`() = runTest {
        val cardDao = FakeCardDao()
        val (r, _) = repo(cardDao)
        cardDao.insertAll(listOf(card(dueAt = 1000L)))

        val updated = r.gradeCard(card(dueAt = 1000L), Sm2Engine.Grade.GOOD, now = 5_000L)

        assertEquals(1, updated.repetitions)
        assertEquals(1, updated.intervalDays)
        assertEquals("GOOD", updated.lastGrade)
        // Đã lưu vào DAO: không còn thẻ đến hạn tại now=5000.
        assertEquals(0, r.getDueCards(1, 5_000L).size)
    }

    @Test
    fun `lay the den han dung thu tu`() = runTest {
        val cardDao = FakeCardDao()
        val (r, _) = repo(cardDao)
        cardDao.insertAll(
            listOf(
                card(id = 1, dueAt = 3000L),
                card(id = 2, dueAt = 1000L),
                card(id = 3, dueAt = 9_000L)
            )
        )
        val due = r.getDueCards(1, now = 5_000L)
        assertEquals(listOf(2L, 1L), due.map { it.id })
    }

    @Test
    fun `luu lich su thi thu`() = runTest {
        val (r, attemptDao) = repo()
        val id = r.recordAttempt(
            deckId = 1, startedAt = 1000L, finishedAt = 2000L,
            totalQuestions = 10, correctMcq = 7,
            essayScoreAvg = 8.0f, durationSec = 600L
        )
        assertEquals(1L, id)
        assertEquals(1, attemptDao.all().size)
        assertEquals(7, attemptDao.all().single().correctMcq)
    }
}
