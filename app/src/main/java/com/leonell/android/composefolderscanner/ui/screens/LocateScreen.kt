package com.leonell.android.composefolderscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonell.android.composefolderscanner.services.FolderIcons
import com.leonell.android.composefolderscanner.ui.components.Empty
import com.leonell.android.composefolderscanner.ui.components.Thermometer
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunState
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.LocateViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.TriangulationViewModel
import kotlinx.coroutines.delay

@Composable
internal fun LocateRoute(
   barcodeId: String?,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   triangulationModel: TriangulationViewModel,
   locateModel: LocateViewModel = hiltViewModel(),
) {
   LocateScreen(
      barcodeIdSeries = barcodeId,
      scanModel = scanModel,
      convergenceModel = convergenceModel,
      triangulationModel = triangulationModel,
      locateModel = locateModel,
   )
}

/**
 * Geiger search: point the reader at a shelf and follow the meter to one folder.
 */
@Composable
internal fun LocateScreen(
   barcodeIdSeries: String?,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   triangulationModel: TriangulationViewModel,
   locateModel: LocateViewModel,
) {
   val snackbarHostState = remember { SnackbarHostState() }
   val target by locateModel.target.collectAsStateWithLifecycle()
   val connectedGun by convergenceModel.uiState.collectAsStateWithLifecycle()
   val scanState by scanModel.uiState.collectAsStateWithLifecycle()
   val sensorData by triangulationModel.sensorData.collectAsStateWithLifecycle()

   // Target passed in on the route, e.g. from a folder tapped on the profile screen.
   LaunchedEffect(barcodeIdSeries) {
      val argument = barcodeIdSeries?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
      locateModel.selectTarget(argument.substringBefore("-"))
      convergenceModel.startSearch(argument.toByteArray(Charsets.US_ASCII))
   }

   // Target picked up by scanning the folder's barcode on this screen.
   LaunchedEffect(scanState.barcode) {
      val barcode = scanState.barcode ?: return@LaunchedEffect
      locateModel.selectTarget(barcode.barcodeId)
      barcode.rawRead?.let { convergenceModel.startSearch(it) }
   }

   /*
    Drive the meter from the reader's tag batches.

    RSSI arrives as an offset value; map it onto 0..1 across the useful dynamic range so the
    thermometer reads as "warmer/colder" rather than raw dBm.
   */
   LaunchedEffect(Unit) {
      convergenceModel.tagReads.collect { reads ->
         val strongest = reads.maxByOrNull { it.rssi } ?: return@collect
         sensorData?.let { triangulationModel.saveRfidSensorData(it, strongest) }

         val dbm = strongest.rssi - LocateViewModel.DB_CONSTANT
         val normalised = (dbm - LocateViewModel.SIGNAL_MIN_DBM) /
            (LocateViewModel.SIGNAL_MAX_DBM - LocateViewModel.SIGNAL_MIN_DBM)
         locateModel.updateSignal(normalised)
      }
   }

   // Bleed the meter down between reads so a stale peak does not read as a live one.
   LaunchedEffect(target.signal) {
      if (target.signal <= 0f) return@LaunchedEffect
      delay(250)
      locateModel.decaySignal()
   }

   // Leaving the screen puts the reader back into bulk-inventory mode.
   DisposableEffect(Unit) {
      onDispose { convergenceModel.stopSearch() }
   }

   Scaffold(
      floatingActionButton = {
         if (target.displayName.isNotEmpty() || connectedGun == ConvergenceHandgunState.Scanning) {
            FloatingActionButton(
               onClick = {
                  scanModel.clearGeiger()
                  triangulationModel.deleteSessions()
                  locateModel.clear()
               },
            ) {
               Icon(FolderIcons.DELETE, "Delete")
            }
         }
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
   ) { padding ->
      GeigerBody(
         paddingValues = padding,
         percentState = target.signal,
         name = target.displayName,
         searching = connectedGun == ConvergenceHandgunState.Scanning,
      )
   }
}

@Composable
private fun GeigerBody(
   paddingValues: PaddingValues,
   percentState: Float,
   name: String,
   searching: Boolean,
) {
   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(paddingValues),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Column(
         modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
         horizontalAlignment = Alignment.CenterHorizontally,
      ) {
         Text(
            "SEARCH FOR RFIDS",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
         )
      }
      HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

      Box(
         modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
      ) {
         if (name.isEmpty()) {
            Empty(Modifier)
            return@Box
         }

         Text(
            text = "$name - ${(percentState * 100).toInt()}%",
            modifier = Modifier
               .fillMaxSize()
               .align(Alignment.Center),
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
         )
         if (!searching) {
            Empty(Modifier, "BEGIN SEARCH")
         }
         Thermometer(
            modifier = Modifier
               .padding(20.dp)
               .fillMaxSize(),
            percentState,
            triggered = searching,
         )
      }
   }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLocateScreen() {
   IgniteFolderScannerTheme {
      GeigerBody(PaddingValues(16.dp), 0.75f, "Test Name", searching = true)
   }
}
