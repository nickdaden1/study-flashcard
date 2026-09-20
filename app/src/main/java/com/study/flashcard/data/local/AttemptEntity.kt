package com.study.flashcard.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Một lần thi thử — lưu kết quả để xem lại tiến độ. */
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = System.currentTimeMillis(),
    val totalQuestions: Int = 0,
    val correctMcq: Int = 0,
    val essayScoreAvg: Float? = null,
    val durationSec: Long = 0
)
