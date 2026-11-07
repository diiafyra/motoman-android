package io.github.diiafyra.motoman.core.domain.model

import java.time.LocalDate

/**
 * A recurring job such as an oil change. It is due by distance, by time, or both, whichever comes
 * first. At least one interval is always set.
 */
public data class MaintenanceItem(
    val id: String,
    val vehicleId: String,
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastDoneKm: Int?,
    val lastDoneDate: LocalDate?,
) {
    init {
        require(intervalKm != null || intervalMonths != null) { "A maintenance item needs at least one interval" }
        require(intervalKm == null || intervalKm in 1..MAX_ODOMETER_KM) { "intervalKm out of range" }
        require(intervalMonths == null || intervalMonths in 1..MAX_INTERVAL_MONTHS) { "intervalMonths out of range" }
        require(lastDoneKm == null || lastDoneKm in 0..MAX_ODOMETER_KM) { "lastDoneKm out of range" }
    }
}

/** Ten years. Longer intervals are typos, and the cap keeps date arithmetic small. */
public const val MAX_INTERVAL_MONTHS: Int = 120

public data class MaintenanceRecord(
    val id: String,
    val itemId: String,
    val vehicleId: String,
    val doneDate: LocalDate,
    val odometerKm: Int,
    val costVnd: Long?,
    val note: String = "",
)
