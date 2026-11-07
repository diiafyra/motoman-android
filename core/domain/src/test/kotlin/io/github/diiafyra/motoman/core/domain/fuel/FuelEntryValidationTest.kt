package io.github.diiafyra.motoman.core.domain.fuel

import io.github.diiafyra.motoman.core.domain.day
import io.github.diiafyra.motoman.core.domain.fill
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEntryValidationTest {
    private val now = day(100)
    private val vehicle = vehicle(initialOdometerKm = 1_000)

    private fun draft(
        day: Long = 50,
        km: Int? = 1_500,
        volumeMl: Long? = 4_000,
        costVnd: Long? = 100_000,
    ) = FuelEntryDraft(filledAt = day(day), odometerKm = km, volumeMl = volumeMl, totalCostVnd = costVnd)

    private fun validate(
        draft: FuelEntryDraft,
        others: List<io.github.diiafyra.motoman.core.domain.model.FuelEntry> = emptyList(),
    ) = validateFuelEntry(draft, vehicle, others, now)

    @Test
    fun `a complete draft with no other entries is valid`() {
        val errors = validate(draft())

        assertTrue(errors.isValid)
        assertEquals(FuelEntryErrors(), errors)
    }

    @Test
    fun `empty fields are reported as missing`() {
        val errors = validate(draft(km = null, volumeMl = null, costVnd = null))

        assertFalse(errors.isValid)
        assertEquals(OdometerError.Missing, errors.odometer)
        assertEquals(VolumeError.Missing, errors.volume)
        assertEquals(CostError.MISSING, errors.cost)
    }

    @Test
    fun `a fill-up in the future is rejected`() {
        assertEquals(FilledAtError.IN_FUTURE, validate(draft(day = 101)).filledAt)
    }

    @Test
    fun `a fill-up exactly now is accepted`() {
        assertNull(validate(draft(day = 100)).filledAt)
    }

    @Test
    fun `odometer below the initial reading is rejected`() {
        assertEquals(OdometerError.BelowInitial(1_000), validate(draft(km = 999)).odometer)
    }

    @Test
    fun `odometer equal to the initial reading is accepted`() {
        assertNull(validate(draft(km = 1_000)).odometer)
    }

    @Test
    fun `odometer lower than the previous fill-up is rejected`() {
        val others =
            listOf(fill("a", day = 10, km = 1_200, liters = 4.0), fill("b", day = 40, km = 1_600, liters = 4.0))

        assertEquals(OdometerError.BelowPrevious(1_600), validate(draft(day = 50, km = 1_599), others).odometer)
    }

    @Test
    fun `odometer equal to the previous fill-up is accepted`() {
        val others = listOf(fill("a", day = 40, km = 1_600, liters = 4.0))

        assertNull(validate(draft(day = 50, km = 1_600), others).odometer)
    }

    @Test
    fun `odometer higher than the next fill-up is rejected`() {
        val others =
            listOf(fill("a", day = 60, km = 1_800, liters = 4.0), fill("b", day = 90, km = 2_400, liters = 4.0))

        assertEquals(OdometerError.AboveNext(1_800), validate(draft(day = 50, km = 1_801), others).odometer)
    }

    @Test
    fun `odometer between the neighbours is accepted when inserting an old fill-up`() {
        val others =
            listOf(fill("a", day = 40, km = 1_400, liters = 4.0), fill("b", day = 60, km = 1_800, liters = 4.0))

        assertNull(validate(draft(day = 50, km = 1_600), others).odometer)
    }

    @Test
    fun `a fill-up at the same time as another counts that one as previous`() {
        val others = listOf(fill("a", day = 50, km = 1_600, liters = 4.0))

        assertEquals(OdometerError.BelowPrevious(1_600), validate(draft(day = 50, km = 1_500), others).odometer)
    }

    @Test
    fun `odometer beyond seven digits is rejected`() {
        assertEquals(OdometerError.TooLarge(MAX_ODOMETER_KM), validate(draft(km = MAX_ODOMETER_KM + 1)).odometer)
        assertNull(validate(draft(km = MAX_ODOMETER_KM)).odometer)
    }

    @Test
    fun `entries from another vehicle are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            validate(draft(), listOf(fill("a", day = 10, km = 1_200, liters = 4.0, vehicleId = "v2")))
        }
    }

    @Test
    fun `zero or negative volume is rejected`() {
        assertEquals(VolumeError.NotPositive, validate(draft(volumeMl = 0)).volume)
        assertEquals(VolumeError.NotPositive, validate(draft(volumeMl = -1)).volume)
    }

    @Test
    fun `volume above the limit is rejected and the limit itself is accepted`() {
        assertEquals(
            VolumeError.TooLarge(MAX_FILL_VOLUME_ML),
            validate(draft(volumeMl = MAX_FILL_VOLUME_ML + 1)).volume,
        )
        assertNull(validate(draft(volumeMl = MAX_FILL_VOLUME_ML)).volume)
    }

    @Test
    fun `zero or negative cost is rejected`() {
        assertEquals(CostError.NOT_POSITIVE, validate(draft(costVnd = 0)).cost)
        assertEquals(CostError.NOT_POSITIVE, validate(draft(costVnd = -5_000)).cost)
    }

    @Test
    fun `every field reports its own error at once`() {
        val errors = validate(draft(day = 101, km = 10, volumeMl = 0, costVnd = 0))

        assertEquals(FilledAtError.IN_FUTURE, errors.filledAt)
        assertEquals(OdometerError.BelowInitial(1_000), errors.odometer)
        assertEquals(VolumeError.NotPositive, errors.volume)
        assertEquals(CostError.NOT_POSITIVE, errors.cost)
    }
}
