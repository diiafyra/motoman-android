package io.github.diiafyra.motoman.core.domain

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToLong

internal val BASE_TIME: Instant = Instant.parse("2026-01-01T00:00:00Z")

/** The nth day after [BASE_TIME], so tests read as a sequence of days. */
internal fun day(n: Long): Instant = BASE_TIME.plusSeconds(n * 86_400)

internal fun vehicle(initialOdometerKm: Int = 1_000) =
    Vehicle(
        id = "v1",
        name = "Wave",
        type = VehicleType.MOTORBIKE,
        fuelType = FuelType.RON95,
        initialOdometerKm = initialOdometerKm,
    )

internal fun fill(
    id: String,
    day: Long,
    km: Int,
    liters: Double,
    costVnd: Long = (liters * 25_000).roundToLong(),
    full: Boolean = true,
    missedPrevious: Boolean = false,
    vehicleId: String = "v1",
) = FuelEntry(
    id = id,
    vehicleId = vehicleId,
    filledAt = day(day),
    odometerKm = km,
    // Rounded, not truncated: 1.15 * 1000 is 1149.999... as a Double.
    volumeMl = (liters * 1_000).roundToLong(),
    totalCostVnd = costVnd,
    isFullTank = full,
    missedPrevious = missedPrevious,
)

internal fun item(
    intervalKm: Int? = null,
    intervalMonths: Int? = null,
    lastDoneKm: Int? = null,
    lastDoneDate: LocalDate? = null,
) = MaintenanceItem(
    id = "i1",
    vehicleId = "v1",
    name = "Oil change",
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    lastDoneKm = lastDoneKm,
    lastDoneDate = lastDoneDate,
)

internal fun record(km: Int) =
    MaintenanceRecord(
        id = "r$km",
        itemId = "i1",
        vehicleId = "v1",
        doneDate = LocalDate.of(2026, 1, 1),
        odometerKm = km,
        costVnd = null,
    )
