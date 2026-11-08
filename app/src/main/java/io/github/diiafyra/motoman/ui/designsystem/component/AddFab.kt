package io.github.diiafyra.motoman.ui.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The "add" button of a list screen. The label is also set as the content description: the extended
 * button's text did not reach the accessibility tree (an audit found a button TalkBack read without a
 * name), so it is stated explicitly.
 */
@Composable
fun AddFab(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(label) },
        modifier = modifier.semantics { contentDescription = label },
    )
}
