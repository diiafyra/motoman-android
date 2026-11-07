package io.github.diiafyra.motoman.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.FuelType
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.core.domain.model.VehicleType
import io.github.diiafyra.motoman.core.domain.vehicle.InitialOdometerError
import io.github.diiafyra.motoman.core.domain.vehicle.MAX_VEHICLE_NAME_LENGTH
import io.github.diiafyra.motoman.core.domain.vehicle.VehicleNameError
import io.github.diiafyra.motoman.ui.designsystem.component.ConfirmDialog
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.MotoManTextField
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.labelRes

@Composable
fun VehicleEditScreen(
    onDone: () -> Unit,
    viewModel: VehicleEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    VehicleEditContent(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onFuelTypeChange = viewModel::onFuelTypeChange,
        onOdometerChange = viewModel::onOdometerChange,
        onSave = viewModel::onSaveClick,
        onDelete = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun VehicleEditContent(
    state: VehicleEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (VehicleType) -> Unit,
    onFuelTypeChange: (FuelType) -> Unit,
    onOdometerChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.vehicle_new_title else R.string.vehicle_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MotoManTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.vehicle_name),
                error =
                    when (state.errors.name) {
                        VehicleNameError.BLANK -> {
                            stringResource(R.string.error_name_blank)
                        }

                        VehicleNameError.TOO_LONG -> {
                            pluralStringResource(
                                R.plurals.error_name_too_long,
                                MAX_VEHICLE_NAME_LENGTH,
                                MAX_VEHICLE_NAME_LENGTH,
                            )
                        }

                        null -> {
                            null
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.vehicle_type))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VehicleType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, VehicleType.entries.size),
                    ) { Text(stringResource(type.labelRes)) }
                }
            }

            SectionLabel(stringResource(R.string.vehicle_fuel_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FuelType.entries.forEach { fuel ->
                    FilterChip(
                        selected = state.fuelType == fuel,
                        onClick = { onFuelTypeChange(fuel) },
                        label = { Text(stringResource(fuel.labelRes)) },
                    )
                }
            }

            MotoManTextField(
                value = state.odometerText,
                onValueChange = onOdometerChange,
                label = stringResource(R.string.vehicle_initial_odometer),
                suffix = stringResource(R.string.unit_km),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                error =
                    when (state.errors.initialOdometer) {
                        InitialOdometerError.MISSING -> {
                            stringResource(R.string.error_odometer_missing)
                        }

                        InitialOdometerError.TOO_LARGE -> {
                            stringResource(R.string.error_odometer_too_large, formatInteger(MAX_ODOMETER_KM.toLong()))
                        }

                        null -> {
                            null
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.vehicle_initial_odometer_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
            if (!state.isNew) {
                OutlinedButton(onClick = onDelete, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (state.isDeleteConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.vehicle_delete_title, state.name),
            text = stringResource(R.string.vehicle_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
            destructive = true,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

@Preview(showBackground = true)
@Composable
private fun VehicleEditPreview() {
    MotoManTheme {
        VehicleEditContent(
            state = VehicleEditUiState(isNew = false, isLoading = false, name = "Wave Alpha", odometerText = "12300"),
            onBack = {},
            onNameChange = {},
            onTypeChange = {},
            onFuelTypeChange = {},
            onOdometerChange = {},
            onSave = {},
            onDelete = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
        )
    }
}
