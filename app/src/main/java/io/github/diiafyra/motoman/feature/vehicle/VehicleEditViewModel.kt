package io.github.diiafyra.motoman.feature.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.maintenance.TemplateNames
import io.github.diiafyra.motoman.core.domain.maintenance.templateItems
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.repository.PreferencesRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import io.github.diiafyra.motoman.core.domain.vehicle.VehicleDraft
import io.github.diiafyra.motoman.core.domain.vehicle.VehicleErrors
import io.github.diiafyra.motoman.core.domain.vehicle.validateVehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean,
    val name: String = "",
    val type: VehicleType = VehicleType.MOTORBIKE,
    val fuelType: FuelType = FuelType.RON95,
    /** Digits only, as typed. */
    val odometerText: String = "",
    /** Empty until the first save attempt, then updated as the user types. */
    val errors: VehicleErrors = VehicleErrors(),
    val isSaving: Boolean = false,
    val isDeleteConfirmVisible: Boolean = false,
    /** Set after a save or delete completed; the screen then closes. */
    val isDone: Boolean = false,
)

@HiltViewModel
class VehicleEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val vehicles: VehicleRepository,
        private val maintenance: MaintenanceRepository,
        private val preferences: PreferencesRepository,
        private val idGenerator: IdGenerator,
        private val templateNames: TemplateNames,
    ) : ViewModel() {
        // Navigation puts VehicleEditRoute.vehicleId into the saved state under its property name.
        private val vehicleId: String? = savedStateHandle["vehicleId"]
        private var showErrors = false

        private val state =
            MutableStateFlow(VehicleEditUiState(isNew = vehicleId == null, isLoading = vehicleId != null))
        val uiState: StateFlow<VehicleEditUiState> = state.asStateFlow()

        init {
            if (vehicleId != null) {
                viewModelScope.launch {
                    val vehicle = vehicles.observeVehicle(vehicleId).first()
                    state.update {
                        if (vehicle == null) {
                            // Deleted from another screen while this one was opening.
                            it.copy(isLoading = false, isDone = true)
                        } else {
                            it.copy(
                                isLoading = false,
                                name = vehicle.name,
                                type = vehicle.type,
                                fuelType = vehicle.fuelType,
                                odometerText = vehicle.initialOdometerKm.toString(),
                            )
                        }
                    }
                }
            }
        }

        fun onNameChange(name: String) = edit { it.copy(name = name) }

        fun onTypeChange(type: VehicleType) = edit { it.copy(type = type) }

        fun onFuelTypeChange(fuelType: FuelType) = edit { it.copy(fuelType = fuelType) }

        fun onOdometerChange(text: String) {
            // Ignore anything that is not a digit, and stop at the longest valid reading.
            val digits = text.filter(Char::isDigit).take(MAX_ODOMETER_KM.toString().length)
            edit { it.copy(odometerText = digits) }
        }

        fun onSaveClick() {
            val current = state.value
            if (current.isSaving || current.isLoading) return
            showErrors = true
            val draft = current.toDraft()
            val errors = validateVehicle(draft)
            state.update { it.copy(errors = errors) }
            if (!errors.isValid) return

            state.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val vehicle =
                    Vehicle(
                        id = vehicleId ?: idGenerator.newId(),
                        name = draft.name.trim(),
                        type = draft.type,
                        fuelType = draft.fuelType,
                        initialOdometerKm = requireNotNull(draft.initialOdometerKm),
                    )
                vehicles.save(vehicle)
                if (vehicleId == null) {
                    // A new vehicle starts with the usual maintenance items, and becomes the selected one.
                    maintenance.saveItems(templateItems(vehicle, templateNames, idGenerator))
                    preferences.setSelectedVehicleId(vehicle.id)
                }
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        fun onDeleteClick() = state.update { it.copy(isDeleteConfirmVisible = true) }

        fun onDeleteDismiss() = state.update { it.copy(isDeleteConfirmVisible = false) }

        fun onDeleteConfirm() {
            val id = vehicleId ?: return
            state.update { it.copy(isDeleteConfirmVisible = false, isSaving = true) }
            viewModelScope.launch {
                vehicles.delete(id)
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        private fun edit(change: (VehicleEditUiState) -> VehicleEditUiState) {
            state.update { old ->
                val new = change(old)
                if (showErrors) new.copy(errors = validateVehicle(new.toDraft())) else new
            }
        }

        private fun VehicleEditUiState.toDraft() = VehicleDraft(name, type, fuelType, odometerText.toIntOrNull())
    }
