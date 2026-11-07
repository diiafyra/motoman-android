package io.github.diiafyra.motoman.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceItemDraft
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceItemErrors
import io.github.diiafyra.motoman.core.domain.maintenance.validateMaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.core.domain.model.MaintenanceRecord
import io.github.diiafyra.motoman.core.domain.repository.MaintenanceRepository
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MaintenanceItemEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = true,
    val name: String = "",
    val intervalKmText: String = "",
    val intervalMonthsText: String = "",
    val lastDoneKm: Int? = null,
    val lastDoneDate: LocalDate? = null,
    /** This item's services, newest first. */
    val history: List<MaintenanceRecord> = emptyList(),
    val errors: MaintenanceItemErrors = MaintenanceItemErrors(),
    val isSaving: Boolean = false,
    val isDeleteConfirmVisible: Boolean = false,
    val isDone: Boolean = false,
)

@HiltViewModel
class MaintenanceItemEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val maintenance: MaintenanceRepository,
        private val idGenerator: IdGenerator,
    ) : ViewModel() {
        private val vehicleId: String = requireNotNull(savedStateHandle["vehicleId"])
        private val itemId: String? = savedStateHandle["itemId"]
        private var showErrors = false

        private val state =
            MutableStateFlow(
                MaintenanceItemEditUiState(
                    isNew = itemId == null,
                    isLoading =
                        itemId != null,
                ),
            )
        val uiState: StateFlow<MaintenanceItemEditUiState> = state.asStateFlow()

        init {
            if (itemId != null) {
                viewModelScope.launch {
                    val item = maintenance.observeItems(vehicleId).first().firstOrNull { it.id == itemId }
                    if (item == null) {
                        state.update { it.copy(isLoading = false, isDone = true) }
                        return@launch
                    }
                    state.update {
                        it.copy(
                            isLoading = false,
                            name = item.name,
                            intervalKmText = item.intervalKm?.toString().orEmpty(),
                            intervalMonthsText = item.intervalMonths?.toString().orEmpty(),
                            lastDoneKm = item.lastDoneKm,
                            lastDoneDate = item.lastDoneDate,
                        )
                    }
                }
                viewModelScope.launch {
                    maintenance.observeRecords(vehicleId).collect { records ->
                        state.update { s -> s.copy(history = records.filter { it.itemId == itemId }) }
                    }
                }
            }
        }

        fun onNameChange(name: String) = edit { it.copy(name = name) }

        fun onIntervalKmChange(text: String) =
            edit { it.copy(intervalKmText = text.filter(Char::isDigit).take(MAX_KM_DIGITS)) }

        fun onIntervalMonthsChange(text: String) =
            edit { it.copy(intervalMonthsText = text.filter(Char::isDigit).take(MAX_MONTH_DIGITS)) }

        fun onSaveClick() {
            val current = state.value
            if (current.isSaving || current.isLoading) return
            showErrors = true
            val draft = current.toDraft()
            val errors = validateMaintenanceItem(draft)
            state.update { it.copy(errors = errors) }
            if (!errors.isValid) return

            state.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                // The repository keeps the stored last-done values of an existing item.
                maintenance.saveItem(
                    MaintenanceItem(
                        id = itemId ?: idGenerator.newId(),
                        vehicleId = vehicleId,
                        name = draft.name.trim(),
                        intervalKm = draft.intervalKm,
                        intervalMonths = draft.intervalMonths,
                        lastDoneKm = current.lastDoneKm,
                        lastDoneDate = current.lastDoneDate,
                    ),
                )
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        fun onDeleteClick() = state.update { it.copy(isDeleteConfirmVisible = true) }

        fun onDeleteDismiss() = state.update { it.copy(isDeleteConfirmVisible = false) }

        fun onDeleteConfirm() {
            val id = itemId ?: return
            state.update { it.copy(isDeleteConfirmVisible = false, isSaving = true) }
            viewModelScope.launch {
                maintenance.deleteItem(id)
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        private fun edit(change: (MaintenanceItemEditUiState) -> MaintenanceItemEditUiState) =
            state.update { old ->
                val new = change(old)
                if (showErrors) new.copy(errors = validateMaintenanceItem(new.toDraft())) else new
            }

        private fun MaintenanceItemEditUiState.toDraft() =
            MaintenanceItemDraft(name, intervalKmText.toIntOrNull(), intervalMonthsText.toIntOrNull())

        private companion object {
            const val MAX_KM_DIGITS = 7
            const val MAX_MONTH_DIGITS = 3
        }
    }
