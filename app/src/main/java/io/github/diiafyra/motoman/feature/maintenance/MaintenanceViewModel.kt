package io.github.diiafyra.motoman.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.maintenance.ItemStatus
import io.github.diiafyra.motoman.core.domain.maintenance.currentOdometerKm
import io.github.diiafyra.motoman.core.domain.maintenance.rankByUrgency
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.feature.vehicle.STOP_TIMEOUT_MS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MaintenanceUiState {
    data object Loading : MaintenanceUiState

    data class Ready(
        val vehicleId: String,
        val odometerKm: Int,
        /** Most urgent first. */
        val items: List<ItemStatus>,
    ) : MaintenanceUiState
}

@HiltViewModel
class MaintenanceViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
        fuel: FuelEntryRepository,
        maintenance: MaintenanceRepository,
        clock: Clock,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<MaintenanceUiState> =
            observeSelectedVehicle()
                .flatMapLatest { selection ->
                    val vehicle = selection.selected ?: return@flatMapLatest flowOf(MaintenanceUiState.Loading)
                    combine(
                        fuel.observeEntries(vehicle.id),
                        maintenance.observeItems(vehicle.id),
                        maintenance.observeRecords(vehicle.id),
                    ) { entries, items, records ->
                        val odometer = currentOdometerKm(vehicle, entries, records)
                        MaintenanceUiState.Ready(vehicle.id, odometer, rankByUrgency(items, odometer, clock.today()))
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MaintenanceUiState.Loading)
    }
