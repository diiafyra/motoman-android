package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import java.time.LocalDate

public data class ItemStatus(
    val item: MaintenanceItem,
    val status: MaintenanceStatus,
)

/**
 * Every item with its status, most urgent first: overdue, then due soon, then ok, then items with no
 * baseline. Within a group, the item with the smallest share of its interval left comes first, so that
 * 200 km left on a 1,000 km interval ranks above 300 km left on a 10,000 km one. Ties keep the input
 * order.
 */
public fun rankByUrgency(
    items: List<MaintenanceItem>,
    currentOdometerKm: Int,
    today: LocalDate,
    threshold: DueSoonThreshold = DueSoonThreshold.DEFAULT,
): List<ItemStatus> =
    items
        .map { ItemStatus(it, maintenanceStatus(it, currentOdometerKm, today, threshold)) }
        .sortedWith(compareBy({ groupRank(it.status) }, { shareLeft(it) }))

private fun groupRank(status: MaintenanceStatus): Int =
    when (status) {
        is MaintenanceStatus.Overdue -> 0
        is MaintenanceStatus.DueSoon -> 1
        is MaintenanceStatus.Ok -> 2
        MaintenanceStatus.NoBaseline -> 3
    }

/** The smaller of distance left / interval and days left / interval days. Negative once overdue. */
private fun shareLeft(itemStatus: ItemStatus): Double {
    val due =
        when (val status = itemStatus.status) {
            is MaintenanceStatus.Overdue -> status.due
            is MaintenanceStatus.DueSoon -> status.due
            is MaintenanceStatus.Ok -> status.due
            MaintenanceStatus.NoBaseline -> return 0.0
        }
    val item = itemStatus.item
    val byKm =
        if (due.remainingKm != null &&
            item.intervalKm != null
        ) {
            due.remainingKm.toDouble() / item.intervalKm
        } else {
            null
        }
    val byDays =
        if (due.remainingDays != null && item.intervalMonths != null) {
            due.remainingDays / (item.intervalMonths * DAYS_PER_MONTH)
        } else {
            null
        }
    return listOfNotNull(byKm, byDays).minOrNull() ?: 0.0
}

private const val DAYS_PER_MONTH = 30.44
