package com.study.flashcard.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.flashcard.data.local.CardEntity
import com.study.flashcard.data.model.decodedChoices
import com.study.flashcard.data.model.isMcq
import com.study.flashcard.data.study.Sm2Engine
import com.study.flashcard.di.AppContainer

/** Màn hình học flashcard: xem câu hỏi → lật đáp án → tự đánh Chưa nhớ/Nhớ/Thuộc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(container: AppContainer, deckId: Long, onBack: () -> Unit) {
    val vm: StudyViewModel = viewModel(
        factory = StudyViewModel.Factory(deckId, container.studyRepository)
    )
    val s by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Học flashcard") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                s.loading -> Text("Đang tải thẻ...")
                s.finished || s.current == null -> FinishedStep(s = s, onBack = onBack)
                else -> {
                    val card = s.current!!
                    val total = s.queue.size
                    Text("Thẻ ${s.index + 1}/$total")
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else s.index / total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    StudyCard(card = card, flipped = s.flipped, onFlip = vm::flip)
                    if (s.flipped) {
                        GradeRow(
                            onAgain = { vm.grade(Sm2Engine.Grade.AGAIN) },
                            onGood = { vm.grade(Sm2Engine.Grade.GOOD) },
                            onEasy = { vm.grade(Sm2Engine.Grade.EASY) }
                        )
                    } else {
                        Button(onClick = vm::flip, modifier = Modifier.fillMaxWidth()) {
                            Text("Lật xem đáp án")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCard(card: CardEntity, flipped: Boolean, onFlip: () -> Unit) {
    Card(onClick = onFlip, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (card.isMcq()) "Trắc nghiệm" else "Tự luận",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(card.question, style = MaterialTheme.typography.titleMedium)
            if (card.isMcq()) {
                Spacer(Modifier.height(12.dp))
                // MCQ: lúc chưa lật chỉ hiện lựa chọn, che đáp án.
                val choices = card.decodedChoices() ?: listOf("(thẻ lỗi lựa chọn)")
                choices.forEachIndexed { i, c ->
                    val suffix = if (flipped && c == card.answer) " ✓" else ""
                    Text("${'A' + i}. $c$suffix", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (flipped) {
                Spacer(Modifier.height(12.dp))
                Text("Đáp án:", style = MaterialTheme.typography.labelLarge)
                Text(card.answer, style = MaterialTheme.typography.bodyLarge)
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "(Bấm để lật xem đáp án, rồi tự đánh giá thật lòng nhé)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GradeRow(onAgain: () -> Unit, onGood: () -> Unit, onEasy: () -> Unit) {
    Text("Bạn nhớ câu này ở mức nào?", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onAgain, modifier = Modifier.weight(1f)) { Text("Chưa nhớ") }
        Button(onClick = onGood, modifier = Modifier.weight(1f)) { Text("Nhớ") }
        OutlinedButton(onClick = onEasy, modifier = Modifier.weight(1f)) { Text("Thuộc") }
    }
}

@Composable
private fun FinishedStep(s: StudyUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Xong buổi học!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Thuộc: ${s.easyCount} • Nhớ: ${s.goodCount} • Chưa nhớ: ${s.againCount}")
        Spacer(Modifier.height(4.dp))
        Text(
            "Thẻ \"Chưa nhớ\" đã được ôn lại ngay trong buổi. " +
                "Thẻ còn lại sẽ hẹn sang hôm sau.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Về Bộ đề") }
    }
}
