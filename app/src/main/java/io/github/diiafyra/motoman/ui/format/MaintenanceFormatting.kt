package io.github.diiafyra.motoman.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.DuePoint
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import kotlin.math.abs

@Composable
fun maintenanceStatusLabel(status: MaintenanceStatus): String =
    stringResource(
        when (status) {
            is MaintenanceStatus.Overdue -> R.string.status_overdue
            is MaintenanceStatus.DueSoon -> R.string.status_due_soon
            is MaintenanceStatus.Ok -> R.string.status_ok
            MaintenanceStatus.NoBaseline -> R.string.status_no_baseline
        },
    )

/**
 * How far off the due point is, on the side that comes first: "in 300 km", "5 days over", "today".
 * Null when there is no baseline.
 */
@Composable
fun maintenanceDueText(status: MaintenanceStatus): String? {
    val due =
        when (status) {
            is MaintenanceStatus.Overdue -> status.due
            is MaintenanceStatus.DueSoon -> status.due
            is MaintenanceStatus.Ok -> status.due
            MaintenanceStatus.NoBaseline -> return null
        }
    return dueText(due, isOverdue = status is MaintenanceStatus.Overdue)
}

@Composable
private fun dueText(
    due: DuePoint,
    isOverdue: Boolean,
): String {
    val km = due.remainingKm
    val days = due.remainingDays
    // When overdue, describe the side that is past due; otherwise the side with less left.
    val useDays =
        when {
            km == null -> true
            days == null -> false
            isOverdue -> days < 0 && (km >= 0 || days * KM_PER_DAY_ESTIMATE < km)
            else -> days * KM_PER_DAY_ESTIMATE < km
        }
    return if (useDays) {
        val d = requireNotNull(days)
        when {
            d == 0L -> stringResource(R.string.due_today)
            d > 0 -> pluralStringResource(R.plurals.due_in_days, d.toInt(), d.toInt())
            else -> pluralStringResource(R.plurals.due_over_days, abs(d).toInt(), abs(d).toInt())
        }
    } else {
        val k = requireNotNull(km)
        if (k >= 0) {
            stringResource(R.string.due_in_km, formatInteger(k.toLong()))
        } else {
            stringResource(R.string.due_over_km, formatInteger(abs(k).toLong()))
        }
    }
}

/**
 * Only used to pick which side to describe when both are set, never shown. A typical Vietnamese
 * motorbike rides 20–40 km a day.
 */
private const val KM_PER_DAY_ESTIMATE = 30
