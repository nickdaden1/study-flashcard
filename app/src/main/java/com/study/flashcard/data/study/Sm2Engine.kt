package com.study.flashcard.data.study

import com.study.flashcard.data.local.CardEntity
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Thuật toán lặp lại ngắt quãng SM-2 (theo spec §5.3).
 * Thuần Kotlin để unit test được trên CI.
 *
 * - Chưa nhớ (AGAIN): reset về 0, ôn lại ngay trong buổi (dueAt = now).
 * - Nhớ (GOOD): repetitions+1, khoảng lặp 1 → 6 → round(trước × độ dễ) ngày.
 * - Thuộc (EASY): như GOOD + độ dễ tăng 0.15 (chặn 1.3–3.0).
 */
object Sm2Engine {

    enum class Grade { AGAIN, GOOD, EASY }

    const val MIN_EASINESS = 1.3f
    const val MAX_EASINESS = 3.0f
    const val EASY_BONUS = 0.15f

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun review(
        card: CardEntity,
        grade: Grade,
        now: Long = System.currentTimeMillis()
    ): CardEntity = when (grade) {
        Grade.AGAIN -> card.copy(
            repetitions = 0,
            intervalDays = 0,
            dueAt = now,
            lastGrade = Grade.AGAIN.name
        )
        Grade.GOOD -> {
            val interval = nextInterval(card, card.easiness)
            card.copy(
                repetitions = card.repetitions + 1,
                intervalDays = interval,
                dueAt = now + interval * DAY_MS,
                lastGrade = Grade.GOOD.name
            )
        }
        Grade.EASY -> {
            val easiness = (card.easiness + EASY_BONUS).coerceIn(MIN_EASINESS, MAX_EASINESS)
            val interval = nextInterval(card, easiness)
            card.copy(
                easiness = easiness,
                repetitions = card.repetitions + 1,
                intervalDays = interval,
                dueAt = now + interval * DAY_MS,
                lastGrade = Grade.EASY.name
            )
        }
    }

    private fun nextInterval(card: CardEntity, easiness: Float): Int = when (card.repetitions) {
        0 -> 1
        1 -> 6
        else -> max(1, (card.intervalDays * easiness).roundToInt())
    }
}
