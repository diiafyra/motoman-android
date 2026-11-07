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
import io.github.diiafyra.motoman.feature.fuel.FuelEditScreen
import io.github.diiafyra.motoman.feature.fuel.FuelLogScreen
import io.github.diiafyra.motoman.feature.maintenance.MaintenanceDoneScreen
import io.github.diiafyra.motoman.feature.maintenance.MaintenanceItemEditScreen
import io.github.diiafyra.motoman.feature.maintenance.MaintenanceScreen
import io.github.diiafyra.motoman.feature.overview.OverviewScreen
import io.github.diiafyra.motoman.feature.placeholder.TabPlaceholder
import io.github.diiafyra.motoman.feature.stats.StatsScreen
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
        composable<OverviewRoute> {
            withVehicle(TopLevelDestination.OVERVIEW) { vehicle ->
                OverviewScreen(
                    onAddFillUp = { navController.navigate(FuelEditRoute(vehicle.id)) },
                    onOpenStats = { navController.navigate(StatsRoute) },
                    onOpenMaintenance = { navController.navigateToTab(TopLevelDestination.MAINTENANCE) },
                )
            }
        }
        composable<FuelRoute> {
            withVehicle(TopLevelDestination.FUEL) {
                FuelLogScreen(
                    onAddEntry = { vehicleId -> navController.navigate(FuelEditRoute(vehicleId)) },
                    onEditEntry = { vehicleId, entryId -> navController.navigate(FuelEditRoute(vehicleId, entryId)) },
                )
            }
        }
        composable<MaintenanceRoute> {
            withVehicle(TopLevelDestination.MAINTENANCE) {
                MaintenanceScreen(
                    onAddItem = { vehicleId -> navController.navigate(MaintenanceItemRoute(vehicleId)) },
                    onEditItem = {
                        vehicleId,
                        itemId,
                        ->
                        navController.navigate(MaintenanceItemRoute(vehicleId, itemId))
                    },
                    onMarkDone = {
                        vehicleId,
                        itemId,
                        ->
                        navController.navigate(MaintenanceDoneRoute(vehicleId, itemId))
                    },
                )
            }
        }
        composable<NearbyRoute> { Tab(TopLevelDestination.NEARBY, withVehicle) }

        composable<FuelEditRoute> {
            FuelEditScreen(onDone = navController::popBackStack)
        }
        composable<MaintenanceItemRoute> {
            MaintenanceItemEditScreen(onDone = navController::popBackStack)
        }
        composable<MaintenanceDoneRoute> {
            MaintenanceDoneScreen(onDone = navController::popBackStack)
        }
        composable<StatsRoute> {
            StatsScreen(onBack = navController::popBackStack)
        }
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
