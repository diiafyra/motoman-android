package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ItemStatusTest {
    private val today = LocalDate.of(2026, 6, 1)

    private fun item(
        id: String,
        intervalKm: Int? = null,
        intervalMonths: Int? = null,
        lastDoneKm: Int? = null,
        lastDoneDate: LocalDate? = null,
    ) = MaintenanceItem(id, "v1", id, intervalKm, intervalMonths, lastDoneKm, lastDoneDate)

    @Test
    fun `groups come in order overdue, due soon, ok, no baseline`() {
        val ranked =
            rankByUrgency(
                listOf(
                    item("none", intervalKm = 1_000),
                    item("ok", intervalKm = 10_000, lastDoneKm = 5_000),
                    item("soon", intervalKm = 1_000, lastDoneKm = 5_000),
                    item("late", intervalKm = 1_000, lastDoneKm = 3_000),
                ),
                currentOdometerKm = 5_600,
                today = today,
            )

        assertEquals(listOf("late", "soon", "ok", "none"), ranked.map { it.item.id })
        assertTrue(ranked[0].status is MaintenanceStatus.Overdue)
        assertEquals(MaintenanceStatus.NoBaseline, ranked[3].status)
    }

    @Test
    fun `within a group the smaller share of the interval left comes first`() {
        // Both ok. 2,000 of 10,000 km left (20%) beats 900 of 3,000 km left (30%).
        val ranked =
            rankByUrgency(
                listOf(
                    item("thirty", intervalKm = 3_000, lastDoneKm = 5_000),
                    item("twenty", intervalKm = 10_000, lastDoneKm = 0),
                ),
                currentOdometerKm = 7_100,
                today = today,
            )

        assertEquals(listOf("twenty", "thirty"), ranked.map { it.item.id })
    }

    @Test
    fun `time and distance shares are compared on the same scale`() {
        // Ok by km with 50% left; ok by time with about 25% left (3 of 12 months).
        val ranked =
            rankByUrgency(
                listOf(
                    item("km", intervalKm = 10_000, lastDoneKm = 0),
                    item("time", intervalMonths = 12, lastDoneDate = today.minusMonths(9)),
                ),
                currentOdometerKm = 5_000,
                today = today,
            )

        assertEquals(listOf("time", "km"), ranked.map { it.item.id })
    }

    @Test
    fun `ties keep the input order`() {
        val ranked =
            rankByUrgency(
                listOf(item("b", intervalKm = 1_000), item("a", intervalKm = 1_000)),
                currentOdometerKm = 0,
                today = today,
            )

        assertEquals(listOf("b", "a"), ranked.map { it.item.id })
    }
}
