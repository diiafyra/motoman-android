package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import java.time.Instant

/**
 * Fuel used between two consecutive full-tank fill-ups. Both ends are full, so the fuel bought after
 * the first one up to and including the second one is exactly what was burned over [distanceKm].
 */
public data class FuelSegment(
    val startEntryId: String,
    val endEntryId: String,
    val endFilledAt: Instant,
    val distanceKm: Int,
    val volumeMl: Long,
    val costVnd: Long,
) {
    init {
        require(distanceKm > 0) { "A segment must cover a positive distance" }
    }

    /** mL per km × 100 km / 1000 mL per L = mL / (10 × km). */
    val litersPer100Km: Double get() = volumeMl / (10.0 * distanceKm)

    val costPerKmVnd: Double get() = costVnd.toDouble() / distanceKm
}

public data class FuelEconomy(
    /** Valid segments, oldest first. */
    val segments: List<FuelSegment>,
) {
    /**
     * The most recent valid segment. When the newest segment was dropped (a missed fill-up), this is
     * an older one; compare [FuelSegment.endEntryId] with the newest full fill-up to tell.
     */
    val latest: FuelSegment? get() = segments.lastOrNull()

    /**
     * Total fuel over total distance, not the mean of per-segment figures, so that a short segment
     * does not weigh as much as a long one. Null until there is at least one valid segment.
     */
    val averageLitersPer100Km: Double?
        get() = if (segments.isEmpty()) null else segments.sumOf { it.volumeMl } / (10.0 * totalDistanceKm)

    val averageCostPerKmVnd: Double?
        get() = if (segments.isEmpty()) null else segments.sumOf { it.costVnd }.toDouble() / totalDistanceKm

    private val totalDistanceKm: Long get() = segments.sumOf { it.distanceKm.toLong() }
}

/**
 * Full-tank method. A segment runs from one full fill-up (i) to the next full fill-up (j); its fuel is
 * the sum of every fill-up after i up to and including j, partial ones included.
 *
 * A segment is dropped when:
 * - any fill-up in (i, j] is marked [FuelEntry.missedPrevious]: some fuel was never logged, so the
 *   figure would be too low;
 * - its distance is not positive, which only happens with inconsistent odometer readings.
 *
 * Fill-ups before the first full tank cannot be measured and are ignored.
 *
 * @param entries the fill-ups of one vehicle, in any order.
 */
public fun calculateFuelEconomy(entries: List<FuelEntry>): FuelEconomy {
    require(entries.distinctBy { it.vehicleId }.size <= 1) { "Entries must belong to one vehicle" }

    // Ties on time and reading: partial fills first, so a full tank logged at the same moment closes
    // the segment after them; then id, so the order never depends on how the database returned rows.
    val sorted =
        entries.sortedWith(
            compareBy<FuelEntry>({ it.filledAt }, { it.odometerKm }, { it.isFullTank }, { it.id }),
        )
    val segments = mutableListOf<FuelSegment>()
    var lastFullIndex: Int? = null

    for ((index, entry) in sorted.withIndex()) {
        if (!entry.isFullTank) continue
        val startIndex = lastFullIndex
        lastFullIndex = index
        if (startIndex == null) continue

        val start = sorted[startIndex]
        val window = sorted.subList(startIndex + 1, index + 1)
        val distanceKm = entry.odometerKm - start.odometerKm
        if (distanceKm <= 0 || window.any { it.missedPrevious }) continue

        segments +=
            FuelSegment(
                startEntryId = start.id,
                endEntryId = entry.id,
                endFilledAt = entry.filledAt,
                distanceKm = distanceKm,
                volumeMl = window.sumOf { it.volumeMl },
                costVnd = window.sumOf { it.totalCostVnd },
            )
    }
    return FuelEconomy(segments)
}
