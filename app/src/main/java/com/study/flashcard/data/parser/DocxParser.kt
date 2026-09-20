package com.study.flashcard.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Đọc .docx (paragraph + table theo đúng thứ tự trong file). */
class DocxParser : FileParser {

    override suspend fun parse(input: InputStream, fileName: String): ParsedDocument =
        withContext(Dispatchers.IO) {
            val bytes = input.readBytesCapped()
            if (isOle2(bytes)) throw OldDocException()
            val ext = fileName.substringAfterLast('.', "")
            if (!isZip(bytes)) throw UnsupportedFormatException(ext.ifBlank { "?" })

            try {
                XWPFDocument(ByteArrayInputStream(bytes)).use { doc ->
                    val sb = StringBuilder()
                    var units = 0
                    for (element in doc.bodyElements) {
                        when (element) {
                            is XWPFParagraph -> {
                                val t = element.text.trim()
                                if (t.isNotEmpty()) {
                                    sb.appendLine(t)
                                    units++
                                }
                            }
                            is XWPFTable -> {
                                for (row in element.rows) {
                                    val cells = row.tableCells
                                        .map { it.text.trim() }
                                        .filter { it.isNotEmpty() }
                                    if (cells.isNotEmpty()) {
                                        sb.appendLine(cells.joinToString(" | "))
                                        units++
                                    }
                                }
                            }
                        }
                    }
                    val text = TextChunker.normalize(sb.toString())
                    if (text.isBlank()) throw EmptyDocumentException()
                    ParsedDocument(text, units, "đoạn")
                }
            } catch (e: ParseException) {
                throw e
            } catch (e: Exception) {
                throw CorruptFileException(
                    "File .docx bị lỗi, không mở được. Hãy mở bằng Word rồi lưu lại rồi thử tiếp."
                )
            }
        }
}
