package com.study.flashcard.data.model

import com.study.flashcard.data.local.DeckEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalcTest {

    @Test
    fun `chua co the nao thi 0 phan tram`() {
        assertEquals(0, calcMasteredPercent(0, 0))
    }

    @Test
    fun `thuoc mot nua thi 50 phan tram`() {
        assertEquals(50, calcMasteredPercent(10, 5))
    }

    @Test
    fun `thuoc vuot tong thi chan o 100`() {
        assertEquals(100, calcMasteredPercent(10, 12))
    }

    @Test
    fun `DeckWithProgress uy thac tinh phan tram`() {
        val item = DeckWithProgress(DeckEntity(id = 1, name = "Toán"), total = 4, mastered = 3, due = 1)
        assertEquals(75, item.percent)
    }
}
