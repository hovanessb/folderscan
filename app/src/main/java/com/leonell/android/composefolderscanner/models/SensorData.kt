package com.leonell.android.composefolderscanner.models

import com.leonell.android.composefolderscanner.database.model.SensorDataEntity
import java.util.UUID

data class SensorData(
   var session : String,
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
   var rssi: Float = 0.0f,
   var phase: Float = 0.0f
)


fun SensorData.asSensorDataEntity() =
   SensorDataEntity(
         id = UUID.randomUUID().toString(),
         session =session,
         azimuth = azimuth,
         roll = roll,
         pitch =pitch,
         acceleratorX =acceleratorX,
         acceleratorY =acceleratorY,
         acceleratorZ = acceleratorZ,
         gravityX =gravityX,
         gravityY = gravityY,
         gravityZ =gravityZ,
         gyroscopeX =gyroscopeX,
         gyroscopeY =gyroscopeY,
         gyroscopeZ =gyroscopeZ,
         timeDelta =timeDelta,
         currentX = currentX,
         currentY =currentY,
         currentZ = currentZ,
         currentBX = currentBX,
         currentBY =currentBY,
         currentBZ = currentBZ,
         orientationX =orientationX,
         orientationY =orientationY,
         orientationZ =orientationZ,
         rssi = rssi,
         phase = phase
   )