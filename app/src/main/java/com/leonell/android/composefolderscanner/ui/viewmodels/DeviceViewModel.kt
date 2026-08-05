package com.leonell.android.composefolderscanner.ui.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.lifecycle.ViewModel
import com.leonell.android.composefolderscanner.services.SERVICE_UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val SCAN_PERIOD = 20000L

private fun deviceScanUiState(): DeviceUiState {
   return DeviceUiState.Ready
}


sealed interface DeviceUiState {
   object Ready : DeviceUiState
   object Scanning : DeviceUiState
   class  Results(val scanResults: Map<String, BluetoothDevice>) : DeviceUiState
   class  Error(val message: String) : DeviceUiState
   object AdvertisementNotSupported : DeviceUiState
   object BluetoothNotSupported : DeviceUiState
}


@SuppressLint("MissingPermission")
class DeviceViewModel : ViewModel(){
   private val mutableUiState = MutableStateFlow(deviceScanUiState())
   val viewState : StateFlow<DeviceUiState> = mutableUiState.asStateFlow()
   private var adapter: BluetoothAdapter? = null
   private var scanner: BluetoothLeScanner? = null
   private val scanResults = mutableMapOf<String, BluetoothDevice>()
   private var scanCallback: DeviceScanCallback? = null

   // The no-arg Handler() is deprecated and throws on a thread without a Looper. Holding one
   // instance also lets the timeout be cancelled, which the old code could not do -- a stale
   // callback would stop a scan the user had just restarted.
   private val handler = Handler(Looper.getMainLooper())
   private val stopScanRunnable = Runnable { stopScanning() }
   private lateinit var scanFilters: List<ScanFilter>
   private lateinit var scanSettings: ScanSettings
   fun initAdapter(app: Context)  {
      val btManager  = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
      adapter = btManager.adapter
   }
   override fun onCleared() {
      super.onCleared()
      stopScanning()
   }

   fun startScan() {
      scanFilters = buildScanFilters()
      scanSettings = buildScanSettings()
      if (adapter == null){
         mutableUiState.update { DeviceUiState.BluetoothNotSupported }
         return
      } else if (!adapter!!.isMultipleAdvertisementSupported) {
         mutableUiState.update { DeviceUiState.AdvertisementNotSupported }
         return
      }

      if (scanCallback == null && adapter != null) {
         scanner = adapter!!.bluetoothLeScanner
         mutableUiState.update { DeviceUiState.Scanning }
         handler.postDelayed(stopScanRunnable, SCAN_PERIOD)

         scanCallback = DeviceScanCallback()
         scanner?.startScan(scanFilters, scanSettings, scanCallback)
      }
   }

   private fun stopScanning() {
      handler.removeCallbacks(stopScanRunnable)
      scanner?.stopScan(scanCallback)
      scanCallback = null
      mutableUiState.update { DeviceUiState.Results(scanResults)}
   }

   private fun buildScanFilters(): List<ScanFilter> {
      val builder = ScanFilter.Builder()
      builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
      val filter = builder.build()
      return listOf(filter)
   }

   private fun buildScanSettings(): ScanSettings {
      return ScanSettings.Builder()
         .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
         .build()
   }

   private inner class DeviceScanCallback : ScanCallback() {
      override fun onBatchScanResults(results: List<ScanResult>) {
         super.onBatchScanResults(results)
         for (item in results) {
            item.device?.let { device ->
               scanResults[device.address] = device
            }
         }
         mutableUiState.update { DeviceUiState.Results(scanResults)}
      }

      override fun onScanResult(
         callbackType: Int,
         result: ScanResult
      ) {
         super.onScanResult(callbackType, result)
         result.device?.let { device ->
            scanResults[device.address] = device
         }
         mutableUiState.update { DeviceUiState.Results(scanResults)}
      }

      override fun onScanFailed(errorCode: Int) {
         super.onScanFailed(errorCode)
         val errorMessage = "Scan failed with error: $errorCode"
         mutableUiState.update { DeviceUiState.Error(errorMessage)}
      }
   }
}