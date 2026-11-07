package io.github.diiafyra.motoman.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.diiafyra.motoman.core.data.database.dao.FuelEntryDao
import io.github.diiafyra.motoman.core.data.database.dao.MaintenanceDao
import io.github.diiafyra.motoman.core.data.database.dao.VehicleDao
import io.github.diiafyra.motoman.core.data.database.entity.FuelEntryEntity
import io.github.diiafyra.motoman.core.data.database.entity.MaintenanceItemEntity
import io.github.diiafyra.motoman.core.data.database.entity.MaintenanceRecordEntity
import io.github.diiafyra.motoman.core.data.database.entity.VehicleEntity

/**
 * The app's source of truth. Every schema change bumps [version], exports a new schema file to
 * `core/data/schemas/` and comes with a migration test.
 */
@Database(
    entities = [
        VehicleEntity::class,
        FuelEntryEntity::class,
        MaintenanceItemEntity::class,
        MaintenanceRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class MotoManDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    abstract fun fuelEntryDao(): FuelEntryDao

    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val NAME = "motoman.db"
    }
}
