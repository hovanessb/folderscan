package com.leonell.android.composefolderscanner.ui.viewmodels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leonell.android.composefolderscanner.cs108library.Cs108Connector
import com.leonell.android.composefolderscanner.cs108library.Cs108Library4A
import com.leonell.android.composefolderscanner.cs108library.HostCmdResponseTypes
import com.leonell.android.composefolderscanner.cs108library.HostCommands
import com.leonell.android.composefolderscanner.cs108library.OperationTypes
import com.leonell.android.composefolderscanner.cs108library.ReaderDevice
import com.leonell.android.composefolderscanner.cs108library.Rx000pkgData
import com.leonell.android.composefolderscanner.models.ScanModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Start RFID Tag/Label Inventory using Non-Default Configuration
 * 1. abortOperation(): stop any previous RFID inventory and check RFID module health
 * 2. setPowerLevel(300)
 * 3. setAntennaDwell(0)
 * 4. setCurrentLinkProfile(1)
 * 5. setTagGroup(0, 0, 0): select = all, session = S0, target = A
 * 6. setInvAlgo(3): algorithm = dynamic
 * 7. setDynamicQParms(8, 0, 15, 0) -- or setFixedQParms(8, 0, false) for the fixed algorithm
 * 8. startOperation(OperationTypes.TAG_INVENTORY)
 * 9. drain inventory packets
 * 10. abortOperation() when finished
 *
 * Search an RFID Tag/Label (Geiger Search)
 * 1. abortOperation()
 * 2. setSelectedTag("201700000000000000000001", 300)
 * 3. setCurrentLinkProfile(1)
 * 4. setTagGroup(0, 1, 2): select = all, session = S1, target = A/B toggle
 * 5. setInvAlgo(0): algorithm = fixed
 * 6. setFixedQParms(0, 0, false)
 * 7. startOperation(OperationTypes.TAG_SEARCHING)
 * 8. drain inventory packets
 * 9. abortOperation() when finished
 */
sealed interface ConvergenceHandgunState {
   data class Connect(val device: BluetoothDevice) : ConvergenceHandgunState
   data object NeverConnected : ConvergenceHandgunState
   data object Disconnected : ConvergenceHandgunState
   data object Busy : ConvergenceHandgunState
   data object Ready : ConvergenceHandgunState
   data object Scanning : ConvergenceHandgunState
   data object Writing : ConvergenceHandgunState
}

/** What the reader is currently being asked to do. */
private enum class ReaderMode { IDLE, INVENTORY, GEIGER }

class ConvergenceHandgunViewModel : ViewModel() {

   private var bleLibrary: Cs108Library4A? = null

   var currentDevice: BluetoothDevice? = null
      private set

   private val mutableState = MutableStateFlow<ConvergenceHandgunState>(
      ConvergenceHandgunState.NeverConnected,
   )
   val uiState: StateFlow<ConvergenceHandgunState> = mutableState.asStateFlow()

   /**
    * Tag reads, published in batches.
    *
    * Deliberately separate from [uiState]. The previous design folded each tag into the
    * connection state, so every single read replaced the state object and recomposed the
    * whole screen. A dense inventory reports hundreds of tags per second; the UI only needs
    * the accumulated result a few times a second.
    */
   private val mutableTags = MutableSharedFlow<List<ScanModel>>(
      replay = 0,
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
   )
   val tagReads: SharedFlow<List<ScanModel>> = mutableTags.asSharedFlow()

   private var readerJob: Job? = null

   @Volatile
   private var mode: ReaderMode = ReaderMode.IDLE

   suspend fun startBleLibrary(context: Context) = withContext(Dispatchers.Main) {
      if (bleLibrary != null) return@withContext
      Log.i(TAG, "Starting the BLE library")
      // The library only uses its log view for optional on-screen tracing. Passing null
      // keeps an Activity-scoped View out of this ViewModel, which previously leaked the
      // Activity across every configuration change.
      bleLibrary = Cs108Library4A(context.applicationContext, null)
   }

   fun setCurrentConnection(device: BluetoothDevice) {
      viewModelScope.launch {
         disconnect()
         currentDevice = device
         connectToHandgunDevice(device)
      }
   }

