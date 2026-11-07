package io.github.diiafyra.motoman.core.data.repository

import androidx.room.withTransaction
import io.github.diiafyra.motoman.core.data.database.MotoManDatabase
import io.github.diiafyra.motoman.core.data.database.entity.timesForSave
import io.github.diiafyra.motoman.core.data.database.entity.toDomain
import io.github.diiafyra.motoman.core.data.database.entity.toEntity
import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class FuelEntryRepositoryImpl
    @Inject
    constructor(
        private val db: MotoManDatabase,
        private val clock: Clock,
    ) : FuelEntryRepository {
        private val dao = db.fuelEntryDao()

        override fun observeEntries(vehicleId: String): Flow<List<FuelEntry>> =
            dao.observeForVehicle(vehicleId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun save(entry: FuelEntry) {
            db.withTransaction {
                check(db.vehicleDao().isActive(entry.vehicleId)) { "Vehicle ${entry.vehicleId} is missing or deleted" }
                val times = dao.rowState(entry.id).timesForSave(clock.now().toEpochMilli())
                dao.upsert(entry.toEntity(times))
            }
        }

        override suspend fun delete(id: String) = dao.softDelete(id, clock.now().toEpochMilli())

        override suspend fun restore(id: String) = dao.restore(id, clock.now().toEpochMilli())
    }
