package io.github.diiafyra.motoman.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.fuel.MonthStats
import io.github.diiafyra.motoman.core.domain.fuel.monthlyStats
import io.github.diiafyra.motoman.core.domain.repository.FuelEntryRepository
import io.github.diiafyra.motoman.core.domain.usecase.ObserveSelectedVehicle
import io.github.diiafyra.motoman.core.domain.util.Clock
import io.github.diiafyra.motoman.feature.vehicle.STOP_TIMEOUT_MS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    /** The last [MONTHS_SHOWN] months, oldest first. */
    val months: List<MonthStats> = emptyList(),
) {
    val hasEconomy: Boolean get() = months.any { it.litersPer100Km != null }
}

/** Six bars fit a phone in portrait with readable month labels. */
const val MONTHS_SHOWN = 6

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
        fuel: FuelEntryRepository,
        clock: Clock,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<StatsUiState> =
            observeSelectedVehicle()
                .flatMapLatest { selection ->
                    val vehicle = selection.selected ?: return@flatMapLatest flowOf(StatsUiState(isLoading = false))
                    fuel.observeEntries(vehicle.id).map { entries ->
                        StatsUiState(
                            isLoading = false,
                            months = monthlyStats(entries, YearMonth.from(clock.today()), MONTHS_SHOWN, clock.zone()),
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StatsUiState())
    }
