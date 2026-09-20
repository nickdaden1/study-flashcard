package com.study.flashcard.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.flashcard.data.model.DeckWithProgress
import com.study.flashcard.di.AppContainer
import kotlinx.coroutines.launch

/** Màn hình Bộ đề: danh sách đề cương + % thuộc, nút học / thi / xóa / đổi tên. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenImport: () -> Unit,
    onStudy: (Long) -> Unit,
    onExam: (Long) -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container.deckRepository))
    val decks by vm.decks.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var renameTarget by remember { mutableStateOf<DeckWithProgress?>(null) }
    var deleteTarget by remember { mutableStateOf<DeckWithProgress?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bộ đề của tôi") }) },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            Button(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                Text("+ Import đề cương (Word/PDF)")
            }
            Spacer(Modifier.height(12.dp))
            if (decks.isEmpty()) {
                Text(
                    "Chưa có bộ đề nào. Bấm nút trên để import file đề cương thầy cô cho, " +
                        "AI sẽ sinh flashcard để bạn học.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(decks, key = { it.deck.id }) { item ->
                        DeckCard(
                            item = item,
                            onStudy = { onStudy(item.deck.id) },
                            onExam = { onExam(item.deck.id) },
                            onRename = { renameTarget = item },
                            onDelete = { deleteTarget = item }
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { item ->
        var name by remember(item.deck.id) { mutableStateOf(item.deck.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Đổi tên bộ đề") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên môn") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameDeck(item.deck.id, name) { msg ->
                        scope.launch { snack.showSnackbar(msg) }
                    }
                    renameTarget = null
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Hủy") }
            }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa bộ đề?") },
            text = { Text("Xóa \"${item.deck.name}\" cùng toàn bộ ${item.total} thẻ và lịch sử thi?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDeck(item.deck.id)
                    deleteTarget = null
                }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun DeckCard(
    item: DeckWithProgress,
    onStudy: () -> Unit,
    onExam: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onStudy)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.deck.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.mastered}/${item.total} thẻ đã thuộc • ${item.due} thẻ đến hạn",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("${item.percent}%", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStudy, enabled = item.due > 0) { Text("Học") }
                OutlinedButton(onClick = onExam, enabled = item.total > 0) { Text("Thi thử") }
                TextButton(onClick = onRename) { Text("Đổi tên") }
                TextButton(onClick = onDelete) { Text("Xóa") }
            }
            if (item.due == 0 && item.total > 0) {
                Text(
                    "Hôm nay hết thẻ đến hạn. Mai quay lại nhé!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
