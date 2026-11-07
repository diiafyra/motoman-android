package io.github.diiafyra.motoman.feature.vehicle

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.labelRes

@Composable
fun VehicleListScreen(
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit,
    viewModel: VehicleListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onBack() }
    VehicleListContent(
        state = state,
        onBack = onBack,
        onAddVehicle = onAddVehicle,
        onVehicleClick = viewModel::onVehicleClick,
        onEditVehicle = onEditVehicle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleListContent(
    state: VehicleListUiState,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onVehicleClick: (String) -> Unit,
    onEditVehicle: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vehicles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.vehicles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddVehicle,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.vehicle_add)) },
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingState(Modifier.padding(padding))
            }

            state.vehicles.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.vehicles_empty_title),
                    body = stringResource(R.string.vehicles_empty_body),
                    actionLabel = stringResource(R.string.vehicle_add),
                    onAction = onAddVehicle,
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    // Room for the floating button over the last row.
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.vehicles, key = { it.id }) { vehicle ->
                        VehicleRow(
                            vehicle = vehicle,
                            isSelected = vehicle.id == state.selectedId,
                            onClick = { onVehicleClick(vehicle.id) },
                            onEdit = { onEditVehicle(vehicle.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(vehicle.name) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.vehicle_summary,
                    stringResource(vehicle.type.labelRes),
                    stringResource(vehicle.fuelType.labelRes),
                    stringResource(R.string.value_km, formatInteger(vehicle.initialOdometerKm.toLong())),
                ),
            )
        },
        // The radio button is part of the row's single selectable target, so TalkBack reads one item.
        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
        trailingContent = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, stringResource(R.string.vehicle_edit_named, vehicle.name))
            }
        },
        modifier = Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun VehicleListPreview() {
    MotoManTheme {
        VehicleListContent(
            state =
                VehicleListUiState(
                    isLoading = false,
                    vehicles =
                        listOf(
                            Vehicle("1", "Wave Alpha", VehicleType.MOTORBIKE, FuelType.E5_RON92, 12_300),
                            Vehicle("2", "Vios", VehicleType.CAR, FuelType.RON95, 45_000),
                        ),
                    selectedId = "1",
                ),
            onBack = {},
            onAddVehicle = {},
            onVehicleClick = {},
            onEditVehicle = {},
        )
    }
}
