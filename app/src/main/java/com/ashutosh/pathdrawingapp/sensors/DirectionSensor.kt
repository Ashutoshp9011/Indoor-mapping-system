package com.ashutosh.pathdrawingapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * DirectionSensor
 * ─────────────────────────────────────────────────────────────
 * Reads device heading from TYPE_ROTATION_VECTOR and applies a
 * low-pass filter to eliminate jitter from small phone rotations.
 *
 * alpha = 0.10f → very smooth (slower response)
 * alpha = 0.20f → good balance  ← default
 * alpha = 0.35f → faster, slightly more reactive
 */
class DirectionSensor(
    context: Context,
    private val onDirectionChanged: (Float) -> Unit,
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientation    = FloatArray(3)

    private var smoothedAngle: Float? = null
    private val alpha = 0.18f

    fun start() {
        rotationSensor ?: return
        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val rawAngle = Math.toDegrees(orientation[0].toDouble()).toFloat()

        // First sample — no filter yet
        val current = smoothedAngle
        if (current == null) {
            smoothedAngle = rawAngle
            onDirectionChanged(rawAngle)
            return
        }

        // Handle 359° ↔ 0° wraparound
        var delta = rawAngle - current
        if (delta >  180f) delta -= 360f
        if (delta < -180f) delta += 360f

        var result = current + alpha * delta
        if (result >  180f) result -= 360f
        if (result < -180f) result += 360f

        smoothedAngle = result
        onDirectionChanged(result)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
