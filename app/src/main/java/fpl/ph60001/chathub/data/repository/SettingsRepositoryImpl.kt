package fpl.ph60001.chathub.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import fpl.ph60001.chathub.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Hiện thực hóa SettingsRepository bằng cách sử dụng Jetpack DataStore Preferences.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_MUTED_CONVERSATIONS = stringPreferencesKey("muted_conversations")
    }

    override val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    override val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override val mutedConversations: Flow<Map<String, Long>> = dataStore.data.map { preferences ->
        parseMutedMap(preferences[KEY_MUTED_CONVERSATIONS])
    }

    override suspend fun muteConversation(conversationId: String, durationMs: Long) {
        dataStore.edit { preferences ->
            val currentRaw = preferences[KEY_MUTED_CONVERSATIONS]
            val currentMap = parseMutedMap(currentRaw).toMutableMap()
            val expiry = if (durationMs == -1L) -1L else System.currentTimeMillis() + durationMs
            currentMap[conversationId] = expiry
            preferences[KEY_MUTED_CONVERSATIONS] = formatMutedMap(currentMap)
        }
    }

    override suspend fun unmuteConversation(conversationId: String) {
        dataStore.edit { preferences ->
            val currentRaw = preferences[KEY_MUTED_CONVERSATIONS]
            val currentMap = parseMutedMap(currentRaw).toMutableMap()
            currentMap.remove(conversationId)
            preferences[KEY_MUTED_CONVERSATIONS] = formatMutedMap(currentMap)
        }
    }

    private fun parseMutedMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, Long>()
        raw.split(";").forEach { item ->
            val parts = item.split(":")
            if (parts.size == 2) {
                val id = parts[0]
                val expiry = parts[1].toLongOrNull() ?: 0L
                map[id] = expiry
            }
        }
        return map
    }

    private fun formatMutedMap(map: Map<String, Long>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }
}