   /** Tears down the reader session. Safe to call when nothing is connected. */
   fun disconnect() {
      readerJob?.cancel()
      readerJob = null
      mode = ReaderMode.IDLE
      val library = bleLibrary
      try {
         if (library != null && library.isBleConnected) {
            library.sameCheck = true
            library.setInvBrandId(false)
            library.restoreAfterTagSelect()
            library.disconnect(true)
         }
      } catch (exception: Exception) {
         Log.w(TAG, "Error while disconnecting", exception)
      } finally {
         mutableState.value = ConvergenceHandgunState.Disconnected
         currentDevice = null
      }
   }

   override fun onCleared() {
      super.onCleared()
      disconnect()
   }

   @SuppressLint("MissingPermission")
   private suspend fun connectToHandgunDevice(device: BluetoothDevice) {
      val library = bleLibrary ?: run {
         Log.w(TAG, "BLE library not started")
         return
      }
      if (library.isBleConnected) return

      mutableState.value = ConvergenceHandgunState.Connect(device)
      mutableState.value = ConvergenceHandgunState.Busy

      // BluetoothDevice.name is null until the device has been resolved; fall back to the
      // address so the reader still gets a usable label.
      val readerDevice = ReaderDevice(
         device.name ?: device.address,
         device.address,
         false,
         "",
         1,
         1.0,
         0,
      )
      library.connect(readerDevice)

      if (!awaitCondition(CONNECT_TIMEOUT_MS) { library.isBleConnected }) {
         mutableState.value = ConvergenceHandgunState.Disconnected
         currentDevice = null
         return
      }

      library.setReaderDefault()
      // Let the queued configuration commands drain before handing over to the reader loop.
      awaitCondition(COMMAND_FLUSH_TIMEOUT_MS) { library.mrfidToWriteSize() == 0 }

      library.setNotificationListener(object : Cs108Connector.NotificationListener {
         override fun onChange() = onTriggerChanged()
      })

      readerDevice.isConnected = true
      setScanModeDefaults()
      mutableState.value = ConvergenceHandgunState.Ready
      startReaderLoop()
   }

   /** Polls [condition] on a short cadence instead of blocking a thread. */
   private suspend fun awaitCondition(timeoutMs: Long, condition: () -> Boolean): Boolean =
      withTimeoutOrNull(timeoutMs) {
         while (!condition()) delay(POLL_INTERVAL_MS)
         true
      } ?: false

   private fun onTriggerChanged() {
      val library = bleLibrary ?: return
      if (!library.isBleConnected || library.isRfidFailure) {
         mutableState.value = ConvergenceHandgunState.Disconnected
         currentDevice = null
         return
      }

      val pressed = library.triggerButtonStatus
      when {
         pressed && mutableState.value == ConvergenceHandgunState.Ready -> startOperation()
         !pressed && mutableState.value == ConvergenceHandgunState.Scanning -> {
            mutableState.value = ConvergenceHandgunState.Ready
            library.abortOperation()
         }
      }
   }

   private fun startOperation() {
      val library = bleLibrary ?: return
      // Discard packets left over from the previous pull of the trigger, otherwise the
      // first tags reported belong to the last scan.
      library.flushRfidEvents()
      mutableState.value = ConvergenceHandgunState.Scanning
      library.startOperation(currentOperation())
   }

   private fun currentOperation(): OperationTypes = when (mode) {
      ReaderMode.GEIGER -> OperationTypes.TAG_SEARCHING
      // Compact inventory reports the same EPCs in noticeably fewer bytes, which matters
      // over a BLE link; it is what the reference demo app uses for continuous reads.
      else -> OperationTypes.TAG_INVENTORY_COMPACT
   }

