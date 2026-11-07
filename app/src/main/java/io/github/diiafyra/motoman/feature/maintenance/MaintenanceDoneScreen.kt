package io.github.diiafyra.motoman.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.RecordOdometerError
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.ui.designsystem.component.DateField
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.MotoManTextField
import io.github.diiafyra.motoman.ui.format.formatInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDoneScreen(
    onDone: () -> Unit,
    viewModel: MaintenanceDoneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.maintenance_done_title, state.itemName)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
                label = stringResource(R.string.maintenance_done_date),
                date = state.date,
                latest = state.today,
                onDateChange = viewModel::onDateChange,
                error = if (state.errors.dateInFuture) stringResource(R.string.error_date_future) else null,
            )
            MotoManTextField(
                value = state.odometerText,
                onValueChange = viewModel::onOdometerChange,
                label = stringResource(R.string.maintenance_done_odometer),
                suffix = stringResource(R.string.unit_km),
                keyboardType = KeyboardType.Number,
                error =
                    when (state.errors.odometer) {
                        RecordOdometerError.MISSING -> {
                            stringResource(R.string.error_odometer_missing)
                        }

                        RecordOdometerError.TOO_LARGE -> {
                            stringResource(R.string.error_odometer_too_large, formatInteger(MAX_ODOMETER_KM.toLong()))
                        }

                        null -> {
                            null
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
            )
            MotoManTextField(
                value = state.costText,
                onValueChange = viewModel::onCostChange,
                label = stringResource(R.string.maintenance_done_cost),
                suffix = stringResource(R.string.unit_vnd),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
            )
            MotoManTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = stringResource(R.string.maintenance_done_note),
                imeAction = ImeAction.Done,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::onSaveClick, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
