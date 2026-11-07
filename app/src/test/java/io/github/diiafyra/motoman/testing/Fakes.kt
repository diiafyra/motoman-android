package io.github.diiafyra.motoman.testing

import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
