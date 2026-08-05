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
import androidx.room.Update
import androidx.room.Upsert
import com.leonell.android.composefolderscanner.database.model.FolderLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [FolderLocationEntity] access
 */
@Dao
interface FolderLocationDao {
    @Query(
        value = """
                    SELECT * FROM folder_location
                    WHERE id = :id
                """
    )
    fun getFolderLocationEntity(id: String): Flow<FolderLocationEntity>

    @Query(value = "SELECT * FROM folder_location order by favorite desc, upper(name)")
    fun getFolderLocationEntities(): Flow<List<FolderLocationEntity>>

    @Query(value = "SELECT * FROM folder_location")
    suspend fun getFolderLocationEntitiesSaved(): List<FolderLocationEntity>

    /** True when the server-provided locations have already been cached. */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM folder_location WHERE custom = 0)")
    suspend fun hasServerLocations(): Boolean

    /**
     * Inserts [FolderLocationEntities] into the db if they don't exist, and ignores those that do
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreFolderLocations(FolderLocationEntities: List<FolderLocationEntity>): List<Long>

    /**
     * Updates [folder] in the db that match the primary key, and no-ops if they don't
     */
    @Update
    suspend fun updateFolderLocation(folder: FolderLocationEntity)

    /**
     * Updates [entities] in the db that match the primary key, and no-ops if they don't
     */
    @Update
    suspend fun updateFolderLocations(entities: List<FolderLocationEntity>)

    /**
     * Inserts or updates [entities] in the db under the specified primary keys
     */
    @Upsert
    suspend fun upsertFolderLocations(entities: List<FolderLocationEntity>)

    /**
     * Deletes rows in the db matching the specified [ids]
     */
    @Query(
        value = """
                    DELETE FROM folder_location
                    WHERE id in (:ids)
                """
    )
    suspend fun deleteFolderLocations(ids: List<String>)
}
