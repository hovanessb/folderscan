package com.leonell.android.composefolderscanner.services

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.leonell.android.composefolderscanner.R
import com.leonell.android.composefolderscanner.ui.deviceScreen
import com.leonell.android.composefolderscanner.ui.locateScreen
import com.leonell.android.composefolderscanner.ui.profileScreen
import com.leonell.android.composefolderscanner.ui.scanNavigationRoute
import com.leonell.android.composefolderscanner.ui.scanScreen
import com.leonell.android.composefolderscanner.ui.settingsScreen
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.TriangulationViewModel
import com.leonell.android.composefolderscanner.ui.writeScreen

/** Material icons are [ImageVector]s; custom icons are drawable resource IDs. */
object FolderIcons {
   val PROFILE = R.drawable.account_circle
   val BLUETOOTH = R.drawable.bluetooth
   val QR = R.drawable.qr_code_scanner
   val ADD = Icons.Rounded.Add
   val PENCIL = R.drawable.auto_fix
   val DELETE = Icons.Rounded.Delete
   val LOCATE = R.drawable.token
   val SETTINGS = Icons.Rounded.Settings
}

/**
 * Makes [ImageVector] and [DrawableRes] icons interchangeable at a call site.
 *
 * Named `NavIcon` rather than `Icon` so it does not shadow Material 3's `Icon` composable in
 * files that wildcard-import this package.
 */
sealed interface NavIcon {
   data class ImageVectorIcon(val imageVector: ImageVector) : NavIcon
   data class DrawableResourceIcon(@DrawableRes val id: Int) : NavIcon
}

/**
 * Top-level destinations.
 *
 * Named `NavigationDestination` rather than `Navigation` so it does not collide with
 * `androidx.navigation` at import sites.
 */
enum class NavigationDestination(
   val selectedIcon: NavIcon,
   val unselectedIcon: NavIcon,
   val iconText: String,
   val titleText: String,
) {
   PROFILE(
      selectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.PROFILE),
      unselectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.PROFILE),
      iconText = "PROFILE",
      titleText = "PROFILE",
   ),
   SCAN(
      selectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.QR),
      unselectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.QR),
      iconText = "LOG",
      titleText = "LOG",
   ),
   LOCATE(
      selectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.LOCATE),
      unselectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.LOCATE),
      iconText = "LOCATE",
      titleText = "LOCATE",
   ),
   WRITE(
      selectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.PENCIL),
      unselectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.PENCIL),
      iconText = "REWRITE",
      titleText = "REWRITE",
   ),
   DEVICES(
      selectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.BLUETOOTH),
      unselectedIcon = NavIcon.DrawableResourceIcon(FolderIcons.BLUETOOTH),
      iconText = "DEVICES",
      titleText = "DEVICES",
   ),
   SETTINGS(
      selectedIcon = NavIcon.ImageVectorIcon(FolderIcons.SETTINGS),
      unselectedIcon = NavIcon.ImageVectorIcon(FolderIcons.SETTINGS),
      iconText = "SETUP",
      titleText = "SETUP",
   ),
}

@Composable
fun FolderNavHost(
   navController: NavHostController,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   triangulationModel: TriangulationViewModel,
   modifier: Modifier = Modifier,
   startDestination: String = scanNavigationRoute,
) {
   NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = modifier,
   ) {
      profileScreen(navController = navController, scanModel = scanModel)
      scanScreen(scanModel = scanModel, convergenceModel = convergenceModel)
      writeScreen(scanModel = scanModel, convergenceModel = convergenceModel)
      locateScreen(
         scanModel = scanModel,
         convergenceModel = convergenceModel,
         triangulationViewModel = triangulationModel,
      )
      deviceScreen(convergenceModel = convergenceModel)
      settingsScreen()
   }
}

@Composable
fun RowScope.FolderNavigationBarItem(
   selected: Boolean,
   onClick: () -> Unit,
   icon: @Composable () -> Unit,
   modifier: Modifier = Modifier,
   selectedIcon: @Composable () -> Unit = icon,
   enabled: Boolean = true,
   label: @Composable (() -> Unit)? = null,
   alwaysShowLabel: Boolean = true,
) {
   NavigationBarItem(
      selected = selected,
      onClick = onClick,
      icon = if (selected) selectedIcon else icon,
      modifier = modifier,
      enabled = enabled,
      label = label,
      alwaysShowLabel = alwaysShowLabel,
   )
}

/** Wraps Material 3 [NavigationBar] with the app's elevation. */
@Composable
fun FolderNavigationBar(
   modifier: Modifier = Modifier,
   content: @Composable RowScope.() -> Unit,
) {
   NavigationBar(
      modifier = modifier,
      tonalElevation = 5.dp,
      content = content,
   )
}
