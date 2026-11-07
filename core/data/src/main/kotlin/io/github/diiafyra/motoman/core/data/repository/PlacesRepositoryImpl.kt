package io.github.diiafyra.motoman.core.data.repository

import io.github.diiafyra.motoman.core.data.network.OverpassApi
import io.github.diiafyra.motoman.core.data.network.nearbyPlacesQuery
import io.github.diiafyra.motoman.core.data.network.safeCall
import io.github.diiafyra.motoman.core.data.network.toPlace
import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.distanceMeters
import io.github.diiafyra.motoman.core.domain.repository.PlacesRepository
import io.github.diiafyra.motoman.core.domain.result.AppResult
import io.github.diiafyra.motoman.core.domain.result.map
import javax.inject.Inject

internal class PlacesRepositoryImpl
    @Inject
    constructor(
        private val api: OverpassApi,
    ) : PlacesRepository {
        /** Nearest first. Duplicates (the same place returned twice) are dropped by id. */
        override suspend fun nearby(
            center: GeoPoint,
            radiusMeters: Int,
        ): AppResult<List<Place>> =
            safeCall { api.interpreter(nearbyPlacesQuery(center, radiusMeters)) }.map { response ->
                response.elements
                    .mapNotNull { it.toPlace() }
                    .distinctBy { it.id }
                    .sortedBy { distanceMeters(center, it.location) }
            }
    }
