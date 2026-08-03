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
import androidx.room.PrimaryKey
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.FolderEntityType

@Entity(
    tableName = "folder_location",
)
data class FolderLocationEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(defaultValue = "")
    val name: String,
    @ColumnInfo(defaultValue = "")
    val original: String?,
    @ColumnInfo(defaultValue = "")
    val spaceA	: String? ,
    @ColumnInfo(defaultValue = "")
    val spaceB	: String?,
    @ColumnInfo(defaultValue = "")
    val spaceC	: String?,
    @ColumnInfo(defaultValue = "false")
    val filtered: Boolean ,
    @ColumnInfo(defaultValue = "false")
    val favorite:  Boolean  ,
    @ColumnInfo(defaultValue = "true")
    val custom: Boolean,
    val folderEntityType : FolderEntityType? = FolderEntityType.LOCATION
)

fun FolderLocationEntity.asExternalModel() = BarcodeEntityModel.FolderLocation(
    id = id,
    name = name,
    original=original,
    spaceA = spaceA,
    spaceB = spaceB,
    spaceC = spaceC,
    filtered =  filtered  ,
    favorite = favorite ,
    // `custom` was dropped on the way out, so every round-tripped location looked
    // server-owned and the "delete only custom locations" guard could never fire.
    custom = custom,
    folderEntityType = folderEntityType
)
