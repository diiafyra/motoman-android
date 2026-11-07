package io.github.diiafyra.motoman.feature.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.DuePoint
import io.github.diiafyra.motoman.core.domain.maintenance.ItemStatus
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.core.domain.model.MaintenanceItem
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.StatusBadge
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatDecimal
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.formatLiters
import io.github.diiafyra.motoman.ui.format.maintenanceDueText
import kotlin.math.roundToLong

@Composable
fun OverviewScreen(
    onAddFillUp: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenMaintenance: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OverviewContent(state, onAddFillUp, onOpenStats, onOpenMaintenance)
}

@Composable
internal fun OverviewContent(
    state: OverviewUiState,
    onAddFillUp: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenMaintenance: () -> Unit,
) {
    val ready =
        when (state) {
            OverviewUiState.Loading -> return LoadingState()
            is OverviewUiState.Ready -> state
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!ready.hasFillUps) {
            Card(Modifier.fillMaxWidth()) {
                EmptyState(
                    title = stringResource(R.string.overview_no_fill_ups_title),
                    body = stringResource(R.string.overview_no_fill_ups_body),
                    icon = ImageVector.vectorResource(R.drawable.ic_fuel),
                    actionLabel = stringResource(R.string.fuel_log_add),
                    onAction = onAddFillUp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            EconomyCard(ready.economy)
            MonthCard(ready)
            OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.overview_see_stats))
            }
        }
        MaintenanceCard(ready, onOpenMaintenance)
    }
}

@Composable
private fun EconomyCard(economy: EconomySummary?) {
    OverviewCard(stringResource(R.string.overview_economy_title)) {
        if (economy == null) {
            Text(stringResource(R.string.overview_economy_need_full_tank), style = MaterialTheme.typography.bodyMedium)
            return@OverviewCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Figure(
                label = stringResource(R.string.overview_economy_latest),
                value = stringResource(R.string.value_l_per_100km, formatDecimal(economy.latestLitersPer100Km, 2)),
                modifier = Modifier.weight(1f),
            )
            Figure(
                label = stringResource(R.string.overview_economy_average),
                value = stringResource(R.string.value_l_per_100km, formatDecimal(economy.averageLitersPer100Km, 2)),
                supporting =
                    stringResource(R.string.value_vnd_per_km, formatInteger(economy.averageCostPerKmVnd.roundToLong())),
                modifier = Modifier.weight(1f),
            )
        }
        if (economy.isLatestFromEarlierSegment) {
            Text(
                stringResource(R.string.overview_economy_latest_skipped),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthCard(state: OverviewUiState.Ready) {
    OverviewCard(stringResource(R.string.overview_month_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Figure(
                label = stringResource(R.string.overview_month_spent),
                value = stringResource(R.string.value_vnd, formatInteger(state.monthCostVnd)),
                modifier = Modifier.weight(1f),
            )
            Figure(
                label = stringResource(R.string.overview_month_volume),
                value = stringResource(R.string.value_liters, formatLiters(state.monthVolumeMl)),
                modifier = Modifier.weight(1f),
            )
        }
        Figure(
            label = stringResource(R.string.overview_odometer),
            value = stringResource(R.string.value_km, formatInteger(state.odometerKm.toLong())),
        )
    }
}

@Composable
private fun MaintenanceCard(
    state: OverviewUiState.Ready,
    onOpenMaintenance: () -> Unit,
) {
    OverviewCard(stringResource(R.string.overview_maintenance_title)) {
        when {
            !state.hasMaintenanceItems -> {
                Text(stringResource(R.string.overview_maintenance_none))
            }

            state.attention.isEmpty() -> {
                Text(stringResource(R.string.overview_maintenance_all_ok))
            }

            else -> {
                state.attention.forEach { itemStatus ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(itemStatus.item.name, style = MaterialTheme.typography.bodyLarge)
                            maintenanceDueText(itemStatus.status)?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        StatusBadge(itemStatus.status)
                    }
                }
            }
        }
        TextButton(onClick = onOpenMaintenance) { Text(stringResource(R.string.overview_see_maintenance)) }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

/** A labelled number: the label in muted ink above, the value large. */
@Composable
private fun Figure(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        if (supporting != null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewPreview() {
    MotoManTheme {
        OverviewContent(
            state =
                OverviewUiState.Ready(
                    hasFillUps = true,
                    economy = EconomySummary(2.14, 2.31, 571.0, isLatestFromEarlierSegment = false),
                    monthCostVnd = 232_500,
                    monthVolumeMl = 9_300,
                    odometerKm = 12_600,
                    attention =
                        listOf(
                            ItemStatus(
                                MaintenanceItem("i", "v", "Oil change", 2_000, 6, 10_800, null),
                                MaintenanceStatus.DueSoon(DuePoint(12_800, null, 200, null)),
                            ),
                        ),
                    hasMaintenanceItems = true,
                ),
            onAddFillUp = {},
            onOpenStats = {},
            onOpenMaintenance = {},
        )
    }
}
