package io.github.diiafyra.motoman.feature.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.IntervalError
import io.github.diiafyra.motoman.core.domain.maintenance.ItemNameError
import io.github.diiafyra.motoman.core.domain.maintenance.MAX_ITEM_NAME_LENGTH
import io.github.diiafyra.motoman.core.domain.model.MAX_INTERVAL_MONTHS
import io.github.diiafyra.motoman.core.domain.model.MAX_ODOMETER_KM
import io.github.diiafyra.motoman.ui.designsystem.component.ConfirmDialog
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.component.MotoManTextField
import io.github.diiafyra.motoman.ui.format.formatDate
import io.github.diiafyra.motoman.ui.format.formatInteger

@Composable
fun MaintenanceItemEditScreen(
    onDone: () -> Unit,
    viewModel: MaintenanceItemEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    MaintenanceItemEditContent(
        state = state,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onIntervalKmChange = viewModel::onIntervalKmChange,
        onIntervalMonthsChange = viewModel::onIntervalMonthsChange,
        onSave = viewModel::onSaveClick,
        onDelete = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaintenanceItemEditContent(
    state: MaintenanceItemEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onIntervalKmChange: (String) -> Unit,
    onIntervalMonthsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title =
                        if (state.isNew) R.string.maintenance_new_item_title else R.string.maintenance_edit_item_title
                    Text(stringResource(title))
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MotoManTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.maintenance_item_name),
                error =
                    when (state.errors.name) {
                        ItemNameError.BLANK -> {
                            stringResource(R.string.error_item_name_blank)
                        }

                        ItemNameError.TOO_LONG -> {
                            pluralStringResource(
                                R.plurals.error_name_too_long,
                                MAX_ITEM_NAME_LENGTH,
                                MAX_ITEM_NAME_LENGTH,
                            )
                        }

                        null -> {
                            null
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MotoManTextField(
                    value = state.intervalKmText,
                    onValueChange = onIntervalKmChange,
                    label = stringResource(R.string.maintenance_interval_km),
                    suffix = stringResource(R.string.unit_km),
                    keyboardType = KeyboardType.Number,
                    error =
                        when (state.errors.intervalKm) {
                            IntervalError.NOT_POSITIVE -> {
                                stringResource(R.string.error_interval_not_positive)
                            }

                            IntervalError.TOO_LARGE -> {
                                stringResource(
                                    R.string.error_interval_km_too_large,
                                    formatInteger(MAX_ODOMETER_KM.toLong()),
                                )
                            }

                            null -> {
                                null
                            }
                        },
                    modifier = Modifier.weight(1f),
                )
                MotoManTextField(
                    value = state.intervalMonthsText,
                    onValueChange = onIntervalMonthsChange,
                    label = stringResource(R.string.maintenance_interval_months),
                    suffix = stringResource(R.string.unit_months),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    error =
                        when (state.errors.intervalMonths) {
                            IntervalError.NOT_POSITIVE -> {
                                stringResource(R.string.error_interval_not_positive)
                            }

                            IntervalError.TOO_LARGE -> {
                                pluralStringResource(
                                    R.plurals.error_interval_months_too_large,
                                    MAX_INTERVAL_MONTHS,
                                    MAX_INTERVAL_MONTHS,
                                )
                            }

                            null -> {
                                null
                            }
                        },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text =
                    if (state.errors.noInterval) {
                        stringResource(R.string.error_no_interval)
                    } else {
                        stringResource(R.string.maintenance_interval_help)
                    },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (state.errors.noInterval) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            if (!state.isNew) {
                Text(
                    text =
                        when {
                            state.lastDoneDate != null && state.lastDoneKm != null -> {
                                stringResource(
                                    R.string.maintenance_last_done,
                                    formatDate(state.lastDoneDate),
                                    formatInteger(state.lastDoneKm.toLong()),
                                )
                            }

                            state.lastDoneKm != null -> {
                                stringResource(
                                    R.string.maintenance_last_done_km,
                                    formatInteger(state.lastDoneKm.toLong()),
                                )
                            }

                            else -> {
                                stringResource(R.string.maintenance_never_done)
                            }
                        },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
            if (!state.isNew) {
                OutlinedButton(onClick = onDelete, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
                History(state)
            }
        }
    }

    if (state.isDeleteConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.maintenance_delete_title, state.name),
            text = stringResource(R.string.maintenance_delete_text),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = onDeleteConfirm,
            onDismiss = onDeleteDismiss,
            destructive = true,
        )
    }
}

@Composable
private fun History(state: MaintenanceItemEditUiState) {
    Text(
        stringResource(R.string.maintenance_history_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
    if (state.history.isEmpty()) {
        Text(stringResource(R.string.maintenance_history_empty), style = MaterialTheme.typography.bodyMedium)
        return
    }
    state.history.forEachIndexed { index, record ->
        if (index > 0) HorizontalDivider()
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(
                    R.string.maintenance_history_entry,
                    formatDate(record.doneDate),
                    formatInteger(record.odometerKm.toLong()),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            val details =
                listOfNotNull(
                    record.costVnd?.let { stringResource(R.string.value_vnd, formatInteger(it)) },
                    record.note.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
