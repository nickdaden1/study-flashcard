package com.study.flashcard.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportValidatorTest {

    private fun validJson() = """
        {
          "schemaVersion": 1,
          "deck": {"name": "Triết học", "sourceFileName": "triet.docx"},
          "cards": [
            {"type": "MCQ", "question": "Ai là ai?",
             "choices": ["A", "B", "C", "D"], "answer": "A"},
            {"type": "ESSAY", "question": "Trình bày X.",
             "choices": null, "answer": "X là..."}
          ]
        }
    """.trimIndent()

    @Test
    fun `json dung thi pass`() {
        val dto = JsonImportValidator.validate(validJson())
        assertEquals("Triết học", dto.deck.name)
        assertEquals(2, dto.cards.size)
    }

    @Test
    fun `json vo van thi bao loi dinh dang`() {
        assertThrows(InvalidImportException::class.java) {
            JsonImportValidator.validate("{khong phai json")
        }
    }

    @Test
    fun `sai schemaVersion thi bao khong ho tro`() {
        val ex = assertThrows(InvalidImportException::class.java) {
            JsonImportValidator.validate(validJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
        }
        assertEquals(true, ex.message!!.contains("2"))
    }

    @Test
    fun `trac nghiem thieu lua chon thi bao loi`() {
        val bad = validJson().replace(
            "\"choices\": [\"A\", \"B\", \"C\", \"D\"]",
            "\"choices\": [\"A\", \"B\", \"C\"]"
        )
        assertThrows(InvalidImportException::class.java) {
            JsonImportValidator.validate(bad)
        }
    }

    @Test
    fun `the thieu cau hoi thi bao loi`() {
        val bad = validJson().replace(
            "\"question\": \"Trình bày X.\"",
            "\"question\": \"  \""
        )
        assertThrows(InvalidImportException::class.java) {
            JsonImportValidator.validate(bad)
        }
    }

    @Test
    fun `file khong co the nao thi bao loi`() {
        val bad = validJson().replace(
            Regex("\"cards\": \\[.*?\\]", RegexOption.DOT_MATCHES_ALL),
            "\"cards\": []"
        )
        assertThrows(InvalidImportException::class.java) {
            JsonImportValidator.validate(bad)
        }
    }
}
