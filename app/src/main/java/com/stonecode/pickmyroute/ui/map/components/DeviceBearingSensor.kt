package com.stonecode.pickmyroute.ui.map.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/**
 * Composable that provides device compass bearing updates
 * Uses device rotation vector sensor for accurate orientation
 */
@Composable
fun rememberDeviceBearing(
    isEnabled: Boolean,
    onBearingChanged: (Float) -> Unit
): Float {
    val context = LocalContext.current
    var currentBearing by remember { mutableStateOf(0f) }

    DisposableEffect(isEnabled) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = if (isEnabled) {
            object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        // Get rotation matrix from rotation vector
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)

                        // Get device orientation (azimuth, pitch, roll)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        // Azimuth is the bearing (rotation around Z axis)
                        // Convert from radians to degrees and normalize to 0-360
                        var bearing = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (bearing < 0) {
                            bearing += 360f
                        }

                        currentBearing = bearing
                        onBearingChanged(bearing)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Not needed for rotation vector sensor
                }
            }.also { listener ->
                // Register sensor listener only if enabled
                rotationSensor?.let {
                    sensorManager.registerListener(
                        listener,
                        it,
                        SensorManager.SENSOR_DELAY_UI // Update at UI refresh rate
                    )
                }
            }
        } else {
            null
        }

        onDispose {
            listener?.let { sensorManager.unregisterListener(it) }
        }
    }

    return currentBearing
}

/**
 * Format bearing to cardinal direction (N, NE, E, SE, S, SW, W, NW)
 */
fun formatBearing(bearing: Float): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((bearing + 22.5f) / 45f).roundToInt() % 8
    return directions[index]
}
