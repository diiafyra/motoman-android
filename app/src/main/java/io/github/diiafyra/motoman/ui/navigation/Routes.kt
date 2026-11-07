package io.github.diiafyra.motoman.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe routes: arguments are typed properties, never strings built by hand.

@Serializable
data object OverviewRoute

@Serializable
data object FuelRoute

@Serializable
data object MaintenanceRoute

@Serializable
data object NearbyRoute

@Serializable
data object VehicleListRoute

/** [vehicleId] null opens an empty form for a new vehicle. */
@Serializable
data class VehicleEditRoute(
    val vehicleId: String? = null,
)
