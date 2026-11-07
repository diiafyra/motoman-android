package io.github.diiafyra.motoman.core.domain.repository

import io.github.diiafyra.motoman.core.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

public interface VehicleRepository {
    /** Vehicles that are not deleted, oldest first. */
    public fun observeVehicles(): Flow<List<Vehicle>>

    /** Emits null when the vehicle does not exist or is deleted. */
    public fun observeVehicle(id: String): Flow<Vehicle?>

    /** Inserts, or updates when a vehicle with this id exists. Saving never brings a deleted vehicle back. */
    public suspend fun save(vehicle: Vehicle)

    /** Soft-deletes the vehicle together with its fill-ups and maintenance data. */
    public suspend fun delete(id: String)
}
