package io.github.diiafyra.motoman.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.diiafyra.motoman.core.data.database.entity.MaintenanceItemEntity
import io.github.diiafyra.motoman.core.data.database.entity.MaintenanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_item WHERE vehicleId = :vehicleId AND deletedAt IS NULL ORDER BY createdAt, id")
    fun observeItems(vehicleId: String): Flow<List<MaintenanceItemEntity>>

    @Query(
        """
        SELECT * FROM maintenance_record
        WHERE vehicleId = :vehicleId AND deletedAt IS NULL
        ORDER BY doneDate DESC, odometerKm DESC, id DESC
        """,
    )
    fun observeRecords(vehicleId: String): Flow<List<MaintenanceRecordEntity>>

    /** The item whether or not it is deleted. */
    @Query("SELECT * FROM maintenance_item WHERE id = :id")
    suspend fun itemById(id: String): MaintenanceItemEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM maintenance_record WHERE id = :id)")
    suspend fun recordExists(id: String): Boolean

    @Upsert
    suspend fun upsertItem(item: MaintenanceItemEntity)

    @Upsert
    suspend fun upsertRecord(record: MaintenanceRecordEntity)

    @Query(
        "UPDATE maintenance_item SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1) WHERE id = :id AND deletedAt IS NULL",
    )
    suspend fun softDeleteItem(
        id: String,
        now: Long,
    )

    @Query(
        "UPDATE maintenance_record SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1) WHERE itemId = :itemId AND deletedAt IS NULL",
    )
    suspend fun softDeleteRecordsForItem(
        itemId: String,
        now: Long,
    )

    @Query(
        """
        UPDATE maintenance_item SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1)
        WHERE vehicleId = :vehicleId AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteItemsForVehicle(
        vehicleId: String,
        now: Long,
    )

    @Query(
        """
        UPDATE maintenance_record SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1)
        WHERE vehicleId = :vehicleId AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteRecordsForVehicle(
        vehicleId: String,
        now: Long,
    )
}