   /**
    * Single reader coroutine, running for the lifetime of the connection.
    *
    * The previous implementation called `onRFIDEvent()` in a tight loop with no yield at
    * all -- burning a core whenever the queue was empty -- blocked the dispatcher with
    * `Thread.sleep` whenever it was not, and pushed one state update per tag.
    *
    * This drains the whole packet backlog in one pass, parses and de-duplicates off the main
    * thread, and emits at most one batch per [EMIT_WINDOW_MS]. Between drains it backs off
    * from [MIN_IDLE_DELAY_MS] to [MAX_IDLE_DELAY_MS], so an idle reader costs almost nothing
    * while a busy one is never throttled.
    */
   private fun startReaderLoop() {
      readerJob?.cancel()
      readerJob = viewModelScope.launch(Dispatchers.IO) {
         val library = bleLibrary ?: return@launch
         val pending = LinkedHashMap<String, ScanModel>()
         var lastEmit = 0L
         var idleDelay = MIN_IDLE_DELAY_MS

         while (isActive && library.isBleConnected) {
            // Commands still queued for the reader take priority over reading; pushing them
            // out first is what the reference implementation does too.
            if (library.mrfidToWriteSize() != 0) {
               delay(COMMAND_DRAIN_DELAY_MS)
               continue
            }

            val packets = library.drainRfidEvents()
            if (packets.isEmpty()) {
               // Flush anything buffered before going quiet, so the last few tags of a scan
               // are not held back waiting for a batch that never fills.
               if (pending.isNotEmpty()) {
                  mutableTags.emit(pending.values.toList())
                  pending.clear()
                  lastEmit = System.currentTimeMillis()
               }
               delay(idleDelay)
               idleDelay = (idleDelay * 2).coerceAtMost(MAX_IDLE_DELAY_MS)
               continue
            }

            idleDelay = MIN_IDLE_DELAY_MS
            for (packet in packets) {
               handlePacket(library, packet, pending)
            }

            val now = System.currentTimeMillis()
            if (pending.isNotEmpty() && now - lastEmit >= EMIT_WINDOW_MS) {
               mutableTags.emit(pending.values.toList())
               pending.clear()
               lastEmit = now
            }
         }

         mutableState.value = ConvergenceHandgunState.Disconnected
         currentDevice = null
      }
   }

   private fun handlePacket(
      library: Cs108Library4A,
      packet: Rx000pkgData,
      pending: MutableMap<String, ScanModel>,
   ) {
      if (packet.decodedError != null) return

      when (packet.responseType) {
         HostCmdResponseTypes.TYPE_18K6C_INVENTORY,
         HostCmdResponseTypes.TYPE_18K6C_INVENTORY_COMPACT,
         HostCmdResponseTypes.TYPE_18K6C_TAG_ACCESS,
         -> {
            if (!library.triggerButtonStatus) return
            val scan = parseData(packet) ?: return
            // Keep the strongest read of each tag within a batch: during a Geiger search
            // the RSSI is the signal the operator is steering by.
            val existing = pending[scan.barcodeId]
            if (existing == null || scan.rssi > existing.rssi) {
               pending[scan.barcodeId] = scan
            }
         }

         HostCmdResponseTypes.TYPE_COMMAND_END -> {
            // The reader stops at the end of each inventory round; restart while the trigger
            // is still held so a scan feels continuous.
            if (library.triggerButtonStatus) {
               library.startOperation(currentOperation())
            }
         }

         HostCmdResponseTypes.TYPE_COMMAND_ABORT_RETURN -> {
            if (mutableState.value == ConvergenceHandgunState.Scanning) {
               mutableState.value = ConvergenceHandgunState.Ready
            }
         }

         else -> Unit
      }
   }

   /** Switches the reader into Geiger mode and targets [selectedTag]. */
   fun startSearch(selectedTag: ByteArray) {
      val library = bleLibrary ?: return
      mode = ReaderMode.GEIGER
      setGeigerModeDefaults()
      library.setSelectedTag(bytesToHex(selectedTag), 1, GEIGER_POWER_LEVEL)
      library.flushRfidEvents()
      library.startOperation(OperationTypes.TAG_SEARCHING)
   }

   /** Returns the reader to bulk inventory mode. */
   fun stopSearch() {
      bleLibrary?.abortOperation()
      setScanModeDefaults()
   }

   private fun parseData(packet: Rx000pkgData): ScanModel? {
      val epcBytes = trimmedEpc(packet) ?: return null
      val epc = epcBytes.toString(Charsets.US_ASCII)
      if (epc.isBlank()) return null

      val parts = epc.split("-")
      val barcodeId = parts.firstOrNull().orEmpty()
      if (barcodeId.isBlank()) return null

      val seriesId = parts.getOrNull(1)
         ?.filter(Char::isDigit)
         ?.takeIf { it.isNotEmpty() }
         ?.toLongOrNull()

      return ScanModel(
         barcodeId = barcodeId,
         phase = packet.decodedPhase.toFloat(),
         rssi = packet.decodedRssi.toFloat(),
         rawRead = epcBytes,
         seriesId = seriesId,
      )
   }

   /**
    * Strips any extra memory banks the reader appended to the EPC.
    *
    * Unlike the previous version this returns a copy instead of mutating [packet]. That
    * matters now that packets are batched: re-reading a mutated packet truncated the EPC a
    * second time.
    */
   private fun trimmedEpc(packet: Rx000pkgData): ByteArray? {
      val epc = packet.decodedEpc ?: return null
      val extraLength = (packet.decodedData1?.size ?: 0) + (packet.decodedData2?.size ?: 0)
      if (extraLength <= 0) return epc
      val keep = epc.size - extraLength
      return if (keep > 0) epc.copyOf(keep) else null
   }

