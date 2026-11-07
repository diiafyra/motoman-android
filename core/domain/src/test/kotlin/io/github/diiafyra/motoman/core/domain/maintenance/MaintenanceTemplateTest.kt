package io.github.diiafyra.motoman.core.domain.maintenance

import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceTemplateTest {
    private val names = TemplateNames { it.name.lowercase() }

    private fun ids(): () -> String {
        var n = 0
        return { "id-${++n}" }
    }

    @Test
    fun `a motorbike gets four items without inspection`() {
        val next = ids()
        val items = templateItems(vehicle(), names) { next() }

        assertEquals(listOf("oil_change", "air_filter", "tyres", "brakes"), items.map { it.name })
        assertEquals(listOf("id-1", "id-2", "id-3", "id-4"), items.map { it.id })
        assertTrue(items.all { it.vehicleId == "v1" && it.lastDoneKm == null && it.lastDoneDate == null })
        assertEquals(2_000, items.first().intervalKm)
    }

    @Test
    fun `a car also gets the yearly inspection, by time only`() {
        val next = ids()
        val items = templateItems(vehicle().copy(type = VehicleType.CAR), names) { next() }

        val inspection = items.last()
        assertEquals("inspection", inspection.name)
        assertEquals(null, inspection.intervalKm)
        assertEquals(12, inspection.intervalMonths)
        assertEquals(5, items.size)
    }
}
