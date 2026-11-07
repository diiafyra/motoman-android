package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.item
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class RecordDoneTest {
    private val june1 = LocalDate.of(2026, 6, 1)

    private fun record(
        date: LocalDate,
        km: Int,
        itemId: String = "i1",
    ) = MaintenanceRecord(
        id = "r",
        itemId = itemId,
        vehicleId = "v1",
        doneDate = date,
        odometerKm = km,
        costVnd = null,
    )

    @Test
    fun `the first record sets the baseline`() {
        val updated = item(intervalKm = 4_000).withRecord(record(june1, 5_000))

        assertEquals(5_000, updated.lastDoneKm)
        assertEquals(june1, updated.lastDoneDate)
    }

    @Test
    fun `a later record moves the baseline forward`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 1_000, lastDoneDate = june1)

        val updated = item.withRecord(record(june1.plusDays(1), 5_000))

        assertEquals(5_000, updated.lastDoneKm)
        assertEquals(june1.plusDays(1), updated.lastDoneDate)
    }

    @Test
    fun `an older record leaves the baseline alone`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 5_000, lastDoneDate = june1)

        assertSame(item, item.withRecord(record(june1.minusDays(30), 1_000)))
    }

    @Test
    fun `on the same date the higher reading wins`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 5_000, lastDoneDate = june1)

        assertEquals(5_100, item.withRecord(record(june1, 5_100)).lastDoneKm)
        assertSame(item, item.withRecord(record(june1, 4_900)))
    }

    @Test
    fun `a reading-only baseline is compared by reading`() {
        val item = item(intervalKm = 4_000, lastDoneKm = 5_000)

        assertEquals(6_000, item.withRecord(record(june1, 6_000)).lastDoneKm)
        assertSame(item, item.withRecord(record(june1, 4_000)))
    }

    @Test
    fun `a record for another item is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            item(intervalKm = 4_000).withRecord(record(june1, 5_000, itemId = "other"))
        }
    }
}
