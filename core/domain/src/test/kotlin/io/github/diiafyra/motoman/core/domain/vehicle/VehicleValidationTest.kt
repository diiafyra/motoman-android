package io.github.diiafyra.motoman.core.domain.vehicle

import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleValidationTest {
    private fun draft(
        name: String = "Wave",
        km: Int? = 12_000,
    ) = VehicleDraft(name, VehicleType.MOTORBIKE, FuelType.RON95, km)

    @Test
    fun `a named vehicle with a reading is valid`() {
        assertTrue(validateVehicle(draft()).isValid)
    }

    @Test
    fun `a blank name is rejected`() {
        assertEquals(VehicleNameError.BLANK, validateVehicle(draft(name = "   ")).name)
    }

    @Test
    fun `the name length limit ignores surrounding spaces`() {
        val longest = "a".repeat(MAX_VEHICLE_NAME_LENGTH)

        assertNull(validateVehicle(draft(name = "  $longest  ")).name)
        assertEquals(VehicleNameError.TOO_LONG, validateVehicle(draft(name = longest + "a")).name)
    }

    @Test
    fun `a missing or too large reading is rejected, zero and the limit are accepted`() {
        assertEquals(InitialOdometerError.MISSING, validateVehicle(draft(km = null)).initialOdometer)
        assertEquals(InitialOdometerError.TOO_LARGE, validateVehicle(draft(km = MAX_ODOMETER_KM + 1)).initialOdometer)
        assertNull(validateVehicle(draft(km = 0)).initialOdometer)
        assertNull(validateVehicle(draft(km = MAX_ODOMETER_KM)).initialOdometer)
    }
}
