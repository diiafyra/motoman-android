package io.github.diiafyra.motoman.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Every table has a client-generated UUID id plus createdAt, updatedAt and deletedAt (epoch millis).
// Rows are soft-deleted so that undo is a single update and so that sync can later upload deletions.

@Entity(tableName = "vehicle")
internal data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** [io.github.diiafyra.motoman.core.domain.model.VehicleType] name. */
    val type: String,
    /** [io.github.diiafyra.motoman.core.domain.model.FuelType] name. */
    val fuelType: String,
    val initialOdometerKm: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "fuel_entry",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId", "filledAt")],
)
internal data class FuelEntryEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    /** Epoch millis. */
    val filledAt: Long,
    val odometerKm: Int,
    val volumeMl: Long,
    val totalCostVnd: Long,
    val isFullTank: Boolean,
    val missedPrevious: Boolean,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "maintenance_item",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId")],
)
internal data class MaintenanceItemEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastDoneKm: Int?,
    /** Epoch day. */
    val lastDoneDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "maintenance_record",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId"), Index("vehicleId", "doneDate")],
)
internal data class MaintenanceRecordEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val vehicleId: String,
    /** Epoch day. */
    val doneDate: Long,
    val odometerKm: Int,
    val costVnd: Long?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
