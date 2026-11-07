package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.fill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEconomyTest {
    @Test
    fun `no entries gives no segments and no average`() {
        val economy = calculateFuelEconomy(emptyList())

        assertTrue(economy.segments.isEmpty())
        assertNull(economy.latest)
        assertNull(economy.averageLitersPer100Km)
        assertNull(economy.averageCostPerKmVnd)
    }

    @Test
    fun `a single full tank cannot be measured`() {
        val economy = calculateFuelEconomy(listOf(fill("a", day = 0, km = 1_000, liters = 4.0)))

        assertTrue(economy.segments.isEmpty())
    }

    @Test
    fun `two full tanks make one segment from the fuel of the second`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 3, km = 1_200, liters = 4.0, costVnd = 100_000),
                ),
            )

        val segment = economy.segments.single()
        assertEquals("a", segment.startEntryId)
        assertEquals("b", segment.endEntryId)
        assertEquals(200, segment.distanceKm)
        assertEquals(4_000, segment.volumeMl)
        assertEquals(100_000, segment.costVnd)
        assertEquals(2.0, segment.litersPer100Km, 1e-9)
        assertEquals(500.0, segment.costPerKmVnd, 1e-9)
    }

    @Test
    fun `partial fills between two full tanks count toward the segment`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 1, km = 1_100, liters = 1.5, costVnd = 37_500, full = false),
                    fill("c", day = 2, km = 1_150, liters = 1.0, costVnd = 25_000, full = false),
                    fill("d", day = 3, km = 1_250, liters = 2.5, costVnd = 62_500),
                ),
            )

        val segment = economy.segments.single()
        assertEquals("a", segment.startEntryId)
        assertEquals("d", segment.endEntryId)
        assertEquals(250, segment.distanceKm)
        assertEquals(5_000, segment.volumeMl)
        assertEquals(125_000, segment.costVnd)
        assertEquals(2.0, segment.litersPer100Km, 1e-9)
    }

    @Test
    fun `fills before the first full tank are ignored`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 3.0, full = false),
                    fill("b", day = 1, km = 1_100, liters = 4.0),
                    fill("c", day = 2, km = 1_300, liters = 5.0),
                ),
            )

        val segment = economy.segments.single()
        assertEquals("b", segment.startEntryId)
        assertEquals(5_000, segment.volumeMl)
    }

    @Test
    fun `a missed fill inside a segment drops only that segment`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 1, km = 1_100, liters = 2.0, full = false, missedPrevious = true),
                    fill("c", day = 2, km = 1_300, liters = 4.0),
                    fill("d", day = 3, km = 1_500, liters = 5.0),
                ),
            )

        val segment = economy.segments.single()
        assertEquals("c", segment.startEntryId)
        assertEquals("d", segment.endEntryId)
    }

    @Test
    fun `a missed fill on the closing full tank drops the segment`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 1, km = 1_300, liters = 4.0, missedPrevious = true),
                ),
            )

        assertTrue(economy.segments.isEmpty())
    }

    @Test
    fun `a missed fill on the opening full tank does not affect the segment after it`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0, missedPrevious = true),
                    fill("b", day = 1, km = 1_200, liters = 4.0),
                ),
            )

        assertEquals(1, economy.segments.size)
    }

    @Test
    fun `a segment with no distance is dropped`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 0, km = 1_000, liters = 0.5),
                ),
            )

        assertTrue(economy.segments.isEmpty())
    }

    @Test
    fun `a dropped zero-distance segment still opens the next one`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 0, km = 1_000, liters = 0.5),
                    fill("c", day = 2, km = 1_200, liters = 4.0),
                ),
            )

        val segment = economy.segments.single()
        assertEquals("b", segment.startEntryId)
        assertEquals("c", segment.endEntryId)
    }

    @Test
    fun `a partial and a full fill-up logged at the same time and reading put the partial first`() {
        val entries =
            listOf(
                fill("a", day = 0, km = 1_000, liters = 4.0),
                fill("z-partial", day = 2, km = 1_200, liters = 1.0, full = false),
                fill("b-full", day = 2, km = 1_200, liters = 3.0),
                fill("c", day = 4, km = 1_400, liters = 4.0),
            )

        for (order in listOf(entries, entries.reversed())) {
            val segments = calculateFuelEconomy(order).segments
            assertEquals(listOf(4_000L, 4_000L), segments.map { it.volumeMl })
            assertEquals(listOf("b-full", "c"), segments.map { it.endEntryId })
        }
    }

    @Test
    fun `the result does not depend on input order when everything ties`() {
        val entries =
            listOf(
                fill("a", day = 0, km = 1_000, liters = 4.0),
                fill("b", day = 2, km = 1_200, liters = 4.0),
                fill("c", day = 2, km = 1_200, liters = 1.0),
                fill("d", day = 4, km = 1_400, liters = 4.0),
            )

        assertEquals(calculateFuelEconomy(entries), calculateFuelEconomy(entries.reversed()))
    }

    @Test
    fun `latest is the newest valid segment even when a newer one was dropped`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 1, km = 1_200, liters = 4.0),
                    fill("c", day = 2, km = 1_400, liters = 4.0, missedPrevious = true),
                ),
            )

        assertEquals("b", economy.latest?.endEntryId)
    }

    @Test
    fun `entries are ordered by date whatever the input order`() {
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("c", day = 2, km = 1_400, liters = 6.0),
                    fill("a", day = 0, km = 1_000, liters = 4.0),
                    fill("b", day = 1, km = 1_200, liters = 4.0),
                ),
            )

        assertEquals(listOf("b", "c"), economy.segments.map { it.endEntryId })
        assertEquals("c", economy.latest?.endEntryId)
    }

    @Test
    fun `average is weighted by distance, not the mean of segments`() {
        // 2 L over 100 km (2.0 L/100km), then 9 L over 300 km (3.0 L/100km).
        // Weighted: 11 L / 400 km = 2.75. A plain mean would give 2.5.
        val economy =
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0, costVnd = 0),
                    fill("b", day = 1, km = 1_100, liters = 2.0, costVnd = 50_000),
                    fill("c", day = 2, km = 1_400, liters = 9.0, costVnd = 150_000),
                ),
            )

        assertEquals(2.75, economy.averageLitersPer100Km!!, 1e-9)
        assertEquals(500.0, economy.averageCostPerKmVnd!!, 1e-9)
    }

    @Test
    fun `sums stay exact over many entries`() {
        // 0.1 L as a Double cannot be represented exactly; in mL it is the integer 100.
        val entries =
            listOf(fill("start", day = 0, km = 0, liters = 1.0)) +
                (1..999).map { fill("p$it", day = it.toLong(), km = it, liters = 0.1, full = false) } +
                fill("end", day = 1_000, km = 1_000, liters = 0.1)

        val segment = calculateFuelEconomy(entries).segments.single()

        assertEquals(100_000, segment.volumeMl)
        assertEquals(10.0, segment.litersPer100Km, 0.0)
    }

    @Test
    fun `entries from several vehicles are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculateFuelEconomy(
                listOf(
                    fill("a", day = 0, km = 1_000, liters = 4.0, vehicleId = "v1"),
                    fill("b", day = 1, km = 1_200, liters = 4.0, vehicleId = "v2"),
                ),
            )
        }
    }
}
