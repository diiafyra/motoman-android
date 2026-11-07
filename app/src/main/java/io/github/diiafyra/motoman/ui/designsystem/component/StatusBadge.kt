package io.github.diiafyra.motoman.ui.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.ui.format.maintenanceStatusLabel

/**
 * A maintenance status as icon + label on a tinted chip. Status is never told by color alone: the
 * label is always there, and the icon differs per status.
 */
@Composable
fun StatusBadge(
    status: MaintenanceStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content, icon) =
        when (status) {
            is MaintenanceStatus.Overdue -> {
                Triple(colors.errorContainer, colors.onErrorContainer, Icons.Filled.Warning)
            }

            is MaintenanceStatus.DueSoon -> {
                Triple(
                    colors.tertiaryContainer,
                    colors.onTertiaryContainer,
                    Icons.Filled.Info,
                )
            }

            is MaintenanceStatus.Ok -> {
                Triple(
                    colors.secondaryContainer,
                    colors.onSecondaryContainer,
                    Icons.Filled.CheckCircle,
                )
            }

            MaintenanceStatus.NoBaseline -> {
                Triple(colors.surfaceVariant, colors.onSurfaceVariant, Icons.Filled.Info)
            }
        }
    StatusChip(maintenanceStatusLabel(status), container, content, icon, modifier)
}

@Composable
private fun StatusChip(
    label: String,
    container: Color,
    content: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            // Decorative: the label next to it says the same.
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
