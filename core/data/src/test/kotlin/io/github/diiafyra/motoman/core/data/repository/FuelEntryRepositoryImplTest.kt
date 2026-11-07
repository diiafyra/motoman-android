package io.github.diiafyra.motoman.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.diiafyra.motoman.core.data.FakeClock
import io.github.diiafyra.motoman.core.data.assertFailsWith
import io.github.diiafyra.motoman.core.data.fuelEntry
import io.github.diiafyra.motoman.core.data.inMemoryDatabase
import io.github.diiafyra.motoman.core.data.longColumn
import io.github.diiafyra.motoman.core.data.vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class FuelEntryRepositoryImplTest {
    private val db = inMemoryDatabase()
    private val clock = FakeClock()
    private val fuel = FuelEntryRepositoryImpl(db, clock)

    @Before
    fun addVehicles() =
        runTest {
            val vehicles = VehicleRepositoryImpl(db, clock)
            vehicles.save(vehicle("v1"))
            vehicles.save(vehicle("v2"))
        }

    @After
    fun closeDb() = db.close()

    @Test
    fun entriesAreNewestFirstAndOnlyForTheVehicle() =
        runTest {
            fuel.save(fuelEntry("old", filledAt = Instant.parse("2026-05-01T08:00:00Z"), odometerKm = 1_100))
            fuel.save(fuelEntry("new", filledAt = Instant.parse("2026-05-10T08:00:00Z"), odometerKm = 1_300))
            fuel.save(fuelEntry("other", vehicleId = "v2"))

            assertEquals(listOf("new", "old"), fuel.observeEntries("v1").first().map { it.id })
        }

    @Test
    fun allFieldsSurviveTheRoundTrip() =
        runTest {
            val entry =
                fuelEntry("f1").copy(
                    volumeMl = 3_456,
                    totalCostVnd = 87_654,
                    isFullTank = false,
                    missedPrevious = true,
                    note = "Petrolimex Cầu Giấy",
                )

            fuel.save(entry)

            assertEquals(entry, fuel.observeEntries("v1").first().single())
        }

    @Test
    fun deleteHidesTheEntryAndRestoreBringsItBack() =
        runTest {
            fuel.save(fuelEntry("f1"))

            fuel.delete("f1")
            assertTrue(fuel.observeEntries("v1").first().isEmpty())

            fuel.restore("f1")
            assertEquals(listOf("f1"), fuel.observeEntries("v1").first().map { it.id })
            assertNull(db.longColumn("fuel_entry", "deletedAt", "f1"))
        }

    @Test
    fun deleteAndRestoreMoveUpdatedAt() =
        runTest {
            fuel.save(fuelEntry("f1"))
            clock.advanceSeconds(60)
            fuel.delete("f1")
            assertEquals(clock.now.toEpochMilli(), db.longColumn("fuel_entry", "updatedAt", "f1"))

            clock.advanceSeconds(60)
            fuel.restore("f1")
            assertEquals(clock.now.toEpochMilli(), db.longColumn("fuel_entry", "updatedAt", "f1"))
        }

    @Test
    fun restoreDoesNothingOnceTheVehicleIsDeleted() =
        runTest {
            // Delete a fill-up, delete its vehicle, then the fill-up's undo fires.
            fuel.save(fuelEntry("f1"))
            fuel.delete("f1")
            VehicleRepositoryImpl(db, clock).delete("v1")

            fuel.restore("f1")

            assertNotNull(db.longColumn("fuel_entry", "deletedAt", "f1"))
        }

    @Test
    fun savingUnderADeletedVehicleFails() =
        runTest {
            VehicleRepositoryImpl(db, clock).delete("v1")

            assertFailsWith<IllegalStateException> { fuel.save(fuelEntry("f1")) }
            assertTrue(fuel.observeEntries("v1").first().isEmpty())
        }

    @Test
    fun aStaleSaveAfterDeletionKeepsTheEntryDeleted() =
        runTest {
            fuel.save(fuelEntry("f1"))
            fuel.delete("f1")

            fuel.save(fuelEntry("f1", odometerKm = 1_250))

            assertTrue(fuel.observeEntries("v1").first().isEmpty())
        }

    @Test
    fun deletingTwiceKeepsTheFirstDeletionTime() =
        runTest {
            fuel.save(fuelEntry("f1"))
            fuel.delete("f1")
            val firstDeletion = db.longColumn("fuel_entry", "deletedAt", "f1")
            clock.advanceSeconds(60)

            fuel.delete("f1")

            assertEquals(firstDeletion, db.longColumn("fuel_entry", "deletedAt", "f1"))
        }
}
