package io.github.diiafyra.motoman.ui.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * One series of bars on an even time axis. Built for few bars (a handful of months):
 *
 * - Thin bars with a 4 dp rounded top, anchored to a recessive baseline; no grid.
 * - One value label, over the tapped bar (the last bar with data until one is tapped), never one per bar.
 * - A null value is a gap, not a zero.
 * - TalkBack reads [description] instead of the drawing; the screen also shows the values as a table.
 */
@Composable
fun BarChart(
    values: List<Double?>,
    labels: List<String>,
    formatValue: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
) {
    require(values.size == labels.size) { "One label per bar" }
    val barColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val textMeasurer = rememberTextMeasurer()
    val lastWithData = values.indexOfLast { it != null }
    var selected by rememberSaveable(values) { mutableIntStateOf(lastWithData) }
    val maxValue = values.filterNotNull().maxOrNull()?.takeIf { it > 0 } ?: 1.0

    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
                    .pointerInput(values) {
                        detectTapGestures { offset ->
                            val index = (offset.x / (size.width / values.size)).toInt().coerceIn(values.indices)
                            if (values[index] != null) selected = index
                        }
                    },
        ) {
            val slot = size.width / values.size
            val barWidth = min(BAR_MAX_WIDTH.toPx(), slot * BAR_SLOT_SHARE)
            val labelSpace = LABEL_SPACE.toPx()
            val plotHeight = size.height - labelSpace
            val radius = CornerRadius(BAR_RADIUS.toPx())

            values.forEachIndexed { index, value ->
                if (value == null || value <= 0) return@forEachIndexed
                val height = max((value / maxValue * plotHeight).toFloat(), BAR_RADIUS.toPx())
                val left = slot * index + (slot - barWidth) / 2
                val top = size.height - height
                // Rounded at the top only: the bar stands on the baseline.
                val path =
                    Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = left + barWidth,
                                bottom = size.height,
                                topLeftCornerRadius = radius,
                                topRightCornerRadius = radius,
                            ),
                        )
                    }
                drawPath(path, color = if (index == selected) barColor else barColor.copy(alpha = UNSELECTED_ALPHA))
            }
            drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())

            val selectedValue = values.getOrNull(selected)
            if (selectedValue != null) {
                val text = textMeasurer.measure(formatValue(selectedValue), labelStyle)
                val barHeight = (selectedValue / maxValue * plotHeight).toFloat()
                val x = (slot * selected + slot / 2 - text.size.width / 2).coerceIn(0f, size.width - text.size.width)
                val y = size.height - barHeight - text.size.height - LABEL_GAP.toPx()
                drawText(text, topLeft = Offset(x, max(0f, y)))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val CHART_HEIGHT = 160.dp
private val BAR_MAX_WIDTH = 28.dp
private const val BAR_SLOT_SHARE = 0.6f
private val BAR_RADIUS = 4.dp
private val LABEL_SPACE = 24.dp
private val LABEL_GAP = 4.dp
private const val UNSELECTED_ALPHA = 0.55f
