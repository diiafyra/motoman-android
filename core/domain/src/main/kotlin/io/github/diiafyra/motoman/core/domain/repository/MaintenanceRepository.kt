package io.github.diiafyra.motoman.core.domain.repository

import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

public interface MaintenanceRepository {
    /** Items of one vehicle that are not deleted, in the order they were created. */
    public fun observeItems(vehicleId: String): Flow<List<MaintenanceItem>>

    /** Records of one vehicle that are not deleted, newest first. */
    public fun observeRecords(vehicleId: String): Flow<List<MaintenanceRecord>>

    /**
     * Inserts the item, or updates its name and intervals. The last-done values of an existing item are
     * kept: they only change through [recordDone].
     */
    public suspend fun saveItem(item: MaintenanceItem)

    /** [saveItem] for several items in one transaction, keeping their order. */
    public suspend fun saveItems(items: List<MaintenanceItem>)

    /** Soft-deletes the item and its records. */
    public suspend fun deleteItem(id: String)

    /**
     * Stores a new record and, in the same transaction, moves the item's last-done values to it unless
     * the item already has a later one (a record entered after the fact for an older service).
     * Editing an existing record is not supported.
     */
    public suspend fun recordDone(record: MaintenanceRecord)
}
