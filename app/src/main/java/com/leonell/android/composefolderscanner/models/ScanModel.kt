package com.leonell.android.composefolderscanner.models

import com.leonell.android.composefolderscanner.database.model.LoggingQueueEntity

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

/**
 * One folder read, from an RFID tag or a barcode.
 *
 * Immutable on purpose. This used to be a mutable class holding a Compose `MutableState` for
 * [logged], which made the model layer depend on the UI toolkit and let the reader thread
 * mutate state the UI was concurrently reading. Copies are cheap, and Compose diffs value
 * changes the way it expects.
 */
data class ScanModel(
   val id: Long? = null,
   val rawRead: ByteArray? = null,
   val barcodeId: String = "",
   val phase: Float = 0.0f,
   val seriesId: Long? = -1,
   val rssi: Float = 0.0f,
   val logged: Boolean = false,
   val locationId: String? = null,
) {
   // A ByteArray property makes the generated equals/hashCode compare by array identity,
   // which quietly breaks de-duplication and LazyColumn keys. Both are defined over the
   // barcode, which is what actually identifies a folder.
   override fun equals(other: Any?): Boolean =
      this === other || (other is ScanModel && barcodeId == other.barcodeId)

   override fun hashCode(): Int = barcodeId.hashCode()
}

/**
 * Maps a scan onto its outbound queue row, or null when it cannot be logged.
 *
 * The previous version used `barcodeId.toLong()` as the primary key, which threw
 * NumberFormatException on any non-numeric barcode and silently dropped a folder re-scanned
 * at a second location, since the barcode alone was the key.
 */
fun ScanModel.asLoggingQueueModel(): LoggingQueueEntity? {
   val location = locationId ?: return null
   if (barcodeId.isBlank()) return null
   return LoggingQueueEntity(
      barcodeId = barcodeId,
      locationId = location,
      rawRead = rawRead?.toString(Charsets.US_ASCII).orEmpty(),
   )
}
