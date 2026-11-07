package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import java.time.Instant

/** What the form holds before it becomes a [FuelEntry]. Null means the field is empty. */
public data class FuelEntryDraft(
    val filledAt: Instant,
    val odometerKm: Int?,
    val volumeMl: Long?,
    val totalCostVnd: Long?,
)

/** One error per field at most, so the form can show each next to its field. */
public data class FuelEntryErrors(
    val filledAt: FilledAtError? = null,
    val odometer: OdometerError? = null,
    val volume: VolumeError? = null,
    val cost: CostError? = null,
) {
    val isValid: Boolean get() = filledAt == null && odometer == null && volume == null && cost == null
}

public enum class FilledAtError { IN_FUTURE }

public sealed interface OdometerError {
    public data object Missing : OdometerError

    public data class BelowInitial(
        val initialKm: Int,
    ) : OdometerError

    /** Lower than the fill-up just before this one in time. */
    public data class BelowPrevious(
        val previousKm: Int,
    ) : OdometerError

    /** Higher than the fill-up just after this one in time. */
    public data class AboveNext(
        val nextKm: Int,
    ) : OdometerError

    public data class TooLarge(
        val maxKm: Int,
    ) : OdometerError
}

public sealed interface VolumeError {
    public data object Missing : VolumeError

    public data object NotPositive : VolumeError

    public data class TooLarge(
        val maxMl: Long,
    ) : VolumeError
}

public enum class CostError { MISSING, NOT_POSITIVE }

/** No passenger vehicle tank comes close; anything above is a typo such as an extra zero. */
public const val MAX_FILL_VOLUME_ML: Long = 200_000

/**
 * Checks a fill-up against the vehicle and its other fill-ups. Readings must not decrease over time, so
 * the odometer must lie between the fill-ups just before and just after [FuelEntryDraft.filledAt].
 * An equal reading is allowed: two fill-ups without driving in between are possible.
 *
 * @param otherEntries the vehicle's fill-ups, without the one being edited.
 */
public fun validateFuelEntry(
    draft: FuelEntryDraft,
    vehicle: Vehicle,
    otherEntries: List<FuelEntry>,
    now: Instant,
): FuelEntryErrors {
    require(otherEntries.all { it.vehicleId == vehicle.id }) { "Entries must belong to the vehicle" }
    return FuelEntryErrors(
        filledAt = if (draft.filledAt.isAfter(now)) FilledAtError.IN_FUTURE else null,
        odometer = validateOdometer(draft, vehicle, otherEntries),
        volume =
            when {
                draft.volumeMl == null -> VolumeError.Missing
                draft.volumeMl <= 0 -> VolumeError.NotPositive
                draft.volumeMl > MAX_FILL_VOLUME_ML -> VolumeError.TooLarge(MAX_FILL_VOLUME_ML)
                else -> null
            },
        cost =
            when {
                draft.totalCostVnd == null -> CostError.MISSING
                draft.totalCostVnd <= 0 -> CostError.NOT_POSITIVE
                else -> null
            },
    )
}

private fun validateOdometer(
    draft: FuelEntryDraft,
    vehicle: Vehicle,
    otherEntries: List<FuelEntry>,
): OdometerError? {
    val odometerKm = draft.odometerKm ?: return OdometerError.Missing
    if (odometerKm > MAX_ODOMETER_KM) return OdometerError.TooLarge(MAX_ODOMETER_KM)
    if (odometerKm < vehicle.initialOdometerKm) return OdometerError.BelowInitial(vehicle.initialOdometerKm)

    val previous = otherEntries.filter { !it.filledAt.isAfter(draft.filledAt) }.maxOfOrNull { it.odometerKm }
    if (previous != null && odometerKm < previous) return OdometerError.BelowPrevious(previous)

    val next = otherEntries.filter { it.filledAt.isAfter(draft.filledAt) }.minOfOrNull { it.odometerKm }
    if (next != null && odometerKm > next) return OdometerError.AboveNext(next)

    return null
}
