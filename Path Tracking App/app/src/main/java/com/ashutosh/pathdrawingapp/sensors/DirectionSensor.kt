package com.ashutosh.pathdrawingapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * DirectionSensor
 * ─────────────────────────────────────────────────────────────────────────────
 * Reads device heading from TYPE_ROTATION_VECTOR and passes it through
 * the three-stage HeadingStabiliser pipeline before firing the callback.
 *
 * What changed vs the old version:
 *   OLD → single fixed-alpha low-pass filter (jittery on slow rotation,
 *          sluggish on fast rotation, no corridor awareness)
 *   NEW → HeadingStabiliser with:
 *           • Median spike filter
 *           • Adaptive low-pass (alpha scales with rotation speed)
 *           • Soft map-based corridor hint (no rigid straight lock)
 */
class DirectionSensor(
    context: Context,
    private val onDirectionChanged: (Float) -> Unit,
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientation    = FloatArray(3)

    // Three-stage heading pipeline
    private val stabiliser = HeadingStabiliser()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        rotationSensor ?: return
        stabiliser.reset()
        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // ── SensorEventListener ───────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        // 1. Get raw azimuth from rotation vector
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

        // 2. Run through stabiliser pipeline
        val stable = stabiliser.process(rawAzimuth)

        // 3. Fire callback with clean heading
        onDirectionChanged(stable)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
