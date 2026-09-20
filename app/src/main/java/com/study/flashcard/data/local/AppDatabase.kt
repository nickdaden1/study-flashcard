package com.study.flashcard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeckEntity::class, CardEntity::class, AttemptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun attemptDao(): AttemptDao
}
