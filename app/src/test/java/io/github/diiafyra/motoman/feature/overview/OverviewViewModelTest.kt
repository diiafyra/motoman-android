package io.github.diiafyra.motoman.feature.overview

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.FakeFuelEntryRepository
import io.github.diiafyra.motoman.testing.FakeMaintenanceRepository
import io.github.diiafyra.motoman.testing.FakeMaintenanceSettings
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.MainDispatcherRule
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

class OverviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val vehicles = FakeVehicleRepository(testVehicle(id = "v1", initialOdometerKm = 1_000))
    private val preferences = FakePreferencesRepository("v1")
    private val maintenance = FakeMaintenanceRepository()
    private val settings = FakeMaintenanceSettings()

    private fun viewModel(vararg entries: FuelEntry) =
        OverviewViewModel(
            ObserveSelectedVehicle(vehicles, preferences),
            FakeFuelEntryRepository(*entries),
            maintenance,
            settings,
            FakeClock(),
        )

    @Test
    fun `no fill-ups shows the empty state and the initial odometer`() =
        runTest {
            viewModel().uiState.test {
                val state = awaitReady()
                assertFalse(state.hasFillUps)
                assertNull(state.economy)
                assertEquals(1_000, state.odometerKm)
            }
        }

    @Test
    fun `a single full tank has no economy yet`() =
        runTest {
            viewModel(testFuelEntry("a", Instant.parse("2026-11-01T03:00:00Z"), 1_100)).uiState.test {
                val state = awaitReady()
                assertTrue(state.hasFillUps)
                assertNull(state.economy)
            }
        }

    @Test
    fun `economy and this month's spending come from the fill-ups`() =
        runTest {
            val entries =
                arrayOf(
                    // October: opens the segment, not counted in November spending.
                    testFuelEntry("a", Instant.parse("2026-10-30T03:00:00Z"), 1_000, 4_000, 100_000),
                    testFuelEntry("b", Instant.parse("2026-11-03T03:00:00Z"), 1_200, 4_000, 100_000),
                    testFuelEntry("c", Instant.parse("2026-11-06T03:00:00Z"), 1_400, 6_000, 150_000),
                )
            viewModel(*entries).uiState.test {
                val state = awaitReady()
                val economy = requireNotNull(state.economy)
                assertEquals(3.0, economy.latestLitersPer100Km, 1e-9)
                assertEquals(2.5, economy.averageLitersPer100Km, 1e-9)
                assertEquals(625.0, economy.averageCostPerKmVnd, 1e-9)
                assertFalse(economy.isLatestFromEarlierSegment)
                assertEquals(250_000L, state.monthCostVnd)
                assertEquals(10_000L, state.monthVolumeMl)
                assertEquals(1_400, state.odometerKm)
            }
        }

    @Test
    fun `a newest segment dropped for a missed fill-up is flagged`() =
        runTest {
            val entries =
                arrayOf(
                    testFuelEntry("a", Instant.parse("2026-11-01T03:00:00Z"), 1_000),
                    testFuelEntry("b", Instant.parse("2026-11-03T03:00:00Z"), 1_200),
                    testFuelEntry("c", Instant.parse("2026-11-06T03:00:00Z"), 1_400).copy(missedPrevious = true),
                )
            viewModel(*entries).uiState.test {
                assertTrue(requireNotNull(awaitReady().economy).isLatestFromEarlierSegment)
            }
        }

    @Test
    fun `only overdue and due-soon items need attention, most urgent first, at most three`() =
        runTest {
            maintenance.items.value =
                listOf(
                    testItem("ok", intervalKm = 10_000, lastDoneKm = 1_000),
                    testItem("soon", intervalKm = 1_000, lastDoneKm = 600),
                    testItem("late-a", intervalKm = 100, lastDoneKm = 0),
                    testItem("late-b", intervalKm = 500, lastDoneKm = 0),
                    testItem("late-c", intervalKm = 900, lastDoneKm = 0),
                    testItem("new", intervalKm = 1_000),
                )
            viewModel().uiState.test {
                val state = awaitReady()
                assertTrue(state.hasMaintenanceItems)
                assertEquals(listOf("late-a", "late-b", "late-c"), state.attention.map { it.item.id })
                assertTrue(state.attention.all { it.status is MaintenanceStatus.Overdue })
            }
        }

    @Test
    fun `the due-soon limit comes from the remote setting`() =
        runTest {
            // 600 km left: ok with the default 500 km limit, due soon with a limit of 800.
            maintenance.items.value = listOf(testItem("oil", intervalKm = 1_000, lastDoneKm = 600))
            settings.threshold.value = DueSoonThreshold(km = 800, days = 14)

            viewModel().uiState.test {
                assertEquals(listOf("oil"), awaitReady().attention.map { it.item.id })
            }
        }

    private suspend fun ReceiveTurbine<OverviewUiState>.awaitReady(): OverviewUiState.Ready {
        var state = awaitItem()
        while (state !is OverviewUiState.Ready) state = awaitItem()
        return state
    }
}
