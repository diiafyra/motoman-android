package io.github.diiafyra.motoman.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Small settings kept on the device only. */
public interface PreferencesRepository {
    /** The vehicle the user last chose, or null if they never chose one. */
    public fun observeSelectedVehicleId(): Flow<String?>

    public suspend fun setSelectedVehicleId(id: String)
}
