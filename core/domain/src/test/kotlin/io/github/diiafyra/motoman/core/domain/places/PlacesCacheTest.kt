package io.github.diiafyra.motoman.core.domain.places

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.Place
import io.github.diiafyra.motoman.core.domain.model.PlaceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacesCacheTest {
    private val here = GeoPoint(21.0285, 105.8542)
    private val sameCell = GeoPoint(21.0299, 105.8599)
    private val nextCell = GeoPoint(21.0301, 105.8542)

    private fun place(id: String) = Place(id, PlaceKind.FUEL_STATION, here, null, null, null)

    @Test
    fun `points about a kilometre apart share a cell, further ones do not`() {
        assertEquals(GridCell.of(here), GridCell.of(sameCell))
        assertNotEquals(GridCell.of(here), GridCell.of(nextCell))
    }

    @Test
    fun `negative coordinates fall in the cell below, not toward zero`() {
        assertEquals(-1, GridCell.of(GeoPoint(-0.005, 0.005)).latIndex)
        assertEquals(0, GridCell.of(GeoPoint(0.005, 0.005)).latIndex)
    }

    @Test
    fun `a stored result is returned for any point in its cell until it expires`() {
        val cache = PlacesCache(ttlMillis = 1_000)
        cache.put(here, listOf(place("a")), nowMillis = 0)

        assertEquals(listOf(place("a")), cache.get(sameCell, nowMillis = 999))
        assertNull(cache.get(sameCell, nowMillis = 1_000))
        assertNull("expired entries are removed", cache.get(here, nowMillis = 1_001))
    }

    @Test
    fun `another cell is a miss`() {
        val cache = PlacesCache()
        cache.put(here, listOf(place("a")), 0)

        assertNull(cache.get(nextCell, 0))
    }

    @Test
    fun `putting again replaces the result and restarts the clock`() {
        val cache = PlacesCache(ttlMillis = 1_000)
        cache.put(here, listOf(place("a")), 0)
        cache.put(here, listOf(place("b")), 800)

        assertEquals(listOf(place("b")), cache.get(here, 1_500))
    }

    @Test
    fun `the oldest cell is dropped when the cache is full`() {
        val cache = PlacesCache(maxCells = 2)
        val p1 = GeoPoint(21.01, 105.80)
        val p2 = GeoPoint(21.03, 105.80)
        val p3 = GeoPoint(21.05, 105.80)
        cache.put(p1, listOf(place("1")), 0)
        cache.put(p2, listOf(place("2")), 0)
        cache.put(p3, listOf(place("3")), 0)

        assertNull(cache.get(p1, 0))
        assertEquals(listOf(place("2")), cache.get(p2, 0))
        assertEquals(listOf(place("3")), cache.get(p3, 0))
    }
}
