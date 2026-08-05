package com.leonell.android.composefolderscanner.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.services.*
import com.leonell.android.composefolderscanner.ui.components.FolderLoadingWheel
import com.leonell.android.composefolderscanner.ui.components.Loading
import com.leonell.android.composefolderscanner.ui.components.LocationHeader
import com.leonell.android.composefolderscanner.ui.components.SelectLocations
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunState
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.FolderLocationUiState
import com.leonell.android.composefolderscanner.ui.viewmodels.FolderLocationViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import kotlinx.coroutines.launch

@Composable
@ExperimentalAnimationApi
internal fun WriteRoute(
   scanModel: ScanViewModel = hiltViewModel(),
   folderLocationModel: FolderLocationViewModel = hiltViewModel(),
   convergenceModel: ConvergenceHandgunViewModel = hiltViewModel()
) {
   WriteScreen(folderLocationModel = folderLocationModel, scanModel = scanModel, convergenceModel = convergenceModel)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@ExperimentalAnimationApi
internal fun WriteScreen(
   folderLocationModel: FolderLocationViewModel,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel
) {
   val snackbarHostState = remember { SnackbarHostState() }
   val connectedGun by convergenceModel.uiState.collectAsStateWithLifecycle()
   val barcodeData by scanModel.uiState.collectAsStateWithLifecycle()
   val emptyArray = ByteArray(16)
   val readBarcode = remember { mutableStateOf(emptyArray) }
   val writeBarcode = remember { mutableStateOf(emptyArray) }
   val readBarcodeFolderType = remember { mutableLongStateOf(0L) }
   val writeBarcodeFolderType = remember { mutableLongStateOf(0L) }
   val activeField = remember { mutableStateOf(true) }
   val openDialog = remember { mutableStateOf(false) }
   val tabState = remember { mutableIntStateOf(0) }
   val selectedReadFolderSeries = remember { mutableStateOf("CF") }
   val selectedWriteFolderSeries = remember { mutableStateOf("CF")}
   val folderLocationsState by folderLocationModel.uiState.collectAsStateWithLifecycle()
   val titles = listOf("Rewrite RFIDs", "Locations")
   val folderLocations by folderLocationModel.locations.collectAsStateWithLifecycle()
   val currentLocationUiState by folderLocationModel.currentLocationState.collectAsStateWithLifecycle()
   val folderLocation : BarcodeEntityModel.FolderLocation? =  currentLocationUiState.location
   val coroutineScope = rememberCoroutineScope()

   val folderSeries = listOf("CF","Prospect","Ethics","Solo","PC")


   /*
    Apply the newest RFID read to whichever field is focused.

    This used to run during composition, reading the tag out of the connection state. The
    reader now publishes batches on its own flow, and applying them in an effect keeps the
    writes out of the composition path.
   */
   LaunchedEffect(Unit) {
      convergenceModel.tagReads.collect { reads ->
         val rfidData = reads.lastOrNull { it.rawRead != null && it.barcodeId.length >= 12 }
            ?: return@collect

         if (activeField.value) {
            readBarcode.value = rfidData.rawRead!!
            rfidData.seriesId?.let { series ->
               readBarcodeFolderType.longValue = series
               selectedReadFolderSeries.value =
                  convergenceModel.folderSeriesIdToName(series.toInt())
            }
         } else {
            writeBarcode.value = rfidData.rawRead!!
               .toString(Charsets.US_ASCII)
               .substringBefore("-")
               .toByteArray(Charsets.US_ASCII)
            rfidData.seriesId?.let { series ->
               writeBarcodeFolderType.longValue = series
               selectedWriteFolderSeries.value =
                  convergenceModel.folderSeriesIdToName(series.toInt())
                     .take(2)
                     .uppercase()
            }
         }
      }
   }

   // A barcode scanned on the device fills the write field directly.
   LaunchedEffect(barcodeData.barcode) {
      val scanned = barcodeData.barcode ?: return@LaunchedEffect
      if (!activeField.value) {
         writeBarcode.value = scanned.rawRead ?: scanned.barcodeId.toByteArray(Charsets.US_ASCII)
      }
   }

   Column {
      TabRow(selectedTabIndex = tabState.intValue) {
         titles.forEachIndexed { index, title ->
            Tab(
               selected = tabState.intValue == index,
               onClick = { tabState.intValue = index},
               text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            )
         }
      }

      AnimatedContent(targetState = tabState.intValue,
         transitionSpec = {
            if(tabState.intValue > 0){
               slideInHorizontally{width -> width} togetherWith slideOutHorizontally { width-> -width }
            } else {
               slideInHorizontally{width -> -width} togetherWith slideOutHorizontally { width-> width }
            }}, label = ""
      ) { myState ->
         when(myState){
            0 -> {
               Scaffold(
                  floatingActionButton = {
                     if (!readBarcode.value.contentEquals(emptyArray) ||
                         !writeBarcode.value.contentEquals(emptyArray) ||
                          connectedGun == ConvergenceHandgunState.Scanning) {
                        FloatingActionButton(onClick = {
                           readBarcode.value = emptyArray
                           writeBarcode.value = emptyArray
                        }) {
                           AnimatedContent(targetState = connectedGun, label = "") { myGunState ->
                              when (myGunState) {
                                 ConvergenceHandgunState.Scanning -> {
                                    Text(text = "Scanning!")
                                 }
                                 is ConvergenceHandgunState.Writing -> {
                                    FolderLoadingWheel(contentDesc = "Writing...")
                                 }
                                 else -> {
                                    Icon(FolderIcons.DELETE, "Delete")
                                 }
                              }
                           }
                        }
                     }
                  },
                  snackbarHost = { SnackbarHost(snackbarHostState) })
               {
                  Column(
                  modifier = Modifier.fillMaxSize(),
                  horizontalAlignment = Alignment.CenterHorizontally
               )  {
                     LocationHeader(currentLocationUiState, "")
                     Row( Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                           value=readBarcode.value.toString(Charsets.US_ASCII).split("-").first().trim(),
                           onValueChange={},
                           readOnly=true,
                           placeholder= {Text("Select RFID by Scanning")},
                           modifier = Modifier.onFocusEvent { activeField.value=true},
                           colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                           leadingIcon = { Icon(Icons.Default.Search, "Read Data") })
                        Spacer(Modifier.width(16.dp))
                        Text(text=selectedReadFolderSeries.value)
                     }
                     Row( Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                           value = writeBarcode.value.toString(Charsets.US_ASCII).split("-").first().trim(),
                           placeholder={Text("Rewrite To")},
                           onValueChange = {changed ->writeBarcode.value = changed.split("-").first().trim().toByteArray(Charsets.US_ASCII)},
                           modifier = Modifier
                              .onFocusEvent { activeField.value = false },
                           colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                           leadingIcon =  {Text( selectedWriteFolderSeries.value.uppercase())})

                        Spacer(Modifier.width(16.dp))
                        MyDrop(folderSeries, selectedWriteFolderSeries)
                     }

                     Row(modifier=Modifier.padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly){
                        ElevatedButton(enabled = !writeBarcode.value.contentEquals(emptyArray) && !readBarcode.value.contentEquals(emptyArray),
                                       onClick = { openDialog.value = true }) {
                                                   Text("Confirm")
                                                 }
                        Spacer(Modifier.width(16.dp))
                        ElevatedButton(enabled = !writeBarcode.value.contentEquals(emptyArray) && folderLocation != null,
                           onClick = {
                              val barcode = writeBarcode.value.toString(Charsets.US_ASCII)
                              folderLocationModel.logOneLocation(barcode, folderLocation!!.id as Long)
                              coroutineScope.launch {
                                 when (folderLocationsState) {
                                    is FolderLocationUiState.Error -> {
                                          snackbarHostState.showSnackbar(
                                             message = (folderLocationsState as FolderLocationUiState.Error).message,
                                             duration = SnackbarDuration.Short
                                          )
                                    }
                                    is FolderLocationUiState.Success -> {
                                          snackbarHostState.showSnackbar(
                                             message = "Recorded as completed",
                                             duration = SnackbarDuration.Short
                                          )
                                       }
                                    else -> {}
                                 }
                              }
                           })
                        {
                           Text("Log Folder")
                        }
                     }

                     Row(modifier=Modifier.padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly){
                         if(writeBarcode.value.contentEquals(readBarcode.value) &&  !writeBarcode.value.contentEquals(emptyArray)){
                            Text("Match!", textAlign = TextAlign.Center, color =  MaterialTheme.colorScheme.primary)
                         }
                     }
                     if(openDialog.value && !writeBarcode.value.contentEquals(emptyArray) ){
                        AlertDialog(
                           onDismissRequest = {
                              openDialog.value = false
                           },
                           icon = { Icon(FolderIcons.ADD, "Add") },
                           title = { Text(text = "Write to RFID?", textAlign = TextAlign.Center) },
                           text = {
                              Column(
                                 modifier = Modifier.padding(10.dp),
                                 verticalArrangement = Arrangement.SpaceEvenly
                              ) {
                                 Text("You are about to overwrite.")
                                 Text(readBarcode.value.toString(Charsets.US_ASCII) + " " + selectedReadFolderSeries.value)
                                 Text("To:")
                                 Text(writeBarcode.value.toString(Charsets.US_ASCII) + " " + selectedWriteFolderSeries.value)
                              }
                           },
                           confirmButton = {
                              TextButton(
                                 onClick = {
                                    coroutineScope.launch {
                                       openDialog.value = false
                                       val resultMessage = convergenceModel.writeRfidData(readBarcode.value, writeBarcode.value, selectedWriteFolderSeries.value)
                                       readBarcode.value = emptyArray
                                       writeBarcode.value = emptyArray
                                       snackbarHostState.showSnackbar(
                                          message = resultMessage,
                                          duration = SnackbarDuration.Short
                                       )
                                    }
                                 }
                              ) {
                                 Text("Write")
                              }
                           },
                           dismissButton = {
                              TextButton(
                                 onClick = {
                                    openDialog.value = false
                                 }
                              ) {
                                 Text("Cancel")
                              }
                           }
                        )
                     }
                  }
               }
            }
            1 -> {
               SelectLocations( folderLocationsState ,
                                folderLocationModel,
                                openDialog ,
                                snackbarHostState,
                                folderLocations,
                                currentLocationUiState,
                                tabState ,
                                scanModel,
                                folderLocation)
            }
         }
      }
   }
}

