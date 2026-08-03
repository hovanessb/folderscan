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
import androidx.room.Query
import androidx.room.Upsert
import com.leonell.android.composefolderscanner.database.model.SensorDataEntity

/**
 * DAO for [SensorDataEntity] access
 */
@Dao
interface SensorDataDao {
    @Upsert
    suspend fun upsertSensorEntities(entities: List<SensorDataEntity>)
    /**
     * Deletes rows in the db matching the specified [ids]
     */
    @Query(
        value = """
                    DELETE FROM sensor_data 
                """
    )
    suspend fun deleteSessions()

}
