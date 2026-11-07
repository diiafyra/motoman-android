package io.github.diiafyra.motoman.core.domain.places

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.repository.PlacesRepository
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.util.Clock
import javax.inject.Inject

/**
 * Nearby fuel stations and garages, from the cache when the same ~1 km cell was fetched within the
 * last 30 minutes. A failed request is never cached, so the next try asks the server again.
 */
public class FindNearbyPlaces
    @Inject
    constructor(
        private val repository: PlacesRepository,
        private val cache: PlacesCache,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(
            center: GeoPoint,
            radiusMeters: Int = RADIUS_METERS,
        ): AppResult<List<Place>> {
            val now = clock.now().toEpochMilli()
            cache.get(center, now)?.let { return AppResult.Success(it) }
            val result = repository.nearby(center, radiusMeters)
            if (result is AppResult.Success) cache.put(center, result.value, now)
            return result
        }

        public companion object {
            public const val RADIUS_METERS: Int = 3_000
        }
    }
