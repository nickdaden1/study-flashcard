package com.study.flashcard.data.parser

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Kết quả đọc file: text đã chuẩn hóa + số đơn vị (trang/đoạn) để hiện tiến trình. */
data class ParsedDocument(
    val text: String,
    val unitCount: Int,
    val unitName: String
)

interface FileParser {
    @Throws(ParseException::class)
    suspend fun parse(input: InputStream, fileName: String): ParsedDocument
}

open class ParseException(message: String) : Exception(message)

/** File .doc đời cũ (OLE2) — POI trên máy chỉ đọc .docx. */
class OldDocException : ParseException(
    "File .doc đời cũ không hỗ trợ. Hãy mở bằng Word hoặc Google Docs rồi lưu thành .docx rồi thử lại."
)

class UnsupportedFormatException(ext: String) : ParseException(
    "Định dạng .$ext chưa hỗ trợ. Hãy dùng file .docx, .pdf hoặc .json."
)

class FileTooLargeException(mb: Long) : ParseException(
    "File quá lớn (khoảng ${mb}MB, giới hạn 100MB). Hãy tách nhỏ file rồi thử lại."
)

class CorruptFileException(message: String) : ParseException(message)

/** File không có chữ (ví dụ ảnh scan) — OCR để ngoài MVP. */
class EmptyDocumentException : ParseException(
    "Không đọc được chữ nào từ file. File có thể là ảnh scan — bản này chưa hỗ trợ ảnh, hãy dùng file Word/PDF có chữ."
)

private const val MAX_FILE_BYTES = 100L * 1024 * 1024

/** Đọc stream có giới hạn dung lượng để máy yếu không bị tràn RAM. */
fun InputStream.readBytesCapped(maxBytes: Long = MAX_FILE_BYTES): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n < 0) break
        total += n
        if (total > maxBytes) throw FileTooLargeException(total / 1024 / 1024)
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

private val OLE2_MAGIC = byteArrayOf(
    0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(),
    0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()
)

fun isOle2(bytes: ByteArray): Boolean =
    bytes.size >= 8 && bytes.sliceArray(0..7).contentEquals(OLE2_MAGIC)

fun isZip(bytes: ByteArray): Boolean =
    bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
