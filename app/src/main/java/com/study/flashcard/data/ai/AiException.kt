package com.study.flashcard.data.ai

/** Lỗi AI — mọi message đều tiếng Việt để UI hiện thẳng cho user. */
open class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** API key sai / hết hạn / không có quyền. */
class AiAuthException :
    AiException("API key không đúng hoặc đã hết hạn. Hãy tạo key mới ở AI Studio rồi nhập lại trong Cài đặt.")

/** Hết quota miễn phí / bị rate-limit. */
class AiQuotaException :
    AiException("Hết lượt miễn phí của Gemini, hãy đợi vài phút rồi thử lại. Phần đã sinh được vẫn giữ nguyên.")

/** Mất mạng. */
class AiNetworkException :
    AiException("Mất mạng rồi. Hãy kiểm tra Wi-Fi/4G rồi thử lại — phần đã sinh được vẫn giữ nguyên.")

/** AI trả về sai format sau khi đã retry. Giữ text thô để user sửa tay. */
class AiParseException(val rawText: String) :
    AiException("AI trả về sai định dạng, hãy sửa tay hoặc thử lại. Nội dung thô đã được giữ lại.")
