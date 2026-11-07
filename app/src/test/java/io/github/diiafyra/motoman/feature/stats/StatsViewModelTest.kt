package io.github.diiafyra.motoman.feature.stats

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class StatsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun `shows the last six months ending this month`() =
        runTest {
            val viewModel =
                StatsViewModel(
                    ObserveSelectedVehicle(FakeVehicleRepository(testVehicle()), FakePreferencesRepository()),
                    FakeFuelEntryRepository(
                        testFuelEntry("a", Instant.parse("2026-10-01T03:00:00Z"), 12_100, 4_000, 100_000),
                        testFuelEntry("b", Instant.parse("2026-10-10T03:00:00Z"), 12_300, 5_000, 125_000),
                    ),
                    FakeClock(),
                )

            viewModel.uiState.test {
                var state = awaitItem()
                while (state.isLoading) state = awaitItem()
                assertEquals(YearMonth.of(2026, 6), state.months.first().month)
                assertEquals(YearMonth.of(2026, 11), state.months.last().month)
                assertEquals(MONTHS_SHOWN, state.months.size)
                assertEquals(225_000L, state.months[4].costVnd)
                assertTrue(state.hasEconomy)
                assertFalse(state.months[5].litersPer100Km != null)
            }
        }
}
