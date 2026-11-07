package io.github.diiafyra.motoman.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.feature.vehicle.STOP_TIMEOUT_MS
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppUiState {
    data object Loading : AppUiState

    /** [selected] is null when there are no vehicles yet. */
    data class Ready(
        val selected: Vehicle?,
    ) : AppUiState
}

/** State shared by the whole shell: which vehicle the tabs work on. */
@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
    ) : ViewModel() {
        val uiState: StateFlow<AppUiState> =
            observeSelectedVehicle()
                .map { AppUiState.Ready(it.selected) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState.Loading)
    }
