package io.github.diiafyra.motoman.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.diiafyra.motoman.core.data.database.entity.RowState
import io.github.diiafyra.motoman.core.data.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface VehicleDao {
    @Query("SELECT * FROM vehicle WHERE deletedAt IS NULL ORDER BY createdAt, id")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicle WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: String): Flow<VehicleEntity?>

    @Query("SELECT createdAt, updatedAt, deletedAt FROM vehicle WHERE id = :id")
    suspend fun rowState(id: String): RowState?

    @Query("SELECT EXISTS(SELECT 1 FROM vehicle WHERE id = :id AND deletedAt IS NULL)")
    suspend fun isActive(id: String): Boolean

    // @Upsert updates the existing row in place. @Insert(REPLACE) would delete it first and cascade
    // the delete to every fill-up and maintenance row of the vehicle.
    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Query(
        "UPDATE vehicle SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1) WHERE id = :id AND deletedAt IS NULL",
    )
    suspend fun softDelete(
        id: String,
        now: Long,
    )
}
