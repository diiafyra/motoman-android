package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.MAX_INTERVAL_MONTHS
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceValidationTest {
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun `an item with a name and one interval is valid`() {
        assertTrue(validateMaintenanceItem(MaintenanceItemDraft("Oil", 2_000, null)).isValid)
        assertTrue(validateMaintenanceItem(MaintenanceItemDraft("Inspection", null, 12)).isValid)
    }

    @Test
    fun `an item needs a name and at least one interval`() {
        val errors = validateMaintenanceItem(MaintenanceItemDraft(" ", null, null))

        assertEquals(ItemNameError.BLANK, errors.name)
        assertTrue(errors.noInterval)
        assertFalse(errors.isValid)
    }

    @Test
    fun `intervals must be positive and within range`() {
        val zero = validateMaintenanceItem(MaintenanceItemDraft("Oil", 0, 0))
        assertEquals(IntervalError.NOT_POSITIVE, zero.intervalKm)
        assertEquals(IntervalError.NOT_POSITIVE, zero.intervalMonths)

        val big = validateMaintenanceItem(MaintenanceItemDraft("Oil", MAX_ODOMETER_KM + 1, MAX_INTERVAL_MONTHS + 1))
        assertEquals(IntervalError.TOO_LARGE, big.intervalKm)
        assertEquals(IntervalError.TOO_LARGE, big.intervalMonths)
    }

    @Test
    fun `item names longer than the limit are rejected`() {
        assertEquals(
            ItemNameError.TOO_LONG,
            validateMaintenanceItem(MaintenanceItemDraft("a".repeat(41), 1, null)).name,
        )
    }

    @Test
    fun `a record needs a reading and a date that is not in the future`() {
        val errors = validateMaintenanceRecord(MaintenanceRecordDraft(today.plusDays(1), null, null), today)

        assertTrue(errors.dateInFuture)
        assertEquals(RecordOdometerError.MISSING, errors.odometer)
    }

    @Test
    fun `a record today with any reading up to seven digits is valid, cost optional`() {
        assertTrue(validateMaintenanceRecord(MaintenanceRecordDraft(today, 0, null), today).isValid)
        assertNull(validateMaintenanceRecord(MaintenanceRecordDraft(today, MAX_ODOMETER_KM, 1), today).odometer)
        assertEquals(
            RecordOdometerError.TOO_LARGE,
            validateMaintenanceRecord(MaintenanceRecordDraft(today, MAX_ODOMETER_KM + 1, null), today).odometer,
        )
    }
}
