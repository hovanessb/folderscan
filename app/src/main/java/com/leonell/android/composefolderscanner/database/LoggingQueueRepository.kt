package com.leonell.android.composefolderscanner.database

import com.leonell.android.composefolderscanner.database.dao.LoggingQueueDao
import com.leonell.android.composefolderscanner.database.model.LoggingQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Access to the outbound scan queue.
 *
 * This no longer implements [LoggingQueueDao] itself. A repository that mirrors its DAO
 * one-for-one adds a layer without adding a seam, and it forced every caller to work with
 * DAO-shaped signatures.
 */
@Singleton
class LoggingQueueRepository @Inject constructor(
   private val loggingQueueDao: LoggingQueueDao,
) {
   fun getLoggingQueueCount(): Flow<Int> =
      loggingQueueDao.getLoggingQueueCount().distinctUntilChanged()

   fun getLoggingQueueEntities(): Flow<List<LoggingQueueEntity>> =
      loggingQueueDao.getLoggingQueueEntities()

   suspend fun getPendingUploads(): List<LoggingQueueEntity> =
      loggingQueueDao.getLoggingQueueEntitiesPost()

   suspend fun upsertLoggingQueueEntities(entities: List<LoggingQueueEntity>) =
      loggingQueueDao.insertOrIgnoreLoggingQueueEntities(entities)

   suspend fun deleteLoggingQueueEntity(ids: List<Long>) =
      loggingQueueDao.deleteLoggingQueueEntity(ids)
}
