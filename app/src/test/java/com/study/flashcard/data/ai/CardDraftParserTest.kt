package com.study.flashcard.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardDraftParserTest {

    @Test
    fun `parse json array chuan`() {
        val raw = """
            [{"type":"MCQ","question":"2+2?","choices":["1","2","3","4"],"answer":"4"},
             {"type":"ESSAY","question":"Nêu X.","choices":null,"answer":"X là..."}]
        """.trimIndent()
        val r = CardDraftParser.parse(raw)
        assertEquals(2, r.drafts.size)
        assertEquals(0, r.invalidCount)
        assertEquals("MCQ", r.drafts[0].type)
    }

    @Test
    fun `parse boc markdown fence`() {
        val raw = "```json\n[{\"type\":\"ESSAY\",\"question\":\"Q\",\"answer\":\"A\"}]\n```"
        val r = CardDraftParser.parse(raw)
        assertEquals(1, r.drafts.size)
    }

    @Test
    fun `parse object boc cards`() {
        val raw = """{"cards": [{"type":"ESSAY","question":"Q","answer":"A"}]}"""
        val r = CardDraftParser.parse(raw)
        assertEquals(1, r.drafts.size)
    }

    @Test
    fun `the loi bi loai va dem`() {
        val raw = """
            [{"type":"MCQ","question":"Q1","choices":["A","B"],"answer":"A"},
             {"type":"MCQ","question":"Q2","choices":["A","B","C","D"],"answer":"B"},
             {"type":"ESSAY","question":"  ","answer":"A"}]
        """.trimIndent()
        val r = CardDraftParser.parse(raw)
        assertEquals(1, r.drafts.size)
        assertEquals(2, r.invalidCount)
    }

    @Test
    fun `type thuong duoc chuan hoa`() {
        val raw = """[{"type":"mcq","question":"Q","choices":["A","B","C","D"],"answer":"A"}]"""
        assertEquals("MCQ", CardDraftParser.parse(raw).drafts[0].type)
    }

    @Test
    fun `json vo van thi nem AiParseException`() {
        assertThrows(AiParseException::class.java) {
            CardDraftParser.parse("xin chào, tôi không phải json")
        }
    }
}
