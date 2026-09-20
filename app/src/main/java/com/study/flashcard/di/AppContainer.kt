package com.study.flashcard.di

import android.content.Context
import androidx.room.Room
import com.study.flashcard.data.local.AppDatabase
import com.study.flashcard.data.local.SecurePrefs
import com.study.flashcard.data.parser.DocxParser
import com.study.flashcard.data.parser.FileParser
import com.study.flashcard.data.parser.PdfParser
import com.study.flashcard.data.parser.UnsupportedFormatException

/**
 * Manual DI — không dùng Hilt để nhẹ APK + nhẹ CI.
 * Phase 4-5 sẽ thêm Repository/ViewModel vào đây.
 */
class AppContainer(appContext: Context) {

    private val context = appContext.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "study-flashcard.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val deckDao get() = database.deckDao()
    val cardDao get() = database.cardDao()
    val attemptDao get() = database.attemptDao()

    val securePrefs by lazy { SecurePrefs(context) }

    private val pdfParser by lazy { PdfParser(context) }
    private val docxParser by lazy { DocxParser() }

    /**
     * Chọn parser theo đuôi file. File .doc đời cũ vẫn đi qua DocxParser
     * để phát hiện magic bytes OLE2 và báo convert (thay vì báo sai định dạng).
     */
    fun parserFor(fileName: String): FileParser {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> pdfParser
            "docx", "doc" -> docxParser
            else -> throw UnsupportedFormatException(
                fileName.substringAfterLast('.', "?")
            )
        }
    }
}
