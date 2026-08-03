package com.leonell.android.composefolderscanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonell.android.composefolderscanner.services.FolderLoggerWorker
import com.leonell.android.composefolderscanner.services.FolderNavHost
import com.leonell.android.composefolderscanner.ui.components.FolderBottomBar
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunState
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.TriangulationViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FolderApp(
   appState: FolderAppState,
   scanModel: ScanViewModel,
   convergenceModel: ConvergenceHandgunViewModel,
   triangulationModel: TriangulationViewModel,
) {
   val snackbarHostState = remember { SnackbarHostState() }
   val isOffline by appState.isOffline.collectAsStateWithLifecycle()
   val connectedGun by convergenceModel.uiState.collectAsStateWithLifecycle()
   val currentQueue by scanModel.getLoggingCount().collectAsStateWithLifecycle(initialValue = 0)
   val context = LocalContext.current

   LaunchedEffect(Unit) {
      FolderLoggerWorker.initLogger(context)
   }

   LaunchedEffect(isOffline) {
      if (isOffline) {
         snackbarHostState.showSnackbar(
            message = "Lost WiFi Connection",
            duration = SnackbarDuration.Indefinite,
         )
      }
   }

   LaunchedEffect(connectedGun) {
      when (connectedGun) {
         ConvergenceHandgunState.Disconnected -> snackbarHostState.showSnackbar(
            message = "Disconnected from Bluetooth device.",
            duration = SnackbarDuration.Long,
         )

         ConvergenceHandgunState.Busy -> snackbarHostState.showSnackbar(
            message = "Bluetooth device is busy, please wait.",
            duration = SnackbarDuration.Indefinite,
         )

         else -> Unit
      }
   }

   Scaffold(
      modifier = Modifier.semantics { testTagsAsResourceId = true },
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.onBackground,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
         FolderBottomBar(
            destinations = appState.navigations,
            onNavigateToDestination = appState::navigateToTopLevelDestination,
            currentDestination = appState.currentDestination,
            modifier = Modifier
               .testTag("FolderBottomBar")
               .height(80.dp),
            scanModel = scanModel,
            folderCount = currentQueue,
         )
      },
   ) { padding ->
      Row(
         Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
      ) {
         Column(Modifier.fillMaxSize()) {
            FolderNavHost(
               navController = appState.navController,
               scanModel = scanModel,
               convergenceModel = convergenceModel,
               triangulationModel = triangulationModel,
            )
         }
      }
   }
}
