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

package com.leonell.android.composefolderscanner.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Locations that always exist, independent of what the server returns. */
private val SEEDED_LOCATIONS = listOf(
   Triple(16266L, "Rotational Shelf", "Cheesecake Bldg CF-19 ROT OUT-1-1"),
   Triple(47550L, "ANNEX CF", "ANNEX CF"),
   Triple(53686L, "Tour Files", "Cheesecake Bldg CF-13-1-5"),
)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

   @Provides
   @Singleton
   fun providesFolderScannerDatabase(
      @ApplicationContext context: Context,
   ): FolderScannerDatabase = Room.databaseBuilder(
      context,
      FolderScannerDatabase::class.java,
      "folder-scanner-database",
   )
      // `createFromAsset("databases/folder-scanner-database")` was removed: that asset is a
      // zero-byte file, so Room threw "file is not a database" on first launch and the app
      // could never open its own store. The seed rows below are the only content it held.
      .addCallback(object : RoomDatabase.Callback() {
         override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.beginTransaction()
            try {
               SEEDED_LOCATIONS.forEach { (id, name, original) ->
                  db.insert("folder_location", CONFLICT_IGNORE, seedLocation(id, name, original))
               }
               db.setTransactionSuccessful()
            } catch (exception: Exception) {
               Log.e("DatabaseModule", "Failed to seed folder locations", exception)
            } finally {
               db.endTransaction()
            }
         }
      })
      // The store is a cache plus a short-lived upload queue; rebuilding it beats shipping a
      // migration for data that is re-fetched from the server on the next launch.
      .fallbackToDestructiveMigration()
      .build()

   private fun seedLocation(id: Long, name: String, original: String) = ContentValues().apply {
      put("id", id)
      put("name", name)
      put("original", original)
      put("spaceA", "")
      put("spaceB", "")
      put("spaceC", "")
      put("filtered", 0)
      put("favorite", 1)
      put("custom", 1)
      put("folderEntityType", "LOCATION")
   }
}
