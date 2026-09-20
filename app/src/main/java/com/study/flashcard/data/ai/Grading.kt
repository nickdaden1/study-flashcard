package com.study.flashcard.data.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Kết quả AI chấm tự luận — mang tính tham khảo, user tự chốt điểm cuối (theo spec). */
@Serializable
data class GradingResult(
    /** Điểm 0-10. */
    val score: Float,
    /** Các ý còn thiếu so với đáp án mẫu. */
    val missingPoints: List<String> = emptyList(),
    val feedback: String = ""
)

object GradingParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Throws(AiParseException::class)
    fun parse(rawText: String): GradingResult {
        var clean = rawText.trim()
        if (clean.startsWith("```")) {
            clean = clean.substringAfter("\n", clean)
            if (!clean.contains("\n")) clean = rawText.trim().removePrefix("```json").removePrefix("```")
        }
        if (clean.endsWith("```")) clean = clean.substringBeforeLast("```")
        clean = clean.trim()

        val result = try {
            json.decodeFromString<GradingResult>(clean.trim())
        } catch (e: Exception) {
            throw AiParseException(rawText)
        }
        return result.copy(score = result.score.coerceIn(0f, 10f))
    }
}
