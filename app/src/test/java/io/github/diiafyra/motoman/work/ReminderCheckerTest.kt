package io.github.diiafyra.motoman.work

import io.github.diiafyra.motoman.core.domain.maintenance.DueSoonThreshold
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.core.domain.maintenance.Reminder
import io.github.diiafyra.motoman.testing.FakeClock
import io.github.diiafyra.motoman.testing.FakeFuelEntryRepository
import io.github.diiafyra.motoman.testing.FakeMaintenanceRepository
import io.github.diiafyra.motoman.testing.FakeMaintenanceSettings
import io.github.diiafyra.motoman.testing.FakePreferencesRepository
import io.github.diiafyra.motoman.testing.FakeVehicleRepository
import io.github.diiafyra.motoman.testing.testFuelEntry
import io.github.diiafyra.motoman.testing.testItem
import io.github.diiafyra.motoman.testing.testVehicle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderCheckerTest {
    private val vehicles =
        FakeVehicleRepository(
            testVehicle(id = "wave", name = "Wave", initialOdometerKm = 1_000),
            testVehicle(id = "vios", name = "Vios", initialOdometerKm = 50_000),
        )

    // Wave is at 1,900 km: oil (due at 2,000) is due soon, tyres are fine.
    private val fuel =
        FakeFuelEntryRepository(testFuelEntry("f", Instant.parse("2026-11-01T03:00:00Z"), 1_900, vehicleId = "wave"))
    private val maintenance =
        FakeMaintenanceRepository(
            testItem("oil", name = "Oil", intervalKm = 1_000, lastDoneKm = 1_000, vehicleId = "wave"),
            testItem("tyres", name = "Tyres", intervalKm = 20_000, lastDoneKm = 1_000, vehicleId = "wave"),
            // Vios is at 50,000 km: brakes due at 45,000 are overdue.
            testItem("brakes", name = "Brakes", intervalKm = 20_000, lastDoneKm = 25_000, vehicleId = "vios"),
        )
    private val preferences = FakePreferencesRepository()
    private val notifier = FakeNotifier()
    private val settings = FakeMaintenanceSettings()

    private val checker = ReminderChecker(vehicles, fuel, maintenance, preferences, settings, FakeClock(), notifier)

    @Test
    fun `notifies due items of every vehicle and remembers them`() =
        runTest {
            assertEquals(2, checker.check())

            val sent = notifier.sent.single()
            assertEquals(listOf("oil", "brakes"), sent.map { it.itemStatus.item.id })
            assertEquals(listOf("Wave", "Vios"), sent.map { it.vehicle.name })
            assertTrue(sent[1].itemStatus.status is MaintenanceStatus.Overdue)
            assertEquals(2, preferences.sentKeys.size)
        }

    @Test
    fun `the next day does not repeat the same reminders`() =
        runTest {
            checker.check()

            assertEquals(0, checker.check())
            assertEquals(1, notifier.sent.size)
        }

    @Test
    fun `nothing is sent or remembered while reminders are off`() =
        runTest {
            preferences.remindersEnabled.value = false

            assertEquals(0, checker.check())
            assertTrue(notifier.sent.isEmpty())
            assertTrue(preferences.sentKeys.isEmpty())
        }

    @Test
    fun `without notification permission the reminders wait until it is granted`() =
        runTest {
            notifier.allowed = false
            assertEquals(0, checker.check())
            assertTrue(preferences.sentKeys.isEmpty())

            notifier.allowed = true
            assertEquals(2, checker.check())
        }

    @Test
    fun `a wider remote limit makes more items due soon`() =
        runTest {
            // Due at 2,500 with the Wave at 1,900: 600 km left, outside the 500 km default.
            maintenance.items.value +=
                testItem("filter", name = "Filter", intervalKm = 1_500, lastDoneKm = 1_000, vehicleId = "wave")
            assertEquals(2, checker.check())

            preferences.sentKeys = emptySet()
            settings.threshold.value = DueSoonThreshold(km = 800, days = 14)
            assertEquals(3, checker.check())
        }

    private class FakeNotifier : ReminderNotifier {
        var allowed = true
        val sent = mutableListOf<List<Reminder>>()

        override fun canNotify() = allowed

        override fun notify(reminders: List<Reminder>) {
            sent += reminders
        }
    }
}
