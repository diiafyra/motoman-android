package io.github.diiafyra.motoman.core.domain.model

public data class Vehicle(
    val id: String,
    val name: String,
    val type: VehicleType,
    val fuelType: FuelType,
    /** Odometer reading when the vehicle was added. Lower bound for every later reading. */
    val initialOdometerKm: Int,
) {
    init {
        require(initialOdometerKm in 0..MAX_ODOMETER_KM) { "initialOdometerKm out of range" }
    }
}

/** Seven digits, as on a real odometer. Keeps every sum of readings and intervals far from Int overflow. */
public const val MAX_ODOMETER_KM: Int = 9_999_999

public enum class VehicleType { MOTORBIKE, CAR }

/** Fuels sold at Vietnamese stations. */
public enum class FuelType { RON95, E5_RON92, DIESEL, OTHER }
