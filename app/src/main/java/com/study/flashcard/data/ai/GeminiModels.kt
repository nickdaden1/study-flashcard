package com.study.flashcard.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Request ----------

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 4096,
    // Ép model trả JSON thuần để parser đỡ phải đoán.
    val responseMimeType: String = "application/json"
)

// ---------- Response ----------

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

/** Lấy text của candidate đầu tiên; null nếu không có. */
fun GeminiResponse.firstText(): String? =
    candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
