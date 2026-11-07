package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import java.time.YearMonth
import java.time.ZoneId

/**
 * Spending and fuel economy for one calendar month.
 *
 * Spending counts every fill-up made in the month. Economy counts the valid full-tank segments that
 * ended in the month; a segment is not split across months, since the fuel inside it cannot be
 * placed on a date.
 */
public data class MonthStats(
    val month: YearMonth,
    val costVnd: Long,
    val volumeMl: Long,
    val segmentDistanceKm: Long,
    val segmentVolumeMl: Long,
    val segmentCostVnd: Long,
) {
    /** Null when no segment ended in the month. */
    val litersPer100Km: Double?
        get() = if (segmentDistanceKm == 0L) null else segmentVolumeMl / (10.0 * segmentDistanceKm)

    val costPerKmVnd: Double?
        get() = if (segmentDistanceKm == 0L) null else segmentCostVnd.toDouble() / segmentDistanceKm
}

/**
 * The [months] calendar months up to and including [lastMonth], oldest first. Months without any
 * fill-up are included with zeros, so that charts keep an even time axis.
 */
public fun monthlyStats(
    entries: List<FuelEntry>,
    lastMonth: YearMonth,
    months: Int,
    zone: ZoneId,
): List<MonthStats> {
    require(months > 0) { "months must be positive" }
    val first = lastMonth.minusMonths(months.toLong() - 1)
    val segmentsByMonth =
        calculateFuelEconomy(entries).segments.groupBy { YearMonth.from(it.endFilledAt.atZone(zone)) }
    val entriesByMonth = entries.groupBy { YearMonth.from(it.filledAt.atZone(zone)) }

    return (0 until months).map { offset ->
        val month = first.plusMonths(offset.toLong())
        val inMonth = entriesByMonth[month].orEmpty()
        val segments = segmentsByMonth[month].orEmpty()
        MonthStats(
            month = month,
            costVnd = inMonth.sumOf { it.totalCostVnd },
            volumeMl = inMonth.sumOf { it.volumeMl },
            segmentDistanceKm = segments.sumOf { it.distanceKm.toLong() },
            segmentVolumeMl = segments.sumOf { it.volumeMl },
            segmentCostVnd = segments.sumOf { it.costVnd },
        )
    }
}
