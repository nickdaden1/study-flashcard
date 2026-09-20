package com.study.flashcard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE deckId = :deckId ORDER BY finishedAt DESC")
    fun observeByDeck(deckId: Long): Flow<List<AttemptEntity>>

    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("DELETE FROM attempts WHERE deckId = :deckId")
    suspend fun deleteByDeck(deckId: Long)
}
