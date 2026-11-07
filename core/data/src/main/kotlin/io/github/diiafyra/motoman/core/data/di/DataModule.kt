package io.github.diiafyra.motoman.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.diiafyra.motoman.core.data.database.MotoManDatabase
import io.github.diiafyra.motoman.core.data.repository.FuelEntryRepositoryImpl
import io.github.diiafyra.motoman.core.data.repository.MaintenanceRepositoryImpl
import io.github.diiafyra.motoman.core.data.repository.VehicleRepositoryImpl
import io.github.diiafyra.motoman.core.data.time.SystemClock
import io.github.diiafyra.motoman.core.data.time.UuidIdGenerator
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import javax.inject.Singleton

/** The only place outside this module that sees the implementations: everything else gets interfaces. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun vehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository

    @Binds
    abstract fun fuelEntryRepository(impl: FuelEntryRepositoryImpl): FuelEntryRepository

    @Binds
    abstract fun maintenanceRepository(impl: MaintenanceRepositoryImpl): MaintenanceRepository

    @Binds
    abstract fun clock(impl: SystemClock): Clock

    @Binds
    abstract fun idGenerator(impl: UuidIdGenerator): IdGenerator

    companion object {
        @Provides
        @Singleton
        fun database(
            @ApplicationContext context: Context,
        ): MotoManDatabase = Room.databaseBuilder(context, MotoManDatabase::class.java, MotoManDatabase.NAME).build()
    }
}
