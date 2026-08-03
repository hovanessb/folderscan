package com.leonell.android.composefolderscanner.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.leonell.android.composefolderscanner.ui.components.*
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunState
import com.leonell.android.composefolderscanner.ui.viewmodels.ConvergenceHandgunViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.DeviceUiState
import com.leonell.android.composefolderscanner.ui.viewmodels.DeviceViewModel

private const val TAG = "DeviceScanCompose"


@Composable
internal fun DeviceRoute(modifier: Modifier = Modifier,
                         viewModel: DeviceViewModel = hiltViewModel(),
                         convergenceModel : ConvergenceHandgunViewModel = hiltViewModel(),) {
   val deviceState by viewModel.viewState.collectAsStateWithLifecycle()
   DeviceScreen(deviceState = deviceState, modifier = modifier, viewModel =viewModel, convergenceModel = convergenceModel)
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
internal fun DeviceScreen(
   deviceState: DeviceUiState,
   modifier: Modifier = Modifier,
   viewModel: DeviceViewModel = hiltViewModel(),
   convergenceModel : ConvergenceHandgunViewModel = hiltViewModel(),
) {
   val connectedGun by convergenceModel.uiState.collectAsStateWithLifecycle()
   val current = LocalContext.current
   val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      rememberMultiplePermissionsState(permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
         )
      )
   } else {
      rememberMultiplePermissionsState(permissions = listOf(
         Manifest.permission.ACCESS_COARSE_LOCATION,
         Manifest.permission.ACCESS_FINE_LOCATION,
         Manifest.permission.BLUETOOTH,
         Manifest.permission.BLUETOOTH_ADMIN,
      ))
   }

   LaunchedEffect(key1 = 1){
      viewModel.initAdapter(current)
   }

   Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {

      Text("Bluetooth Connections", modifier.padding(16.dp),style = MaterialTheme.typography.titleLarge)

      HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

      when (deviceState) {
         DeviceUiState.Ready -> {
            if(!permissionState.allPermissionsGranted){
               Button(modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                     onClick = {permissionState.launchMultiplePermissionRequest()},
                     content={ Text(text = "Grant Permissions") })
            } else {
               Scanning(viewModel)
            }
         }

         is DeviceUiState.BluetoothNotSupported -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
               Text(
                  text = "Bluetooth is not supported on this device",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Light
               )
            }
         }

         is DeviceUiState.Scanning -> {
            Loading()
            Spacer(modifier = Modifier.height(15.dp))
            Text(
               text = "Scanning for devices",
               fontSize = 15.sp,
               fontWeight = FontWeight.Light
            )
         }

         is DeviceUiState.Results -> if (deviceState.scanResults.isNotEmpty()) {
            val disconnected = connectedGun is ConvergenceHandgunState.Disconnected || connectedGun is ConvergenceHandgunState.NeverConnected
            val busy = connectedGun is ConvergenceHandgunState.Busy
            val currentContainerColor = if (disconnected)  {
               MaterialTheme.colorScheme.background
            } else if(busy) {
               MaterialTheme.colorScheme.secondaryContainer
            }  else MaterialTheme.colorScheme.primary
            val currentColor  = if (disconnected)  {
               MaterialTheme.colorScheme.onBackground
            } else if(busy) {
               MaterialTheme.colorScheme.onSecondaryContainer
            } else {
               MaterialTheme.colorScheme.onPrimary
            }

            val view = LocalView.current

            LazyColumn(
               modifier = Modifier
                  .padding(10.dp)
                  .fillMaxWidth()
            ) {
               itemsIndexed(deviceState.scanResults.keys.toList()) { _, key ->
                  val name = deviceState.scanResults[key]?.name ?: "Unknown Device"
                  val title = if (disconnected) name else if(busy) "Connecting..." else "Connected "
                  val device: BluetoothDevice = deviceState.scanResults.get(key = key)!!
                  Column(modifier = modifier) {
                     Row(horizontalArrangement = Arrangement.SpaceBetween){
                        Column(modifier = Modifier
                           .clickable {
                              if (convergenceModel.currentDevice?.address != device.address) {
                                 convergenceModel.setCurrentConnection(device = device)
                                 view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                              } else {
                                 convergenceModel.disconnect()
                              }
                           }
                           .background(
                              currentContainerColor,
                              shape = RoundedCornerShape(10.dp)
                           )
                           .fillMaxWidth()
                           .border(1.dp, Color.Black, shape = RoundedCornerShape(10.dp))
                           .padding(5.dp)
                        ) {
                           Text(text=title, color=currentColor)
                           Text( text = deviceState.scanResults[key]?.address ?: "",
                                 fontWeight = FontWeight.Light,
                                 color=currentColor)
                        }
                     }
                  }
               }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally){
               if (busy) {
                  FolderLoadingWheel(contentDesc = "Please wait while this fully connects.")
               } else if(!disconnected) {
                  val isRfidOnFlow by convergenceModel.isRfidOn.collectAsStateWithLifecycle(initialValue = false,lifecycle = LocalLifecycleOwner.current.lifecycle)
                  val isBarcodeOnFlow by convergenceModel.isBarcodeOn.collectAsStateWithLifecycle(initialValue = false,lifecycle =LocalLifecycleOwner.current.lifecycle)
                  val batteryLevelFlow by convergenceModel.batteryLevel.collectAsStateWithLifecycle(initialValue = (0).toFloat(),lifecycle =LocalLifecycleOwner.current.lifecycle)
                  val currentPowerLevel = remember{ mutableIntStateOf(convergenceModel.getRfidPowerLevel().toInt()) }
                  val tagPopulation = remember{ mutableIntStateOf(convergenceModel.getPopulation()) }
                  val batteryLevel: Float by animateFloatAsState(
                     targetValue = batteryLevelFlow / 100,
                     animationSpec = tween(300, 100, FastOutSlowInEasing),
                     label = "battery"
                  )

                  Text(
                     text = "Battery Level: ${batteryLevelFlow.toInt()}%",
                     style = MaterialTheme.typography.titleMedium
                  )
                  Spacer(modifier=Modifier.padding(8.dp))
                  LinearProgressIndicator(
                     modifier= Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                     progress = { batteryLevel })
                  Spacer(modifier=Modifier.padding(8.dp))
                  Text(
                     text = "RFID Power Level: ${currentPowerLevel.intValue}",
                     style = MaterialTheme.typography.titleMedium
                  )
                  Spacer(modifier=Modifier.padding(4.dp))
                  Slider(currentPowerLevel.intValue.toFloat(),
                         onValueChange = {currentPowerLevel.intValue = it.toInt()},
                         valueRange = (0).toFloat()..(300).toFloat(),
                         modifier= Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                         onValueChangeFinished = {convergenceModel.setRfidPowerLevel(currentPowerLevel.intValue)})
                  Spacer(modifier=Modifier.padding(8.dp))
                  Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically){
                     Text(
                        text = "RFID Module ",
                        style = MaterialTheme.typography.titleSmall
                     )
                     Switch(checked = isRfidOnFlow, onCheckedChange = convergenceModel::setRfidModule)
                     Spacer(modifier=Modifier.padding(8.dp))
                     Text(
                        text = "Barcode Module ",
                        style = MaterialTheme.typography.titleSmall
                     )
                     Switch(checked = isBarcodeOnFlow, onCheckedChange = convergenceModel::setBarcodeModule)
                  }

                  Spacer(modifier=Modifier.padding(16.dp))
                  Text(
                     text = "Average Number of RFIDs to Read Per Scan: ${tagPopulation.intValue}",
                     style = MaterialTheme.typography.titleSmall
                  )
                  Spacer(modifier=Modifier.padding(4.dp))
                  Column(modifier=Modifier.padding(horizontal = 16.dp)){
                     Text(
                        text = "Current RFID Search Mode: ",
                        style = MaterialTheme.typography.titleSmall
                     )
                     Spacer(modifier=Modifier.padding(4.dp))
                     Text(text=convergenceModel.getInvAlgo(), style=MaterialTheme.typography.bodySmall)
                  }
               }
            }
         }
         is DeviceUiState.Error -> {
            Text(text = deviceState.message)
         } else -> {
            Empty(modifier)
         }
      }
   }
}

@Composable
private fun Scanning(viewModel : DeviceViewModel){
   Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Button(modifier = Modifier
         .fillMaxWidth()
         .padding(16.dp), onClick = {viewModel.startScan()}, content={ Text(text = "Scan Devices")})
   }
}

@Preview
@Composable
private fun LoadingStatePreview() {
   IgniteFolderScannerTheme {
      Loading()
   }
}


@Preview
@Composable
private fun DeviceScanScreenPreview() {
   IgniteFolderScannerTheme {
      Loading()
   }
}
