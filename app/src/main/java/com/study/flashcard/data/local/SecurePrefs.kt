package com.study.flashcard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Lưu mã hóa: API key Gemini + giờ nhắc học.
 * Key chỉ nằm trên máy, app gọi thẳng tới Google, không qua server trung gian.
 */
class SecurePrefs(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var geminiApiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_HOUR, value.coerceIn(0, 23)).apply()

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val FILE_NAME = "secure_prefs"
        private const val KEY_API = "gemini_api_key"
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_ENABLED = "reminder_enabled"
    }
}
