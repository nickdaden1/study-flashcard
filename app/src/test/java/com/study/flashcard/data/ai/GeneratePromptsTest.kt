package com.study.flashcard.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratePromptsTest {

    @Test
    fun `prompt sinh the chua du so luong va schema`() {
        val p = GeneratePrompts.buildGeneratePrompt("Quang hợp là...", 3, 2)
        assertTrue(p.contains("3 câu trắc nghiệm"))
        assertTrue(p.contains("2 câu tự luận"))
        assertTrue(p.contains("Quang hợp là..."))
        assertTrue(p.contains("\"type\": \"MCQ\""))
    }

    @Test
    fun `prompt cham bai chua du 3 phan`() {
        val p = GeneratePrompts.buildGradePrompt("Q?", "Đáp án mẫu", "Bài làm")
        assertTrue(p.contains("Q?") && p.contains("Đáp án mẫu") && p.contains("Bài làm"))
        assertTrue(p.contains("\"score\""))
    }

    @Test
    fun `chia the cho chunk giu tong`() {
        assertEquals(listOf(4, 3, 3), GeneratePrompts.allocatePerChunk(10, 3))
        assertEquals(listOf(2, 2), GeneratePrompts.allocatePerChunk(4, 2))
        assertEquals(listOf(1), GeneratePrompts.allocatePerChunk(1, 1))
        assertEquals(emptyList<Int>(), GeneratePrompts.allocatePerChunk(0, 3))
        assertEquals(emptyList<Int>(), GeneratePrompts.allocatePerChunk(5, 0))
    }

    @Test
    fun `chia MCQ ESSAY theo ti le`() {
        assertEquals(6 to 4, GeneratePrompts.splitMcqEssay(10, 0.6f))
        assertEquals(0 to 5, GeneratePrompts.splitMcqEssay(5, 0f))
        assertEquals(5 to 0, GeneratePrompts.splitMcqEssay(5, 1f))
    }

    @Test
    fun `grading parse chuan va chan diem 0-10`() {
        val r = GradingParser.parse(
            """{"score": 15, "missingPoints": ["ý A"], "feedback": "Tốt"}"""
        )
        assertEquals(10f, r.score, 0.001f)
        assertEquals(listOf("ý A"), r.missingPoints)
    }

    @Test
    fun `grading sai format thi nem`() {
        try {
            GradingParser.parse("không phải json")
            throw AssertionError("phải ném AiParseException")
        } catch (e: AiParseException) {
            assertTrue(e.rawText.isNotEmpty())
        }
    }
}
