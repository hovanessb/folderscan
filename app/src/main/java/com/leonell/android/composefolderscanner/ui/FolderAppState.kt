package com.leonell.android.composefolderscanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.trace
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.leonell.android.composefolderscanner.services.NavigationDestination
import com.leonell.android.composefolderscanner.services.NetworkMonitor
import com.leonell.android.composefolderscanner.ui.screens.DeviceRoute
import com.leonell.android.composefolderscanner.ui.screens.LocateRoute
import com.leonell.android.composefolderscanner.ui.screens.ProfileRoute
import com.leonell.android.composefolderscanner.ui.screens.ScanRoute
import com.leonell.android.composefolderscanner.ui.screens.SettingsRoute
import com.leonell.android.composefolderscanner.ui.screens.WriteRoute
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.TriangulationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun rememberFolderAppState(
   networkMonitor: NetworkMonitor,
   coroutineScope: CoroutineScope = rememberCoroutineScope(),
   navController: NavHostController = rememberNavController(),
): FolderAppState = remember(navController, coroutineScope, networkMonitor) {
   FolderAppState(navController, coroutineScope, networkMonitor)
}

@Stable
class FolderAppState(
   val navController: NavHostController,
   coroutineScope: CoroutineScope,
   networkMonitor: NetworkMonitor,
) {
   val currentDestination: NavDestination?
      @Composable get() = navController.currentBackStackEntryAsState().value?.destination

   val isOffline = networkMonitor.isOnline
      .map(Boolean::not)
      .stateIn(
         scope = coroutineScope,
         started = SharingStarted.WhileSubscribed(5_000),
         initialValue = false,
      )

   /** Top-level destinations shown in the bottom bar. */
   val navigations: List<NavigationDestination> = NavigationDestination.entries

   /**
    * Navigates to a top-level destination, keeping one copy of it on the back stack and
    * restoring whatever state it had.
    */
   fun navigateToTopLevelDestination(destination: NavigationDestination) {
      trace("Navigation: ${destination.name}") {
         val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
         }

         when (destination) {
            NavigationDestination.PROFILE -> navController.navigateToProfile(topLevelNavOptions)
            NavigationDestination.SCAN -> navController.navigateToScan(topLevelNavOptions)
            NavigationDestination.LOCATE -> navController.navigateToLocate(null, topLevelNavOptions)
            NavigationDestination.WRITE -> navController.navigateToWrite(topLevelNavOptions)
            NavigationDestination.DEVICES -> navController.navigateToDevice(topLevelNavOptions)
            NavigationDestination.SETTINGS -> navController.navigateToSettings(topLevelNavOptions)
         }
      }
   }

   fun onBackClick() {
      navController.popBackStack()
   }
}

const val profileNavigationRoute = "profile_route"
const val deviceNavigationRoute = "device_route"
const val scanNavigationRoute = "scan_route"
const val writeNavigationRoute = "write_route"
const val locateNavigationRoute = "locate_route"
const val settingsNavigationRoute = "settings_route"

private const val LOCATE_ARG = "barcodeIdSeries"

/** Sentinel for "open Locate with nothing selected"; an empty path segment is not routable. */
private const val LOCATE_NONE = "none"

fun NavController.navigateToProfile(navOptions: NavOptions? = null) =
   navigate(profileNavigationRoute, navOptions)

fun NavController.navigateToWrite(navOptions: NavOptions? = null) =
   navigate(writeNavigationRoute, navOptions)

fun NavController.navigateToScan(navOptions: NavOptions? = null) =
   navigate(scanNavigationRoute, navOptions)

fun NavController.navigateToDevice(navOptions: NavOptions? = null) =
   navigate(deviceNavigationRoute, navOptions)

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
   navigate(settingsNavigationRoute, navOptions)

/**
 * Opens the Locate screen, optionally pre-targeting a folder.
 *
 * The previous version built `"locate_route/{$barcodeId}"` -- literal braces around the
 * value -- and the destination stripped them back off with `replace("}", "")`. An empty
 * barcode produced `locate_route/{}`, and any value needing escaping broke the route.
 */
fun NavController.navigateToLocate(barcodeIdSeries: String? = null, navOptions: NavOptions? = null) {
   val argument = barcodeIdSeries
      ?.takeIf { it.isNotBlank() }
      ?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
      ?: LOCATE_NONE
   navigate("$locateNavigationRoute/$argument", navOptions)
}

fun NavGraphBuilder.profileScreen(navController: NavController, scanModel: ScanViewModel) {
   composable(route = profileNavigationRoute) {
      ProfileRoute(navController = navController, scanModel = scanModel)
   }
}

fun NavGraphBuilder.deviceScreen(convergenceModel: ConvergenceHandgunViewModel) {
   composable(route = deviceNavigationRoute) {
      DeviceRoute(convergenceModel = convergenceModel)
   }
}

fun NavGraphBuilder.settingsScreen() {
   composable(route = settingsNavigationRoute) { SettingsRoute() }
}

fun NavGraphBuilder.locateScreen(
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   triangulationViewModel: TriangulationViewModel,
) {
   composable(
      route = "$locateNavigationRoute/{$LOCATE_ARG}",
      arguments = listOf(navArgument(LOCATE_ARG) { type = NavType.StringType }),
   ) { backStackEntry ->
      val argument = backStackEntry.arguments?.getString(LOCATE_ARG)
      LocateRoute(
         barcodeId = argument?.takeIf { it != LOCATE_NONE },
         scanModel = scanModel,
         convergenceModel = convergenceModel,
         triangulationModel = triangulationViewModel,
      )
   }
}

fun NavGraphBuilder.writeScreen(
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
) {
   composable(route = writeNavigationRoute) {
      WriteRoute(scanModel = scanModel, convergenceModel = convergenceModel)
   }
}

fun NavGraphBuilder.scanScreen(
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
) {
   composable(route = scanNavigationRoute) {
      ScanRoute(scanModel = scanModel, convergenceModel = convergenceModel)
   }
}
