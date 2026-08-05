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
package com.leonell.android.composefolderscanner.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.leonell.android.composefolderscanner.models.ScanModel

/**
 * A folder scan waiting to be uploaded.
 *
 * The primary key is generated rather than derived from the barcode: barcodes are not always
 * numeric (the old `barcodeId.toLong()` key threw on those), and the same folder is
 * legitimately logged at more than one location. The unique index instead collapses repeat
 * scans of the same folder *at the same location*, which is the duplicate worth suppressing.
 */
@Entity(
   tableName = "logging_queue",
   indices = [Index(value = ["barcodeId", "locationId"], unique = true)],
)
data class LoggingQueueEntity(
   @PrimaryKey(autoGenerate = true)
   val id: Long = 0,
   @ColumnInfo(defaultValue = "")
   val barcodeId: String,
   val locationId: String,
   @ColumnInfo(defaultValue = "")
   val rawRead: String,
)

fun LoggingQueueEntity.asExternalModel() = ScanModel(
   id = id,
   barcodeId = barcodeId,
   seriesId = null,
   locationId = locationId,
   logged = false,
   rawRead = null,
)
