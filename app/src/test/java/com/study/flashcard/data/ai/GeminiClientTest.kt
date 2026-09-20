package com.study.flashcard.data.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Fake GeminiApi để test retry + gom nhiều chunk mà không cần mạng. */
private class FakeApi(
    private val texts: List<String>,
    private val failAtCalls: Set<Int> = emptySet(),
    private val httpCode: Int = 429
) : GeminiApi {
    var calls = 0

    override suspend fun generateContent(
        model: String,
        apiKey: String,
        body: GeminiRequest
    ): GeminiResponse {
        calls++
        if (calls in failAtCalls) {
            throw HttpException(
                Response.error<GeminiResponse>(
                    httpCode,
                    "".toResponseBody(null)
                )
            )
        }
        val text = texts[(calls - 1) % texts.size]
        return GeminiResponse(
            listOf(GeminiCandidate(GeminiContent(listOf(GeminiPart(text)))))
        )
    }
}

class GeminiClientTest {

    private fun goodJson() =
        """[{"type":"MCQ","question":"Q?","choices":["A","B","C","D"],"answer":"A"}]"""

    @Test
    fun `sinh 1 chunk thanh cong`() = runTest {
        val client = GeminiClient(FakeApi(listOf(goodJson())), delayBetweenChunksMs = 0)
        val r = client.generateForChunk("chunk", 1, 0, "key")
        assertEquals(1, r.drafts.size)
    }

    @Test
    fun `parse loi thi retry 1 lan roi duoc`() = runTest {
        val client = GeminiClient(
            FakeApi(listOf("không phải json", goodJson())),
            delayBetweenChunksMs = 0
        )
        val r = client.generateForChunk("chunk", 1, 0, "key")
        assertEquals(1, r.drafts.size)
        assertEquals(2, (client as GeminiClient).let {
            (it.javaClass.getDeclaredField("api").apply { isAccessible = true }
                .get(it) as FakeApi).calls
        })
    }

    @Test
    fun `generateAll gom nhieu chunk va bao tien do`() = runTest {
        val client = GeminiClient(FakeApi(listOf(goodJson())), delayBetweenChunksMs = 0)
        val progress = mutableListOf<Pair<Int, Int>>()
        val r = client.generateAll(
            chunks = listOf("c1", "c2", "c3"),
            totalCards = 3,
            mcqRatio = 1f,
            apiKey = "key",
            onProgress = { done, total -> progress += done to total }
        )
        assertEquals(3, r.drafts.size)
        assertEquals(0, r.failedChunks)
        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), progress)
    }

    @Test
    fun `chunk loi quota thi nem de caller giu phan da co`() = runTest {
        val client = GeminiClient(
            FakeApi(listOf(goodJson()), failAtCalls = setOf(2), httpCode = 429),
            delayBetweenChunksMs = 0
        )
        try {
            client.generateAll(listOf("c1", "c2"), 2, 1f, "key")
            throw AssertionError("phải ném AiQuotaException")
        } catch (e: AiQuotaException) {
            assertTrue(e.message!!.contains("miễn phí"))
        }
    }

    @Test
    fun `map loi http dung tieng Viet`() {
        fun code(c: Int) = GeminiClient.mapHttpError(
            HttpException(Response.error<GeminiResponse>(c, "".toResponseBody(null)))
        )
        assertTrue(code(401) is AiAuthException)
        assertTrue(code(429) is AiQuotaException)
        assertTrue(code(500) is AiException)
    }
}
