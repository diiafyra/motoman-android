package io.github.diiafyra.motoman.core.domain.model

import java.time.Instant

/**
 * One fill-up. Volume and cost are integers (millilitres and dong) so that sums over hundreds of
 * entries stay exact; they are converted to decimals only for display.
 */
public data class FuelEntry(
    val id: String,
    val vehicleId: String,
    val filledAt: Instant,
    val odometerKm: Int,
    val volumeMl: Long,
    val totalCostVnd: Long,
    /** The tank was filled to the top, which closes a measurable segment. */
    val isFullTank: Boolean,
    /** At least one fill-up before this one was not logged, so the fuel used is unknown. */
    val missedPrevious: Boolean,
    val note: String = "",
)
