package com.study.flashcard.data.parser

import com.study.flashcard.data.model.CardImport
import com.study.flashcard.data.model.DeckImport
import kotlinx.serialization.json.Json

class InvalidImportException(message: String) : Exception(message)

/** Kiểm tra file JSON nhập vào có đúng schema của app không. Thuần Kotlin, test được. */
object JsonImportValidator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Throws(InvalidImportException::class)
    fun validate(raw: String): DeckImport {
        val dto = try {
            json.decodeFromString<DeckImport>(raw)
        } catch (e: Exception) {
            throw InvalidImportException(
                "File không đúng định dạng JSON của app. Hãy xuất file từ app rồi nhập lại."
            )
        }

        if (dto.schemaVersion != 1) {
            throw InvalidImportException(
                "Phiên bản file (${dto.schemaVersion}) không hỗ trợ. App chỉ đọc schemaVersion = 1."
            )
        }
        if (dto.deck.name.isBlank()) {
            throw InvalidImportException("File thiếu tên bộ đề.")
        }
        if (dto.cards.isEmpty()) {
            throw InvalidImportException("File không có thẻ nào.")
        }
        dto.cards.forEachIndexed { index, card ->
            validateCard(index + 1, card)
        }
        return dto
    }

    private fun validateCard(position: Int, card: CardImport) {
        val type = card.type.uppercase()
        if (type != "MCQ" && type != "ESSAY") {
            throw InvalidImportException(
                "Thẻ số $position: loại '${card.type}' không hợp lệ (chỉ nhận MCQ hoặc ESSAY)."
            )
        }
        if (card.question.isBlank() || card.answer.isBlank()) {
            throw InvalidImportException("Thẻ số $position: thiếu câu hỏi hoặc đáp án.")
        }
        if (type == "MCQ" && (card.choices == null || card.choices.size != 4)) {
            throw InvalidImportException("Thẻ số $position: trắc nghiệm phải có đúng 4 lựa chọn.")
        }
    }
}
