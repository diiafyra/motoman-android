package io.github.diiafyra.motoman.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.feature.vehicle.STOP_TIMEOUT_MS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelLogUiState(
    val isLoading: Boolean = true,
    val vehicleId: String? = null,
    val months: List<FuelMonth> = emptyList(),
    /** A fill-up just deleted; the screen offers to undo it, then calls [FuelLogViewModel.onUndoShown]. */
    val undoEntryId: String? = null,
)

@HiltViewModel
class FuelLogViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
        private val fuel: FuelEntryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val undoEntryId = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val content =
            observeSelectedVehicle().flatMapLatest { selection ->
                val vehicle = selection.selected ?: return@flatMapLatest flowOf(null to emptyList<FuelMonth>())
                fuel.observeEntries(vehicle.id).map { vehicle.id to groupByMonth(it, clock.zone()) }
            }

        val uiState: StateFlow<FuelLogUiState> =
            combine(content, undoEntryId) { (vehicleId, months), undo ->
                FuelLogUiState(isLoading = false, vehicleId = vehicleId, months = months, undoEntryId = undo)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FuelLogUiState())

        fun onDelete(entryId: String) {
            viewModelScope.launch {
                fuel.delete(entryId)
                undoEntryId.value = entryId
            }
        }

        fun onUndo(entryId: String) {
            viewModelScope.launch { fuel.restore(entryId) }
        }

        /** Clears the offer, unless a newer deletion has replaced it meanwhile. */
        fun onUndoShown(entryId: String) = undoEntryId.update { if (it == entryId) null else it }
    }
