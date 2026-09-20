package com.study.flashcard.ui.importscreen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.flashcard.data.ai.CardDraft
import com.study.flashcard.di.AppContainer

/** Màn hình Import: chọn file → xem preview → chọn số thẻ → AI sinh → duyệt → lưu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(container: AppContainer, onDone: () -> Unit) {
    val context = LocalContext.current
    val vm: ImportViewModel = viewModel(
        factory = ImportViewModel.Factory(
            container.deckRepository,
            apiKeyProvider = { container.securePrefs.geminiApiKey }
        )
    )
    val state by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.pickFile(context, uri)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Import đề cương") }) }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val s = state) {
                is ImportState.Idle, is ImportState.Error -> {
                    if (s is ImportState.Error) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                    Text("Chọn file .docx, .pdf (AI sẽ sinh thẻ) hoặc .json đã xuất từ app.")
                    Button(onClick = {
                        picker.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/pdf",
                            "application/json"
                        ))
                    }) { Text("Chọn file") }
                }
                is ImportState.Parsing -> {
                    CircularProgressIndicator()
                    Text("Đang đọc ${s.fileName}...")
                }
                is ImportState.Ready -> ReadyStep(
                    state = s,
                    onGenerate = { count, mcq -> vm.setOptions(count, mcq); vm.generate() }
                )
                is ImportState.Generating -> {
                    Text("AI đang sinh thẻ... (${s.done}/${s.total} đoạn)")
                    LinearProgressIndicator(
                        progress = { if (s.total == 0) 0f else s.done / s.total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Giữ màn hình, đừng tắt app. Mỗi đoạn mất vài giây.")
                }
                is ImportState.Review -> ReviewStep(
                    state = s,
                    initialName = "",
                    onSave = vm::saveReviewed
                )
                is ImportState.Saving -> {
                    Text("Đã lưu bộ đề! Về trang Bộ đề để bắt đầu học nhé.")
                    Button(onClick = onDone) { Text("Về Bộ đề") }
                }
            }
        }
    }
}

@Composable
private fun ReadyStep(state: ImportState.Ready, onGenerate: (Int, Int) -> Unit) {
    var count by remember { mutableIntStateOf(20) }
    var mcq by remember { mutableFloatStateOf(60f) }
    Text("Đã đọc: ${state.fileName} (${state.chunkCount} đoạn)")
    Text("Xem trước nội dung:", style = MaterialTheme.typography.labelLarge)
    Card(Modifier.fillMaxWidth()) {
        Text(
            state.preview.take(500) + if (state.preview.length > 500) "..." else "",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text("Số thẻ muốn sinh: $count")
    Slider(
        value = count.toFloat(),
        onValueChange = { count = it.toInt().coerceIn(5, 100) },
        valueRange = 5f..100f,
        steps = 18
    )
    Text("Tỉ lệ trắc nghiệm: ${mcq.toInt()}%")
    Slider(value = mcq, onValueChange = { mcq = it }, valueRange = 0f..100f, steps = 19)
    Button(onClick = { onGenerate(count, mcq.toInt()) }) { Text("Sinh thẻ bằng AI") }
}

@Composable
private fun ReviewStep(state: ImportState.Review, initialName: String, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    Text("AI sinh được ${state.drafts.size} thẻ.")
    if (state.invalidCount > 0) {
        Text("${state.invalidCount} thẻ lỗi đã bị loại.", color = MaterialTheme.colorScheme.error)
    }
    if (state.failedChunks > 0) {
        Text(
            "${state.failedChunks} đoạn bị lỗi (mất mạng/quota) — phần làm được vẫn giữ.",
            color = MaterialTheme.colorScheme.error
        )
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Tên bộ đề (tên môn)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(state.drafts, key = { i, d -> "$i-${d.question.hashCode()}" }) { i, draft ->
            DraftCard(index = i + 1, draft = draft)
        }
    }
    Button(
        onClick = { onSave(name.ifBlank { "Bộ đề mới" }) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Lưu bộ đề") }
}

@Composable
private fun DraftCard(index: Int, draft: CardDraft) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Câu $index [${if (draft.type == "MCQ") "Trắc nghiệm" else "Tự luận"}]",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(draft.question, style = MaterialTheme.typography.bodyMedium)
            draft.choices?.forEachIndexed { ci, c ->
                Text("${'A' + ci}. $c", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text("Đáp án: ${draft.answer}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
