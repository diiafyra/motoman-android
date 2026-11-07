package io.github.diiafyra.motoman.core.data.repository

import androidx.room.withTransaction
import io.github.diiafyra.motoman.core.data.database.MotoManDatabase
import io.github.diiafyra.motoman.core.data.database.entity.MaintenanceItemEntity
import io.github.diiafyra.motoman.core.data.database.entity.RowState
import io.github.diiafyra.motoman.core.data.database.entity.timesForSave
import io.github.diiafyra.motoman.core.data.database.entity.toDomain
import io.github.diiafyra.motoman.core.data.database.entity.toEntity
import io.github.diiafyra.motoman.core.domain.maintenance.withRecord
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class MaintenanceRepositoryImpl
    @Inject
    constructor(
        private val db: MotoManDatabase,
        private val clock: Clock,
    ) : MaintenanceRepository {
        private val dao = db.maintenanceDao()

        override fun observeItems(vehicleId: String): Flow<List<MaintenanceItem>> =
            dao.observeItems(vehicleId).map { rows -> rows.map { it.toDomain() } }

        override fun observeRecords(vehicleId: String): Flow<List<MaintenanceRecord>> =
            dao.observeRecords(vehicleId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun saveItem(item: MaintenanceItem) = saveItems(listOf(item))

        override suspend fun saveItems(items: List<MaintenanceItem>) {
            db.withTransaction {
                val now = clock.now().toEpochMilli()
                items.forEachIndexed { index, item ->
                    check(
                        db.vehicleDao().isActive(item.vehicleId),
                    ) { "Vehicle ${item.vehicleId} is missing or deleted" }
                    val stored = dao.itemById(item.id)
                    // An existing item keeps its stored baseline: only recordDone moves it. Otherwise an
                    // edit form opened before a service was recorded would put the old baseline back.
                    val toSave =
                        if (stored == null) {
                            item
                        } else {
                            item.copy(lastDoneKm = stored.lastDoneKm, lastDoneDate = stored.toDomain().lastDoneDate)
                        }
                    // New items saved together get increasing createdAt values so they keep their order.
                    val times = stored?.rowState().timesForSave(now, newCreatedAt = now + index)
                    dao.upsertItem(toSave.toEntity(times))
                }
            }
        }

        override suspend fun deleteItem(id: String) {
            db.withTransaction {
                val now = clock.now().toEpochMilli()
                dao.softDeleteRecordsForItem(id, now)
                dao.softDeleteItem(id, now)
            }
        }

        override suspend fun recordDone(record: MaintenanceRecord) {
            db.withTransaction {
                require(!dao.recordExists(record.id)) { "recordDone adds new records only; ${record.id} exists" }
                val stored = dao.itemById(record.itemId)
                require(
                    stored != null && stored.deletedAt == null,
                ) { "Maintenance item ${record.itemId} is missing or deleted" }
                require(stored.vehicleId == record.vehicleId) { "The record and its item belong to different vehicles" }

                val now = clock.now().toEpochMilli()
                dao.upsertRecord(record.toEntity(null.timesForSave(now)))

                val item = stored.toDomain()
                val updated = item.withRecord(record)
                if (updated != item) dao.upsertItem(updated.toEntity(stored.rowState().timesForSave(now)))
            }
        }

        private fun MaintenanceItemEntity.rowState() = RowState(createdAt, updatedAt, deletedAt)
    }
