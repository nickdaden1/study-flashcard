package com.study.flashcard.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun `text ngan giu nguyen 1 chunk`() {
        val text = "Câu 1 là gì? Đáp án A."
        assertEquals(listOf(text), TextChunker.chunk(text))
    }

    @Test
    fun `text dai duoc cat va noi lai dung thu tu`() {
        val text = (1..200).joinToString(" ") { "Đây là câu số $it với nội dung ôn thi." }
        val chunks = TextChunker.chunk(text, 4000)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 4000 && it.isNotBlank() })
        assertEquals(
            TextChunker.normalize(text).replace("\n", " "),
            chunks.joinToString(" ")
        )
    }

    @Test
    fun `text rong tra ve rong`() {
        assertTrue(TextChunker.chunk("   \n  ").isEmpty())
    }

    @Test
    fun `doan dai bat thuong bi cat cung`() {
        val chunks = TextChunker.chunk("a".repeat(9000), 4000)
        assertEquals(3, chunks.size)
        assertEquals(9000, chunks.sumOf { it.length })
    }

    @Test
    fun `normalize loai dong trang`() {
        assertEquals("a\nb", TextChunker.normalize("  a  \n\n   \n b "))
    }
}
