package io.github.diiafyra.motoman.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceRecordDraft
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceRecordErrors
import io.github.diiafyra.motoman.core.domain.maintenance.currentOdometerKm
import io.github.diiafyra.motoman.core.domain.maintenance.validateMaintenanceRecord
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceDoneUiState(
    val isLoading: Boolean = true,
    val itemName: String = "",
    val date: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    /** Prefilled with the current odometer, which is usually right for a service done today. */
    val odometerText: String = "",
    val costText: String = "",
    val note: String = "",
    val errors: MaintenanceRecordErrors = MaintenanceRecordErrors(),
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
)

@HiltViewModel
class MaintenanceDoneViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val vehicles: VehicleRepository,
        private val fuel: FuelEntryRepository,
        private val maintenance: MaintenanceRepository,
        private val clock: Clock,
        private val idGenerator: IdGenerator,
    ) : ViewModel() {
        private val vehicleId: String = requireNotNull(savedStateHandle["vehicleId"])
        private val itemId: String = requireNotNull(savedStateHandle["itemId"])
        private var showErrors = false

        private val state = MutableStateFlow(MaintenanceDoneUiState())
        val uiState: StateFlow<MaintenanceDoneUiState> = state.asStateFlow()

        init {
            viewModelScope.launch {
                val vehicle = vehicles.observeVehicle(vehicleId).first()
                val item = maintenance.observeItems(vehicleId).first().firstOrNull { it.id == itemId }
                if (vehicle == null || item == null) {
                    state.update { it.copy(isLoading = false, isDone = true) }
                    return@launch
                }
                val odometer =
                    currentOdometerKm(
                        vehicle,
                        fuel.observeEntries(vehicleId).first(),
                        maintenance.observeRecords(vehicleId).first(),
                    )
                val today = clock.today()
                state.update {
                    it.copy(
                        isLoading = false,
                        itemName = item.name,
                        date = today,
                        today = today,
                        odometerText = odometer.toString(),
                    )
                }
            }
        }

        fun onDateChange(date: LocalDate) = edit { it.copy(date = minOf(date, it.today)) }

        fun onOdometerChange(text: String) =
            edit { it.copy(odometerText = text.filter(Char::isDigit).take(MAX_ODOMETER_KM.toString().length)) }

        fun onCostChange(text: String) = edit { it.copy(costText = text.filter(Char::isDigit).take(MAX_COST_DIGITS)) }

        fun onNoteChange(note: String) = edit { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

        fun onSaveClick() {
            val current = state.value
            if (current.isSaving || current.isLoading) return
            showErrors = true
            val draft = current.toDraft()
            val errors = validateMaintenanceRecord(draft, clock.today())
            state.update { it.copy(errors = errors) }
            if (!errors.isValid) return

            state.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                maintenance.recordDone(
                    MaintenanceRecord(
                        id = idGenerator.newId(),
                        itemId = itemId,
                        vehicleId = vehicleId,
                        doneDate = draft.doneDate,
                        odometerKm = requireNotNull(draft.odometerKm),
                        costVnd = draft.costVnd,
                        note = current.note.trim(),
                    ),
                )
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        private fun edit(change: (MaintenanceDoneUiState) -> MaintenanceDoneUiState) =
            state.update { old ->
                val new = change(old)
                if (showErrors) new.copy(errors = validateMaintenanceRecord(new.toDraft(), clock.today())) else new
            }

        private fun MaintenanceDoneUiState.toDraft() =
            MaintenanceRecordDraft(date, odometerText.toIntOrNull(), costText.toLongOrNull())

        private companion object {
            const val MAX_COST_DIGITS = 12
            const val MAX_NOTE_LENGTH = 200
        }
    }
