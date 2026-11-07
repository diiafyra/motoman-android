package io.github.diiafyra.motoman.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.diiafyra.motoman.core.domain.maintenance.IntervalError
import io.github.diiafyra.motoman.core.domain.maintenance.ItemNameError
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.core.domain.maintenance.RecordOdometerError
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.FakeFuelEntryRepository
import io.github.diiafyra.motoman.testing.FakeMaintenanceRepository
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
import io.github.diiafyra.motoman.testing.SequentialIdGenerator
import io.github.diiafyra.motoman.testing.testFuelEntry
import io.github.diiafyra.motoman.testing.testItem
import io.github.diiafyra.motoman.testing.testVehicle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MaintenanceViewModelsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FakeClock()
    private val today = LocalDate.of(2026, 11, 8)
    private val vehicles = FakeVehicleRepository(testVehicle(id = "v1", initialOdometerKm = 1_000))
    private val fuel = FakeFuelEntryRepository(testFuelEntry("f", Instant.parse("2026-11-01T03:00:00Z"), 5_000))
    private val maintenance =
        FakeMaintenanceRepository(
            testItem("oil", name = "Oil change", intervalKm = 2_000, lastDoneKm = 3_200),
            testItem("tyres", name = "Tyres", intervalKm = 20_000),
        )

    // List

    @Test
    fun `the list ranks items by urgency at the current odometer`() =
        runTest {
            val viewModel =
                MaintenanceViewModel(
                    ObserveSelectedVehicle(vehicles, FakePreferencesRepository()),
                    fuel,
                    maintenance,
                    FakePreferencesRepository(),
                    clock,
                )

            viewModel.uiState.test {
                var state = awaitItem()
                while (state !is MaintenanceUiState.Ready) state = awaitItem()
                assertEquals(5_000, state.odometerKm)
                assertEquals(listOf("oil", "tyres"), state.items.map { it.item.id })
                assertTrue(state.items[0].status is MaintenanceStatus.DueSoon)
                assertEquals(MaintenanceStatus.NoBaseline, state.items[1].status)
                assertTrue(state.mayAskForNotifications)
            }
        }

    @Test
    fun `the notification prompt goes away after not now, or with reminders off`() =
        runTest {
            val preferences = FakePreferencesRepository()
            val viewModel =
                MaintenanceViewModel(
                    ObserveSelectedVehicle(vehicles, preferences),
                    fuel,
                    maintenance,
                    preferences,
                    clock,
                )

            viewModel.uiState.test {
                var state = awaitItem()
                while (state !is MaintenanceUiState.Ready) state = awaitItem()
                assertTrue(state.mayAskForNotifications)

                viewModel.onNotificationPromptDismiss()
                assertFalse((expectMostRecentItem() as MaintenanceUiState.Ready).mayAskForNotifications)

                preferences.promptDismissed.value = false
                preferences.remindersEnabled.value = false
                assertFalse((expectMostRecentItem() as MaintenanceUiState.Ready).mayAskForNotifications)
            }
        }

    // Item form

    private fun itemViewModel(itemId: String? = null) =
        MaintenanceItemEditViewModel(
            SavedStateHandle(mapOf("vehicleId" to "v1", "itemId" to itemId)),
            maintenance,
            SequentialIdGenerator(),
        )

    @Test
    fun `a new item needs a name and an interval`() {
        val viewModel = itemViewModel()

        viewModel.onSaveClick()

        val errors = viewModel.uiState.value.errors
        assertEquals(ItemNameError.BLANK, errors.name)
        assertTrue(errors.noInterval)
        assertEquals(2, maintenance.items.value.size)
    }

    @Test
    fun `a new item is saved with its intervals and no baseline`() {
        val viewModel = itemViewModel()
        viewModel.onNameChange(" Chain ")
        viewModel.onIntervalKmChange("0")
        viewModel.onSaveClick()
        assertEquals(IntervalError.NOT_POSITIVE, viewModel.uiState.value.errors.intervalKm)

        viewModel.onIntervalKmChange("3000")
        viewModel.onIntervalMonthsChange("6")
        viewModel.onSaveClick()

        val saved = maintenance.items.value.single { it.id == "id-1" }
        assertEquals("Chain", saved.name)
        assertEquals(3_000, saved.intervalKm)
        assertEquals(6, saved.intervalMonths)
        assertNull(saved.lastDoneKm)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `editing loads the item and its history, and keeps the baseline`() =
        runTest {
            maintenance.recordDone(MaintenanceRecord("r1", "oil", "v1", LocalDate.of(2026, 10, 1), 3_200, 120_000))
            val viewModel = itemViewModel("oil")

            assertEquals("Oil change", viewModel.uiState.value.name)
            assertEquals("2000", viewModel.uiState.value.intervalKmText)
            assertEquals(
                listOf("r1"),
                viewModel.uiState.value.history
                    .map { it.id },
            )

            viewModel.onIntervalKmChange("1500")
            viewModel.onSaveClick()

            val saved = maintenance.items.value.single { it.id == "oil" }
            assertEquals(1_500, saved.intervalKm)
            assertEquals(3_200, saved.lastDoneKm)
        }

    @Test
    fun `deleting an item asks first and removes it`() {
        val viewModel = itemViewModel("tyres")

        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.isDeleteConfirmVisible)
        viewModel.onDeleteConfirm()

        assertEquals(listOf("oil"), maintenance.items.value.map { it.id })
        assertTrue(viewModel.uiState.value.isDone)
    }

    // Mark done

    private fun doneViewModel(itemId: String = "tyres") =
        MaintenanceDoneViewModel(
            SavedStateHandle(mapOf("vehicleId" to "v1", "itemId" to itemId)),
            vehicles,
            fuel,
            maintenance,
            clock,
            SequentialIdGenerator(),
        )

    @Test
    fun `mark done starts today at the current odometer`() {
        val state = doneViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Tyres", state.itemName)
        assertEquals(today, state.date)
        assertEquals("5000", state.odometerText)
    }

    @Test
    fun `mark done stores the record and sets the item's baseline`() {
        val viewModel = doneViewModel()
        viewModel.onCostChange("1.200.000")
        viewModel.onNoteChange(" Michelin ")

        viewModel.onSaveClick()

        val record = maintenance.records.value.single()
        assertEquals("tyres", record.itemId)
        assertEquals(5_000, record.odometerKm)
        assertEquals(1_200_000L, record.costVnd)
        assertEquals("Michelin", record.note)
        assertEquals(
            5_000,
            maintenance.items.value
                .single { it.id == "tyres" }
                .lastDoneKm,
        )
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `mark done without a reading shows an error and stores nothing`() {
        val viewModel = doneViewModel()
        viewModel.onOdometerChange("")

        viewModel.onSaveClick()

        assertEquals(RecordOdometerError.MISSING, viewModel.uiState.value.errors.odometer)
        assertTrue(maintenance.records.value.isEmpty())
    }

    @Test
    fun `an empty cost is stored as no cost, and an earlier date is allowed`() {
        val viewModel = doneViewModel()
        viewModel.onDateChange(LocalDate.of(2026, 9, 1))
        viewModel.onDateChange(LocalDate.of(2027, 1, 1))
        assertEquals(today, viewModel.uiState.value.date)

        viewModel.onDateChange(LocalDate.of(2026, 9, 1))
        viewModel.onSaveClick()

        val record = maintenance.records.value.single()
        assertNull(record.costVnd)
        assertEquals(LocalDate.of(2026, 9, 1), record.doneDate)
    }

    @Test
    fun `mark done for an item that no longer exists closes`() {
        assertTrue(doneViewModel("gone").uiState.value.isDone)
    }
}
