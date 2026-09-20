package com.study.flashcard.data.model

import com.study.flashcard.data.local.CardEntity
import kotlinx.serialization.json.Json

private val cardUiJson = Json { ignoreUnknownKeys = true }

/** Giải choicesJson thành list để UI hiện 4 đáp án. Lỗi thì trả null (coi như thẻ lỗi). */
fun CardEntity.decodedChoices(): List<String>? {
    val raw = choicesJson ?: return null
    return try {
        cardUiJson.decodeFromString<List<String>>(raw).takeIf { it.size == 4 }
    } catch (e: Exception) {
        null
    }
}

fun CardEntity.isMcq(): Boolean = type.uppercase() == "MCQ"
