package io.github.diiafyra.motoman.feature.fuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsEvent
import io.github.diiafyra.motoman.core.domain.analytics.AnalyticsTracker
import io.github.diiafyra.motoman.core.domain.fuel.FuelEntryDraft
import io.github.diiafyra.motoman.core.domain.fuel.FuelEntryErrors
import io.github.diiafyra.motoman.core.domain.fuel.parseVolumeMl
import io.github.diiafyra.motoman.core.domain.fuel.validateFuelEntry
import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.repository.VehicleRepository
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.core.domain.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class FuelEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = true,
    val date: LocalDate = LocalDate.MIN,
    /** The latest date the picker allows. */
    val today: LocalDate = LocalDate.MIN,
    val odometerText: String = "",
    val volumeText: String = "",
    val costText: String = "",
    val isFullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val note: String = "",
    /** The reading just before the chosen date, as a hint next to the odometer field. */
    val previousOdometerKm: Int? = null,
    /** Shown once volume and cost are both readable. */
    val pricePerLiterVnd: Long? = null,
    val errors: FuelEntryErrors = FuelEntryErrors(),
    /** The volume text is not a number like 4.25, which the domain check cannot see. */
    val isVolumeUnreadable: Boolean = false,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
)

@HiltViewModel
class FuelEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val vehicles: VehicleRepository,
        private val fuel: FuelEntryRepository,
        private val clock: Clock,
        private val idGenerator: IdGenerator,
        private val analytics: AnalyticsTracker,
    ) : ViewModel() {
        private val vehicleId: String = requireNotNull(savedStateHandle["vehicleId"])
        private val entryId: String? = savedStateHandle["entryId"]

        private var vehicle: Vehicle? = null
        private var original: FuelEntry? = null
        private var otherEntries: List<FuelEntry> = emptyList()
        private var showErrors = false

        private val state = MutableStateFlow(FuelEditUiState(isNew = entryId == null))
        val uiState: StateFlow<FuelEditUiState> = state.asStateFlow()

        init {
            viewModelScope.launch {
                val loadedVehicle = vehicles.observeVehicle(vehicleId).first()
                val entries = fuel.observeEntries(vehicleId).first()
                val entry = entryId?.let { id -> entries.firstOrNull { it.id == id } }
                if (loadedVehicle == null || (entryId != null && entry == null)) {
                    // The vehicle or the fill-up was deleted while the form was opening.
                    state.update { it.copy(isLoading = false, isDone = true) }
                    return@launch
                }
                vehicle = loadedVehicle
                original = entry
                otherEntries = entries.filterNot { it.id == entryId }
                val today = clock.today()
                state.update {
                    derive(
                        it.copy(
                            isLoading = false,
                            today = today,
                            date = entry?.filledAt?.atZone(clock.zone())?.toLocalDate() ?: today,
                            odometerText = entry?.odometerKm?.toString().orEmpty(),
                            volumeText = entry?.let { e -> volumeText(e.volumeMl) }.orEmpty(),
                            costText = entry?.totalCostVnd?.toString().orEmpty(),
                            isFullTank = entry?.isFullTank ?: true,
                            missedPrevious = entry?.missedPrevious ?: false,
                            note = entry?.note.orEmpty(),
                        ),
                    )
                }
            }
        }

        fun onDateChange(date: LocalDate) = edit { it.copy(date = minOf(date, it.today)) }

        fun onOdometerChange(text: String) =
            edit { it.copy(odometerText = text.filter(Char::isDigit).take(MAX_ODOMETER_KM.toString().length)) }

        fun onVolumeChange(text: String) =
            edit {
                it.copy(
                    volumeText =
                        text.filter { c ->
                            c.isDigit() || c == '.' ||
                                c == ','
                        },
                )
            }

        fun onCostChange(text: String) = edit { it.copy(costText = text.filter(Char::isDigit).take(MAX_COST_DIGITS)) }

        fun onFullTankChange(isFullTank: Boolean) = edit { it.copy(isFullTank = isFullTank) }

        fun onMissedPreviousChange(missed: Boolean) = edit { it.copy(missedPrevious = missed) }

        fun onNoteChange(note: String) = edit { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

        fun onSaveClick() {
            val current = state.value
            val vehicle = vehicle ?: return
            if (current.isSaving || current.isLoading) return
            showErrors = true
            val checked = derive(current)
            state.value = checked
            if (!checked.errors.isValid || checked.isVolumeUnreadable) return

            val draft = current.toDraft()
            state.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                fuel.save(
                    FuelEntry(
                        id = entryId ?: idGenerator.newId(),
                        vehicleId = vehicle.id,
                        filledAt = draft.filledAt,
                        odometerKm = requireNotNull(draft.odometerKm),
                        volumeMl = requireNotNull(draft.volumeMl),
                        totalCostVnd = requireNotNull(draft.totalCostVnd),
                        isFullTank = current.isFullTank,
                        missedPrevious = current.missedPrevious,
                        note = current.note.trim(),
                    ),
                )
                // Editing a fill-up is not logging one.
                if (entryId == null) analytics.track(AnalyticsEvent.FUEL_LOGGED)
                state.update { it.copy(isSaving = false, isDone = true) }
            }
        }

        private fun edit(change: (FuelEditUiState) -> FuelEditUiState) = state.update { derive(change(it)) }

        /** Recomputes everything that follows from the fields: hint, price, and errors once shown. */
        private fun derive(s: FuelEditUiState): FuelEditUiState {
            val draft = s.toDraft()
            val previousKm =
                otherEntries
                    .filter { !it.filledAt.isAfter(draft.filledAt) }
                    .maxOfOrNull { it.odometerKm } ?: vehicle?.initialOdometerKm
            val volumeMl = draft.volumeMl
            val costVnd = draft.totalCostVnd
            val price =
                if (volumeMl != null && volumeMl > 0 && costVnd != null) {
                    // Rounded to the dong: cost × 1000 / mL.
                    (costVnd * 1_000 + volumeMl / 2) / volumeMl
                } else {
                    null
                }
            val vehicle = vehicle
            return s.copy(
                previousOdometerKm = previousKm,
                pricePerLiterVnd = price,
                errors =
                    if (showErrors && vehicle != null) {
                        validateFuelEntry(draft, vehicle, otherEntries, clock.now())
                    } else {
                        FuelEntryErrors()
                    },
                isVolumeUnreadable = showErrors && s.volumeText.isNotBlank() && draft.volumeMl == null,
            )
        }

        private fun FuelEditUiState.toDraft() =
            FuelEntryDraft(
                filledAt = instantFor(date),
                odometerKm = odometerText.toIntOrNull(),
                volumeMl = parseVolumeMl(volumeText),
                totalCostVnd = costText.toLongOrNull(),
            )

        /**
         * The picker gives a date; the entry needs an instant. An edit keeps its original time when the
         * date did not change, today means now, and any other day means noon, which keeps fill-ups of
         * the same day in a stable order without asking for a time.
         */
        private fun instantFor(date: LocalDate): Instant {
            val original = original
            val zone = clock.zone()
            return when {
                original != null && original.filledAt.atZone(zone).toLocalDate() == date -> original.filledAt
                date == clock.today() -> clock.now()
                else -> date.atTime(LocalTime.NOON).atZone(zone).toInstant()
            }
        }

        private fun volumeText(volumeMl: Long): String {
            val whole = volumeMl / 1_000
            val fraction = (volumeMl % 1_000).toString().padStart(3, '0').trimEnd('0')
            return if (fraction.isEmpty()) "$whole" else "$whole.$fraction"
        }

        private companion object {
            /** Up to 999 billion dong, far beyond any fill-up. */
            const val MAX_COST_DIGITS = 12
            const val MAX_NOTE_LENGTH = 200
        }
    }
