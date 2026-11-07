package io.github.diiafyra.motoman.feature.placeholder

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState

/** Stands in for a tab until its feature lands. */
@Composable
fun TabPlaceholder(
    title: String,
    @DrawableRes icon: Int,
) {
    EmptyState(
        title = title,
        body = stringResource(R.string.placeholder_body),
        icon = ImageVector.vectorResource(icon),
    )
}
