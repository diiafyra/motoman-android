package io.github.diiafyra.motoman.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.diiafyra.motoman.core.domain.fuel.calculateFuelEconomy
import io.github.diiafyra.motoman.core.domain.maintenance.ItemStatus
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
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
import java.time.YearMonth
import javax.inject.Inject

sealed interface OverviewUiState {
    data object Loading : OverviewUiState

    data class Ready(
        val hasFillUps: Boolean,
        val economy: EconomySummary?,
        val monthCostVnd: Long,
        val monthVolumeMl: Long,
        val odometerKm: Int,
        /** The most urgent items needing attention, at most [MAX_ATTENTION_ITEMS]. */
        val attention: List<ItemStatus>,
        val hasMaintenanceItems: Boolean,
    ) : OverviewUiState
}

/** Null in [OverviewUiState.Ready] means no valid full-tank segment yet. */
data class EconomySummary(
    val latestLitersPer100Km: Double,
    val averageLitersPer100Km: Double,
    val averageCostPerKmVnd: Double,
    /** The newest full fill-up did not close a valid segment, so "latest" is from an earlier one. */
    val isLatestFromEarlierSegment: Boolean,
)

const val MAX_ATTENTION_ITEMS = 3

@HiltViewModel
class OverviewViewModel
    @Inject
    constructor(
        observeSelectedVehicle: ObserveSelectedVehicle,
        fuel: FuelEntryRepository,
        maintenance: MaintenanceRepository,
        clock: Clock,
    ) : ViewModel() {
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<OverviewUiState> =
            observeSelectedVehicle()
                .flatMapLatest { selection ->
                    val vehicle = selection.selected ?: return@flatMapLatest flowOf(OverviewUiState.Loading)
                    combine(
                        fuel.observeEntries(vehicle.id),
                        maintenance.observeItems(vehicle.id),
                        maintenance.observeRecords(vehicle.id),
                    ) { entries, items, records ->
                        val economy = calculateFuelEconomy(entries)
                        val latest = economy.latest
                        val newestFull = entries.filter { it.isFullTank }.maxByOrNull { it.filledAt }
                        val today = clock.today()
                        val thisMonth = YearMonth.from(today)
                        val inMonth = entries.filter { YearMonth.from(it.filledAt.atZone(clock.zone())) == thisMonth }
                        val odometer = currentOdometerKm(vehicle, entries, records)
                        OverviewUiState.Ready(
                            hasFillUps = entries.isNotEmpty(),
                            economy =
                                latest?.let {
                                    EconomySummary(
                                        latestLitersPer100Km = it.litersPer100Km,
                                        averageLitersPer100Km = requireNotNull(economy.averageLitersPer100Km),
                                        averageCostPerKmVnd = requireNotNull(economy.averageCostPerKmVnd),
                                        isLatestFromEarlierSegment =
                                            newestFull != null && newestFull.id != it.endEntryId,
                                    )
                                },
                            monthCostVnd = inMonth.sumOf { it.totalCostVnd },
                            monthVolumeMl = inMonth.sumOf { it.volumeMl },
                            odometerKm = odometer,
                            attention =
                                rankByUrgency(items, odometer, today)
                                    .filter {
                                        it.status is MaintenanceStatus.Overdue ||
                                            it.status is MaintenanceStatus.DueSoon
                                    }.take(MAX_ATTENTION_ITEMS),
                            hasMaintenanceItems = items.isNotEmpty(),
                        )
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OverviewUiState.Loading)
    }
