package io.github.diiafyra.motoman.core.domain

import io.github.diiafyra.motoman.core.domain.model.GeoPoint
import io.github.diiafyra.motoman.core.domain.model.distanceMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DistanceTest {
    @Test
    fun `the same point is zero metres away`() {
        val hoanKiem = GeoPoint(21.0285, 105.8542)

        assertEquals(0.0, distanceMeters(hoanKiem, hoanKiem), 1e-9)
    }

    @Test
    fun `one thousandth of a degree of latitude is about 111 metres`() {
        assertEquals(111.2, distanceMeters(GeoPoint(21.0, 105.8), GeoPoint(21.001, 105.8)), 0.5)
    }

    @Test
    fun `Hanoi to Ho Chi Minh City is about 1,140 km`() {
        val hanoi = GeoPoint(21.0285, 105.8542)
        val saigon = GeoPoint(10.7769, 106.7009)

        assertEquals(1_140_000.0, distanceMeters(hanoi, saigon), 10_000.0)
        assertEquals(distanceMeters(hanoi, saigon), distanceMeters(saigon, hanoi), 1e-6)
    }

    @Test
    fun `coordinates outside the globe are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(0.0, 181.0) }
    }
}
