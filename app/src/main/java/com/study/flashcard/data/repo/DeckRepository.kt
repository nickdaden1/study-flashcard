package com.study.flashcard.data.repo

import com.study.flashcard.data.ai.CardDraft
import com.study.flashcard.data.ai.GenerateAllResult
import com.study.flashcard.data.ai.GeminiClient
import com.study.flashcard.data.local.AttemptDao
import com.study.flashcard.data.local.CardDao
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.local.DeckDao
import com.study.flashcard.data.local.DeckEntity
import com.study.flashcard.data.model.DeckWithProgress
import com.study.flashcard.data.model.calcMasteredPercent
import com.study.flashcard.data.parser.FileParser
import com.study.flashcard.data.parser.ParsedDocument
import com.study.flashcard.data.parser.TextChunker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.InputStream

private val repoJson = Json { ignoreUnknownKeys = true }

/** Thẻ nháp (AI sinh, user đã duyệt) → Entity để lưu Room. */
internal fun CardDraft.toEntity(deckId: Long): CardEntity = CardEntity(
    deckId = deckId,
    type = type.uppercase(),
    question = question,
    answer = answer,
    choicesJson = choices?.let { repoJson.encodeToString(it) }
)

/**
 * Quản lý deck + thẻ + pipeline import (parse → chunk → AI sinh → lưu).
 * ViewModel (Phase 5) chỉ gọi các hàm này, không chạm DAO/AI trực tiếp.
 */
class DeckRepository(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val attemptDao: AttemptDao,
    private val gemini: GeminiClient,
    private val parserFor: (fileName: String) -> FileParser
) {

    fun observeDecksWithProgress(
        nowProvider: () -> Long = System::currentTimeMillis
    ): Flow<List<DeckWithProgress>> =
        deckDao.observeAll().flatMapLatest { decks ->
            flow { emit(buildProgress(decks, nowProvider())) }
        }

    suspend fun getDeck(id: Long): DeckEntity? = deckDao.getById(id)

    suspend fun createDeck(name: String, sourceFileName: String? = null): Long {
        require(name.isNotBlank()) { "Tên bộ đề không được để trống." }
        return deckDao.insert(DeckEntity(name = name.trim(), sourceFileName = sourceFileName))
    }

    suspend fun renameDeck(id: Long, name: String) {
        require(name.isNotBlank()) { "Tên bộ đề không được để trống." }
        val deck = deckDao.getById(id) ?: return
        deckDao.update(deck.copy(name = name.trim()))
    }

    /** Xóa deck; thẻ + lịch sử thi trong deck tự xóa theo (FK cascade). */
    suspend fun deleteDeck(id: Long) {
        deckDao.deleteById(id)
    }

    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cardDao.observeByDeck(deckId)

    suspend fun deleteCard(card: CardEntity) {
        cardDao.deleteById(card.id)
        deckDao.updateCardCount(card.deckId, cardDao.countTotal(card.deckId))
    }

    // ---------- Pipeline import ----------

    /** Bước 1: đọc file → text (ném ParseException tiếng Việt nếu file lỗi). */
    suspend fun parseDocument(fileName: String, input: InputStream): ParsedDocument =
        parserFor(fileName).parse(input, fileName)

    /** Bước 2: cắt text thành chunk gửi AI. Thuần logic, không cần test trên máy. */
    fun chunkText(text: String): List<String> = TextChunker.chunk(text)

    /** Bước 3: gọi AI sinh thẻ nháp cho toàn bộ chunk (có tiến trình). */
    suspend fun generateDrafts(
        chunks: List<String>,
        totalCards: Int,
        mcqRatio: Float,
        apiKey: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): GenerateAllResult = gemini.generateAll(chunks, totalCards, mcqRatio, apiKey, onProgress)

    /** Bước 4: user duyệt xong → lưu deck + thẻ, trả về deckId. */
    suspend fun saveDeckWithCards(
        name: String,
        sourceFileName: String?,
        drafts: List<CardDraft>
    ): Long {
        require(name.isNotBlank()) { "Tên bộ đề không được để trống." }
        require(drafts.isNotEmpty()) { "Chưa có thẻ nào để lưu." }
        val deckId = deckDao.insert(
            DeckEntity(name = name.trim(), sourceFileName = sourceFileName)
        )
        cardDao.insertAll(drafts.map { it.toEntity(deckId) })
        deckDao.updateCardCount(deckId, drafts.size)
        return deckId
    }

    private suspend fun buildProgress(
        decks: List<DeckEntity>,
        now: Long
    ): List<DeckWithProgress> = decks.map { deck ->
        val total = cardDao.countTotal(deck.id)
        val mastered = cardDao.countMastered(deck.id, now)
        DeckWithProgress(
            deck = deck,
            total = total,
            mastered = mastered,
            due = cardDao.countDue(deck.id, now)
        )
    }

    /** % đã thuộc — dùng chung công thức với UI. */
    fun percentOf(total: Int, mastered: Int): Int = calcMasteredPercent(total, mastered)
}
