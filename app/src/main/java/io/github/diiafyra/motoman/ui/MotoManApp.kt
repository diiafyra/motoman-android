package io.github.diiafyra.motoman.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.ui.designsystem.component.EmptyState
import io.github.diiafyra.motoman.ui.designsystem.component.LoadingState
import io.github.diiafyra.motoman.ui.navigation.MotoManNavHost
import io.github.diiafyra.motoman.ui.navigation.TopLevelDestination
import io.github.diiafyra.motoman.ui.navigation.VehicleEditRoute
import io.github.diiafyra.motoman.ui.navigation.VehicleListRoute

/**
 * The shell: a top bar showing the selected vehicle, the bottom bar, and the navigation host. Screens
 * outside the four tabs (vehicle list and form) draw their own bars, so the shell hides its own there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoManApp(viewModel: AppViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.topLevelDestination()

    Scaffold(
        topBar = {
            if (currentTab != null) {
                TopAppBar(
                    title = {
                        val selected = (state as? AppUiState.Ready)?.selected
                        if (selected == null) {
                            Text(stringResource(R.string.app_name))
                        } else {
                            VehicleSwitcher(selected, onClick = { navController.navigate(VehicleListRoute) })
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(ImageVector.vectorResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        MotoManNavHost(
            navController = navController,
            // Only the tabs sit inside the shell's bars; the other screens handle insets themselves.
            contentPadding = if (currentTab != null) padding else PaddingValues(0.dp),
            withVehicle = { tab, content ->
                when (val current = state) {
                    AppUiState.Loading -> {
                        LoadingState()
                    }

                    is AppUiState.Ready -> {
                        if (current.selected == null) {
                            EmptyState(
                                title = stringResource(R.string.no_vehicle_title),
                                body = stringResource(R.string.no_vehicle_body),
                                icon = ImageVector.vectorResource(tab.icon),
                                actionLabel = stringResource(R.string.vehicle_add),
                                onAction = { navController.navigate(VehicleEditRoute()) },
                            )
                        } else {
                            content(current.selected)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun VehicleSwitcher(
    vehicle: Vehicle,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.switch_vehicle, vehicle.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                },
    ) {
        Text(vehicle.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
}

private fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { tab -> hierarchy.any { it.hasRoute(tab.routeClass) } }

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        // One copy of each tab on the back stack, each keeping its scroll position and state.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
