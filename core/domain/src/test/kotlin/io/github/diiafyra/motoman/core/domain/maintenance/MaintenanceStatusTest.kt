package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.fill
import io.github.diiafyra.motoman.core.domain.item
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.record
import io.github.diiafyra.motoman.core.domain.vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class MaintenanceStatusTest {
    private val today = LocalDate.of(2026, 6, 1)
    private val threshold = DueSoonThreshold(km = 500, days = 14)

    private fun status(
        item: io.github.diiafyra.motoman.core.domain.model.MaintenanceItem,
        currentKm: Int,
    ) = maintenanceStatus(item, currentKm, today, threshold)

    // Current odometer

    @Test
    fun `current odometer is the initial reading when nothing is logged`() {
        assertEquals(1_000, currentOdometerKm(vehicle(1_000), emptyList(), emptyList()))
    }

    @Test
    fun `current odometer is the highest of fill-ups and records`() {
        val fills = listOf(fill("a", day = 0, km = 1_500, liters = 4.0), fill("b", day = 1, km = 1_800, liters = 4.0))

        assertEquals(1_800, currentOdometerKm(vehicle(1_000), fills, listOf(record(1_700))))
        assertEquals(2_100, currentOdometerKm(vehicle(1_000), fills, listOf(record(2_100))))
    }

    // No baseline

    @Test
    fun `an item never done has no baseline`() {
        assertEquals(
            MaintenanceStatus.NoBaseline,
            status(item(intervalKm = 4_000, intervalMonths = 6), currentKm = 5_000),
        )
    }

    @Test
    fun `a last date without a time interval has no baseline`() {
        val item = item(intervalKm = 4_000, lastDoneDate = today.minusMonths(1))

        assertEquals(MaintenanceStatus.NoBaseline, status(item, currentKm = 5_000))
    }

    @Test
    fun `a last reading without a distance interval has no baseline`() {
        val item = item(intervalMonths = 6, lastDoneKm = 1_000)

        assertEquals(MaintenanceStatus.NoBaseline, status(item, currentKm = 5_000))
    }

    // By distance

    @Test
    fun `well before the due reading is ok`() {
        val result = status(item(intervalKm = 4_000, lastDoneKm = 1_000), currentKm = 3_000)

        assertEquals(MaintenanceStatus.Ok(DuePoint(5_000, null, 2_000, null)), result)
    }

    @Test
    fun `within the threshold of the due reading is due soon, the edge included`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 1_000)

        assertEquals(MaintenanceStatus.Ok::class, status(item, currentKm = 4_499)::class)
        assertEquals(MaintenanceStatus.DueSoon::class, status(item, currentKm = 4_500)::class)
    }

    @Test
    fun `reaching the due reading exactly is due soon, passing it is overdue`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 1_000)

        assertEquals(MaintenanceStatus.DueSoon(DuePoint(5_000, null, 0, null)), status(item, currentKm = 5_000))
        assertEquals(MaintenanceStatus.Overdue(DuePoint(5_000, null, -1, null)), status(item, currentKm = 5_001))
    }

    // By time

    @Test
    fun `well before the due date is ok`() {
        val item = item(intervalMonths = 6, lastDoneDate = LocalDate.of(2026, 3, 1))

        assertEquals(
            MaintenanceStatus.Ok(DuePoint(null, LocalDate.of(2026, 9, 1), null, 92)),
            status(item, currentKm = 0),
        )
    }

    @Test
    fun `within the threshold of the due date is due soon, the edge included`() {
        assertEquals(
            MaintenanceStatus.Ok::class,
            status(item(intervalMonths = 1, lastDoneDate = LocalDate.of(2026, 5, 16)), currentKm = 0)::class,
        )
        assertEquals(
            MaintenanceStatus.DueSoon::class,
            status(item(intervalMonths = 1, lastDoneDate = LocalDate.of(2026, 5, 15)), currentKm = 0)::class,
        )
    }

    @Test
    fun `the due date itself is due soon, the day after is overdue`() {
        val item = item(intervalMonths = 3, lastDoneDate = LocalDate.of(2026, 3, 1))

        assertEquals(
            MaintenanceStatus.DueSoon(DuePoint(null, today, null, 0)),
            maintenanceStatus(item, 0, today, threshold),
        )
        assertEquals(
            MaintenanceStatus.Overdue(DuePoint(null, today, null, -1)),
            maintenanceStatus(item, 0, today.plusDays(1), threshold),
        )
    }

    @Test
    fun `adding months clamps to the end of a shorter month`() {
        val item = item(intervalMonths = 1, lastDoneDate = LocalDate.of(2026, 1, 31))

        val result = maintenanceStatus(item, 0, LocalDate.of(2026, 1, 31), threshold)

        assertEquals(LocalDate.of(2026, 2, 28), (result as MaintenanceStatus.Ok).due.dueDate)
    }

    // Both intervals: whichever comes first

    @Test
    fun `overdue by distance wins even when the date is far away`() {
        val item = item(intervalKm = 4_000, intervalMonths = 12, lastDoneKm = 1_000, lastDoneDate = today)

        assertEquals(MaintenanceStatus.Overdue::class, status(item, currentKm = 6_000)::class)
    }

    @Test
    fun `overdue by date wins even when the distance is far away`() {
        val item =
            item(intervalKm = 4_000, intervalMonths = 6, lastDoneKm = 1_000, lastDoneDate = today.minusMonths(7))

        assertEquals(MaintenanceStatus.Overdue::class, status(item, currentKm = 1_100)::class)
    }

    @Test
    fun `due soon on one side and ok on the other is due soon`() {
        val item = item(intervalKm = 4_000, intervalMonths = 6, lastDoneKm = 1_000, lastDoneDate = today)

        val result = status(item, currentKm = 4_800)

        assertEquals(
            MaintenanceStatus.DueSoon(DuePoint(5_000, today.plusMonths(6), 200, 183)),
            result,
        )
    }

    @Test
    fun `with both intervals but only one last-done value, that side alone decides`() {
        val item = item(intervalKm = 4_000, intervalMonths = 6, lastDoneKm = 1_000)

        assertEquals(MaintenanceStatus.Ok(DuePoint(5_000, null, 3_000, null)), status(item, currentKm = 2_000))
    }

    // Item invariants

    @Test
    fun `an item needs at least one positive interval`() {
        assertThrows(IllegalArgumentException::class.java) { item() }
        assertThrows(IllegalArgumentException::class.java) { item(intervalKm = 0) }
        assertThrows(IllegalArgumentException::class.java) { item(intervalMonths = -1) }
    }

    @Test
    fun `intervals and readings beyond a real odometer are rejected, so sums cannot overflow`() {
        assertThrows(IllegalArgumentException::class.java) { item(intervalKm = 2_000_000_000) }
        assertThrows(IllegalArgumentException::class.java) { item(intervalMonths = 121) }
        assertThrows(IllegalArgumentException::class.java) { item(intervalKm = 1, lastDoneKm = -1) }
        assertThrows(IllegalArgumentException::class.java) { vehicle(initialOdometerKm = -1) }
        assertThrows(IllegalArgumentException::class.java) { DueSoonThreshold(km = -1, days = 0) }
    }

    @Test
    fun `largest allowed reading plus largest interval is computed without overflow`() {
        val item = item(intervalKm = MAX_ODOMETER_KM, lastDoneKm = MAX_ODOMETER_KM)

        assertEquals(
            MaintenanceStatus.Ok(DuePoint(19_999_998, null, 9_999_999, null)),
            status(item, MAX_ODOMETER_KM),
        )
    }

    @Test
    fun `current odometer rejects data from another vehicle`() {
        assertThrows(IllegalArgumentException::class.java) {
            currentOdometerKm(
                vehicle(),
                listOf(fill("a", day = 0, km = 1_500, liters = 4.0, vehicleId = "v2")),
                emptyList(),
            )
        }
    }
}