@Composable
fun MyDrop( myList : List<String>,  mSelectedText : MutableState<String>){
   // Declaring a boolean value to store
   // the expanded state of the Text Field
   var mExpanded by remember { mutableStateOf(false) }
   // Up Icon when expanded and down icon when collapsed

      // Create an Outlined Text Field
      // with icon and not expanded
      ElevatedButton(
         modifier = Modifier
            .height(30.dp)
            .padding(0.dp)
            .width(30.dp),
         onClick = { mExpanded = !mExpanded }
      ){
         if(mSelectedText.value.length >=2){
            mSelectedText.value = mSelectedText.value.substring(0,2).uppercase()
         }
         Text( text =  mSelectedText.value,
               fontSize = 3.sp,
               fontWeight = FontWeight.Light)
      }

      // Create a drop-down menu with list of cities,
      // when clicked, set the Text Field text as the city selected
      DropdownMenu(
         expanded = mExpanded,
         onDismissRequest = { mExpanded = false }
      ) {
         myList.forEach { label ->
            DropdownMenuItem(text= { Text(text = label) },onClick = {
               mSelectedText.value = label
               mExpanded = false
            })
         }
      }
}
@Preview
@Composable
fun myButton(){
   // with icon and not expanded
   ElevatedButton(
      onClick = {}
   ){
      Text( text =  "CF",
            fontSize = 10.sp,
            modifier =  Modifier.width(50.dp),
            fontWeight = FontWeight.Light)
   }
}