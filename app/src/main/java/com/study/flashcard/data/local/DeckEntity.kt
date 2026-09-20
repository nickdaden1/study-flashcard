package com.study.flashcard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Một bộ đề = một môn/đề cương đã import. */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Denormalized để Home hiện nhanh, Repository đồng bộ sau mỗi lần thêm/xóa thẻ. */
    val cardCount: Int = 0
)
