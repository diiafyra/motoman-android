package io.github.diiafyra.motoman.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.diiafyra.motoman.core.data.database.MotoManDatabase
import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

internal fun inMemoryDatabase(): MotoManDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MotoManDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** A clock tests move by hand. */
internal class FakeClock(
    var now: Instant = Instant.parse("2026-06-01T08:00:00Z"),
) : Clock {
    override fun now(): Instant = now

    override fun today(): LocalDate = now.atZone(zone()).toLocalDate()

    override fun zone(): ZoneId = ZoneOffset.UTC

    fun advanceSeconds(seconds: Long) {
        now = now.plusSeconds(seconds)
    }
}

/** Reads one Long column of one row, including soft-deleted rows the DAOs hide. */
internal fun MotoManDatabase.longColumn(
    table: String,
    column: String,
    id: String,
): Long? =
    openHelper.readableDatabase.query("SELECT $column FROM $table WHERE id = ?", arrayOf(id)).use { cursor ->
        check(cursor.moveToFirst()) { "No row $id in $table" }
        if (cursor.isNull(0)) null else cursor.getLong(0)
    }

internal fun vehicle(
    id: String = "v1",
    name: String = "Wave",
) = Vehicle(id, name, VehicleType.MOTORBIKE, FuelType.RON95, initialOdometerKm = 1_000)

internal fun fuelEntry(
    id: String,
    vehicleId: String = "v1",
    filledAt: Instant = Instant.parse("2026-05-01T08:00:00Z"),
    odometerKm: Int = 1_200,
) = FuelEntry(
    id = id,
    vehicleId = vehicleId,
    filledAt = filledAt,
    odometerKm = odometerKm,
    volumeMl = 4_000,
    totalCostVnd = 100_000,
    isFullTank = true,
    missedPrevious = false,
)

internal fun maintenanceItem(
    id: String,
    vehicleId: String = "v1",
    lastDoneKm: Int? = null,
    lastDoneDate: LocalDate? = null,
) = MaintenanceItem(
    id = id,
    vehicleId = vehicleId,
    name = "Oil change",
    intervalKm = 4_000,
    intervalMonths = 6,
    lastDoneKm = lastDoneKm,
    lastDoneDate = lastDoneDate,
)

internal fun maintenanceRecord(
    id: String,
    itemId: String = "i1",
    vehicleId: String = "v1",
    doneDate: LocalDate = LocalDate.of(2026, 5, 1),
    odometerKm: Int = 5_000,
) = MaintenanceRecord(id, itemId, vehicleId, doneDate, odometerKm, costVnd = 150_000)

/** assertThrows for suspending code, without nesting runBlocking inside runTest. */
internal suspend inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionError("Expected ${T::class.simpleName} but got $e", e)
    }
    throw AssertionError("Expected ${T::class.simpleName} but nothing was thrown")
}
