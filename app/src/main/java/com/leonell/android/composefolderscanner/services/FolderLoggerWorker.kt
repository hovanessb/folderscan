package com.leonell.android.composefolderscanner.services

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.leonell.android.composefolderscanner.database.LoggingQueueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Uploads queued folder scans to the on-premises server.
 *
 * Scans are batched per location, chunked, and removed from the queue only once the server
 * has accepted them, so losing connectivity mid-run costs nothing.
 */
@HiltWorker
class FolderLoggerWorker @AssistedInject constructor(
   @Assisted context: Context,
   @Assisted params: WorkerParameters,
   private val loggingQueueRepo: LoggingQueueRepository,
   private val webApi: WebApi,
) : CoroutineWorker(context, params) {

   companion object {
      const val WORK_NAME = FolderLoggerWorkName
      const val KEY_UPLOADED_BARCODES = "uploadedBarcodes"

      private const val TAG = "FolderLoggerWorker"
      private const val BATCH_SIZE = 20

      fun initLogger(context: Context) {
         val request = OneTimeWorkRequestBuilder<FolderLoggerWorker>()
            // Without a network constraint every run on a dead link burned a retry.
            .setConstraints(
               Constraints.Builder()
                  .setRequiredNetworkType(NetworkType.CONNECTED)
                  .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .addTag(WORK_NAME)
            .build()

         WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            // KEEP, not REPLACE: every scan used to cancel the in-flight upload and restart
            // the timer, so a steady stream of scans meant nothing was ever uploaded.
            ExistingWorkPolicy.KEEP,
            request,
         )
      }
   }

   override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
      val service = webApi.getFolderLocationWebService()
      val pending = loggingQueueRepo.getPendingUploads()
      if (pending.isEmpty()) return@withContext Result.success()

      val uploaded = mutableListOf<Long>()
      val uploadedBarcodes = mutableListOf<String>()
      var sawFailure = false

      // Group by location because the endpoint logs a CSV of barcodes against one location.
      for ((locationId, rows) in pending.groupBy { it.locationId }) {
         val location = locationId.toLongOrNull()
         if (location == null) {
            Log.w(TAG, "Dropping ${rows.size} scan(s) with unusable location '$locationId'")
            uploaded += rows.map { it.id }
            continue
         }

         for (batch in rows.chunked(BATCH_SIZE)) {
            val barcodeCsv = batch.map { it.barcodeId }.distinct().joinToString(",")
            val response = runCatching { service.logBarcode(barcodeCsv, location).execute() }
               .onFailure { Log.w(TAG, "Upload failed for location $location", it) }
               .getOrNull()

            if (response?.isSuccessful == true) {
               uploaded += batch.map { it.id }
               uploadedBarcodes += batch.map { it.barcodeId }
            } else {
               // Keep going: one unreachable batch should not strand the others. The old
               // version overwrote a single shared result, so the last batch decided the
               // outcome for every batch before it.
               sawFailure = true
            }
         }
      }

      if (uploaded.isNotEmpty()) {
         loggingQueueRepo.deleteLoggingQueueEntity(uploaded)
      }

      val output = Data.Builder()
         .putStringArray(KEY_UPLOADED_BARCODES, uploadedBarcodes.toTypedArray())
         .build()

      // Anything left behind is worth another attempt; WorkManager's exponential backoff
      // keeps a persistently unreachable server from spinning.
      if (sawFailure) Result.retry() else Result.success(output)
   }
}
