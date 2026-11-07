package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class MonthStatsTest {
    private val hanoi = ZoneId.of("Asia/Ho_Chi_Minh")
    private val november = YearMonth.of(2026, 11)

    private fun fill(
        id: String,
        at: String,
        km: Int,
        ml: Long,
        cost: Long,
        full: Boolean = true,
    ) = FuelEntry(id, "v1", Instant.parse(at), km, ml, cost, full, missedPrevious = false)

    @Test
    fun `empty input gives zero months, oldest first, ending at the last month`() {
        val stats = monthlyStats(emptyList(), november, months = 3, zone = hanoi)

        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), november), stats.map { it.month })
        stats.forEach {
            assertEquals(0L, it.costVnd)
            assertNull(it.litersPer100Km)
            assertNull(it.costPerKmVnd)
        }
    }

    @Test
    fun `spending counts every fill-up in its month`() {
        val stats =
            monthlyStats(
                listOf(
                    fill("a", "2026-10-05T03:00:00Z", 1_000, 4_000, 100_000, full = false),
                    fill("b", "2026-11-02T03:00:00Z", 1_100, 3_000, 75_000, full = false),
                    fill("c", "2026-11-20T03:00:00Z", 1_200, 2_000, 50_000, full = false),
                ),
                november,
                months = 2,
                zone = hanoi,
            )

        assertEquals(listOf(100_000L, 125_000L), stats.map { it.costVnd })
        assertEquals(listOf(4_000L, 5_000L), stats.map { it.volumeMl })
    }

    @Test
    fun `a segment counts toward the month it ends in`() {
        // Full on 25 Oct, partial on 3 Nov, full on 10 Nov: one segment of 200 km and 5 L, ending in November.
        val stats =
            monthlyStats(
                listOf(
                    fill("a", "2026-10-25T03:00:00Z", 1_000, 4_000, 100_000),
                    fill("b", "2026-11-03T03:00:00Z", 1_120, 2_000, 50_000, full = false),
                    fill("c", "2026-11-10T03:00:00Z", 1_200, 3_000, 75_000),
                ),
                november,
                months = 2,
                zone = hanoi,
            )

        assertNull(stats[0].litersPer100Km)
        assertEquals(200L, stats[1].segmentDistanceKm)
        assertEquals(2.5, stats[1].litersPer100Km!!, 1e-9)
        assertEquals(625.0, stats[1].costPerKmVnd!!, 1e-9)
    }

    @Test
    fun `several segments in a month are weighted by distance`() {
        // 2 L over 100 km and 9 L over 300 km: 11 L / 400 km = 2.75.
        val stats =
            monthlyStats(
                listOf(
                    fill("a", "2026-11-01T03:00:00Z", 1_000, 4_000, 0),
                    fill("b", "2026-11-05T03:00:00Z", 1_100, 2_000, 0),
                    fill("c", "2026-11-15T03:00:00Z", 1_400, 9_000, 0),
                ),
                november,
                months = 1,
                zone = hanoi,
            )

        assertEquals(2.75, stats.single().litersPer100Km!!, 1e-9)
    }

    @Test
    fun `months use the user's time zone`() {
        // 31 Oct 20:00 UTC is 1 Nov in Hanoi.
        val stats = monthlyStats(listOf(fill("a", "2026-10-31T20:00:00Z", 1_000, 1_000, 25_000)), november, 2, hanoi)

        assertEquals(listOf(0L, 25_000L), stats.map { it.costVnd })
    }

    @Test
    fun `entries outside the window are ignored`() {
        val stats = monthlyStats(listOf(fill("a", "2025-01-10T03:00:00Z", 1_000, 1_000, 25_000)), november, 1, hanoi)

        assertEquals(0L, stats.single().costVnd)
    }

    @Test
    fun `the window must have at least one month`() {
        assertThrows(IllegalArgumentException::class.java) { monthlyStats(emptyList(), november, 0, hanoi) }
    }
}
