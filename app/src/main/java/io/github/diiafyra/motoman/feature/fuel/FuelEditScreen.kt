package io.github.diiafyra.motoman.feature.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.fuel.CostError
import io.github.diiafyra.motoman.core.domain.fuel.FilledAtError
import io.github.diiafyra.motoman.core.domain.fuel.MAX_FILL_VOLUME_ML
import io.github.diiafyra.motoman.core.domain.fuel.OdometerError
import io.github.diiafyra.motoman.core.domain.fuel.VolumeError
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.ui.designsystem.component.DateField
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.MotoManTextField
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.formatLiters
import java.time.LocalDate

@Composable
fun FuelEditScreen(
    onDone: () -> Unit,
    viewModel: FuelEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    FuelEditContent(
        state = state,
        actions =
            FuelEditActions(
                onBack = onDone,
                onDateChange = viewModel::onDateChange,
                onOdometerChange = viewModel::onOdometerChange,
                onVolumeChange = viewModel::onVolumeChange,
                onCostChange = viewModel::onCostChange,
                onFullTankChange = viewModel::onFullTankChange,
                onMissedPreviousChange = viewModel::onMissedPreviousChange,
                onNoteChange = viewModel::onNoteChange,
                onSave = viewModel::onSaveClick,
            ),
    )
}

/** Grouped so the content composable does not take ten lambdas. */
internal data class FuelEditActions(
    val onBack: () -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onOdometerChange: (String) -> Unit,
    val onVolumeChange: (String) -> Unit,
    val onCostChange: (String) -> Unit,
    val onFullTankChange: (Boolean) -> Unit,
    val onMissedPreviousChange: (Boolean) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onSave: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FuelEditContent(
    state: FuelEditUiState,
    actions: FuelEditActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.isNew) R.string.fuel_new_title else R.string.fuel_edit_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField(
                label = stringResource(R.string.fuel_date),
                date = state.date,
                latest = state.today,
                onDateChange = actions.onDateChange,
                error =
                    if (state.errors.filledAt ==
                        FilledAtError.IN_FUTURE
                    ) {
                        stringResource(R.string.error_date_future)
                    } else {
                        null
                    },
            )

            MotoManTextField(
                value = state.odometerText,
                onValueChange = actions.onOdometerChange,
                label = stringResource(R.string.fuel_odometer),
                suffix = stringResource(R.string.unit_km),
                keyboardType = KeyboardType.Number,
                error = odometerError(state.errors.odometer),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.errors.odometer == null && state.previousOdometerKm != null) {
                HelpText(
                    stringResource(R.string.fuel_previous_odometer, formatInteger(state.previousOdometerKm.toLong())),
                )
            }

            MotoManTextField(
                value = state.volumeText,
                onValueChange = actions.onVolumeChange,
                label = stringResource(R.string.fuel_volume),
                suffix = stringResource(R.string.unit_liter),
                keyboardType = KeyboardType.Decimal,
                error =
                    if (state.isVolumeUnreadable) {
                        stringResource(R.string.error_volume_unreadable)
                    } else {
                        volumeError(state.errors.volume)
                    },
                modifier = Modifier.fillMaxWidth(),
            )

            MotoManTextField(
                value = state.costText,
                onValueChange = actions.onCostChange,
                label = stringResource(R.string.fuel_cost),
                suffix = stringResource(R.string.unit_vnd),
                keyboardType = KeyboardType.Number,
                error =
                    when (state.errors.cost) {
                        CostError.MISSING -> stringResource(R.string.error_cost_missing)
                        CostError.NOT_POSITIVE -> stringResource(R.string.error_cost_not_positive)
                        null -> null
                    },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.pricePerLiterVnd != null && state.errors.cost == null) {
                HelpText(stringResource(R.string.fuel_price_per_liter, formatInteger(state.pricePerLiterVnd)))
            }

            ToggleRow(
                title = stringResource(R.string.fuel_full_tank),
                help = stringResource(R.string.fuel_full_tank_help),
                checked = state.isFullTank,
                onCheckedChange = actions.onFullTankChange,
                isSwitch = true,
            )
            ToggleRow(
                title = stringResource(R.string.fuel_missed_previous),
                help = stringResource(R.string.fuel_missed_previous_help),
                checked = state.missedPrevious,
                onCheckedChange = actions.onMissedPreviousChange,
                isSwitch = false,
            )

            MotoManTextField(
                value = state.note,
                onValueChange = actions.onNoteChange,
                label = stringResource(R.string.fuel_note),
                imeAction = ImeAction.Done,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = actions.onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun odometerError(error: OdometerError?): String? =
    when (error) {
        OdometerError.Missing -> {
            stringResource(R.string.error_odometer_missing)
        }

        is OdometerError.BelowInitial -> {
            stringResource(R.string.error_odometer_below_initial, formatInteger(error.initialKm.toLong()))
        }

        is OdometerError.BelowPrevious -> {
            stringResource(R.string.error_odometer_below_previous, formatInteger(error.previousKm.toLong()))
        }

        is OdometerError.AboveNext -> {
            stringResource(R.string.error_odometer_above_next, formatInteger(error.nextKm.toLong()))
        }

        is OdometerError.TooLarge -> {
            stringResource(R.string.error_odometer_too_large, formatInteger(MAX_ODOMETER_KM.toLong()))
        }

        null -> {
            null
        }
    }

@Composable
private fun volumeError(error: VolumeError?): String? =
    when (error) {
        VolumeError.Missing -> stringResource(R.string.error_volume_missing)
        VolumeError.NotPositive -> stringResource(R.string.error_volume_not_positive)
        is VolumeError.TooLarge -> stringResource(R.string.error_volume_too_large, formatLiters(MAX_FILL_VOLUME_ML))
        null -> null
    }

@Composable
private fun ToggleRow(
    title: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isSwitch: Boolean,
) {
    // The whole row is one toggle, so the touch target is large and TalkBack reads title and help together.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    role = if (isSwitch) Role.Switch else Role.Checkbox,
                ).padding(vertical = 8.dp),
    ) {
        if (!isSwitch) Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isSwitch) Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun HelpText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun FuelEditPreview() {
    MotoManTheme {
        FuelEditContent(
            state =
                FuelEditUiState(
                    isNew = true,
                    isLoading = false,
                    date = LocalDate.of(2026, 11, 8),
                    today = LocalDate.of(2026, 11, 8),
                    odometerText = "12480",
                    volumeText = "3,8",
                    costText = "95000",
                    previousOdometerKm = 12_300,
                    pricePerLiterVnd = 25_000,
                ),
            actions = FuelEditActions({}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
