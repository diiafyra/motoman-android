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

/** [entryId] null opens an empty form for a new fill-up. */
@Serializable
data class FuelEditRoute(
    val vehicleId: String,
    val entryId: String? = null,
)

@Serializable
data object StatsRoute

/** [itemId] null opens an empty form for a new item. */
@Serializable
data class MaintenanceItemRoute(
    val vehicleId: String,
    val itemId: String? = null,
)

@Serializable
data class MaintenanceDoneRoute(
    val vehicleId: String,
    val itemId: String,
)
