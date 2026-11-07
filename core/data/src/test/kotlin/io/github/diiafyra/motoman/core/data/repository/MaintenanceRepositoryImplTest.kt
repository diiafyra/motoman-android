package io.github.diiafyra.motoman.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.diiafyra.motoman.core.data.FakeClock
import io.github.diiafyra.motoman.core.data.assertFailsWith
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MaintenanceRepositoryImplTest {
    private val db = inMemoryDatabase()
    private val clock = FakeClock()
    private val maintenance = MaintenanceRepositoryImpl(db, clock)

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
    fun itemsSavedTogetherKeepTheirOrder() =
        runTest {
            val ids = listOf("oil", "air-filter", "tyres", "brakes")

            maintenance.saveItems(ids.map { maintenanceItem(it) })

            assertEquals(ids, maintenance.observeItems("v1").first().map { it.id })
        }

    @Test
    fun recordDoneStoresTheRecordAndMovesTheBaseline() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1"))

            maintenance.recordDone(maintenanceRecord("r1", doneDate = LocalDate.of(2026, 5, 1), odometerKm = 5_000))

            val item = maintenance.observeItems("v1").first().single()
            assertEquals(5_000, item.lastDoneKm)
            assertEquals(LocalDate.of(2026, 5, 1), item.lastDoneDate)
            assertEquals(listOf("r1"), maintenance.observeRecords("v1").first().map { it.id })
        }

    @Test
    fun anOlderRecordIsStoredWithoutMovingTheBaselineBack() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1", lastDoneKm = 5_000, lastDoneDate = LocalDate.of(2026, 5, 1)))

            maintenance.recordDone(maintenanceRecord("r0", doneDate = LocalDate.of(2025, 11, 1), odometerKm = 1_000))

            val item = maintenance.observeItems("v1").first().single()
            assertEquals(5_000, item.lastDoneKm)
            assertEquals(1, maintenance.observeRecords("v1").first().size)
        }

    @Test
    fun recordsAreNewestFirst() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.recordDone(maintenanceRecord("r1", doneDate = LocalDate.of(2026, 1, 1), odometerKm = 2_000))
            maintenance.recordDone(maintenanceRecord("r2", doneDate = LocalDate.of(2026, 5, 1), odometerKm = 5_000))

            assertEquals(listOf("r2", "r1"), maintenance.observeRecords("v1").first().map { it.id })
        }

    @Test
    fun deletingAnItemSoftDeletesItsRecords() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.recordDone(maintenanceRecord("r1"))

            maintenance.deleteItem("i1")

            assertTrue(maintenance.observeItems("v1").first().isEmpty())
            assertTrue(maintenance.observeRecords("v1").first().isEmpty())
            assertNotNull(db.longColumn("maintenance_record", "deletedAt", "r1"))
        }

    @Test
    fun recordDoneForAMissingItemFailsWithoutWritingAnything() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                maintenance.recordDone(maintenanceRecord("r1", itemId = "missing"))
            }
            assertTrue(maintenance.observeRecords("v1").first().isEmpty())
        }

    @Test
    fun recordDoneForAnItemOfAnotherVehicleFailsWithoutWritingAnything() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1", vehicleId = "v2"))

            assertFailsWith<IllegalArgumentException> {
                maintenance.recordDone(maintenanceRecord("r1", vehicleId = "v1"))
            }
            assertTrue(maintenance.observeRecords("v1").first().isEmpty())
            assertTrue(maintenance.observeRecords("v2").first().isEmpty())
        }

    @Test
    fun recordDoneRejectsAnExistingRecordId() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.recordDone(maintenanceRecord("r1", odometerKm = 5_000))

            assertFailsWith<IllegalArgumentException> {
                maintenance.recordDone(maintenanceRecord("r1", odometerKm = 4_000))
            }
            assertEquals(
                5_000,
                maintenance
                    .observeRecords("v1")
                    .first()
                    .single()
                    .odometerKm,
            )
        }

    @Test
    fun recordDoneForADeletedItemFails() =
        runTest {
            maintenance.saveItem(maintenanceItem("i1"))
            maintenance.deleteItem("i1")

            assertFailsWith<IllegalArgumentException> { maintenance.recordDone(maintenanceRecord("r1")) }
        }

    @Test
    fun aStaleItemEditKeepsTheBaselineSetByRecordDone() =
        runTest {
            // The edit form loads the item, a service is recorded, then the form saves its old copy.
            val formCopy = maintenanceItem("i1")
            maintenance.saveItem(formCopy)
            maintenance.recordDone(maintenanceRecord("r1", doneDate = LocalDate.of(2026, 5, 1), odometerKm = 5_000))

            maintenance.saveItem(formCopy.copy(name = "Engine oil", intervalKm = 3_000))

            val item = maintenance.observeItems("v1").first().single()
            assertEquals("Engine oil", item.name)
            assertEquals(3_000, item.intervalKm)
            assertEquals(5_000, item.lastDoneKm)
            assertEquals(LocalDate.of(2026, 5, 1), item.lastDoneDate)
        }

    @Test
    fun resavingItemsKeepsTheirCreatedAtAndOrder() =
        runTest {
            maintenance.saveItems(listOf(maintenanceItem("a"), maintenanceItem("b")))
            val createdA = db.longColumn("maintenance_item", "createdAt", "a")
            clock.advanceSeconds(60)

            maintenance.saveItems(listOf(maintenanceItem("b"), maintenanceItem("a")))

            assertEquals(createdA, db.longColumn("maintenance_item", "createdAt", "a"))
            assertEquals(listOf("a", "b"), maintenance.observeItems("v1").first().map { it.id })
        }

    @Test
    fun savingAnItemUnderADeletedVehicleFails() =
        runTest {
            VehicleRepositoryImpl(db, clock).delete("v1")

            assertFailsWith<IllegalStateException> { maintenance.saveItem(maintenanceItem("i1")) }
        }
}
