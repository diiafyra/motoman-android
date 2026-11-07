package io.github.diiafyra.motoman.core.data.network

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale

/**
 * The Overpass API, which answers queries over OpenStreetMap data. A GET, so the retry interceptor may
 * repeat it; the query is short enough for a URL.
 */
internal interface OverpassApi {
    @GET("interpreter")
    suspend fun interpreter(
        @Query("data") query: String,
    ): OverpassResponse

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/"
    }
}

@Serializable
internal data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList(),
)

/** A node has its own coordinates; a way or relation has a computed [center] (`out center`). */
@Serializable
internal data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)

@Serializable
internal data class OverpassCenter(
    val lat: Double,
    val lon: Double,
)

/**
 * Fuel stations and car and motorbike garages within [radiusMeters]. `nwr` covers stations mapped as a
 * point or as a building outline; the server gives up after 25 seconds.
 */
internal fun nearbyPlacesQuery(
    center: GeoPoint,
    radiusMeters: Int,
): String {
    // Always a dot as decimal separator, whatever the phone's language.
    val around = String.format(Locale.ROOT, "around:%d,%.6f,%.6f", radiusMeters, center.latitude, center.longitude)
    return "[out:json][timeout:25];(" +
        "nwr[\"amenity\"=\"fuel\"]($around);" +
        "nwr[\"shop\"=\"car_repair\"]($around);" +
        "nwr[\"shop\"=\"motorcycle_repair\"]($around);" +
        ");out center tags;"
}
