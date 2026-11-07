package io.github.diiafyra.motoman.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.diiafyra.motoman.core.data.database.entity.FuelEntryEntity
import io.github.diiafyra.motoman.core.data.database.entity.RowState
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FuelEntryDao {
    @Query(
        """
        SELECT * FROM fuel_entry
        WHERE vehicleId = :vehicleId AND deletedAt IS NULL
        ORDER BY filledAt DESC, odometerKm DESC, id DESC
        """,
    )
    fun observeForVehicle(vehicleId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT createdAt, updatedAt, deletedAt FROM fuel_entry WHERE id = :id")
    suspend fun rowState(id: String): RowState?

    @Upsert
    suspend fun upsert(entry: FuelEntryEntity)

    @Query(
        "UPDATE fuel_entry SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1) WHERE id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(
        id: String,
        now: Long,
    )

    /** Does nothing when the vehicle itself has been deleted in the meantime. */
    @Query(
        """
        UPDATE fuel_entry SET deletedAt = NULL, updatedAt = MAX(:now, updatedAt + 1)
        WHERE id = :id AND deletedAt IS NOT NULL
          AND vehicleId IN (SELECT id FROM vehicle WHERE deletedAt IS NULL)
        """,
    )
    suspend fun restore(
        id: String,
        now: Long,
    )

    @Query(
        "UPDATE fuel_entry SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1) WHERE vehicleId = :vehicleId AND deletedAt IS NULL",
    )
    suspend fun softDeleteForVehicle(
        vehicleId: String,
        now: Long,
    )
}
