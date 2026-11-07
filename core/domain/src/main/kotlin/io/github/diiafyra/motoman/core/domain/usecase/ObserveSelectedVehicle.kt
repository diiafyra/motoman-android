package io.github.diiafyra.motoman.core.domain.usecase

import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

public data class SelectedVehicle(
    val vehicles: List<Vehicle>,
    /** Null only when there are no vehicles. */
    val selected: Vehicle?,
)

/**
 * The vehicle every tab works on. Falls back to the oldest vehicle when nothing was chosen yet or the
 * chosen one was deleted, so that the tabs never show "no vehicle" while one exists.
 */
public class ObserveSelectedVehicle
    @Inject
    constructor(
        private val vehicles: VehicleRepository,
        private val preferences: PreferencesRepository,
    ) {
        public operator fun invoke(): Flow<SelectedVehicle> =
            combine(vehicles.observeVehicles(), preferences.observeSelectedVehicleId()) { all, selectedId ->
                SelectedVehicle(all, all.firstOrNull { it.id == selectedId } ?: all.firstOrNull())
            }.distinctUntilChanged()
    }
