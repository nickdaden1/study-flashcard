package com.study.flashcard.ui.importscreen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.study.flashcard.data.ai.AiQuotaException
import com.study.flashcard.data.ai.CardDraft
import com.study.flashcard.data.model.CardImport
import com.study.flashcard.data.model.DeckImport
import com.study.flashcard.data.model.DeckImportMeta
import com.study.flashcard.data.parser.JsonImportValidator
import com.study.flashcard.data.repo.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface ImportState {
    data object Idle : ImportState
    data class Parsing(val fileName: String) : ImportState
    /** Chờ user chọn số thẻ + tỉ lệ rồi mới gọi AI. */
    data class Ready(val fileName: String, val chunkCount: Int, val preview: String) : ImportState
    data class Generating(val done: Int, val total: Int) : ImportState
    data class Review(val drafts: List<CardDraft>, val invalidCount: Int, val failedChunks: Int) : ImportState
    data class Saving(val deckId: Long) : ImportState
    data class Error(val message: String) : ImportState
}

private val importJson = Json { prettyPrint = true }

/**
 * Luồng import: chọn file (docx/pdf/json) → parse → AI sinh nháp → duyệt → lưu deck.
 * File JSON thì lưu thẳng, không qua AI.
 */
class ImportViewModel(
    private val repo: DeckRepository,
    private val apiKeyProvider: () -> String
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private var chunks: List<String> = emptyList()
    private var pendingFileName: String = ""
    private var cardCountTarget = 20
    private var mcqRatio = 0.6f

    fun setOptions(cardCount: Int, mcqPercent: Int) {
        cardCountTarget = cardCount.coerceIn(5, 100)
        mcqRatio = mcqPercent.coerceIn(0, 100) / 100f
    }

    fun pickFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState.Parsing("")
            try {
                val fileName = queryName(context, uri) ?: "decuong"
                _state.value = ImportState.Parsing(fileName)
                val ext = fileName.substringAfterLast('.', "").lowercase()

                if (ext == "json") {
                    val raw = readText(context, uri)
                    val dto = JsonImportValidator.validate(raw)
                    saveImported(dto, fileName)
                    return@launch
                }

                val bytes = readBytes(context, uri)
                val doc = withContext(Dispatchers.IO) {
                    repo.parseDocument(fileName, bytes.inputStream())
                }
                chunks = repo.chunkText(doc.text)
                pendingFileName = fileName
                if (chunks.isEmpty()) {
                    _state.value = ImportState.Error("Không tách được nội dung từ file.")
                    return@launch
                }
                _state.value = ImportState.Ready(
                    fileName = fileName,
                    chunkCount = chunks.size,
                    preview = doc.text.take(2000)
                )
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Không đọc được file.")
            }
        }
    }

    fun generate() {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            _state.value = ImportState.Error(
                "Chưa có API key Gemini. Hãy vào Cài đặt để nhập key (miễn phí ở AI Studio)."
            )
            return
        }
        viewModelScope.launch {
            _state.value = ImportState.Generating(0, chunks.size)
            try {
                val result = repo.generateDrafts(
                    chunks, cardCountTarget, mcqRatio, key
                ) { done, total ->
                    _state.value = ImportState.Generating(done, total)
                }
                if (result.drafts.isEmpty()) {
                    _state.value = ImportState.Error(
                        "AI không sinh được thẻ nào. Hãy thử lại hoặc giảm số thẻ."
                    )
                } else {
                    _state.value = ImportState.Review(
                        result.drafts, result.invalidCount, result.failedChunks
                    )
                }
            } catch (e: AiQuotaException) {
                _state.value = ImportState.Error(e.message ?: "Hết lượt miễn phí.")
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Sinh thẻ thất bại.")
            }
        }
    }

    fun saveReviewed(deckName: String) {
        val cur = _state.value as? ImportState.Review ?: return
        viewModelScope.launch {
            try {
                val id = repo.saveDeckWithCards(deckName, pendingFileName, cur.drafts)
                _state.value = ImportState.Saving(id)
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Không lưu được bộ đề.")
            }
        }
    }

    fun backToIdle() {
        _state.value = ImportState.Idle
    }

    private suspend fun saveImported(dto: DeckImport, fileName: String) {
        val drafts = dto.cards.map {
            CardDraft(
                type = it.type.uppercase(),
                question = it.question,
                choices = it.choices,
                answer = it.answer
            )
        }
        val id = repo.saveDeckWithCards(dto.deck.name, fileName, drafts)
        _state.value = ImportState.Saving(id)
    }

    private fun exportJsonFor(deckName: String, drafts: List<CardDraft>): String =
        importJson.encodeToString(
            DeckImport(
                schemaVersion = 1,
                deck = DeckImportMeta(deckName, pendingFileName),
                cards = drafts.map {
                    CardImport(it.type.uppercase(), it.question, it.choices, it.answer)
                }
            )
        )

    private suspend fun readText(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?.toString(Charsets.UTF_8) ?: throw Exception("Không đọc được file.")
        }

    private suspend fun readBytes(context: Context, uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Không đọc được file.")
        }

    private fun queryName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repo: DeckRepository,
        private val apiKeyProvider: () -> String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ImportViewModel(repo, apiKeyProvider) as T
    }
}
