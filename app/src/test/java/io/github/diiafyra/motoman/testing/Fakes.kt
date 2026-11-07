package io.github.diiafyra.motoman.testing

import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakeVehicleRepository(
    vararg initial: Vehicle,
) : VehicleRepository {
    val vehicles = MutableStateFlow(initial.toList())

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<Vehicle?> = vehicles.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun save(vehicle: Vehicle) =
        vehicles.update { all ->
            if (all.any { it.id == vehicle.id }) all.map { if (it.id == vehicle.id) vehicle else it } else all + vehicle
        }

    override suspend fun delete(id: String) = vehicles.update { all -> all.filterNot { it.id == id } }
}

class FakePreferencesRepository(
    selected: String? = null,
) : PreferencesRepository {
    val selectedId = MutableStateFlow(selected)

    override fun observeSelectedVehicleId(): Flow<String?> = selectedId

    override suspend fun setSelectedVehicleId(id: String) {
        selectedId.value = id
    }
}

/** Ids "id-1", "id-2", … so tests can predict them. */
class SequentialIdGenerator : IdGenerator {
    private var next = 1

    override fun newId(): String = "id-${next++}"
}

fun testVehicle(
    id: String = "v1",
    name: String = "Wave",
    type: VehicleType = VehicleType.MOTORBIKE,
    fuelType: FuelType = FuelType.RON95,
    initialOdometerKm: Int = 12_000,
) = Vehicle(id, name, type, fuelType, initialOdometerKm)

/** Keeps deleted entries, like the real repository, so restore can bring them back. */
class FakeFuelEntryRepository(
    vararg initial: FuelEntry,
) : FuelEntryRepository {
    private val rows = MutableStateFlow(initial.associate { it.id to (it to false) })

    /** Entries that are not deleted. */
    val entries: List<FuelEntry> get() =
        rows.value.values
            .filterNot { it.second }
            .map { it.first }

    override fun observeEntries(vehicleId: String): Flow<List<FuelEntry>> =
        rows.map { all ->
            all.values
                .filter { (entry, deleted) -> entry.vehicleId == vehicleId && !deleted }
                .map { it.first }
                .sortedByDescending { it.filledAt }
        }

    override suspend fun save(entry: FuelEntry) =
        rows.update { it + (entry.id to (entry to (it[entry.id]?.second ?: false))) }

    override suspend fun delete(id: String) =
        rows.update { all ->
            all.mapValues { (k, v) ->
                if (k ==
                    id
                ) {
                    v.first to true
                } else {
                    v
                }
            }
        }

    override suspend fun restore(id: String) =
        rows.update { all -> all.mapValues { (k, v) -> if (k == id) v.first to false else v } }
}

/** Hanoi time, fixed at 2026-11-08 10:00 local unless a test moves it. */
class FakeClock(
    var now: Instant = Instant.parse("2026-11-08T03:00:00Z"),
    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
) : Clock {
    override fun now(): Instant = now

    override fun today(): LocalDate = now.atZone(zone).toLocalDate()

    override fun zone(): ZoneId = zone
}

fun testFuelEntry(
    id: String,
    filledAt: Instant,
    odometerKm: Int,
    volumeMl: Long = 4_000,
    totalCostVnd: Long = 100_000,
    vehicleId: String = "v1",
    isFullTank: Boolean = true,
) = FuelEntry(id, vehicleId, filledAt, odometerKm, volumeMl, totalCostVnd, isFullTank, missedPrevious = false)
