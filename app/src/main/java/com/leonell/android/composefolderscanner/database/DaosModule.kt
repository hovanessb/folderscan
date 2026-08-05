package com.leonell.android.composefolderscanner.database


import com.leonell.android.composefolderscanner.database.dao.FolderLocationDao
import com.leonell.android.composefolderscanner.database.dao.LoggingQueueDao
import com.leonell.android.composefolderscanner.database.dao.SensorDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {
    @Provides
    fun providesFolderLocationDao(
        database: FolderScannerDatabase,
    ): FolderLocationDao = database.locationDao()

    @Provides
    fun providesLoggingQueueDao(
        database: FolderScannerDatabase,
    ): LoggingQueueDao = database.loggingQueueDao()

    @Provides
    fun providesSensorDataDao(
        database: FolderScannerDatabase,
    ): SensorDataDao = database.sensorDataDao()

}
