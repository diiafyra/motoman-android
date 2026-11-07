package io.github.diiafyra.motoman.feature.fuel

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.FakeFuelEntryRepository
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import io.github.diiafyra.motoman.testing.testFuelEntry
import io.github.diiafyra.motoman.testing.testVehicle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class FuelLogViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository(testVehicle(id = "v1"), testVehicle(id = "v2"))
    private val preferences = FakePreferencesRepository(selected = "v1")
    private val fuel =
        FakeFuelEntryRepository(
            testFuelEntry("a", Instant.parse("2026-10-20T03:00:00Z"), 12_100),
            testFuelEntry("b", Instant.parse("2026-11-02T03:00:00Z"), 12_300),
            testFuelEntry("other", Instant.parse("2026-11-03T03:00:00Z"), 500, vehicleId = "v2"),
        )
    private val viewModel by lazy { FuelLogViewModel(ObserveSelectedVehicle(vehicles, preferences), fuel, FakeClock()) }

    @Test
    fun `shows the selected vehicle's fill-ups by month`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitLoaded()
                assertEquals("v1", state.vehicleId)
                assertEquals(listOf(listOf("b"), listOf("a")), state.months.map { m -> m.entries.map { it.id } })
            }
        }

    @Test
    fun `switching vehicle switches the list`() =
        runTest {
            viewModel.uiState.test {
                awaitLoaded()

                preferences.selectedId.value = "v2"

                val state = expectMostRecentItem()
                assertEquals("v2", state.vehicleId)
                assertEquals(listOf("other"), state.months.flatMap { m -> m.entries.map { it.id } })
            }
        }

    @Test
    fun `delete offers undo, and undo brings the entry back`() =
        runTest {
            viewModel.uiState.test {
                awaitLoaded()

                viewModel.onDelete("b")
                var state = expectMostRecentItem()
                assertEquals("b", state.undoEntryId)
                assertEquals(listOf("a"), state.months.flatMap { m -> m.entries.map { it.id } })

                viewModel.onUndo("b")
                viewModel.onUndoShown("b")
                state = expectMostRecentItem()
                assertNull(state.undoEntryId)
                assertEquals(listOf("b", "a"), state.months.flatMap { m -> m.entries.map { it.id } })
            }
        }

    @Test
    fun `a newer deletion is not cleared by the older snackbar finishing`() =
        runTest {
            viewModel.uiState.test {
                awaitLoaded()

                viewModel.onDelete("b")
                viewModel.onDelete("a")
                viewModel.onUndoShown("b")

                assertEquals("a", expectMostRecentItem().undoEntryId)
            }
        }

    private suspend fun ReceiveTurbine<FuelLogUiState>.awaitLoaded(): FuelLogUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }
}