   private fun bytesToHex(bytes: ByteArray): String {
      val builder = StringBuilder(bytes.size * 2)
      for (byte in bytes) {
         builder.append(HEX_DIGITS[(byte.toInt() and 0xF0) ushr 4])
         builder.append(HEX_DIGITS[byte.toInt() and 0x0F])
      }
      return builder.toString()
   }

   /*
      |series_id|name|
      |--|-----------|
      |0 |       Solo|
      |1 |    Audited|
      |2 |    Student|
      |3 |     Ethics|
      |4 |  Personnel|
      |10|        CF |
      |11| CF (Div 6)|
      |20|Audited - Staging|
    */
   fun folderSeriesIdToName(seriesId: Int): String = when (seriesId) {
      0 -> "Solo"
      1 -> "Audited"
      3 -> "Ethics"
      11 -> "Prospect"
      else -> "CF"
   }

   private fun folderSeriesToId(seriesName: String): Int = when (seriesName) {
      "Solo" -> 0
      "Audited" -> 1
      "Ethics" -> 3
      "Prospect" -> 11
      else -> 10
   }

   /**
    * Writes [writeData] into the EPC bank of the tag identified by [currentTag].
    *
    * Returns a human-readable result message.
    */
   suspend fun writeRfidData(
      currentTag: ByteArray,
      writeData: ByteArray,
      writeFolderType: String,
   ): String = withContext(Dispatchers.IO) {
      val library = bleLibrary ?: return@withContext "Reader not connected."

      val maxSize = minOf(writeData.size, MAX_EPC_BYTES)
      val minSize = if (maxSize <= EPC_WORD_SPLIT) 0 else EPC_WORD_SPLIT
      val payload = writeData.copyOfRange(minSize, maxSize) +
         "-${folderSeriesToId(writeFolderType)}".toByteArray(Charsets.US_ASCII)
      val writeBytes = bytesToHex(payload).padEnd(EPC_HEX_LENGTH, '0')

      library.abortOperation()
      library.setAccessBank(1)
      library.setAccessOffset(2)
      library.setAccessCount(writeBytes.length / 4)
      library.setRx000AccessPassword("0000")
      library.setAccessRetry(true, WRITE_RETRIES)
      library.setAccessWriteData(writeBytes)
      library.setSelectedTag(bytesToHex(currentTag), 1, WRITE_POWER_LEVEL)
      setWriteModeDefaults()

      mutableState.value = ConvergenceHandgunState.Writing
      library.flushRfidEvents()
      library.sendHostRegRequestHST_CMD(HostCommands.CMD_18K6CWRITE)

      var retries = 0
      // The previous implementation compared a wall-clock timestamp against a bare 5000, so
      // its timeout branch was effectively never reachable and a failed write hung until the
      // reader disconnected. This is a real deadline.
      val result = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
         var message: String? = null
         while (isActive && library.isBleConnected && message == null) {
            for (packet in library.drainRfidEvents()) {
               if (packet.responseType != HostCmdResponseTypes.TYPE_COMMAND_END) continue
               message = when {
                  packet.decodedData1 != null -> packet.decodedError ?: "Success!"
                  retries < WRITE_RETRIES -> {
                     retries++
                     library.sendHostRegRequestHST_CMD(HostCommands.CMD_18K6CWRITE)
                     null
                  }

                  else -> packet.decodedError
                     ?: "Write failed after $WRITE_RETRIES retries."
               }
               if (message != null) break
            }
            if (message == null) delay(WRITE_POLL_INTERVAL_MS)
         }
         message
      }

