package io.github.diiafyra.motoman.feature.fuel

import androidx.lifecycle.SavedStateHandle
import io.github.diiafyra.motoman.core.domain.fuel.CostError
import io.github.diiafyra.motoman.core.domain.fuel.FuelEntryErrors
import io.github.diiafyra.motoman.core.domain.fuel.OdometerError
import io.github.diiafyra.motoman.core.domain.fuel.VolumeError
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.FakeFuelEntryRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import io.github.diiafyra.motoman.testing.SequentialIdGenerator
import io.github.diiafyra.motoman.testing.testFuelEntry
import io.github.diiafyra.motoman.testing.testVehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FuelEditViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FakeClock()
    private val vehicles = FakeVehicleRepository(testVehicle(id = "v1", initialOdometerKm = 12_000))

    // Two earlier fill-ups: 1 Nov at 12,300 km and 5 Nov at 12,450 km.
    private val nov1 = testFuelEntry("nov1", Instant.parse("2026-11-01T05:00:00Z"), 12_300)
    private val nov5 = testFuelEntry("nov5", Instant.parse("2026-11-05T05:00:00Z"), 12_450)
    private val fuel = FakeFuelEntryRepository(nov1, nov5)

    private fun viewModel(entryId: String? = null) =
        FuelEditViewModel(
            SavedStateHandle(mapOf("vehicleId" to "v1", "entryId" to entryId)),
            vehicles,
            fuel,
            clock,
            SequentialIdGenerator(),
        )

    private fun FuelEditViewModel.fill(
        km: String = "12600",
        liters: String = "3,8",
        cost: String = "95000",
    ) {
        onOdometerChange(km)
        onVolumeChange(liters)
        onCostChange(cost)
    }

    @Test
    fun `a new form starts today, full tank, with the last reading as a hint`() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(LocalDate.of(2026, 11, 8), state.date)
        assertTrue(state.isFullTank)
        assertEquals(12_450, state.previousOdometerKm)
        assertEquals(FuelEntryErrors(), state.errors)
    }

    @Test
    fun `price per litre is shown as soon as volume and cost are readable`() {
        val viewModel = viewModel()

        viewModel.onVolumeChange("3,8")
        assertNull(viewModel.uiState.value.pricePerLiterVnd)

        viewModel.onCostChange("95000")
        assertEquals(25_000L, viewModel.uiState.value.pricePerLiterVnd)
    }

    @Test
    fun `saving an empty form shows an error on each field and stores nothing`() {
        val viewModel = viewModel()

        viewModel.onSaveClick()

        val errors = viewModel.uiState.value.errors
        assertEquals(OdometerError.Missing, errors.odometer)
        assertEquals(VolumeError.Missing, errors.volume)
        assertEquals(CostError.MISSING, errors.cost)
        assertEquals(2, fuel.entries.size)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun `a reading below the previous fill-up is rejected`() {
        val viewModel = viewModel()
        viewModel.fill(km = "12400")

        viewModel.onSaveClick()

        assertEquals(OdometerError.BelowPrevious(12_450), viewModel.uiState.value.errors.odometer)
    }

    @Test
    fun `an unreadable volume gets its own message`() {
        val viewModel = viewModel()
        viewModel.fill(liters = "3,8,1")

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.isVolumeUnreadable)
        assertEquals(2, fuel.entries.size)
    }

    @Test
    fun `saving today stores the current instant, millilitres and the flags`() {
        val viewModel = viewModel()
        viewModel.fill()
        viewModel.onFullTankChange(false)
        viewModel.onMissedPreviousChange(true)
        viewModel.onNoteChange("  Petrolimex  ")

        viewModel.onSaveClick()

        val saved = fuel.entries.single { it.id == "id-1" }
        assertEquals(clock.now, saved.filledAt)
        assertEquals(12_600, saved.odometerKm)
        assertEquals(3_800L, saved.volumeMl)
        assertEquals(95_000L, saved.totalCostVnd)
        assertFalse(saved.isFullTank)
        assertTrue(saved.missedPrevious)
        assertEquals("Petrolimex", saved.note)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `an earlier date is stored at noon local time and checked against its neighbours`() {
        val viewModel = viewModel()
        viewModel.onDateChange(LocalDate.of(2026, 11, 3))

        // Between 1 Nov (12,300) and 5 Nov (12,450).
        assertEquals(12_300, viewModel.uiState.value.previousOdometerKm)
        viewModel.fill(km = "12500")
        viewModel.onSaveClick()
        assertEquals(OdometerError.AboveNext(12_450), viewModel.uiState.value.errors.odometer)

        viewModel.onOdometerChange("12380")
        viewModel.onSaveClick()

        // Noon in Hanoi is 05:00 UTC.
        assertEquals(Instant.parse("2026-11-03T05:00:00Z"), fuel.entries.single { it.id == "id-1" }.filledAt)
    }

    @Test
    fun `a future date is clamped to today`() {
        val viewModel = viewModel()

        viewModel.onDateChange(LocalDate.of(2026, 12, 1))

        assertEquals(LocalDate.of(2026, 11, 8), viewModel.uiState.value.date)
    }

    @Test
    fun `editing loads the entry and keeps its id and time when the date is unchanged`() {
        val viewModel = viewModel(entryId = "nov5")

        val state = viewModel.uiState.value
        assertFalse(state.isNew)
        assertEquals("12450", state.odometerText)
        assertEquals("4", state.volumeText)
        assertEquals(LocalDate.of(2026, 11, 5), state.date)
        // The entry itself is not its own neighbour.
        assertEquals(12_300, state.previousOdometerKm)

        viewModel.onCostChange("101000")
        viewModel.onSaveClick()

        val saved = fuel.entries.single { it.id == "nov5" }
        assertEquals(nov5.filledAt, saved.filledAt)
        assertEquals(101_000L, saved.totalCostVnd)
        assertEquals(2, fuel.entries.size)
    }

    @Test
    fun `editing an entry that no longer exists closes the form`() {
        assertTrue(viewModel(entryId = "gone").uiState.value.isDone)
    }
}
