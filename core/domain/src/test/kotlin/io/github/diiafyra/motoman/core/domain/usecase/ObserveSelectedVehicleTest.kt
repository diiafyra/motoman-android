package io.github.diiafyra.motoman.core.domain.usecase

import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveSelectedVehicleTest {
    private val wave = vehicle().copy(id = "wave", name = "Wave")
    private val vios = vehicle().copy(id = "vios", name = "Vios")

    private val vehicles = FakeVehicles()
    private val preferences = FakePreferences()
    private val observe = ObserveSelectedVehicle(vehicles, preferences)

    @Test
    fun `no vehicles means nothing selected`() =
        runTest {
            assertNull(observe().first().selected)
        }

    @Test
    fun `the chosen vehicle is selected`() =
        runTest {
            vehicles.state.value = listOf(wave, vios)
            preferences.state.value = "vios"

            assertEquals(vios, observe().first().selected)
        }

    @Test
    fun `nothing chosen falls back to the oldest vehicle`() =
        runTest {
            vehicles.state.value = listOf(wave, vios)

            assertEquals(wave, observe().first().selected)
        }

    @Test
    fun `a chosen vehicle that was deleted falls back to the oldest one left`() =
        runTest {
            vehicles.state.value = listOf(wave)
            preferences.state.value = "vios"

            val result = observe().first()

            assertEquals(wave, result.selected)
            assertEquals(listOf(wave), result.vehicles)
        }

    private class FakeVehicles : VehicleRepository {
        val state = MutableStateFlow<List<Vehicle>>(emptyList())

        override fun observeVehicles(): Flow<List<Vehicle>> = state

        override fun observeVehicle(id: String): Flow<Vehicle?> = state.map { all -> all.firstOrNull { it.id == id } }

        override suspend fun save(vehicle: Vehicle) = error("unused")

        override suspend fun delete(id: String) = error("unused")
    }

    private class FakePreferences : PreferencesRepository {
        val state = MutableStateFlow<String?>(null)

        override fun observeSelectedVehicleId(): Flow<String?> = state

        override suspend fun setSelectedVehicleId(id: String) {
            state.value = id
        }
    }
}