      setScanModeDefaults()
      mutableState.value = ConvergenceHandgunState.Ready
      result ?: "Timed out waiting for the tag."
   }

   fun getRfidPowerLevel(): Float = bleLibrary?.pwrlevel?.toFloat() ?: 0f

   fun setRfidPowerLevel(level: Number) {
      bleLibrary?.setPowerLevel(level.toLong())
   }

   fun getPopulation(): Int = bleLibrary?.population ?: 0

   fun getProfile(): Int = bleLibrary?.currentProfile ?: -1

   fun getInvAlgo(): String = when (bleLibrary?.invAlgo) {
      true -> "DYNAMIC: Better adapts to different groups of RFID amounts per scan"
      false -> "FIXED: Efficiently searches for groups of ${getPopulation()} RFIDs at a time"
      else -> "Reader not connected"
   }

   /**
    * Battery level as a percentage, polled while collected.
    *
    * [channelFlow] plus `conflate` means a screen that is not visible does not keep the poll
    * alive, and a slow collector never backs the producer up.
    */
   val batteryLevel: Flow<Float> = channelFlow {
      while (isActive) {
         val library = bleLibrary
         val percent = if (library != null && library.isBleConnected) {
            library.getBatteryValue2Percent(library.batteryLevel.toFloat() / 1000).toFloat()
         } else {
            0f
         }
         send(percent)
         delay(BATTERY_POLL_MS)
      }
   }.distinctUntilChanged().conflate()

   val isRfidOn: Flow<Boolean> = moduleStatusFlow { it.mRfidDevice?.onStatus }
   val isBarcodeOn: Flow<Boolean> = moduleStatusFlow { it.mBarcodeDevice?.onStatus }

   /**
    * Polls a module's on/off status.
    *
    * Every access is null-guarded: the previous version dereferenced the library with `!!`
    * inside the flow, so collecting it before a reader was connected crashed the app.
    */
   private fun moduleStatusFlow(selector: (Cs108Library4A) -> Boolean?): Flow<Boolean> =
      channelFlow {
         while (isActive) {
            send(bleLibrary?.let(selector) ?: false)
            delay(MODULE_POLL_MS)
         }
      }.distinctUntilChanged().conflate()

   fun setBarcodeModule(enabled: Boolean) {
      bleLibrary?.setBarcodeOn(enabled)
   }

   fun setRfidModule(enabled: Boolean) {
      bleLibrary?.setRfidOn(enabled)
   }

   fun setWriteModeDefaults() = withReadyReader { library ->
      mode = ReaderMode.IDLE
      library.sameCheck = true
      library.setInvBrandId(false)
      library.restoreAfterTagSelect()
      library.setTagFocus(true)
      library.setTagGroup(3, 1, 2)
      library.population = DEFAULT_POPULATION
      library.invAlgo = false
      library.setCurrentLinkProfile(1)
      library.setFixedQParms(0, 0, false)
   }

   fun setScanModeDefaults() = withReadyReader { library ->
      mode = ReaderMode.INVENTORY
      library.sameCheck = true
      library.setInvBrandId(false)
      library.restoreAfterTagSelect()
      library.setTagFocus(false)
      library.setTagGroup(0, 0, 2)
      library.population = DEFAULT_POPULATION
      library.invAlgo = true
      library.setCurrentLinkProfile(2)
   }

   fun setGeigerModeDefaults() = withReadyReader { library ->
      library.setPowerLevel(GEIGER_POWER_LEVEL)
      library.setTagGroup(0, 1, 2)
      library.population = DEFAULT_POPULATION
      library.invAlgo = false
      library.setFixedQParms(0, 0, false)
      library.setCurrentLinkProfile(1)
   }

   /** Runs [block] only when a healthy reader is attached. */
   private fun withReadyReader(block: (Cs108Library4A) -> Unit) {
      val library = bleLibrary ?: return
      if (library.isBleConnected && !library.isRfidFailure) block(library)
   }

   private companion object {
      const val TAG = "ConvergenceHandgun"
      val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

      const val CONNECT_TIMEOUT_MS = 15_000L
      const val COMMAND_FLUSH_TIMEOUT_MS = 10_000L
      const val POLL_INTERVAL_MS = 100L
      const val COMMAND_DRAIN_DELAY_MS = 50L

      /** Upper bound on how often tag batches reach the UI. */
      const val EMIT_WINDOW_MS = 100L
      const val MIN_IDLE_DELAY_MS = 15L
      const val MAX_IDLE_DELAY_MS = 250L

      const val BATTERY_POLL_MS = 5_000L
      const val MODULE_POLL_MS = 1_000L

      const val DEFAULT_POPULATION = 60
      const val GEIGER_POWER_LEVEL = 300L
      const val WRITE_POWER_LEVEL = 50L
      const val WRITE_RETRIES = 7
      const val WRITE_TIMEOUT_MS = 15_000L
      const val WRITE_POLL_INTERVAL_MS = 50L

      const val MAX_EPC_BYTES = 32
      const val EPC_WORD_SPLIT = 16
      const val EPC_HEX_LENGTH = 32
   }
}
