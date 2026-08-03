/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leonell.android.composefolderscanner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leonell.android.composefolderscanner.database.model.LoggingQueueEntity
import kotlinx.coroutines.flow.Flow

/** DAO for the outbound scan queue. */
@Dao
interface LoggingQueueDao {
   @Query("SELECT * FROM logging_queue ORDER BY id")
   fun getLoggingQueueEntities(): Flow<List<LoggingQueueEntity>>

   /** Counted in SQL rather than by loading every row just to call `.count()` on it. */
   @Query("SELECT COUNT(*) FROM logging_queue")
   fun getLoggingQueueCount(): Flow<Int>

   @Query("SELECT * FROM logging_queue ORDER BY id")
   suspend fun getLoggingQueueEntitiesPost(): List<LoggingQueueEntity>

   /**
    * Queues [entities], ignoring folders already queued for the same location.
    *
    * `@Upsert` would be wrong here: rows carry a generated key, so upserting a duplicate
    * would fall back to updating primary key 0 and silently drop the row.
    */
   @Insert(onConflict = OnConflictStrategy.IGNORE)
   suspend fun insertOrIgnoreLoggingQueueEntities(entities: List<LoggingQueueEntity>)

   @Query("DELETE FROM logging_queue WHERE id IN (:ids)")
   suspend fun deleteLoggingQueueEntity(ids: List<Long>)
}
