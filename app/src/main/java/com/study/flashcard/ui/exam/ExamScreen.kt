package com.study.flashcard.ui.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.model.decodedChoices
import com.study.flashcard.di.AppContainer

/** Màn hình thi thử: setup → làm bài bấm giờ → kết quả + xem lại câu sai. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(container: AppContainer, deckId: Long, onBack: () -> Unit) {
    val vm: ExamViewModel = viewModel(
        factory = ExamViewModel.Factory(
            deckId,
            container.studyRepository,
            apiKeyProvider = { container.securePrefs.geminiApiKey }
        )
    )
    val s by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (s.phase) {
                            ExamPhase.Setup -> "Thi thử"
                            ExamPhase.Doing -> "Còn ${s.secondsLeft / 60}:${(s.secondsLeft % 60).toString().padStart(2, '0')}"
                            ExamPhase.Result -> "Kết quả"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (s.phase) {
                ExamPhase.Setup -> SetupStep(s = s, vm = vm)
                ExamPhase.Doing -> DoingStep(s = s, vm = vm)
                ExamPhase.Result -> ResultStep(s = s, onBack = onBack)
            }
        }
    }
}

@Composable
private fun SetupStep(s: ExamUiState, vm: ExamViewModel) {
    Text("Số câu: ${s.questionCount}")
    Slider(
        value = s.questionCount.toFloat(),
        onValueChange = { vm.setCount(it.toInt()) },
        valueRange = 5f..50f,
        steps = 8
    )
    Text("Thời gian: ${s.minutes} phút")
    Slider(
        value = s.minutes.toFloat(),
        onValueChange = { vm.setMinutes(it.toInt()) },
        valueRange = 5f..120f,
        steps = 22
    )
    Button(onClick = vm::start, enabled = !s.starting, modifier = Modifier.fillMaxWidth()) {
        Text(if (s.starting) "Đang bốc đề..." else "Bắt đầu thi")
    }
    Text(
        "Trắc nghiệm chấm tự động. Tự luận: bấm \"AI chấm tham khảo\" rồi tự chốt điểm.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DoingStep(s: ExamUiState, vm: ExamViewModel) {
    val q = s.current ?: return
    LinearProgressIndicator(
        progress = { (s.index + 1) / s.questions.size.toFloat() },
        modifier = Modifier.fillMaxWidth()
    )
    Text("Câu ${s.index + 1}/${s.questions.size}")
    QuestionCard(q = q, answer = s.answers[q.id]?.text.orEmpty(), onAnswer = vm::answerCurrent)

    if (q.type.uppercase() == "ESSAY") {
        val ans = s.answers[q.id]
        OutlinedButton(
            onClick = vm::gradeCurrentWithAi,
            enabled = ans?.text?.isNotBlank() == true && ans.grading != true
        ) { Text(if (ans?.grading == true) "AI đang chấm..." else "AI chấm tham khảo") }
        ans?.aiGrading?.let { g ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("AI chấm ${g.score}/10 (tham khảo)", style = MaterialTheme.typography.labelLarge)
                    if (g.missingPoints.isNotEmpty()) {
                        Text("Còn thiếu: ${g.missingPoints.joinToString("; ")}")
                    }
                    if (g.feedback.isNotBlank()) Text("Nhận xét: ${g.feedback}")
                }
            }
            Text("Điểm bạn tự chốt: ${ans.essayScore ?: "chưa chốt"}")
            Slider(
                value = ans.essayScore ?: g.score,
                onValueChange = { vm.setEssayScore(it) },
                valueRange = 0f..10f,
                steps = 19
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = vm::prev, enabled = s.index > 0, modifier = Modifier.weight(1f)) {
            Text("Trước")
        }
        if (s.index < s.questions.lastIndex) {
            Button(onClick = vm::next, modifier = Modifier.weight(1f)) { Text("Tiếp") }
        } else {
            Button(onClick = vm::submit, modifier = Modifier.weight(1f)) { Text("Nộp bài") }
        }
    }
    Text("Nhảy tới câu:", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        s.questions.forEachIndexed { i, qq ->
            val done = s.answers[qq.id]?.text?.isNotBlank() == true
            FilterChip(
                selected = i == s.index,
                onClick = { vm.jump(i) },
                label = { Text(if (done) "✓${i + 1}" else "${i + 1}") }
            )
        }
    }
}

@Composable
private fun QuestionCard(q: CardEntity, answer: String, onAnswer: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(q.question, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (q.type.uppercase() == "MCQ") {
                val choices = q.decodedChoices() ?: listOf("(thẻ lỗi lựa chọn)")
                choices.forEach { c ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = answer == c, onClick = { onAnswer(c) })
                        Text(c, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else {
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswer,
                    label = { Text("Câu trả lời của bạn") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("Đáp án mẫu (xem sau khi nộp): ${q.answer.take(120)}...")
            }
        }
    }
}

@Composable
private fun ResultStep(s: ExamUiState, onBack: () -> Unit) {
    Text("Điểm trắc nghiệm: ${s.correctMcq}/${s.totalMcq}", style = MaterialTheme.typography.headlineSmall)
    s.essayAvg?.let { Text("Điểm tự luận trung bình (bạn chốt): ${"%.1f".format(it)}/10") }
    Spacer(Modifier.height(8.dp))
    Text("Xem lại câu sai:", style = MaterialTheme.typography.labelLarge)
    s.questions.forEachIndexed { i, q ->
        val ans = s.answers[q.id]?.text.orEmpty()
        val wrong = q.type.uppercase() == "MCQ" && ans != q.answer
        if (wrong || q.type.uppercase() == "ESSAY") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Câu ${i + 1}: ${q.question}", style = MaterialTheme.typography.bodyMedium)
                    if (wrong) {
                        Text("Bạn chọn: $ans (sai)", color = MaterialTheme.colorScheme.error)
                        Text("Đáp án đúng: ${q.answer}")
                    } else {
                        Text("Bài bạn: ${ans.ifBlank { "(bỏ trống)" }}")
                        Text("Đáp án mẫu: ${q.answer}")
                        s.answers[q.id]?.essayScore?.let { Text("Điểm bạn chốt: $it/10") }
                    }
                }
            }
        }
    }
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Về Bộ đề") }
}
