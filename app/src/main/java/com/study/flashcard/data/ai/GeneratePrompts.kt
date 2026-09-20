package com.study.flashcard.data.ai

/**
 * Prompt sinh thẻ + chấm tự luận. Thuần Kotlin để unit test được.
 * Mọi yêu cầu output đều ép JSON strict để parser đỡ phải đoán.
 */
object GeneratePrompts {

    fun buildGeneratePrompt(chunk: String, mcqCount: Int, essayCount: Int): String =
        """
        Bạn là trợ lý soạn đề cương ôn thi. Từ ĐOẠN KIẾN THỨC dưới đây, hãy tạo đúng ${mcqCount + essayCount} câu hỏi:
        - $mcqCount câu trắc nghiệm (mỗi câu đúng 4 lựa chọn, chỉ 1 đáp án đúng)
        - $essayCount câu tự luận ngắn (kèm đáp án mẫu đầy đủ ý chính)

        Trả về DUY NHẤT một JSON array, không giải thích thêm, mỗi phần tử đúng schema:
        {"type": "MCQ", "question": "...", "choices": ["A", "B", "C", "D"], "answer": "đáp án đúng"}
        {"type": "ESSAY", "question": "...", "choices": null, "answer": "đáp án mẫu"}

        ĐOẠN KIẾN THỨC:
        $chunk
        """.trimIndent()

    fun buildGradePrompt(question: String, modelAnswer: String, userAnswer: String): String =
        """
        Bạn là giáo viên chấm bài tự luận. Cho câu hỏi, đáp án mẫu và câu trả lời của học sinh dưới đây,
        hãy chấm điểm 0-10 và chỉ ra các ý còn thiếu.

        Trả về DUY NHẤT một JSON object đúng schema, không giải thích thêm:
        {"score": 7.5, "missingPoints": ["ý còn thiếu 1", "ý còn thiếu 2"], "feedback": "nhận xét ngắn gọn"}

        CÂU HỎI: $question
        ĐÁP ÁN MẪU: $modelAnswer
        CÂU TRẢ LỜI CỦA HỌC SINH: $userAnswer
        """.trimIndent()

    /**
     * Chia số thẻ cho từng chunk theo tỉ lệ. Chunk đầu nhận phần dư để tổng luôn khớp.
     * Ví dụ: 10 thẻ / 3 chunk → [4, 3, 3].
     */
    fun allocatePerChunk(totalCards: Int, chunkCount: Int): List<Int> {
        if (chunkCount <= 0 || totalCards <= 0) return emptyList()
        val base = totalCards / chunkCount
        val rest = totalCards % chunkCount
        return List(chunkCount) { i -> base + if (i < rest) 1 else 0 }
    }

    /** Chia quota MCQ/ESSAY trong 1 chunk theo tỉ lệ mcqRatio (0.0-1.0), làm tròn MCQ lên. */
    fun splitMcqEssay(chunkCards: Int, mcqRatio: Float): Pair<Int, Int> {
        val mcq = (chunkCards * mcqRatio.coerceIn(0f, 1f)).toInt().coerceIn(0, chunkCards)
        return mcq to (chunkCards - mcq)
    }
}
