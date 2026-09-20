package com.study.flashcard.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Một thẻ học. type = "MCQ" (trắc nghiệm) hoặc "ESSAY" (tự luận). */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId"), Index("dueAt")]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val type: String,
    val question: String,
    /** MCQ: đáp án đúng. ESSAY: đáp án mẫu. */
    val answer: String,
    /** MCQ: 4 lựa chọn dạng JSON list. ESSAY: null. */
    val choicesJson: String? = null,
    // --- Trạng thái SM-2 (Phase 4 dùng để xếp lịch ôn) ---
    val easiness: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = System.currentTimeMillis(),
    /** Lần đánh giá gần nhất: "AGAIN" | "GOOD" | "EASY" */
    val lastGrade: String? = null
)
