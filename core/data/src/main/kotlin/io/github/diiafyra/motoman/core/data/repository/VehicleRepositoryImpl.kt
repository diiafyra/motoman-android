package io.github.diiafyra.motoman.core.data.repository

import androidx.room.withTransaction
import io.github.diiafyra.motoman.core.data.database.MotoManDatabase
import io.github.diiafyra.motoman.core.data.database.entity.timesForSave
import io.github.diiafyra.motoman.core.data.database.entity.toDomain
import io.github.diiafyra.motoman.core.data.database.entity.toEntity
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class VehicleRepositoryImpl
    @Inject
    constructor(
        private val db: MotoManDatabase,
        private val clock: Clock,
    ) : VehicleRepository {
        private val vehicleDao = db.vehicleDao()

        override fun observeVehicles(): Flow<List<Vehicle>> =
            vehicleDao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override fun observeVehicle(id: String): Flow<Vehicle?> = vehicleDao.observe(id).map { it?.toDomain() }

        override suspend fun save(vehicle: Vehicle) {
            db.withTransaction {
                val times = vehicleDao.rowState(vehicle.id).timesForSave(clock.now().toEpochMilli())
                vehicleDao.upsert(vehicle.toEntity(times))
            }
        }

        override suspend fun delete(id: String) {
            db.withTransaction {
                val now = clock.now().toEpochMilli()
                vehicleDao.softDelete(id, now)
                db.fuelEntryDao().softDeleteForVehicle(id, now)
                db.maintenanceDao().softDeleteRecordsForVehicle(id, now)
                db.maintenanceDao().softDeleteItemsForVehicle(id, now)
            }
        }
    }
