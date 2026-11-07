package io.github.diiafyra.motoman.feature.fuel

import io.github.diiafyra.motoman.testing.testFuelEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class FuelMonthTest {
    private val hanoi = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun `no entries give no months`() {
        assertTrue(groupByMonth(emptyList(), hanoi).isEmpty())
    }

    @Test
    fun `months are newest first with entries newest first and totals summed`() {
        val months =
            groupByMonth(
                listOf(
                    testFuelEntry("oct", Instant.parse("2026-10-20T03:00:00Z"), 1_100, 3_000, 75_000),
                    testFuelEntry("nov-1", Instant.parse("2026-11-01T03:00:00Z"), 1_200, 4_000, 100_000),
                    testFuelEntry("nov-7", Instant.parse("2026-11-07T03:00:00Z"), 1_300, 1_500, 37_500),
                ),
                hanoi,
            )

        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), months.map { it.month })
        assertEquals(listOf("nov-7", "nov-1"), months[0].entries.map { it.id })
        assertEquals(137_500L, months[0].totalCostVnd)
        assertEquals(5_500L, months[0].totalVolumeMl)
    }

    @Test
    fun `the month follows the user's time zone, not UTC`() {
        // 31 Oct 20:00 UTC is already 1 Nov 03:00 in Hanoi.
        val months = groupByMonth(listOf(testFuelEntry("a", Instant.parse("2026-10-31T20:00:00Z"), 1_000)), hanoi)

        assertEquals(YearMonth.of(2026, 11), months.single().month)
    }
}
