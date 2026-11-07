package io.github.diiafyra.motoman.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.diiafyra.motoman.core.domain.model.Vehicle
import io.github.diiafyra.motoman.feature.placeholder.TabPlaceholder
import io.github.diiafyra.motoman.feature.vehicle.VehicleEditScreen
import io.github.diiafyra.motoman.feature.vehicle.VehicleListScreen

/**
 * @param withVehicle shows the tab's content for the selected vehicle, or the shell's own state
 *   (loading, no vehicle yet) when there is none.
 */
@Composable
fun MotoManNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    withVehicle: @Composable (TopLevelDestination, @Composable (Vehicle) -> Unit) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = OverviewRoute,
        modifier = Modifier.padding(contentPadding),
    ) {
        composable<OverviewRoute> { Tab(TopLevelDestination.OVERVIEW, withVehicle) }
        composable<FuelRoute> { Tab(TopLevelDestination.FUEL, withVehicle) }
        composable<MaintenanceRoute> { Tab(TopLevelDestination.MAINTENANCE, withVehicle) }
        composable<NearbyRoute> { Tab(TopLevelDestination.NEARBY, withVehicle) }

        composable<VehicleListRoute> {
            VehicleListScreen(
                onBack = navController::popBackStack,
                onAddVehicle = { navController.navigate(VehicleEditRoute()) },
                onEditVehicle = { id -> navController.navigate(VehicleEditRoute(id)) },
            )
        }
        composable<VehicleEditRoute> {
            VehicleEditScreen(onDone = navController::popBackStack)
        }
    }
}

@Composable
private fun Tab(
    tab: TopLevelDestination,
    withVehicle: @Composable (TopLevelDestination, @Composable (Vehicle) -> Unit) -> Unit,
) {
    withVehicle(tab) { TabPlaceholder(title = stringResource(tab.label), icon = tab.icon) }
}
