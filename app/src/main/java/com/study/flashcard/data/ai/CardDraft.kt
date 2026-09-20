package com.study.flashcard.data.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Một thẻ nháp do AI sinh — user duyệt/sửa tay trước khi lưu vào Room (Phase 5). */
@Serializable
data class CardDraft(
    /** "MCQ" hoặc "ESSAY" (chuẩn hóa uppercase lúc parse). */
    val type: String,
    val question: String,
    /** MCQ: đúng 4 lựa chọn. ESSAY: null. */
    val choices: List<String>? = null,
    val answer: String
)

data class DraftParseResult(
    val drafts: List<CardDraft>,
    /** Số thẻ AI sinh ra nhưng sai format (thiếu đáp án, MCQ thiếu lựa chọn...). */
    val invalidCount: Int
)

@Serializable
private data class CardListWrapper(
    val cards: List<CardDraft> = emptyList()
)

object CardDraftParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse text AI trả về thành list thẻ. Chịu được:
     * - JSON array thuần: `[{...}, {...}]`
     * - Bọc markdown fence: ```json ... ```
     * - Object bọc: `{"cards": [...]}` (một số model thích trả kiểu này)
     * Thẻ lỗi format bị loại + đếm vào invalidCount để UI báo user sửa tay.
     */
    fun parse(rawText: String): DraftParseResult {
        val clean = stripCodeFence(rawText).trim()
        if (clean.isBlank()) return DraftParseResult(emptyList(), 0)

        val decoded: List<CardDraft> = try {
            json.decodeFromString(clean)
        } catch (e1: Exception) {
            try {
                json.decodeFromString<CardListWrapper>(clean).cards
            } catch (e2: Exception) {
                throw AiParseException(rawText)
            }
        }

        val valid = mutableListOf<CardDraft>()
        var invalid = 0
        for (item in decoded) {
            val normalized = item.copy(type = item.type.uppercase())
            if (isValid(normalized)) valid += normalized else invalid++
        }
        return DraftParseResult(valid, invalid)
    }

    private fun isValid(card: CardDraft): Boolean {
        if (card.type != "MCQ" && card.type != "ESSAY") return false
        if (card.question.isBlank() || card.answer.isBlank()) return false
        if (card.type == "MCQ" && (card.choices == null || card.choices.size != 4)) return false
        return true
    }

    private fun stripCodeFence(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter("\n", t)
            if (!t.contains("\n")) t = text.trim().removePrefix("```json").removePrefix("```")
        }
        if (t.endsWith("```")) t = t.substringBeforeLast("```")
        return t.trim()
    }
}
