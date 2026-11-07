package io.github.diiafyra.motoman.feature.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.FuelEntry
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatDate
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.formatLiters
import io.github.diiafyra.motoman.ui.format.formatMonth
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun FuelLogScreen(
    onAddEntry: (vehicleId: String) -> Unit,
    onEditEntry: (vehicleId: String, entryId: String) -> Unit,
    viewModel: FuelLogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.fuel_entry_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(state.undoEntryId) {
        val id = state.undoEntryId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndo(id)
        viewModel.onUndoShown(id)
    }

    FuelLogContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onAdd = { state.vehicleId?.let(onAddEntry) },
        onEdit = { entryId -> state.vehicleId?.let { onEditEntry(it, entryId) } },
        onDelete = viewModel::onDelete,
    )
}

@Composable
internal fun FuelLogContent(
    state: FuelLogUiState,
    snackbarHostState: SnackbarHostState,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    // Inside the shell's bars already, so no insets of its own.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.months.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fuel_log_add)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingState(Modifier.padding(padding))
            }

            state.months.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.fuel_log_empty_title),
                    body = stringResource(R.string.fuel_log_empty_body),
                    icon = ImageVector.vectorResource(R.drawable.ic_fuel),
                    actionLabel = stringResource(R.string.fuel_log_add),
                    onAction = onAdd,
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    state.months.forEach { month ->
                        item(key = month.month.toString()) { MonthHeader(month) }
                        items(month.entries, key = { it.id }) { entry ->
                            SwipeToDelete(onDelete = { onDelete(entry.id) }) {
                                FuelEntryRow(
                                    entry,
                                    zone,
                                    onClick = { onEdit(entry.id) },
                                    onDelete = { onDelete(entry.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: FuelMonth) {
    ListItem(
        headlineContent = {
            Text(
                formatMonth(month.month),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        trailingContent = {
            Text(
                stringResource(
                    R.string.fuel_month_total,
                    stringResource(R.string.value_vnd, formatInteger(month.totalCostVnd)),
                    formatLiters(month.totalVolumeMl),
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

@Composable
private fun FuelEntryRow(
    entry: FuelEntry,
    zone: ZoneId,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val deleteLabel = stringResource(R.string.action_delete)
    val tank = stringResource(if (entry.isFullTank) R.string.fuel_entry_full else R.string.fuel_entry_partial)
    val details =
        listOfNotNull(
            stringResource(R.string.value_km, formatInteger(entry.odometerKm.toLong())),
            tank,
            if (entry.missedPrevious) stringResource(R.string.fuel_entry_missed) else null,
        ).joinToString(" · ")
    ListItem(
        overlineContent = { Text(formatDate(entry.filledAt.atZone(zone).toLocalDate())) },
        headlineContent = {
            Text(
                stringResource(
                    R.string.fuel_entry_amount,
                    formatLiters(entry.volumeMl),
                    stringResource(R.string.value_vnd, formatInteger(entry.totalCostVnd)),
                ),
            )
        },
        supportingContent = { Text(details) },
        modifier =
            Modifier
                .clickable(onClick = onClick)
                // Swiping is not available to TalkBack users; the same action is offered here.
                .semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(deleteLabel) {
                                onDelete()
                                true
                            },
                        )
                },
    )
}

@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}

@Preview(showBackground = true)
@Composable
private fun FuelLogPreview() {
    val entries =
        listOf(
            FuelEntry("2", "v", Instant.parse("2026-11-07T03:00:00Z"), 12_480, 3_800, 95_000, true, false),
            FuelEntry("1", "v", Instant.parse("2026-11-01T03:00:00Z"), 12_300, 1_500, 37_500, false, true),
        )
    MotoManTheme {
        FuelLogContent(
            state =
                FuelLogUiState(
                    isLoading = false,
                    vehicleId = "v",
                    months = listOf(FuelMonth(YearMonth.of(2026, 11), 132_500, 5_300, entries)),
                ),
            snackbarHostState = SnackbarHostState(),
            onAdd = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
