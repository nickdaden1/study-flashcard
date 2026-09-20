package com.study.flashcard.data.repo

import app.cash.turbine.test
import com.study.flashcard.data.ai.CardDraft
import com.study.flashcard.data.ai.GeminiApi
import com.study.flashcard.data.ai.GeminiClient
import com.study.flashcard.data.ai.GeminiRequest
import com.study.flashcard.data.ai.GeminiResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun stubGemini() = GeminiClient(
    api = object : GeminiApi {
        override suspend fun generateContent(
            model: String,
            apiKey: String,
            body: GeminiRequest
        ): GeminiResponse = GeminiResponse(emptyList())
    },
    delayBetweenChunksMs = 0
)

private fun repo(
    deckDao: FakeDeckDao = FakeDeckDao(),
    cardDao: FakeCardDao = FakeCardDao()
) = Triple(
    DeckRepository(deckDao, cardDao, FakeAttemptDao(), stubGemini()) { throw UnsupportedOperationException() },
    deckDao,
    cardDao
)

class DeckRepositoryTest {

    @Test
    fun `luu deck kem the va dem so the`() = runTest {
        val (r, deckDao, cardDao) = repo()
        val id = r.saveDeckWithCards(
            "Triết",
            "triet.docx",
            listOf(
                CardDraft("MCQ", "Q1", listOf("A", "B", "C", "D"), "A"),
                CardDraft("essay", "Q2", null, "Đáp án mẫu")
            )
        )
        assertEquals(1L, id)
        assertEquals(2, deckDao.getById(id)!!.cardCount)
        assertEquals(2, cardDao.countTotal(id))
    }

    @Test
    fun `tien do tinh dung so the thuoc va den han`() = runTest {
        val (r, _, cardDao) = repo()
        val now = 10_000_000L
        val day = 86_400_000L
        val id = r.saveDeckWithCards(
            "Toán", null,
            listOf(
                CardDraft("MCQ", "Q1", listOf("A", "B", "C", "D"), "A"),
                CardDraft("MCQ", "Q2", listOf("A", "B", "C", "D"), "B"),
                CardDraft("ESSAY", "Q3", null, "Mẫu")
            )
        )
        // 1 thẻ đã thuộc (repetitions>0, chưa đến hạn), còn lại đến hạn.
        val due = cardDao.getDue(id, Long.MAX_VALUE)
        cardDao.update(due[0].copy(repetitions = 2, dueAt = now + day))

        r.observeDecksWithProgress(nowProvider = { now }).test {
            val item = awaitItem().single()
            assertEquals(3, item.total)
            assertEquals(1, item.mastered)
            assertEquals(2, item.due)
            assertEquals(33, item.percent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ten trong thi bao loi tieng Viet`() = runTest {
        val (r, _, _) = repo()
        // assertThrows không gọi được hàm suspend nên dùng try/catch trực tiếp.
        try {
            r.createDeck("   ")
            throw AssertionError("phải ném IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Tên bộ đề không được để trống.", e.message)
        }
    }

    @Test
    fun `xoa the thi cap nhat lai so the`() = runTest {
        val (r, deckDao, cardDao) = repo()
        val id = r.saveDeckWithCards(
            "Lý", null,
            listOf(CardDraft("MCQ", "Q1", listOf("A", "B", "C", "D"), "A"))
        )
        val card = cardDao.getDue(id, Long.MAX_VALUE).single()
        r.deleteCard(card)
        assertEquals(0, deckDao.getById(id)!!.cardCount)
    }
}
