package com.leonell.android.composefolderscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.util.Consumer
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.services.DataWedgeService
import com.leonell.android.composefolderscanner.services.NetworkMonitor
import com.leonell.android.composefolderscanner.ui.FolderApp
import com.leonell.android.composefolderscanner.ui.rememberFolderAppState
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.TriangulationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

   @Inject
   lateinit var networkMonitor: NetworkMonitor

   private val scanModel: ScanViewModel by viewModels()
   private val convergenceModel: ConvergenceHandgunViewModel by viewModels()
   private val triangulationModel: TriangulationViewModel by viewModels()

   /**
    * Handles DataWedge profiles still configured for "Start Activity" delivery.
    *
    * Registered once here rather than inside a composable: the previous code added a new
    * listener on every recomposition of a `LaunchedEffect` and never removed any, so scans
    * were delivered as many times as the effect had run.
    */
   private val newIntentListener = Consumer<Intent> { intent ->
      DataWedgeService.scanFromIntent(intent)?.let { scan ->
         scanModel.setBarcodeScan(ScanModel(barcodeId = scan.data))
      }
   }

   private val requestNotifications =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

   @SuppressLint("MissingPermission")
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      addOnNewIntentListener(newIntentListener)

      // Android 13 requires an explicit grant before WorkManager can surface upload progress.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
         requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
      }

      setContent {
         val appState = rememberFolderAppState(networkMonitor = networkMonitor)

         LaunchedEffect(Unit) {
            convergenceModel.startBleLibrary(applicationContext)
            DataWedgeService.createDWProfile(applicationContext)
            triangulationModel.getSenses(applicationContext)
         }

         // Broadcast delivery is the profile this app provisions; it reaches the app in any
         // navigation state and does not touch the activity stack.
         LaunchedEffect(Unit) {
            DataWedgeService.scans(applicationContext).collectLatest { scan ->
               scanModel.setBarcodeScan(ScanModel(barcodeId = scan.data))
            }
         }

         IgniteFolderScannerTheme(useDarkTheme = false) {
            FolderApp(
               appState = appState,
               scanModel = scanModel,
               convergenceModel = convergenceModel,
               triangulationModel = triangulationModel,
            )
         }
      }
   }

   override fun onDestroy() {
      removeOnNewIntentListener(newIntentListener)
      super.onDestroy()
   }
}
