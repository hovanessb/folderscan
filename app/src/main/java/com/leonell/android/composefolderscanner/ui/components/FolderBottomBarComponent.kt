package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.leonell.android.composefolderscanner.services.*
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBottomBar(
   destinations: List<NavigationDestination>,
   onNavigateToDestination: (NavigationDestination) -> Unit,
   currentDestination: NavDestination?,
   modifier: Modifier = Modifier,
   scanModel : ScanViewModel,
   folderCount : Int
) {
   FolderNavigationBar(
      modifier = modifier
   ) {
      destinations.forEach { destination ->
         val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
         FolderNavigationBarItem(
            selected = selected,
            onClick = {
               scanModel.clearBarcodes()
               onNavigateToDestination(destination)
            },
            modifier=Modifier.padding(top= if(destination == NavigationDestination.SCAN && folderCount != 0) 10.dp else 0.dp),
            icon = {
               BadgedBox(badge = {
                  if(destination == NavigationDestination.SCAN && folderCount != 0){
                     Badge{Text(folderCount.toString())}
                  }
            }) {
                  if (destination == NavigationDestination.SCAN && folderCount != 0) {
                     FolderLoadingWheel(contentDesc = "")
                  } else {
                     val icon =  if (selected) {
                        destination.selectedIcon
                     } else {
                        destination.unselectedIcon
                     }

                     when (icon) {
                        is NavIcon.ImageVectorIcon -> Icon(
                           imageVector = icon.imageVector,
                           contentDescription = null,
                           modifier=Modifier.width(24.dp)
                        )

                        is NavIcon.DrawableResourceIcon -> Icon(
                           painter = painterResource(id = icon.id),
                           contentDescription = null,
                           modifier=Modifier.width(24.dp)
                        )
                     }
                  }
            }
            },
            label = { Text( if (destination == NavigationDestination.SCAN && folderCount != 0)  ""  else destination.iconText) },
            alwaysShowLabel = if (destination == NavigationDestination.SCAN && folderCount != 0) false else true
         )
      }
   }
}
private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: NavigationDestination) =
   this?.hierarchy?.any {
      it.route?.contains(destination.name, true) ?: false
   } ?: false


