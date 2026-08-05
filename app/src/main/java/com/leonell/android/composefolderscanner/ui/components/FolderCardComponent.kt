package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.google.gson.internal.LazilyParsedNumber
import com.leonell.android.composefolderscanner.models.FolderModel
import com.leonell.android.composefolderscanner.services.Navigation
import com.leonell.android.composefolderscanner.services.dBuV_dBm
import com.leonell.android.composefolderscanner.ui.locateNavigationRoute
import com.leonell.android.composefolderscanner.ui.navigateToLocate
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * #{aDatum.item.folderNumber} - ID: {aDatum.item.barcode} Date: {aDatum.item.fromDate}-{aDatum.item.toDate} {"\n"}
TYPE: {aDatum.item.folderTypeName}{"\n"}
LOCATION: {aDatum.item.currentLocationName} {aDatum.item.currentLocationName &&
aDatum.item.currentLocationName.indexOf(aDatum.item.shelfA) < 0 && aDatum.item.shelfA + "-" + aDatum.item.shelfB + "-" + aDatum.item.shelfC }</Text>
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun  FolderCard(navController: NavController?, data: FolderModel, aShowGeigerBar : Boolean = false){
   ElevatedCard(modifier=Modifier.combinedClickable (
      onClick = {},
      onLongClick = {
         if (navController != null) {

            val topLevelNavOptions = navOptions {
               // Pop up to the start destination of the graph to
               // avoid building up a large stack of destinations
               // on the back stack as users select items
               popUpTo(navController.graph.findStartDestination().id) {
                  saveState = true
               }
               // Avoid multiple copies of the same destination when
               // reselecting the same item
               launchSingleTop = true
            }
            var seriesId = data.folderSeriesId
            if(seriesId.length == 1){
               seriesId = "0${seriesId}"
            }

            navController.navigateToLocate("${data.barcode}-${seriesId}", topLevelNavOptions)
         }
      }
   ),elevation = CardDefaults.cardElevation(1.dp)) {
      Row{
         Column(modifier = Modifier.padding(16.dp)) {
            val title = if(aShowGeigerBar) "${data.folderSeriesName} Folder (${data.count.roundToInt()})"  else "${data.folderSeriesName} Folder"
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyy")
            Text( title,
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Right)
            Text("#${data.folderNumber.toString()} - ID : ${data.barcode}",
                  modifier= Modifier.padding(vertical = 8.dp),
                  style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(Modifier.padding(4.dp))
            Text("Date: ${data.fromDate?.let { Instant.fromEpochMilliseconds(it.toLong())
               .toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().format(formatter)}}" +
                  if(data.toDate != null) " to ${data.toDate.let {Instant.fromEpochMilliseconds(it.toLong()).toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().format(formatter)}}" else "",
                  style = MaterialTheme.typography.bodyMedium)
            Text("TYPE: ${data.folderTypeName}",
                  style = MaterialTheme.typography.bodyMedium)
               if(data.currentOrgId == LazilyParsedNumber("1")) {
                  Text("LOCATION: ${data.currentLocationName}",
                        style = MaterialTheme.typography.bodyMedium)
                  Text("ROW: ${data.shelfA} COLUMN:${data.shelfB} SHELF: ${data.shelfC}",
                        style = MaterialTheme.typography.bodyMedium)
               } else {
                  Text("FOLDER IN ORG: ${data.currentOrgName}", style = MaterialTheme.typography.bodyMedium)
               }

            if(aShowGeigerBar){
               val percent : Float by animateFloatAsState(data.rssi / dBuV_dBm,
                  label = "percentAnimate"
               )
               Spacer(modifier=Modifier.padding(8.dp))
               LinearProgressIndicator(
                  color= if( data.rssi.roundToInt().mod(2) == 0 ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  modifier= Modifier.fillMaxWidth(),
                  progress = percent
               )
            }
         }
      }
   }
}

@Composable
@Preview
fun PreviewFolderCard(){
   val myFolderModel = FolderModel(id=2525,
      folderNumber = "3",
      barcode = "00030030001312",
      currentLocationName = "Annex",
      addoId = 0,
      currentLocationDate =   8888,
      currentLocationFullName = "Annex",
      currentLocationId = 0,
      currentOrgId = 1,
      currentOrgName = "FSO",
      currentParentId = 0,
      currentParentName = "Warehouse",
      folderSeriesId = "1",
      folderSeriesName = "Non-Confidential",
      folderTypeId = 1,
      folderTypeName = "PC Folder",
      familyName = "Snow",
      fromDate =  949381200000,
      fullName = "John Snow",
      givenName = "John",
      loggedBy = "Hovaness",
      maidenName = "",
      persId = "asdasd",
      shelfA = "",
      shelfB = "",
      shelfC = "",
      toDate =   949381200000,
      warehouseLocationId = 0,
      warehouseLocationName = "",
      warehouseParentId = 123,
      rssi = 75.0f,
   )

   IgniteFolderScannerTheme {
      FolderCard(NavController(LocalContext.current),data=myFolderModel)
   }
}