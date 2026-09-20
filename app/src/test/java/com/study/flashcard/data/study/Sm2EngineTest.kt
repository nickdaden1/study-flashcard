package com.study.flashcard.data.study

import com.study.flashcard.data.local.CardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

private const val NOW = 1_000_000L
private const val DAY = 24L * 60 * 60 * 1000

private fun newCard() = CardEntity(
    id = 1, deckId = 1, type = "MCQ",
    question = "Q", answer = "A",
    choicesJson = """["A","B","C","D"]"""
)

class Sm2EngineTest {

    @Test
    fun `Chua nho thi reset va on lai ngay`() {
        val card = newCard().copy(repetitions = 3, intervalDays = 15, dueAt = NOW + 15 * DAY)
        val r = Sm2Engine.review(card, Sm2Engine.Grade.AGAIN, NOW)
        assertEquals(0, r.repetitions)
        assertEquals(0, r.intervalDays)
        assertEquals(NOW, r.dueAt)
        assertEquals("AGAIN", r.lastGrade)
    }

    @Test
    fun `Nho lan dau thi hen 1 ngay`() {
        val r = Sm2Engine.review(newCard(), Sm2Engine.Grade.GOOD, NOW)
        assertEquals(1, r.repetitions)
        assertEquals(1, r.intervalDays)
        assertEquals(NOW + DAY, r.dueAt)
    }

    @Test
    fun `Nho lan hai thi hen 6 ngay`() {
        val card = newCard().copy(repetitions = 1, intervalDays = 1)
        val r = Sm2Engine.review(card, Sm2Engine.Grade.GOOD, NOW)
        assertEquals(2, r.repetitions)
        assertEquals(6, r.intervalDays)
        assertEquals(NOW + 6 * DAY, r.dueAt)
    }

    @Test
    fun `Nho tu lan ba nhan khoang cu voi do de`() {
        val card = newCard().copy(repetitions = 2, intervalDays = 6, easiness = 2.5f)
        val r = Sm2Engine.review(card, Sm2Engine.Grade.GOOD, NOW)
        assertEquals(15, r.intervalDays)
        assertEquals(NOW + 15 * DAY, r.dueAt)
        assertEquals(2.5f, r.easiness, 0.001f)
    }

    @Test
    fun `Thuoc thi tang do de 015`() {
        val r = Sm2Engine.review(newCard(), Sm2Engine.Grade.EASY, NOW)
        assertEquals(2.65f, r.easiness, 0.001f)
        assertEquals(1, r.intervalDays)
        assertEquals("EASY", r.lastGrade)
    }

    @Test
    fun `Do de bi chan tran 30`() {
        val card = newCard().copy(easiness = 2.95f)
        val r = Sm2Engine.review(card, Sm2Engine.Grade.EASY, NOW)
        assertEquals(3.0f, r.easiness, 0.001f)
    }
}
