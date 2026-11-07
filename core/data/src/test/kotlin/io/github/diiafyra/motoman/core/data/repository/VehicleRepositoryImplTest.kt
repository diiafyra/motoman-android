package io.github.diiafyra.motoman.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.diiafyra.motoman.core.data.FakeClock
import io.github.diiafyra.motoman.core.data.fuelEntry
import io.github.diiafyra.motoman.core.data.inMemoryDatabase
import io.github.diiafyra.motoman.core.data.longColumn
import io.github.diiafyra.motoman.core.data.maintenanceItem
import io.github.diiafyra.motoman.core.data.maintenanceRecord
import io.github.diiafyra.motoman.core.data.vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleRepositoryImplTest {
    private val db = inMemoryDatabase()
    private val clock = FakeClock()
    private val vehicles = VehicleRepositoryImpl(db, clock)
    private val fuel = FuelEntryRepositoryImpl(db, clock)
    private val maintenance = MaintenanceRepositoryImpl(db, clock)

    @After
    fun closeDb() = db.close()

    @Test
    fun savedVehiclesAreObservedOldestFirst() =
        runTest {
            vehicles.save(vehicle("v1", "Wave"))
            clock.advanceSeconds(60)
            vehicles.save(vehicle("v2", "Vios"))

            assertEquals(listOf("Wave", "Vios"), vehicles.observeVehicles().first().map { it.name })
            assertEquals("Vios", vehicles.observeVehicle("v2").first()?.name)
        }

    @Test
    fun updatingKeepsCreatedAtAndMovesUpdatedAt() =
        runTest {
            vehicles.save(vehicle(name = "Wave"))
            val createdAt = db.longColumn("vehicle", "createdAt", "v1")
            clock.advanceSeconds(60)

            vehicles.save(vehicle(name = "Wave RSX"))

            assertEquals(createdAt, db.longColumn("vehicle", "createdAt", "v1"))
            assertEquals(clock.now.toEpochMilli(), db.longColumn("vehicle", "updatedAt", "v1"))
            assertEquals("Wave RSX", vehicles.observeVehicle("v1").first()?.name)
        }

    @Test
    fun updatingAVehicleKeepsItsFillUpsAndMaintenance() =
        runTest {
            // Regression guard: an INSERT OR REPLACE would delete the row and cascade to its children.
            vehicles.save(vehicle())
            fuel.save(fuelEntry("f1"))
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.recordDone(maintenanceRecord("r1"))

            vehicles.save(vehicle(name = "Renamed"))

            assertEquals(1, fuel.observeEntries("v1").first().size)
            assertEquals(1, maintenance.observeItems("v1").first().size)
            assertEquals(1, maintenance.observeRecords("v1").first().size)
        }

    @Test
    fun deletingSoftDeletesTheVehicleAndEverythingUnderIt() =
        runTest {
            vehicles.save(vehicle())
            fuel.save(fuelEntry("f1"))
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.recordDone(maintenanceRecord("r1"))
            clock.advanceSeconds(60)

            vehicles.delete("v1")

            assertTrue(vehicles.observeVehicles().first().isEmpty())
            assertNull(vehicles.observeVehicle("v1").first())
            assertTrue(fuel.observeEntries("v1").first().isEmpty())
            assertTrue(maintenance.observeItems("v1").first().isEmpty())
            assertTrue(maintenance.observeRecords("v1").first().isEmpty())
            // Rows stay, marked, so that sync can upload the deletion later.
            val deletedAt = clock.now.toEpochMilli()
            assertEquals(deletedAt, db.longColumn("vehicle", "deletedAt", "v1"))
            assertEquals(deletedAt, db.longColumn("fuel_entry", "deletedAt", "f1"))
            assertEquals(deletedAt, db.longColumn("maintenance_item", "deletedAt", "i1"))
            assertEquals(deletedAt, db.longColumn("maintenance_record", "deletedAt", "r1"))
        }

    @Test
    fun deletingOneVehicleLeavesOthersAlone() =
        runTest {
            vehicles.save(vehicle("v1"))
            vehicles.save(vehicle("v2"))
            fuel.save(fuelEntry("f2", vehicleId = "v2"))

            vehicles.delete("v1")

            assertEquals(listOf("v2"), vehicles.observeVehicles().first().map { it.id })
            assertNull(db.longColumn("fuel_entry", "deletedAt", "f2"))
        }

    @Test
    fun aStaleSaveAfterDeletionKeepsTheVehicleDeleted() =
        runTest {
            // An edit screen still open while the vehicle is deleted elsewhere, then the user taps Save.
            vehicles.save(vehicle())
            vehicles.delete("v1")

            vehicles.save(vehicle(name = "Edited"))

            assertNull(vehicles.observeVehicle("v1").first())
            assertNotNull(db.longColumn("vehicle", "deletedAt", "v1"))
        }

    @Test
    fun updatedAtNeverMovesBackwardsWhenTheClockDoes() =
        runTest {
            vehicles.save(vehicle())
            val firstUpdate = db.longColumn("vehicle", "updatedAt", "v1")!!
            clock.advanceSeconds(-3_600)

            vehicles.save(vehicle(name = "Edited"))
            assertEquals(firstUpdate + 1, db.longColumn("vehicle", "updatedAt", "v1"))

            vehicles.delete("v1")
            assertEquals(firstUpdate + 2, db.longColumn("vehicle", "updatedAt", "v1"))
        }
}
