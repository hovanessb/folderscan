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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_data",
)


data class SensorDataEntity(
    @PrimaryKey
    val id: String,
    val session: String,
    val azimuth: Float,
    val roll: Float,
    val pitch: Float,
    val acceleratorX: Float,
    val acceleratorY: Float,
    val acceleratorZ: Float,
    val gravityX: Float,
    val gravityY: Float,
    val gravityZ: Float,
    val gyroscopeX: Float,
    val gyroscopeY: Float,
    val gyroscopeZ: Float,
    val timeDelta: Float,
    val currentX: Float,
    val currentY: Float,
    val currentZ: Float,
    val currentBX: Float,
    val currentBY: Float,
    val currentBZ: Float,
    val orientationX: Float,
    val orientationY: Float,
    val orientationZ: Float,
    val rssi: Float = 0.0f,
    val phase: Float = 0.0f
)
