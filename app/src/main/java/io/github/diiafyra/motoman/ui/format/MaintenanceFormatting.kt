package io.github.diiafyra.motoman.ui.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import java.text.NumberFormat
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

/** See [dueText]. Recomposes when the language changes. */
@Composable
fun maintenanceDueText(status: MaintenanceStatus): String? = dueText(LocalResources.current, status)

/**
 * How far off the due point is, on the side that comes first: "in 300 km", "5 days over", "today".
 * Null when there is no baseline. Plain function so that notifications can use it outside Compose.
 */
fun dueText(
    resources: Resources,
    status: MaintenanceStatus,
): String? {
    val due =
        when (status) {
            is MaintenanceStatus.Overdue -> status.due
            is MaintenanceStatus.DueSoon -> status.due
            is MaintenanceStatus.Ok -> status.due
            MaintenanceStatus.NoBaseline -> return null
        }
    val km = due.remainingKm
    val days = due.remainingDays
    // When overdue, describe the side that is past due; otherwise the side with less left.
    val useDays =
        when {
            km == null -> true
            days == null -> false
            status is MaintenanceStatus.Overdue -> days < 0 && (km >= 0 || days * KM_PER_DAY_ESTIMATE < km)
            else -> days * KM_PER_DAY_ESTIMATE < km
        }
    return if (useDays) {
        val d = requireNotNull(days).toInt()
        when {
            d == 0 -> resources.getString(R.string.due_today)
            d > 0 -> resources.getQuantityString(R.plurals.due_in_days, d, d)
            else -> resources.getQuantityString(R.plurals.due_over_days, abs(d), abs(d))
        }
    } else {
        val k = requireNotNull(km)
        val formatted = NumberFormat.getIntegerInstance(resources.configuration.locales[0]).format(abs(k).toLong())
        resources.getString(if (k >= 0) R.string.due_in_km else R.string.due_over_km, formatted)
    }
}

/**
 * Only used to pick which side to describe when both are set, never shown. A typical Vietnamese
 * motorbike rides 20–40 km a day.
 */
private const val KM_PER_DAY_ESTIMATE = 30
