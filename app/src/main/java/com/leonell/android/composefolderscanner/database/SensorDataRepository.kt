package com.leonell.android.composefolderscanner.database

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.view.View
import com.leonell.android.composefolderscanner.database.dao.SensorDataDao
import com.leonell.android.composefolderscanner.database.model.SensorDataEntity
import com.leonell.android.composefolderscanner.models.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class SensorDataRepository @Inject constructor(
   private val sensorDataDao: SensorDataDao,
) {

   // defining variable for storing magnetometer values
   private var magnetometerValues = FloatArray(3)
   private var previousTimestamp = 0L
   private var gravity = FloatArray(3)
   private var linearAcceleration = FloatArray(3)
   private var lastAzimuth: Double = 0.0

   // defining variable for stride length
   private var strideLength = 150f
   private var accelerometerReadings = FloatArray(3)

   // variables for tracking the user's path
   private var previousX = 0f
   private var previousY = 0f
   private var previousZ = 0f
   private var currX = 0f
   private var currY = 0f
   private var currZ = 0f
   private var rotationMatrix = FloatArray(9)
   private var inclinationMatrix = FloatArray(9)
   private var orientation = FloatArray(3)
   private var angularVelocity = FloatArray(3)
   private lateinit var gyroscope : Sensor
   private lateinit var gravitation : Sensor
   private lateinit var accelerometer : Sensor
   private lateinit var sensorManager: SensorManager

   //History for Acceleration
   private var accHist = FloatArray(3)
   //History for Velocity
   private var velHist = FloatArray(3)
   //X, Y, Z velocity values
   private var velocity = FloatArray(3)
   //X, Y, Z position values
   private var position = FloatArray(3)
   //X, Y, Z acceleration values
   private var accelFilter = FloatArray(3)
   private var lastTime: Long = 0
   //High-Pass Filtering Variables
   private val NS2S = 1.0f / 1000000000.0f
   private val NOISE = 0.1.toFloat()
   private var mInitialized = false
   fun setSensorManager(aSensorManager : SensorManager?){
      if(aSensorManager !=  null){
         sensorManager = aSensorManager
         gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
         gravitation = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)!!
         accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
      }
   }
   fun getAccelerometerDataFlow(): Flow<SensorData> = callbackFlow {
      val sensorListener = object : SensorEventListener {
         override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type){
               Sensor.TYPE_ACCELEROMETER -> {
                  accelerometerReadings = event.values
                  updateDistancePosition(event)

                  // a low pass filter to smooth out the accelerometer readings and to remove the contribution of gravity
                  val alpha = 0.8f
                  for (i in gravity.indices){
                     gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                  }

                  // removing gravity component from the accelerometer readings
                  for (i in linearAcceleration.indices){
                     linearAcceleration[i] = event.values[i] - gravity[i]
                  }

                  val displacement = strideLength
                  val deltaX = (displacement * sin(Math.toRadians(lastAzimuth))).toFloat()
                  val deltaY = (displacement * cos(Math.toRadians(lastAzimuth))).toFloat()
                  val deltaZ = lastAzimuth.toFloat()

                  // add the displacement to the previous coordinates to get the current coordinates
                  currX = previousX + deltaX
                  currY = previousY + deltaY
                  currZ = previousZ + deltaZ

                  // update the previous coordinates with the current coordinates
                  previousX = currX
                  previousY = currY
                  previousZ = currZ

               }
               Sensor.TYPE_GRAVITY -> {
                  // storing the magnetometer sensor readings in a list to use it in accelerometer
                  magnetometerValues = event.values
                  rotationMatrix = FloatArray(9)
                  inclinationMatrix = FloatArray(9)

                  // Get rotation matrix from inclination matrix, gravity and magnetometer values
                  SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magnetometerValues)

                  // From the rotation matrix we extract orientation angles
                  orientation = FloatArray(3)
                  SensorManager.getOrientation(rotationMatrix, orientation)

                  // converting orientation angle from radian to degrees
                  val azimuth = Math.toDegrees(orientation[0].toDouble())
                  lastAzimuth = azimuth
               }
               Sensor.TYPE_GYROSCOPE -> {
                  // storing the magnetometer sensor readings in a list to use it in accelerometer
                  angularVelocity = event.values
               }
            }

               val mySensorData = SensorData(
                  session = "",
                  currentX = currX,
                  currentY =  currY,
                  currentZ =  currZ,
                  currentBX = position[0],
                  currentBY =  position[1],
                  currentBZ =  position[2],
                  azimuth =  rotationMatrix[0],
                  pitch = rotationMatrix[1],
                  roll = rotationMatrix[2],
                  orientationX = orientation[0],
                  orientationY = orientation[1],
                  orientationZ = orientation[2],
                  acceleratorX = linearAcceleration[0],
                  acceleratorY = linearAcceleration[1],
                  acceleratorZ = linearAcceleration[2],
                  gravityX = gravity[0],
                  gravityY = gravity[1],
                  gravityZ = gravity[2],
                  gyroscopeX = angularVelocity[0],
                  gyroscopeY = angularVelocity[1],
                  gyroscopeZ = angularVelocity[2],
                  timeDelta = (System.currentTimeMillis() - previousTimestamp) / 1000.0f,
                  rssi = 0.0f,
                  phase = 0.0f
               )

               trySend(mySensorData)

               if (event != null) {
                  previousTimestamp = event.timestamp
               }
            }

         override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
         }

      }

      sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
      sensorManager.registerListener(sensorListener, gravitation, SensorManager.SENSOR_DELAY_UI)
      sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_UI)

      awaitClose {
         sensorManager.unregisterListener(sensorListener)
      }
   }.distinctUntilChanged()
   suspend fun upsertSensorEntities(entities: List<SensorDataEntity>) {
      sensorDataDao.upsertSensorEntities(entities)
   }

   suspend fun deleteSessions() {
      sensorDataDao.deleteSessions()
   }

   fun updateDistancePosition(event: SensorEvent) {
      for (i in accelFilter.indices){
         accelFilter[i] = event.values[i]
      }
      val deltaTime: Float = (event.timestamp - lastTime) * NS2S
      if (!mInitialized) {
         for (i in accHist.indices) {
            accHist[i] = accelFilter[i]
         }
         velocity = floatArrayOf(0f, 0f, 0f)
         position = floatArrayOf(0f, 0f, 0f)
         velHist[2] = 0f
         velHist[1] = velHist[2]
         velHist[0] = velHist[1]
         mInitialized = true
      } else {
         val deltaAcc = floatArrayOf(0f, 0f, 0f)
         for (i in deltaAcc.indices) {
            deltaAcc[i] = accelFilter[i] - accHist[i]
            if (abs(deltaAcc[i]) < NOISE) {
               deltaAcc[i] = 0.0.toFloat()
               accHist[i] = 0f
            }
         }

         //Reimann Sums to calculate velocity and position
         for (i in deltaAcc.indices) {
            if (deltaAcc[i] != 0f) velocity[i] += trapArea(
               accHist[i],
               accelFilter[i],
               deltaTime
            )
            if (velocity[i] < 0 || deltaAcc[i] == 0f){
               velocity[i] = 0f
            }
            position[i] += trapArea(velHist[i], velocity[i], deltaTime)
            velHist[i] = velocity[i]
         }
      }
      lastTime = event.timestamp
      for (i in accHist.indices){
         accHist[i] = accelFilter[i]
      }
   }


   fun resetSensorHistory() {
      previousX = 0f
      previousY = 0f
      previousZ = 0f
      currX = 0f
      currY = 0f
      currZ = 0f
      accHist = FloatArray(3)
      //History for Velocity
      velHist = FloatArray(3)
      //X, Y, Z velocity values
      velocity = FloatArray(3)
      //X, Y, Z position values
      position = FloatArray(3)
      //X, Y, Z acceleration values
      accelFilter = FloatArray(3)
      lastTime = 0
      previousTimestamp = 0
   }

   private fun trapArea(past: Float, current: Float, dT: Float): Float {
      return 0.5f * dT * (past + current)
   }


}