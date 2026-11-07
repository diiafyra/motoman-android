package io.github.diiafyra.motoman.core.data.database.entity

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import java.time.Instant
import java.time.LocalDate

/** Timestamps written with a row. See [RowState.timesForSave]. */
internal data class RowTimes(
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/** The bookkeeping columns of a row already in the database. */
internal data class RowState(
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/**
 * Timestamps for saving over this existing row, or for a new row when the receiver is null.
 *
 * - `createdAt` and `deletedAt` are kept: a save never brings a deleted row back. A stale edit screen
 *   saving after the row was deleted elsewhere (or, later, by sync) leaves it deleted.
 * - `updatedAt` never moves backwards, even if the device clock does, so that "newer wins" during sync
 *   keeps meaning "edited later".
 */
internal fun RowState?.timesForSave(
    now: Long,
    newCreatedAt: Long = now,
): RowTimes =
    if (this == null) {
        RowTimes(createdAt = newCreatedAt, updatedAt = now, deletedAt = null)
    } else {
        RowTimes(createdAt = createdAt, updatedAt = maxOf(now, updatedAt + 1), deletedAt = deletedAt)
    }

internal fun VehicleEntity.toDomain() =
    Vehicle(
        id = id,
        name = name,
        type = VehicleType.valueOf(type),
        fuelType = FuelType.valueOf(fuelType),
        initialOdometerKm = initialOdometerKm,
    )

internal fun Vehicle.toEntity(times: RowTimes) =
    VehicleEntity(
        id = id,
        name = name,
        type = type.name,
        fuelType = fuelType.name,
        initialOdometerKm = initialOdometerKm,
        createdAt = times.createdAt,
        updatedAt = times.updatedAt,
        deletedAt = times.deletedAt,
    )

internal fun FuelEntryEntity.toDomain() =
    FuelEntry(
        id = id,
        vehicleId = vehicleId,
        filledAt = Instant.ofEpochMilli(filledAt),
        odometerKm = odometerKm,
        volumeMl = volumeMl,
        totalCostVnd = totalCostVnd,
        isFullTank = isFullTank,
        missedPrevious = missedPrevious,
        note = note,
    )

internal fun FuelEntry.toEntity(times: RowTimes) =
    FuelEntryEntity(
        id = id,
        vehicleId = vehicleId,
        filledAt = filledAt.toEpochMilli(),
        odometerKm = odometerKm,
        volumeMl = volumeMl,
        totalCostVnd = totalCostVnd,
        isFullTank = isFullTank,
        missedPrevious = missedPrevious,
        note = note,
        createdAt = times.createdAt,
        updatedAt = times.updatedAt,
        deletedAt = times.deletedAt,
    )

internal fun MaintenanceItemEntity.toDomain() =
    MaintenanceItem(
        id = id,
        vehicleId = vehicleId,
        name = name,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        lastDoneKm = lastDoneKm,
        lastDoneDate = lastDoneDate?.let(LocalDate::ofEpochDay),
    )

internal fun MaintenanceItem.toEntity(times: RowTimes) =
    MaintenanceItemEntity(
        id = id,
        vehicleId = vehicleId,
        name = name,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        lastDoneKm = lastDoneKm,
        lastDoneDate = lastDoneDate?.toEpochDay(),
        createdAt = times.createdAt,
        updatedAt = times.updatedAt,
        deletedAt = times.deletedAt,
    )

internal fun MaintenanceRecordEntity.toDomain() =
    MaintenanceRecord(
        id = id,
        itemId = itemId,
        vehicleId = vehicleId,
        doneDate = LocalDate.ofEpochDay(doneDate),
        odometerKm = odometerKm,
        costVnd = costVnd,
        note = note,
    )

internal fun MaintenanceRecord.toEntity(times: RowTimes) =
    MaintenanceRecordEntity(
        id = id,
        itemId = itemId,
        vehicleId = vehicleId,
        doneDate = doneDate.toEpochDay(),
        odometerKm = odometerKm,
        costVnd = costVnd,
        note = note,
        createdAt = times.createdAt,
        updatedAt = times.updatedAt,
        deletedAt = times.deletedAt,
    )
