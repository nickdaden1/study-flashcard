package com.study.flashcard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.study.flashcard.di.AppContainer

/** Màn hình Cài đặt: nhập API key Gemini + giờ nhắc học hằng ngày. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val prefs = remember { container.securePrefs }
    var key by remember { mutableStateOf(prefs.geminiApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var reminderOn by remember { mutableStateOf(prefs.reminderEnabled) }
    var hour by remember { mutableFloatStateOf(prefs.reminderHour.toFloat()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Cài đặt") }) }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("API key Gemini", style = MaterialTheme.typography.titleMedium)
            Text(
                "Lấy key miễn phí ở AI Studio (aistudio.google.com → Get API key), " +
                    "dán vào đây. Key chỉ lưu mã hóa trên máy này.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim(); saved = false },
                label = { Text("Gemini API key") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    prefs.geminiApiKey = key
                    saved = true
                }) { Text("Lưu key") }
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Ẩn" else "Hiện")
                }
                if (saved) Text("Đã lưu!", color = MaterialTheme.colorScheme.primary)
            }

            Text("Nhắc học hằng ngày", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bật nhắc học", modifier = Modifier.weight(1f))
                Switch(
                    checked = reminderOn,
                    onCheckedChange = {
                        reminderOn = it
                        prefs.reminderEnabled = it
                    }
                )
            }
            if (reminderOn) {
                Text("Giờ nhắc: ${hour.toInt()}h00")
                Slider(
                    value = hour,
                    onValueChange = {
                        hour = it
                        prefs.reminderHour = it.toInt()
                    },
                    valueRange = 6f..23f,
                    steps = 16
                )
                Text(
                    "App sẽ gửi thông báo nhắc ôn thẻ đến hạn mỗi tối (áp dụng từ Phase 6).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
