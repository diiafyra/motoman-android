package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MAX_INTERVAL_MONTHS
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import java.time.LocalDate

/** The item form. Null means empty. */
public data class MaintenanceItemDraft(
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
)

public data class MaintenanceItemErrors(
    val name: ItemNameError? = null,
    val intervalKm: IntervalError? = null,
    val intervalMonths: IntervalError? = null,
    /** Both intervals are empty. Shown once for the pair, not on each field. */
    val noInterval: Boolean = false,
) {
    val isValid: Boolean get() = name == null && intervalKm == null && intervalMonths == null && !noInterval
}

public enum class ItemNameError { BLANK, TOO_LONG }

public enum class IntervalError { NOT_POSITIVE, TOO_LARGE }

public const val MAX_ITEM_NAME_LENGTH: Int = 40

public fun validateMaintenanceItem(draft: MaintenanceItemDraft): MaintenanceItemErrors =
    MaintenanceItemErrors(
        name =
            when {
                draft.name.isBlank() -> ItemNameError.BLANK
                draft.name.trim().length > MAX_ITEM_NAME_LENGTH -> ItemNameError.TOO_LONG
                else -> null
            },
        intervalKm = intervalError(draft.intervalKm, MAX_ODOMETER_KM),
        intervalMonths = intervalError(draft.intervalMonths, MAX_INTERVAL_MONTHS),
        noInterval = draft.intervalKm == null && draft.intervalMonths == null,
    )

private fun intervalError(
    value: Int?,
    max: Int,
): IntervalError? =
    when {
        value == null -> null
        value <= 0 -> IntervalError.NOT_POSITIVE
        value > max -> IntervalError.TOO_LARGE
        else -> null
    }

/** The "mark as done" form. */
public data class MaintenanceRecordDraft(
    val doneDate: LocalDate,
    val odometerKm: Int?,
    val costVnd: Long?,
)

public data class MaintenanceRecordErrors(
    val dateInFuture: Boolean = false,
    val odometer: RecordOdometerError? = null,
) {
    val isValid: Boolean get() = !dateInFuture && odometer == null
}

public enum class RecordOdometerError { MISSING, TOO_LARGE }

/**
 * A service may be logged with a reading below the vehicle's initial one: the last oil change usually
 * happened before the vehicle was added to the app. Cost is optional.
 */
public fun validateMaintenanceRecord(
    draft: MaintenanceRecordDraft,
    today: LocalDate,
): MaintenanceRecordErrors =
    MaintenanceRecordErrors(
        dateInFuture = draft.doneDate.isAfter(today),
        odometer =
            when {
                draft.odometerKm == null -> RecordOdometerError.MISSING
                draft.odometerKm > MAX_ODOMETER_KM -> RecordOdometerError.TOO_LARGE
                else -> null
            },
    )
