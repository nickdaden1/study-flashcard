package com.study.flashcard.data.parser

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Đọc PDF theo từng trang bằng PdfBox-Android. */
class PdfParser(appContext: Context) : FileParser {

    private val appCtx = appContext.applicationContext

    override suspend fun parse(input: InputStream, fileName: String): ParsedDocument =
        withContext(Dispatchers.IO) {
            ensureInit(appCtx)
            val bytes = input.readBytesCapped()
            try {
                PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
                    val pages = doc.numberOfPages
                    val text = TextChunker.normalize(PDFTextStripper().getText(doc))
                    if (text.isBlank()) throw EmptyDocumentException()
                    ParsedDocument(text, pages, "trang")
                }
            } catch (e: ParseException) {
                throw e
            } catch (e: Exception) {
                throw CorruptFileException(
                    "File PDF bị lỗi, không mở được. Hãy thử mở bằng app khác rồi lưu lại."
                )
            }
        }

    companion object {
        @Volatile
        private var inited = false

        /** PdfBox-Android bắt buộc init 1 lần trước khi dùng (nạp font/asset). */
        fun ensureInit(context: Context) {
            if (inited) return
            synchronized(this) {
                if (!inited) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    inited = true
                }
            }
        }
    }
}
