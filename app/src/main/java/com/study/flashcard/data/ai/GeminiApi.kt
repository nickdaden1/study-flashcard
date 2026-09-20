package com.study.flashcard.data.ai

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Gemini Developer API (REST, v1beta).
 * Key truyền qua query `key=` theo doc Google; không qua server trung gian.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val MODEL_FLASH = "gemini-2.0-flash"
    }
}
