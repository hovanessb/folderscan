package com.leonell.android.composefolderscanner.ui.screens

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.services.FolderIcons
import com.leonell.android.composefolderscanner.services.FolderLoggerWorker
import com.leonell.android.composefolderscanner.ui.components.Barcodes
import com.leonell.android.composefolderscanner.ui.components.FolderLoadingWheel
import com.leonell.android.composefolderscanner.ui.components.LocationHeader
import com.leonell.android.composefolderscanner.ui.components.SelectLocations
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunState
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.FolderLocationViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel

/**
 * Folder series accepted while logging.
 *
 * Everything else on the shelf (staging, personnel) is ignored so a sweep does not pick up
 * folders that are not being inventoried.
 */
private val RFID_SERIES_FILTER = setOf(10L, 11L)

/** Barcodes starting with these digits identify a location, not a folder. */
private val LOCATION_BARCODE_PREFIXES = listOf("1", "2")

private const val MIN_BARCODE_LENGTH = 12

@Composable
internal fun ScanRoute(
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   folderLocationModel: FolderLocationViewModel = hiltViewModel(),
) {
   ScanScreen(
      folderLocationModel = folderLocationModel,
      scanModel = scanModel,
      convergenceModel = convergenceModel,
   )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScanScreen(
   folderLocationModel: FolderLocationViewModel,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
) {
   val snackbarHostState = remember { SnackbarHostState() }
   val folderLocationsState by folderLocationModel.uiState.collectAsStateWithLifecycle()
   val currentLocationUiState by folderLocationModel.currentLocationState.collectAsStateWithLifecycle()
   val scanState by scanModel.uiState.collectAsStateWithLifecycle()
   val connectedGun by convergenceModel.uiState.collectAsStateWithLifecycle()
   val folderLocations by folderLocationModel.locations.collectAsStateWithLifecycle()

   val tabState = remember { mutableIntStateOf(0) }
   val openDialog = remember { mutableStateOf(false) }
   val listState = rememberLazyListState()
   val view = LocalView.current
   val context = LocalContext.current
   val lifecycleOwner = LocalLifecycleOwner.current
   // Keyboard-wedge scanners deliver a barcode one keystroke at a time; buffer until Enter.
   val wedgeBuffer = remember { StringBuilder() }

   val folderLocation = currentLocationUiState.location
   // Read inside the effects below without making them restart every time it changes.
   val currentLocation = rememberUpdatedState(folderLocation)

   LaunchedEffect(scanState.folders.size) {
      if (scanState.folders.isNotEmpty()) listState.scrollToItem(0)
   }

   /*
    RFID tag batches from the handheld reader.

    All of this used to run *during composition*: the screen read the newest tag out of the
    connection state, scanned the whole folder list linearly to de-duplicate it, then mutated
    the list and fired off a database write. Compose may run composition repeatedly or
    discard it, so scans were duplicated or dropped. Collecting the reader's batches in an
    effect keeps every side effect out of composition, and the de-duplication now happens in
    the ViewModel.
   */
   LaunchedEffect(Unit) {
      convergenceModel.tagReads.collect { reads ->
         val location = currentLocation.value ?: return@collect
         val added = scanModel.addRfidReads(
            reads = reads,
            locationId = location.id.toString(),
            seriesFilter = RFID_SERIES_FILTER,
         )
         if (added > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            FolderLoggerWorker.initLogger(context)
         }
      }
   }

   // Single barcode reads, from the Zebra imager or the CS108's barcode module.
   LaunchedEffect(scanState.barcode) {
      val barcode = scanState.barcode ?: return@LaunchedEffect
      if (barcode.barcodeId.length < MIN_BARCODE_LENGTH) return@LaunchedEffect
      scanModel.clearBarcode()

      if (isLocationBarcode(barcode.barcodeId, tabState.intValue)) {
         folderLocationModel.lookup(barcode)
         if (!openDialog.value) tabState.intValue = 0
         return@LaunchedEffect
      }

      val location = currentLocation.value
      if (location == null) {
         snackbarHostState.showSnackbar(
            message = "No scanning location currently selected.",
            duration = SnackbarDuration.Short,
         )
         return@LaunchedEffect
      }

      if (scanModel.addBarcodeRead(barcode, location.id.toString())) {
         view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
         FolderLoggerWorker.initLogger(context)
      }
   }

   /*
    Mark folders the upload worker has confirmed.

    The observer used to be attached inside composition, so a new one was registered on every
    recomposition and none were ever removed. It also queried by tag using the unique *work
    name*, which is not a tag, so nothing ever matched.
   */
   DisposableWorkInfoObserver(lifecycleOwner = lifecycleOwner) { infos ->
      val uploaded = infos
         .filter { it.state == WorkInfo.State.SUCCEEDED }
         .flatMap {
            it.outputData.getStringArray(FolderLoggerWorker.KEY_UPLOADED_BARCODES)
               ?.toList()
               .orEmpty()
         }
         .toSet()
      scanModel.markLogged(uploaded)
   }

   Column(
      modifier = Modifier.onInterceptKeyBeforeSoftKeyboard { keyEvent ->
         handleKeyboardWedge(keyEvent, wedgeBuffer, scanModel)
      },
   ) {
      TabRow(selectedTabIndex = tabState.intValue) {
         listOf("Scan", "Locations").forEachIndexed { index, title ->
            Tab(
               selected = tabState.intValue == index,
               onClick = { tabState.intValue = index },
               text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            )
         }
      }

      AnimatedContent(
         targetState = tabState.intValue,
         transitionSpec = {
            if (targetState > initialState) {
               slideInHorizontally { width -> width } togetherWith
                  slideOutHorizontally { width -> -width }
            } else {
               slideInHorizontally { width -> -width } togetherWith
                  slideOutHorizontally { width -> width }
            }
         },
         label = "ScanTabs",
      ) { tab ->
         when (tab) {
            0 -> Scaffold(
               floatingActionButton = {
                  if (scanState.folders.isNotEmpty() ||
                     connectedGun == ConvergenceHandgunState.Scanning ||
                     connectedGun == ConvergenceHandgunState.Busy
                  ) {
                     FloatingActionButton(onClick = scanModel::clearBarcodes) {
                        AnimatedContent(targetState = connectedGun, label = "ScanStatus") { state ->
                           when (state) {
                              ConvergenceHandgunState.Busy ->
                                 FolderLoadingWheel(contentDesc = "busy...")

                              ConvergenceHandgunState.Scanning -> Text(text = "Scanning!")
                              else -> Icon(FolderIcons.DELETE, "Delete")
                           }
                        }
                     }
                  }
               },
               snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
               Column(
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(padding),
                  horizontalAlignment = Alignment.CenterHorizontally,
               ) {
                  LocationHeader(currentLocationUiState, scanState.folders.size.toString())
                  Barcodes(
                     barcodeList = scanState.folders,
                     aEmptyMessage = "SCAN A BARCODE",
                     listState = listState,
                  )
               }
            }

            else -> SelectLocations(
               folderLocationsState,
               folderLocationModel,
               openDialog,
               snackbarHostState,
               folderLocations,
               currentLocationUiState,
               tabState,
               scanModel,
               folderLocation,
            )
         }
      }
   }
}

/**
 * True when a scanned barcode identifies a location rather than a folder.
 *
 * Either the operator is already on the Locations tab, or the barcode carries a location
 * prefix.
 */
private fun isLocationBarcode(barcodeId: String, currentTab: Int): Boolean =
   currentTab == 1 || LOCATION_BARCODE_PREFIXES.any { barcodeId.startsWith(it) }

/**
 * Accumulates digits from a USB or Bluetooth keyboard-wedge scanner until it sends Enter.
 *
 * Returns false so the key event still reaches the rest of the UI.
 */
private fun handleKeyboardWedge(
   keyEvent: ComposeKeyEvent,
   buffer: StringBuilder,
   scanModel: ScanViewModel,
): Boolean {
   if (keyEvent.type != KeyEventType.KeyUp) return false

   when {
      keyEvent.nativeKeyEvent.keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ->
         buffer.append(
            KeyEvent.keyCodeToString(keyEvent.key.nativeKeyCode).substringAfterLast('_'),
         )

      keyEvent.key == Key.Enter -> {
         val scanned = buffer.toString()
         buffer.setLength(0)
         // Route it through the same path as every other single barcode read, so the
         // location/folder decision lives in exactly one place.
         if (scanned.isNotEmpty()) scanModel.setBarcodeScan(ScanModel(barcodeId = scanned))
      }
   }
   return false
}

/** Observes the upload worker exactly once, releasing the observer with the composition. */
@Composable
private fun DisposableWorkInfoObserver(
   lifecycleOwner: LifecycleOwner,
   onChanged: (List<WorkInfo>) -> Unit,
) {
   val context = LocalContext.current
   val currentOnChanged by rememberUpdatedState(onChanged)

   DisposableEffect(lifecycleOwner) {
      val liveData = WorkManager.getInstance(context)
         .getWorkInfosForUniqueWorkLiveData(FolderLoggerWorker.WORK_NAME)
      val observer = Observer<List<WorkInfo>> { infos -> currentOnChanged(infos.orEmpty()) }
      liveData.observe(lifecycleOwner, observer)
      onDispose { liveData.removeObserver(observer) }
   }
}
