package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How close an item must be to count as due soon. The values come from Remote Config later. */
public data class DueSoonThreshold(
    val km: Int,
    val days: Int,
) {
    init {
        require(km >= 0 && days >= 0) { "Thresholds cannot be negative" }
    }

    public companion object {
        public val DEFAULT: DueSoonThreshold = DueSoonThreshold(km = 500, days = 14)
    }
}

/**
 * When an item is next due. A side is null when the item has no interval of that kind or no last-done
 * value for it. Remaining values are negative once the item is past due.
 */
public data class DuePoint(
    val dueKm: Int?,
    val dueDate: LocalDate?,
    val remainingKm: Int?,
    val remainingDays: Long?,
)

public sealed interface MaintenanceStatus {
    /** Never done, or done without the reading its interval needs. Nothing to count from. */
    public data object NoBaseline : MaintenanceStatus

    public data class Overdue(
        val due: DuePoint,
    ) : MaintenanceStatus

    public data class DueSoon(
        val due: DuePoint,
    ) : MaintenanceStatus

    public data class Ok(
        val due: DuePoint,
    ) : MaintenanceStatus
}

/**
 * The highest reading the app knows of. Odometers only go up, so the largest value among the
 * initial reading, fill-ups and maintenance records is the closest to the real one.
 */
public fun currentOdometerKm(
    vehicle: Vehicle,
    fuelEntries: List<FuelEntry>,
    records: List<MaintenanceRecord>,
): Int {
    require(fuelEntries.all { it.vehicleId == vehicle.id } && records.all { it.vehicleId == vehicle.id }) {
        "Entries and records must belong to the vehicle"
    }
    return maxOf(
        vehicle.initialOdometerKm,
        fuelEntries.maxOfOrNull { it.odometerKm } ?: Int.MIN_VALUE,
        records.maxOfOrNull { it.odometerKm } ?: Int.MIN_VALUE,
    )
}

/**
 * Due by distance or by time, whichever comes first.
 *
 * - Overdue: past the due reading or the due date. Reaching it exactly is due, not overdue.
 * - Due soon: within [threshold] on either side.
 * - Ok: otherwise.
 */
public fun maintenanceStatus(
    item: MaintenanceItem,
    currentOdometerKm: Int,
    today: LocalDate,
    threshold: DueSoonThreshold = DueSoonThreshold.DEFAULT,
): MaintenanceStatus {
    val dueKm = if (item.intervalKm != null && item.lastDoneKm != null) item.lastDoneKm + item.intervalKm else null
    val dueDate =
        if (item.intervalMonths != null && item.lastDoneDate != null) {
            item.lastDoneDate.plusMonths(item.intervalMonths.toLong())
        } else {
            null
        }
    if (dueKm == null && dueDate == null) return MaintenanceStatus.NoBaseline

    val remainingKm = dueKm?.let { it - currentOdometerKm }
    val remainingDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
    val due = DuePoint(dueKm, dueDate, remainingKm, remainingDays)

    val overdue = (remainingKm != null && remainingKm < 0) || (remainingDays != null && remainingDays < 0)
    val dueSoon =
        (remainingKm != null && remainingKm <= threshold.km) ||
            (remainingDays != null && remainingDays <= threshold.days)

    return when {
        overdue -> MaintenanceStatus.Overdue(due)
        dueSoon -> MaintenanceStatus.DueSoon(due)
        else -> MaintenanceStatus.Ok(due)
    }
}
