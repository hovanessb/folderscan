package com.leonell.android.composefolderscanner.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.database.LoggingQueueRepository
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.models.asLoggingQueueModel
import com.leonell.android.composefolderscanner.services.FolderLoggerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
   /** Most recent single barcode read, from a handheld imager or a keyboard wedge. */
   val barcode: ScanModel? = null,
   /** Folders accumulated during the current scanning session, newest first. */
   val folders: List<ScanModel> = emptyList(),
   val geigerFolders: List<BarcodeEntityModel.Person> = emptyList(),
)

@HiltViewModel
class ScanViewModel @Inject constructor(
   private val loggingQueueRepo: LoggingQueueRepository,
) : ViewModel() {

   private val mutableUiState = MutableStateFlow(ScanUiState())
   val uiState: StateFlow<ScanUiState> = mutableUiState.asStateFlow()

   /**
    * Barcodes already accepted in this session.
    *
    * De-duplication used to happen inside composition, as a linear `stream().noneMatch` over
    * the whole folder list for every single tag read. A set makes it O(1) and keeps it out
    * of the composition path entirely.
    */
   private val seenBarcodes = HashSet<String>()

   fun getLoggingCount(): Flow<Int> = loggingQueueRepo.getLoggingQueueCount()

   fun clearBarcodes() {
      seenBarcodes.clear()
      mutableUiState.update { it.copy(barcode = null, folders = emptyList()) }
   }

   fun clearBarcode() = mutableUiState.update { it.copy(barcode = null) }

   fun setBarcodeScan(barcode: ScanModel) =
      mutableUiState.update { it.copy(barcode = barcode) }

   /**
    * Accepts a batch of RFID reads, keeping only new folders whose series is in [seriesFilter].
    *
    * Returns how many folders were actually added, so the caller can decide whether to give
    * haptic feedback without having to diff the list itself.
    */
   fun addRfidReads(
      reads: List<ScanModel>,
      locationId: String,
      seriesFilter: Set<Long>,
      minBarcodeLength: Int = MIN_BARCODE_LENGTH,
   ): Int {
      val accepted = reads.filter { read ->
         read.seriesId in seriesFilter &&
            read.barcodeId.length >= minBarcodeLength &&
            seenBarcodes.add(read.barcodeId)
      }
      if (accepted.isEmpty()) return 0

      val located = accepted.map { it.copy(locationId = locationId) }
      mutableUiState.update { it.copy(folders = located.asReversed() + it.folders) }
      queueForLogging(located)
      return located.size
   }

   /**
    * Accepts a single barcode read from an imager or keyboard wedge.
    *
    * Returns true when the folder was new and has been queued.
    */
   fun addBarcodeRead(barcode: ScanModel, locationId: String): Boolean {
      if (!seenBarcodes.add(barcode.barcodeId)) return false
      val located = barcode.copy(locationId = locationId)
      mutableUiState.update { it.copy(folders = listOf(located) + it.folders) }
      queueForLogging(listOf(located))
      return true
   }

   /** Persists [scans] to the outbound queue; the worker uploads them when the network allows. */
   private fun queueForLogging(scans: List<ScanModel>) {
      val entities = scans.mapNotNull { it.asLoggingQueueModel() }
      if (entities.isEmpty()) return
      viewModelScope.launch {
         loggingQueueRepo.upsertLoggingQueueEntities(entities)
      }
   }

   /** Queues a single scan and nudges the upload worker. */
   fun logBarcode(barcode: ScanModel, context: Context) {
      if (barcode.locationId == null) return
      queueForLogging(listOf(barcode))
      FolderLoggerWorker.initLogger(context)
   }

   /** Flags folders the upload worker has confirmed. */
   fun markLogged(barcodeIds: Set<String>) {
      if (barcodeIds.isEmpty()) return
      mutableUiState.update { state ->
         state.copy(
            folders = state.folders.map {
               if (it.barcodeId in barcodeIds && !it.logged) it.copy(logged = true) else it
            },
         )
      }
   }

   fun addGeiger(person: BarcodeEntityModel.Person) =
      mutableUiState.update { it.copy(geigerFolders = listOf(person) + it.geigerFolders) }

   fun removeGeiger(id: Number) = mutableUiState.update { state ->
      state.copy(geigerFolders = state.geigerFolders.filter { it.id != id })
   }

   fun clearGeiger() = mutableUiState.update { it.copy(geigerFolders = emptyList()) }

   private companion object {
      const val MIN_BARCODE_LENGTH = 12
   }
}
