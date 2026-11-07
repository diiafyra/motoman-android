package io.github.diiafyra.motoman.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Small settings kept on the device only. */
public interface PreferencesRepository {
    /** The vehicle the user last chose, or null if they never chose one. */
    public fun observeSelectedVehicleId(): Flow<String?>

    public suspend fun setSelectedVehicleId(id: String)

    /** Maintenance reminders as notifications. On until the user turns them off. */
    public fun observeRemindersEnabled(): Flow<Boolean>

    public suspend fun setRemindersEnabled(enabled: Boolean)

    /** Keys of the reminders already notified, see `selectReminders`. */
    public suspend fun sentReminderKeys(): Set<String>

    public suspend fun setSentReminderKeys(keys: Set<String>)

    /** The user chose "not now" on the prompt to allow notifications. */
    public fun observeNotificationPromptDismissed(): Flow<Boolean>

    public suspend fun setNotificationPromptDismissed()
}
