package io.github.diiafyra.motoman.feature.fuel

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import java.time.YearMonth
import java.time.ZoneId

data class FuelMonth(
    val month: YearMonth,
    val totalCostVnd: Long,
    val totalVolumeMl: Long,
    /** Newest first. */
    val entries: List<FuelEntry>,
)

/** Groups fill-ups by calendar month in [zone], newest month first. */
fun groupByMonth(
    entries: List<FuelEntry>,
    zone: ZoneId,
): List<FuelMonth> =
    entries
        .sortedWith(compareByDescending<FuelEntry> { it.filledAt }.thenByDescending { it.odometerKm })
        .groupBy { YearMonth.from(it.filledAt.atZone(zone)) }
        .map { (month, inMonth) ->
            FuelMonth(
                month = month,
                totalCostVnd = inMonth.sumOf { it.totalCostVnd },
                totalVolumeMl = inMonth.sumOf { it.volumeMl },
                entries = inMonth,
            )
        }
