package io.github.diiafyra.motoman.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleListUiState(
    val isLoading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedId: String? = null,
    /** Set once a vehicle was chosen and stored; the screen then closes. */
    val isDone: Boolean = false,
)

@HiltViewModel
class VehicleListViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
        private val preferences: PreferencesRepository,
    ) : ViewModel() {
        private val isDone = MutableStateFlow(false)

        val uiState: StateFlow<VehicleListUiState> =
            combine(observeSelectedVehicle(), isDone) { selection, done ->
                VehicleListUiState(
                    isLoading = false,
                    vehicles = selection.vehicles,
                    selectedId = selection.selected?.id,
                    isDone = done,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), VehicleListUiState())

        fun onVehicleClick(id: String) {
            viewModelScope.launch {
                // Close only after the choice is stored: closing cancels this scope.
                preferences.setSelectedVehicleId(id)
                isDone.value = true
            }
        }
    }

/** Keeps upstream flows alive across a configuration change, which takes well under this. */
internal const val STOP_TIMEOUT_MS = 5_000L
