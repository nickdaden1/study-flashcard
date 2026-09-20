package com.study.flashcard.data.parser

/**
 * Chuẩn hóa text + cắt thành chunk ~4000 ký tự để gửi Gemini từng đoạn.
 * Thuần Kotlin (không dính Android) nên unit test được trên CI.
 */
object TextChunker {

    /** Bỏ dòng trắng, trim từng dòng, giữ xuống dòng làm ranh giới câu. */
    fun normalize(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    /**
     * Cắt text đã chuẩn hóa thành list chunk, giữ nguyên thứ tự.
     * Ưu tiên cắt ở ranh giới câu; đoạn đơn dài quá maxSize thì cắt cứng.
     */
    fun chunk(text: String, maxSize: Int = 4000): List<String> {
        val clean = normalize(text)
        if (clean.isBlank()) return emptyList()

        val segments = clean
            .split(Regex("(?<=[.!?…:;\\n])\\s+"))
            .filter { it.isNotBlank() }

        val out = mutableListOf<String>()
        val cur = StringBuilder()

        fun flush() {
            if (cur.isNotBlank()) {
                out += cur.toString().trim()
                cur.clear()
            }
        }

        for (seg in segments) {
            if (seg.length > maxSize) {
                flush()
                var i = 0
                while (i < seg.length) {
                    out += seg.substring(i, minOf(i + maxSize, seg.length))
                    i += maxSize
                }
                continue
            }
            if (cur.isEmpty()) {
                cur.append(seg)
            } else if (cur.length + 1 + seg.length <= maxSize) {
                cur.append(' ').append(seg)
            } else {
                flush()
                cur.append(seg)
            }
        }
        flush()
        return out
    }
}
