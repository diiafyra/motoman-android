package io.github.diiafyra.motoman.core.domain.model

/** A point on the map, in degrees (WGS 84). */
public data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range" }
        require(longitude in -180.0..180.0) { "longitude out of range" }
    }
}

public enum class PlaceKind { FUEL_STATION, CAR_REPAIR, MOTORBIKE_REPAIR }

/** A fuel station or garage from OpenStreetMap. */
public data class Place(
    /** OpenStreetMap element, e.g. "node/123". Stable across requests. */
    val id: String,
    val kind: PlaceKind,
    val location: GeoPoint,
    val name: String?,
    val brand: String?,
    val openingHours: String?,
)
