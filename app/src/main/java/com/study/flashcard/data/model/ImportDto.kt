package com.study.flashcard.data.model

import kotlinx.serialization.Serializable

/** Schema xuất/nhập deck ra JSON (Phase 6 dùng để share qua Zalo/Drive). */
@Serializable
data class DeckImport(
    val schemaVersion: Int,
    val deck: DeckImportMeta,
    val cards: List<CardImport>
)

@Serializable
data class DeckImportMeta(
    val name: String,
    val sourceFileName: String? = null
)

@Serializable
data class CardImport(
    /** "MCQ" hoặc "ESSAY" */
    val type: String,
    val question: String,
    /** MCQ: đúng 4 lựa chọn. ESSAY: null. */
    val choices: List<String>? = null,
    val answer: String
)
