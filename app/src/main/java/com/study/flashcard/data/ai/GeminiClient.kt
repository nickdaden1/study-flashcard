package com.study.flashcard.data.ai

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Kết quả sinh thẻ cho toàn bộ đề cương (nhiều chunk). */
data class GenerateAllResult(
    val drafts: List<CardDraft>,
    /** Số thẻ sai format (giữ để UI báo user sửa tay). */
    val invalidCount: Int,
    /** Số chunk thất bại hẳn (mất mạng/quota) — đã giữ lại phần làm được. */
    val failedChunks: Int
)

/**
 * Client gọi Gemini. Quy tắc theo spec:
 * - Gọi nối tiếp từng chunk + delay tránh rate-limit, có progress callback.
 * - Lỗi parse → retry đúng 1 lần rồi giữ text thô (ném AiParseException).
 * - Hết quota/mất mạng → ném exception tiếng Việt, caller giữ phần đã làm được.
 */
open class GeminiClient(
    private val api: GeminiApi,
    private val model: String = GeminiApi.MODEL_FLASH,
    private val delayBetweenChunksMs: Long = 2000L
) {

    /** Sinh thẻ cho 1 chunk. */
    open suspend fun generateForChunk(
        chunk: String,
        mcqCount: Int,
        essayCount: Int,
        apiKey: String
    ): DraftParseResult {
        require(apiKey.isNotBlank()) { "Thiếu API key" }
        val prompt = GeneratePrompts.buildGeneratePrompt(chunk, mcqCount, essayCount)
        val text = callGemini(prompt, apiKey)
        return try {
            CardDraftParser.parse(text)
        } catch (e: AiParseException) {
            // Retry đúng 1 lần với cùng prompt rồi mới bỏ.
            val retryText = callGemini(prompt, apiKey)
            CardDraftParser.parse(retryText)
        }
    }

    /**
     * Sinh thẻ cho toàn bộ chunks (gọi nối tiếp). Chunk nào lỗi mạng/quota thì bỏ qua
     * và đếm vào failedChunks — caller hiện tiến trình + giữ phần đã sinh được.
     */
    suspend fun generateAll(
        chunks: List<String>,
        totalCards: Int,
        mcqRatio: Float,
        apiKey: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): GenerateAllResult {
        val quotas = GeneratePrompts.allocatePerChunk(totalCards, chunks.size)
        val all = mutableListOf<CardDraft>()
        var invalid = 0
        var failed = 0
        chunks.forEachIndexed { i, chunk ->
            val (mcq, essay) = GeneratePrompts.splitMcqEssay(quotas[i], mcqRatio)
            try {
                val r = generateForChunk(chunk, mcq, essay, apiKey)
                all += r.drafts
                invalid += r.invalidCount
            } catch (e: AiException) {
                // Mất mạng/quota/parse sau retry: bỏ chunk này, giữ phần đã có.
                failed++
                if (e is AiQuotaException) throw e
            }
            onProgress(i + 1, chunks.size)
            if (i < chunks.lastIndex) delay(delayBetweenChunksMs)
        }
        return GenerateAllResult(all, invalid, failed)
    }

    /** AI chấm 1 câu tự luận (tham khảo). */
    open suspend fun gradeEssay(
        question: String,
        modelAnswer: String,
        userAnswer: String,
        apiKey: String
    ): GradingResult {
        require(apiKey.isNotBlank()) { "Thiếu API key" }
        val prompt = GeneratePrompts.buildGradePrompt(question, modelAnswer, userAnswer)
        val text = callGemini(prompt, apiKey)
        return try {
            GradingParser.parse(text)
        } catch (e: AiParseException) {
            GradingParser.parse(callGemini(prompt, apiKey))
        }
    }

    private suspend fun callGemini(prompt: String, apiKey: String): String {
        val body = GeminiRequest(
            contents = listOf(GeminiContent(listOf(GeminiPart(prompt))))
        )
        try {
            val res = api.generateContent(model, apiKey, body)
            return res.firstText()?.takeIf { it.isNotBlank() }
                ?: throw AiParseException("")
        } catch (e: AiException) {
            throw e
        } catch (e: HttpException) {
            throw mapHttpError(e)
        } catch (e: IOException) {
            throw AiNetworkException()
        }
    }

    companion object {
        /** Map mã HTTP → exception tiếng Việt. Thuần logic để unit test được. */
        fun mapHttpError(e: HttpException): AiException = when (e.code()) {
            400, 401, 403 -> AiAuthException()
            429 -> AiQuotaException()
            else -> AiException("Gemini lỗi (${e.code()}), hãy thử lại sau.")
        }

        private val json = Json { ignoreUnknownKeys = true }

        /** Factory thật cho app (OkHttp + Retrofit + kotlinx.serialization). */
        fun create(debug: Boolean = false): GeminiClient {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .apply {
                    if (debug) addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
                    )
                }
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(GeminiApi.BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            return GeminiClient(retrofit.create(GeminiApi::class.java))
        }
    }
}
