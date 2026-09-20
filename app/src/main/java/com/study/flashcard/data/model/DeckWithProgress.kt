package com.study.flashcard.data.model

import com.study.flashcard.data.local.DeckEntity

/** Deck kèm số liệu tiến độ để màn Home hiển thị. */
data class DeckWithProgress(
    val deck: DeckEntity,
    val total: Int,
    val mastered: Int,
    val due: Int
) {
    val percent: Int get() = calcMasteredPercent(total, mastered)
}

/** % đã thuộc. mastered vượt total thì chặn ở 100. */
fun calcMasteredPercent(total: Int, mastered: Int): Int {
    if (total <= 0) return 0
    return (mastered.coerceIn(0, total) * 100 / total)
}
