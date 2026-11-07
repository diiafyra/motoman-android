package io.github.diiafyra.motoman.core.domain.repository

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import kotlinx.coroutines.flow.Flow

public interface FuelEntryRepository {
    /** Fill-ups of one vehicle that are not deleted, newest first. */
    public fun observeEntries(vehicleId: String): Flow<List<FuelEntry>>

    /**
     * Inserts, or updates when an entry with this id exists. Saving never brings a deleted entry back,
     * and fails if the vehicle is deleted.
     */
    public suspend fun save(entry: FuelEntry)

    /** Soft delete, so that [restore] can undo it. */
    public suspend fun delete(id: String)

    /** Undoes [delete]. Does nothing if the vehicle has been deleted since. */
    public suspend fun restore(id: String)
}
