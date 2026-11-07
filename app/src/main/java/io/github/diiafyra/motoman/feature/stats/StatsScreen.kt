package io.github.diiafyra.motoman.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.fuel.MonthStats
import io.github.diiafyra.motoman.ui.designsystem.component.BarChart
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.designsystem.theme.MotoManTheme
import io.github.diiafyra.motoman.ui.format.formatDecimal
import io.github.diiafyra.motoman.ui.format.formatInteger
import io.github.diiafyra.motoman.ui.format.formatMonth
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(state, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsContent(
    state: StatsUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
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
        val locale = LocalConfiguration.current.locales[0]
        val shortMonth = DateTimeFormatter.ofPattern("LLL", locale)
        val labels = state.months.map { shortMonth.format(it.month) }
        val integers = NumberFormat.getIntegerInstance(locale)
        val decimals = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
        val vnd = stringResource(R.string.value_vnd, "%s")
        val perHundred = stringResource(R.string.value_l_per_100km, "%s")
        val formatVnd: (Double) -> String = { vnd.format(integers.format(it.toLong())) }
        val formatEconomy: (Double) -> String = { perHundred.format(decimals.format(it)) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val costTitle = stringResource(R.string.stats_cost_title)
            ChartCard(costTitle) {
                BarChart(
                    values = state.months.map { it.costVnd.toDouble() },
                    labels = labels,
                    formatValue = formatVnd,
                    description =
                        chartDescription(
                            costTitle,
                            labels,
                            state.months.map { formatVnd(it.costVnd.toDouble()) },
                        ),
                )
            }

            val economyTitle = stringResource(R.string.stats_economy_title)
            ChartCard(economyTitle) {
                if (state.hasEconomy) {
                    val noValue = stringResource(R.string.stats_no_value)
                    BarChart(
                        values = state.months.map { it.litersPer100Km },
                        labels = labels,
                        formatValue = formatEconomy,
                        description =
                            chartDescription(
                                economyTitle,
                                labels,
                                state.months.map { m -> m.litersPer100Km?.let(formatEconomy) ?: noValue },
                            ),
                    )
                } else {
                    Text(stringResource(R.string.stats_economy_empty), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                stringResource(R.string.stats_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MonthTable(state.months)
        }
    }
}

@Composable
private fun chartDescription(
    title: String,
    labels: List<String>,
    values: List<String>,
): String =
    stringResource(R.string.stats_chart_description, title, labels.zip(values) { l, v -> "$l: $v" }.joinToString(", "))

@Composable
private fun ChartCard(
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

/** The exact values behind both charts, newest month first. */
@Composable
private fun MonthTable(months: List<MonthStats>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.stats_table_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }.padding(bottom = 8.dp),
            )
            months.asReversed().forEachIndexed { index, month ->
                if (index > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(formatMonth(month.month), Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.value_vnd, formatInteger(month.costVnd)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        month.litersPer100Km?.let { stringResource(R.string.value_l_per_100km, formatDecimal(it, 2)) }
                            ?: stringResource(R.string.stats_no_value),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsPreview() {
    val months =
        (5..10).map { offset ->
            MonthStats(YearMonth.of(2026, offset + 1), 150_000L + offset * 20_000, 6_000, 300, 6_600, 165_000)
        }
    MotoManTheme { StatsContent(StatsUiState(isLoading = false, months = months), onBack = {}) }
}
