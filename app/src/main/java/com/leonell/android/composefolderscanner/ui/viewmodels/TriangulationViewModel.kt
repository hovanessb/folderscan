package com.leonell.android.composefolderscanner.ui.viewmodels

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.database.SensorDataRepository
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.models.SensorData
import com.leonell.android.composefolderscanner.models.asSensorDataEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TriangulationViewModel @Inject constructor(
   private val sensorDataRepo : SensorDataRepository
) : ViewModel() {

   private var sensorManager: SensorManager? = null

   private val mutableSensorData = MutableStateFlow<SensorData?>(null)
   val sensorData: StateFlow<SensorData?> = mutableSensorData.asStateFlow()

   private var collectJob: Job? = null

   suspend fun getSenses(app: Context) = withContext(Dispatchers.Main) {
      // Idempotent: this is called from a LaunchedEffect that can re-run, and stacking
      // collectors would multiply every sensor sample.
      if (collectJob?.isActive == true) return@withContext
      val manager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
      sensorManager = manager
      sensorDataRepo.setSensorManager(manager)
      collectJob = viewModelScope.launch {
         sensorDataRepo.getAccelerometerDataFlow().collect { mutableSensorData.value = it }
      }
   }

   /** Records the reader's signal alongside the pose the device was in when it read. */
   fun saveRfidSensorData(sensorData: SensorData, rfidData: ScanModel) {
      // Copy rather than mutate: this instance is the one currently published on the flow.
      val sample = sensorData.copy(rssi = rfidData.rssi, phase = rfidData.phase)
      viewModelScope.launch {
         sensorDataRepo.upsertSensorEntities(listOf(sample.asSensorDataEntity()))
      }
   }
   fun resetSensorData(){
      sensorDataRepo.resetSensorHistory()
   }

   fun deleteSessions() {
      viewModelScope.launch {
         sensorDataRepo.deleteSessions()
      }
   }

}