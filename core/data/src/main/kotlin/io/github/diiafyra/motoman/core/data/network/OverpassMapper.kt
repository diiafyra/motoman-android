package io.github.diiafyra.motoman.core.data.network

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.PlaceKind

/**
 * The element as a [Place], or null when it is not one of the kinds asked for or has no position.
 * OpenStreetMap is edited by anyone, so missing or odd tags are expected, not errors.
 */
internal fun OverpassElement.toPlace(): Place? {
    val kind =
        when {
            tags["amenity"] == "fuel" -> PlaceKind.FUEL_STATION
            tags["shop"] == "car_repair" -> PlaceKind.CAR_REPAIR
            tags["shop"] == "motorcycle_repair" -> PlaceKind.MOTORBIKE_REPAIR
            else -> return null
        }
    val latitude = lat ?: center?.lat ?: return null
    val longitude = lon ?: center?.lon ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return Place(
        id = "$type/$id",
        kind = kind,
        location = GeoPoint(latitude, longitude),
        // Vietnamese name first: many places have both, and "name" is not always Vietnamese.
        name = (tags["name:vi"] ?: tags["name"])?.takeIf { it.isNotBlank() },
        brand = tags["brand"]?.takeIf { it.isNotBlank() },
        openingHours = tags["opening_hours"]?.takeIf { it.isNotBlank() },
    )
}
