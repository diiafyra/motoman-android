package io.github.diiafyra.motoman.core.domain.places

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import kotlin.math.floor

/**
 * A grid cell of about 1 km. Panning the map inside one cell reuses the cached result instead of
 * calling the shared Overpass service again.
 */
public data class GridCell(
    val latIndex: Int,
    val lonIndex: Int,
) {
    public companion object {
        /** 0.01 degree of latitude is about 1.1 km everywhere; longitude cells are narrower toward the poles, fine at Vietnam's latitudes. */
        private const val CELL_DEGREES = 0.01

        public fun of(point: GeoPoint): GridCell =
            GridCell(floor(point.latitude / CELL_DEGREES).toInt(), floor(point.longitude / CELL_DEGREES).toInt())
    }
}

/**
 * In-memory cache of nearby places by grid cell. Entries expire after [ttlMillis] and are never
 * written to disk: the data is short-lived and the user's locations are not stored.
 */
public class PlacesCache(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val maxCells: Int = DEFAULT_MAX_CELLS,
) {
    private class Entry(
        val storedAtMillis: Long,
        val places: List<Place>,
    )

    // Insertion order, so the oldest cell is dropped first when the cache is full.
    private val entries = LinkedHashMap<GridCell, Entry>()

    /** The places for the cell of [point], or null when absent or older than the time to live. */
    @Synchronized
    public fun get(
        point: GeoPoint,
        nowMillis: Long,
    ): List<Place>? {
        val cell = GridCell.of(point)
        val entry = entries[cell] ?: return null
        if (nowMillis - entry.storedAtMillis >= ttlMillis) {
            entries.remove(cell)
            return null
        }
        return entry.places
    }

    @Synchronized
    public fun put(
        point: GeoPoint,
        places: List<Place>,
        nowMillis: Long,
    ) {
        val cell = GridCell.of(point)
        entries.remove(cell)
        entries[cell] = Entry(nowMillis, places)
        while (entries.size > maxCells) entries.remove(entries.keys.first())
    }

    public companion object {
        public const val DEFAULT_TTL_MILLIS: Long = 30 * 60 * 1_000
        public const val DEFAULT_MAX_CELLS: Int = 20
    }
}
