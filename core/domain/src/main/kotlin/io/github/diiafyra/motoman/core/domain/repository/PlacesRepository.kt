package io.github.diiafyra.motoman.core.domain.repository

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.result.AppResult

/**
 * Fuel stations and garages around a point. Short-lived data: it is never stored in Room and never
 * blocks the rest of the app.
 */
public interface PlacesRepository {
    public suspend fun nearby(
        center: GeoPoint,
        radiusMeters: Int,
    ): AppResult<List<Place>>
}
