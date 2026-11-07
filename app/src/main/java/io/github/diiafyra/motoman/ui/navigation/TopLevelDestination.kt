package io.github.diiafyra.motoman.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.diiafyra.motoman.R
import kotlin.reflect.KClass

/** The four bottom-bar tabs. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    OVERVIEW(OverviewRoute, OverviewRoute::class, R.drawable.ic_overview, R.string.tab_overview),
    FUEL(FuelRoute, FuelRoute::class, R.drawable.ic_fuel, R.string.tab_fuel),
    MAINTENANCE(MaintenanceRoute, MaintenanceRoute::class, R.drawable.ic_maintenance, R.string.tab_maintenance),
    NEARBY(NearbyRoute, NearbyRoute::class, R.drawable.ic_nearby, R.string.tab_nearby),
}

fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        // One copy of each tab on the back stack, each keeping its scroll position and state.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
