package io.github.diiafyra.motoman.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class PreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : PreferencesRepository {
        override fun observeSelectedVehicleId(): Flow<String?> =
            dataStore.data.map { it[SELECTED_VEHICLE_ID] }.distinctUntilChanged()

        override suspend fun setSelectedVehicleId(id: String) {
            dataStore.edit { it[SELECTED_VEHICLE_ID] = id }
        }

        private companion object {
            val SELECTED_VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
        }
    }
