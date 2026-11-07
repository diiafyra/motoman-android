package io.github.diiafyra.motoman.core.domain.vehicle

import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.VehicleType

/** What the vehicle form holds. Null means the field is empty. */
public data class VehicleDraft(
    val name: String,
    val type: VehicleType,
    val fuelType: FuelType,
    val initialOdometerKm: Int?,
)

public data class VehicleErrors(
    val name: VehicleNameError? = null,
    val initialOdometer: InitialOdometerError? = null,
) {
    val isValid: Boolean get() = name == null && initialOdometer == null
}

public enum class VehicleNameError { BLANK, TOO_LONG }

public enum class InitialOdometerError { MISSING, TOO_LARGE }

/** Long enough for "Honda Wave Alpha 110 (đỏ)", short enough for the top bar. */
public const val MAX_VEHICLE_NAME_LENGTH: Int = 40

public fun validateVehicle(draft: VehicleDraft): VehicleErrors =
    VehicleErrors(
        name =
            when {
                draft.name.isBlank() -> VehicleNameError.BLANK
                draft.name.trim().length > MAX_VEHICLE_NAME_LENGTH -> VehicleNameError.TOO_LONG
                else -> null
            },
        initialOdometer =
            when {
                draft.initialOdometerKm == null -> InitialOdometerError.MISSING
                draft.initialOdometerKm > MAX_ODOMETER_KM -> InitialOdometerError.TOO_LARGE
                else -> null
            },
    )
