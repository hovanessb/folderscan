package com.leonell.android.composefolderscanner.database

import android.util.Log
import com.google.gson.Gson
import com.leonell.android.composefolderscanner.database.dao.FolderLocationDao
import com.leonell.android.composefolderscanner.database.model.FolderLocationEntity
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.BarcodeLookup
import com.leonell.android.composefolderscanner.models.FolderEntityType
import com.leonell.android.composefolderscanner.models.ScanModel
import com.leonell.android.composefolderscanner.models.asDatabaseModel
import com.leonell.android.composefolderscanner.services.WebApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FolderLocationRepository"

/**
 * Folder locations, served from Room and backfilled from the server on first use.
 *
 * [WebApi] is injected rather than constructed here; the repository used to build its own,
 * which meant a second OkHttp client (and connection pool) per instance and no way for the
 * credentials screen to reach it.
 */
@Singleton
class FolderLocationRepository @Inject constructor(
   private val folderLocationsDao: FolderLocationDao,
   private val webApi: WebApi,
) {
   private val gson = Gson()

   fun getFolderLocationEntity(id: String): Flow<FolderLocationEntity> =
      folderLocationsDao.getFolderLocationEntity(id)

   fun getFolderLocationEntities(): Flow<List<FolderLocationEntity>> =
      folderLocationsDao.getFolderLocationEntities()

   /** Fetches the server's location list, unless it has already been cached. */
   suspend fun getFolderLocationsREST() = withContext(Dispatchers.IO) {
      try {
         if (folderLocationsDao.hasServerLocations()) {
            Log.d(TAG, "Locations already cached")
            return@withContext
         }
         Log.d(TAG, "Fetching locations from the server")
         val locations = webApi.getFolderLocationWebService().getLocations().locations
         locations.forEach { it.original = it.name }
         folderLocationsDao.insertOrIgnoreFolderLocations(locations.map { it.asDatabaseModel() })
      } catch (exception: Exception) {
         Log.e(TAG, "Could not load folder locations", exception)
      }
   }

   suspend fun upsertFolderLocations(entities: List<FolderLocationEntity>) =
      folderLocationsDao.upsertFolderLocations(entities)

   suspend fun deleteFolderLocations(ids: List<String>) =
      folderLocationsDao.deleteFolderLocations(ids)

   /** Resolves a scanned barcode to whatever entity the server says it is. */
   suspend fun lookup(barcode: ScanModel): BarcodeLookup? = withContext(Dispatchers.IO) {
      val lookup = runCatching {
         webApi.getFolderLocationWebService().resolveBarcode(barcode.barcodeId).execute().body()
      }
         .onFailure { Log.e(TAG, "Barcode lookup failed for ${barcode.barcodeId}", it) }
         .getOrNull() ?: return@withContext null

      lookup.entityObject = when (lookup.entityType) {
         FolderEntityType.LOCATION, FolderEntityType.SHELF, FolderEntityType.UNKNOWN ->
            gson.fromJson(lookup.entity, BarcodeEntityModel.FolderLocation::class.java)

         FolderEntityType.FOLDER ->
            gson.fromJson(lookup.entity, BarcodeEntityModel.Person::class.java)

         null -> null
      }
      lookup
   }

   /** Logs one folder immediately. Returns the HTTP status, or -1 if the call never landed. */
   suspend fun logOneLocation(barcode: String, locationId: Long): Int = withContext(Dispatchers.IO) {
      runCatching {
         webApi.getFolderLocationWebService().logBarcode(barcode, locationId).execute().code()
      }
         .onFailure { Log.e(TAG, "Could not log $barcode at $locationId", it) }
         .getOrDefault(-1)
   }
}
