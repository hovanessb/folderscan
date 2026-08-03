package com.leonell.android.composefolderscanner.database
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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.leonell.android.composefolderscanner.database.dao.FolderLocationDao
import com.leonell.android.composefolderscanner.database.dao.LoggingQueueDao
import com.leonell.android.composefolderscanner.database.dao.SensorDataDao
import com.leonell.android.composefolderscanner.database.model.FolderLocationEntity
import com.leonell.android.composefolderscanner.database.model.LoggingQueueEntity
import com.leonell.android.composefolderscanner.database.model.SensorDataEntity
import com.leonell.android.composefolderscanner.database.util.FolderEntityTypeConverter
import com.leonell.android.composefolderscanner.database.util.InstantConverter

@Database(
    entities = [
        FolderLocationEntity::class,
        LoggingQueueEntity::class,
        SensorDataEntity::class,
    ],
    version = 2,
    exportSchema = true
)

@TypeConverters(
    InstantConverter::class,
    FolderEntityTypeConverter::class,
)
abstract class FolderScannerDatabase : RoomDatabase() {
    abstract fun locationDao(): FolderLocationDao
    abstract fun loggingQueueDao(): LoggingQueueDao
    abstract fun sensorDataDao (): SensorDataDao
}
