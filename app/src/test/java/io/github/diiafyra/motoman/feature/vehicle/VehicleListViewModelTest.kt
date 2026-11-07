package io.github.diiafyra.motoman.feature.vehicle

import app.cash.turbine.test
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import io.github.diiafyra.motoman.testing.testVehicle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VehicleListViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository(testVehicle(id = "v1"), testVehicle(id = "v2"))
    private val preferences = FakePreferencesRepository()

    // Created lazily: viewModelScope must start after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { VehicleListViewModel(ObserveSelectedVehicle(vehicles, preferences), preferences) }

    @Test
    fun `lists vehicles with the oldest selected by default`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitLoaded()
                assertFalse(state.isLoading)
                assertEquals(listOf("v1", "v2"), state.vehicles.map { it.id })
                assertEquals("v1", state.selectedId)
            }
        }

    @Test
    fun `choosing a vehicle stores it, then closes`() =
        runTest {
            viewModel.uiState.test {
                awaitLoaded()

                viewModel.onVehicleClick("v2")

                assertEquals("v2", preferences.selectedId.value)
                val state = expectMostRecentItem()
                assertEquals("v2", state.selectedId)
                assertTrue(state.isDone)
            }
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<VehicleListUiState>.awaitLoaded(): VehicleListUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
