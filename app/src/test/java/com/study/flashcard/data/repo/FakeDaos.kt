package com.study.flashcard.data.repo

import com.study.flashcard.data.local.AttemptDao
import com.study.flashcard.data.local.AttemptEntity
import com.study.flashcard.data.local.CardDao
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.local.DeckDao
import com.study.flashcard.data.local.DeckEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Fake DAO bằng list trong RAM để test repository trên CI (không cần Android). */
class FakeDeckDao : DeckDao {
    private val decks = mutableListOf<DeckEntity>()
    private val flow = MutableStateFlow<List<DeckEntity>>(emptyList())

    override fun observeAll(): Flow<List<DeckEntity>> = flow

    override suspend fun getById(id: Long): DeckEntity? = decks.find { it.id == id }

    override suspend fun insert(deck: DeckEntity): Long {
        val id = (decks.maxOfOrNull { it.id } ?: 0) + 1
        decks += deck.copy(id = id)
        flow.value = decks.toList()
        return id
    }

    override suspend fun update(deck: DeckEntity) {
        decks.replaceAll { if (it.id == deck.id) deck else it }
        flow.value = decks.toList()
    }

    override suspend fun deleteById(id: Long) {
        decks.removeAll { it.id == id }
        flow.value = decks.toList()
    }

    override suspend fun updateCardCount(deckId: Long, count: Int) {
        decks.replaceAll { if (it.id == deckId) it.copy(cardCount = count) else it }
        flow.value = decks.toList()
    }
}

class FakeCardDao : CardDao {
    private val cards = mutableListOf<CardEntity>()
    private val flows = mutableMapOf<Long, MutableStateFlow<List<CardEntity>>>()

    override fun observeByDeck(deckId: Long): Flow<List<CardEntity>> =
        flows.getOrPut(deckId) { MutableStateFlow(cards.filter { it.deckId == deckId }) }
            .map { list -> list.sortedBy { it.id } }

    private fun refresh() {
        flows.forEach { (deckId, f) ->
            f.value = cards.filter { it.deckId == deckId }
        }
    }

    override suspend fun getDue(deckId: Long, now: Long): List<CardEntity> =
        cards.filter { it.deckId == deckId && it.dueAt <= now }.sortedBy { it.dueAt }

    override suspend fun countDue(deckId: Long, now: Long): Int =
        cards.count { it.deckId == deckId && it.dueAt <= now }

    override suspend fun countTotal(deckId: Long): Int =
        cards.count { it.deckId == deckId }

    override suspend fun countMastered(deckId: Long, now: Long): Int =
        cards.count { it.deckId == deckId && it.repetitions > 0 && it.dueAt > now }

    override suspend fun insertAll(newCards: List<CardEntity>): List<Long> {
        var next = (cards.maxOfOrNull { it.id } ?: 0) + 1
        val ids = newCards.map {
            cards += it.copy(id = next)
            next++
        }
        refresh()
        return ids
    }

    override suspend fun update(card: CardEntity) {
        cards.replaceAll { if (it.id == card.id) card else it }
        refresh()
    }

    override suspend fun deleteById(id: Long) {
        cards.removeAll { it.id == id }
        refresh()
    }

    override suspend fun deleteByDeck(deckId: Long) {
        cards.removeAll { it.deckId == deckId }
        refresh()
    }
}

class FakeAttemptDao : AttemptDao {
    private val attempts = mutableListOf<AttemptEntity>()
    private val flow = MutableStateFlow<List<AttemptEntity>>(emptyList())

    override fun observeByDeck(deckId: Long): Flow<List<AttemptEntity>> =
        flow.map { list -> list.filter { it.deckId == deckId } }

    override suspend fun insert(attempt: AttemptEntity): Long {
        val id = (attempts.maxOfOrNull { it.id } ?: 0) + 1
        attempts += attempt.copy(id = id)
        flow.value = attempts.sortedByDescending { it.finishedAt }
        return id
    }

    override suspend fun deleteByDeck(deckId: Long) {
        attempts.removeAll { it.deckId == deckId }
        flow.value = attempts.toList()
    }

    fun all(): List<AttemptEntity> = attempts.toList()
}
