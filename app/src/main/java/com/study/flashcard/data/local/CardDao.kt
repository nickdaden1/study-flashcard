package com.study.flashcard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC")
    fun observeByDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND dueAt <= :now ORDER BY dueAt ASC")
    suspend fun getDue(deckId: Long, now: Long): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND dueAt <= :now")
    suspend fun countDue(deckId: Long, now: Long): Int

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun countTotal(deckId: Long): Int

    /** Số thẻ đã thuộc = repetitions > 0 và chưa đến hạn ôn lại. */
    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND repetitions > 0 AND dueAt > :now")
    suspend fun countMastered(deckId: Long, now: Long): Int

    @Insert
    suspend fun insertAll(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteByDeck(deckId: Long)
}
