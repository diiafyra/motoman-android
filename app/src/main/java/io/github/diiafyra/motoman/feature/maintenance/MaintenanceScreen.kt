package io.github.diiafyra.motoman.feature.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
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
import io.github.diiafyra.motoman.ui.designsystem.component.AddFab
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.StatusBadge
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.maintenanceDueText

@Composable
fun MaintenanceScreen(
    onAddItem: (vehicleId: String) -> Unit,
    onEditItem: (vehicleId: String, itemId: String) -> Unit,
    onMarkDone: (vehicleId: String, itemId: String) -> Unit,
    viewModel: MaintenanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MaintenanceContent(state, onAddItem, onEditItem, onMarkDone, viewModel::onNotificationPromptDismiss)
}

@Composable
internal fun MaintenanceContent(
    state: MaintenanceUiState,
    onAddItem: (String) -> Unit,
    onEditItem: (String, String) -> Unit,
    onMarkDone: (String, String) -> Unit,
    onNotificationPromptDismiss: () -> Unit = {},
) {
    val ready =
        when (state) {
            MaintenanceUiState.Loading -> return LoadingState()
            is MaintenanceUiState.Ready -> state
        }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (ready.items.isNotEmpty()) {
                AddFab(label = stringResource(R.string.maintenance_add_item), onClick = { onAddItem(ready.vehicleId) })
            }
        },
    ) { padding ->
        if (ready.items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.maintenance_empty_title),
                body = stringResource(R.string.maintenance_empty_body),
                icon = ImageVector.vectorResource(R.drawable.ic_maintenance),
                actionLabel = stringResource(R.string.maintenance_add_item),
                onAction = { onAddItem(ready.vehicleId) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.maintenance_odometer, formatInteger(ready.odometerKm.toLong())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.maintenance_manual_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NotificationPrompt(
                    mayAsk = ready.mayAskForNotifications,
                    onDismiss = onNotificationPromptDismiss,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(ready.items, key = { it.item.id }) { itemStatus ->
                ItemRow(
                    itemStatus = itemStatus,
                    onClick = { onEditItem(ready.vehicleId, itemStatus.item.id) },
                    onMarkDone = { onMarkDone(ready.vehicleId, itemStatus.item.id) },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    itemStatus: ItemStatus,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
) {
    val markDoneDescription = stringResource(R.string.maintenance_mark_done_named, itemStatus.item.name)
    ListItem(
        headlineContent = { Text(itemStatus.item.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(maintenanceDueText(itemStatus.status) ?: stringResource(R.string.maintenance_set_first))
                StatusBadge(itemStatus.status)
            }
        },
        trailingContent = {
            OutlinedButton(
                onClick = onMarkDone,
                modifier = Modifier.semantics { contentDescription = markDoneDescription },
            ) { Text(stringResource(R.string.maintenance_mark_done)) }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun MaintenancePreview() {
    fun item(
        id: String,
        name: String,
    ) = MaintenanceItem(id, "v", name, 2_000, 4, null, null)
    MotoManTheme {
        MaintenanceContent(
            state =
                MaintenanceUiState.Ready(
                    vehicleId = "v",
                    odometerKm = 12_844,
                    items =
                        listOf(
                            ItemStatus(
                                item("1", "Brake pads"),
                                MaintenanceStatus.Overdue(DuePoint(12_744, null, -100, null)),
                            ),
                            ItemStatus(
                                item("2", "Oil change"),
                                MaintenanceStatus.DueSoon(DuePoint(12_994, null, 150, null)),
                            ),
                            ItemStatus(item("3", "Tyres"), MaintenanceStatus.NoBaseline),
                        ),
                ),
            onAddItem = {},
            onEditItem = { _, _ -> },
            onMarkDone = { _, _ -> },
        )
    }
}
