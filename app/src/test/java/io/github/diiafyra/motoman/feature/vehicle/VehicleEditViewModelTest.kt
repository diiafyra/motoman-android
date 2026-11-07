package io.github.diiafyra.motoman.feature.vehicle

import androidx.lifecycle.SavedStateHandle
import io.github.diiafyra.motoman.core.domain.maintenance.TemplateNames
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.vehicle.InitialOdometerError
import io.github.diiafyra.motoman.core.domain.vehicle.VehicleErrors
import io.github.diiafyra.motoman.core.domain.vehicle.VehicleNameError
import io.github.diiafyra.motoman.testing.FakeMaintenanceRepository
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import io.github.diiafyra.motoman.testing.SequentialIdGenerator
import io.github.diiafyra.motoman.testing.testVehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VehicleEditViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository()
    private val preferences = FakePreferencesRepository()
    private val maintenance = FakeMaintenanceRepository()

    private fun viewModel(vehicleId: String? = null) =
        VehicleEditViewModel(
            SavedStateHandle(mapOf("vehicleId" to vehicleId)),
            vehicles,
            maintenance,
            preferences,
            SequentialIdGenerator(),
            TemplateNames { it.name },
        )

    @Test
    fun `a new form starts empty without errors`() {
        val state = viewModel().uiState.value

        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("", state.name)
        assertEquals(VehicleErrors(), state.errors)
    }

    @Test
    fun `errors appear only after the first save, then follow typing`() {
        val viewModel = viewModel()

        viewModel.onNameChange(" ")
        assertNull(viewModel.uiState.value.errors.name)

        viewModel.onSaveClick()
        assertEquals(VehicleNameError.BLANK, viewModel.uiState.value.errors.name)
        assertEquals(InitialOdometerError.MISSING, viewModel.uiState.value.errors.initialOdometer)
        assertTrue(vehicles.vehicles.value.isEmpty())

        viewModel.onNameChange("Wave")
        assertNull(viewModel.uiState.value.errors.name)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun `odometer input keeps digits only, up to seven`() {
        val viewModel = viewModel()

        viewModel.onOdometerChange("12.300 km")
        assertEquals("12300", viewModel.uiState.value.odometerText)

        viewModel.onOdometerChange("123456789")
        assertEquals("1234567", viewModel.uiState.value.odometerText)
    }

    @Test
    fun `saving a new vehicle stores it trimmed, selects it and closes`() {
        val viewModel = viewModel()
        viewModel.onNameChange("  Vios  ")
        viewModel.onTypeChange(VehicleType.CAR)
        viewModel.onFuelTypeChange(FuelType.DIESEL)
        viewModel.onOdometerChange("45000")

        viewModel.onSaveClick()

        assertEquals(
            listOf(Vehicle("id-1", "Vios", VehicleType.CAR, FuelType.DIESEL, 45_000)),
            vehicles.vehicles.value,
        )
        assertEquals("id-1", preferences.selectedId.value)
        assertTrue(viewModel.uiState.value.isDone)
        // A car starts with five items, inspection included.
        assertEquals(
            listOf("OIL_CHANGE", "AIR_FILTER", "TYRES", "BRAKES", "INSPECTION"),
            maintenance.items.value.map { it.name },
        )
        assertTrue(maintenance.items.value.all { it.vehicleId == "id-1" })
    }

    @Test
    fun `editing loads the vehicle and saves under the same id without changing the selection`() {
        vehicles.vehicles.value = listOf(testVehicle(id = "v1", name = "Wave"), testVehicle(id = "v2", name = "Vios"))
        preferences.selectedId.value = "v2"
        val viewModel = viewModel(vehicleId = "v1")

        assertFalse(viewModel.uiState.value.isNew)
        assertEquals("Wave", viewModel.uiState.value.name)
        assertEquals("12000", viewModel.uiState.value.odometerText)

        viewModel.onNameChange("Wave RSX")
        viewModel.onSaveClick()

        assertEquals(
            "Wave RSX",
            vehicles.vehicles.value
                .first { it.id == "v1" }
                .name,
        )
        assertEquals(2, vehicles.vehicles.value.size)
        assertEquals("v2", preferences.selectedId.value)
        assertTrue(viewModel.uiState.value.isDone)
        assertTrue("editing adds no items", maintenance.items.value.isEmpty())
    }

    @Test
    fun `editing a vehicle that no longer exists closes the form`() {
        assertTrue(viewModel(vehicleId = "gone").uiState.value.isDone)
    }

    @Test
    fun `delete asks for confirmation, and dismissing keeps the vehicle`() {
        vehicles.vehicles.value = listOf(testVehicle())
        val viewModel = viewModel(vehicleId = "v1")

        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.isDeleteConfirmVisible)

        viewModel.onDeleteDismiss()
        assertFalse(viewModel.uiState.value.isDeleteConfirmVisible)
        assertEquals(1, vehicles.vehicles.value.size)
    }

    @Test
    fun `confirming delete removes the vehicle and closes`() {
        vehicles.vehicles.value = listOf(testVehicle())
        val viewModel = viewModel(vehicleId = "v1")

        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()

        assertTrue(vehicles.vehicles.value.isEmpty())
        assertTrue(viewModel.uiState.value.isDone)
    }
}
