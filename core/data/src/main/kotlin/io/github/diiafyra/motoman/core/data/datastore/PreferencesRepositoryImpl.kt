package io.github.diiafyra.motoman.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class PreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : PreferencesRepository {
        override fun observeSelectedVehicleId(): Flow<String?> = observe { it[SELECTED_VEHICLE_ID] }

        override suspend fun setSelectedVehicleId(id: String) {
            dataStore.edit { it[SELECTED_VEHICLE_ID] = id }
        }

        override fun observeRemindersEnabled(): Flow<Boolean> = observe { it[REMINDERS_ENABLED] ?: true }

        override suspend fun setRemindersEnabled(enabled: Boolean) {
            dataStore.edit { it[REMINDERS_ENABLED] = enabled }
        }

        override suspend fun sentReminderKeys(): Set<String> = dataStore.data.first()[SENT_REMINDER_KEYS].orEmpty()

        override suspend fun setSentReminderKeys(keys: Set<String>) {
            dataStore.edit { it[SENT_REMINDER_KEYS] = keys }
        }

        override fun observeNotificationPromptDismissed(): Flow<Boolean> =
            observe { it[NOTIFICATION_PROMPT_DISMISSED] ?: false }

        override suspend fun setNotificationPromptDismissed() {
            dataStore.edit { it[NOTIFICATION_PROMPT_DISMISSED] = true }
        }

        private fun <T> observe(read: (Preferences) -> T): Flow<T> = dataStore.data.map(read).distinctUntilChanged()

        private companion object {
            val SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
            val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
            val SENT_REMINDER_KEYS = stringSetPreferencesKey("sent_reminder_keys")
            val NOTIFICATION_PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
        }
    }
