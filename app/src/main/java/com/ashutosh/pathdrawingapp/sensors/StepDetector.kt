package com.ashutosh.pathdrawingapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * StepDetector
 * ─────────────────────────────────────────────────────────────
 * Detects steps using the best available sensor:
 * 1. TYPE_STEP_DETECTOR (Hardware)
 * 2. TYPE_STEP_COUNTER (Hardware)
 * 3. TYPE_ACCELEROMETER (Software fallback)
 */
class StepDetector(
    context: Context,
    private val onStep: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val hardwareStepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val hardwareStepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val activeSensor: Sensor? = hardwareStepDetector ?: hardwareStepCounter ?: accelerometer

    private var lastCounterValue: Int? = null

    // For accelerometer fallback
    private var smoothedMag = 0f
    private var adaptiveThreshold = INITIAL_THRESHOLD
    private var lastStepMs = 0L
    private var isRising = false
    private val window = FloatArray(WINDOW_SIZE)
    private var winIdx = 0
    private var winFull = false

    companion object {
        private const val WINDOW_SIZE = 20
        private const val INITIAL_THRESHOLD = 13.0f
        private const val MIN_STEP_DELAY_MS = 300L
    }

    fun start() {
        activeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        reset()
    }

    private fun reset() {
        smoothedMag = 0f
        adaptiveThreshold = INITIAL_THRESHOLD
        lastStepMs = 0L
        isRising = false
        lastCounterValue = null
        window.fill(0f)
        winIdx = 0
        winFull = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                onStep()
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val currentValue = event.values.firstOrNull()?.toInt() ?: return
                val previousValue = lastCounterValue
                lastCounterValue = currentValue
                if (previousValue != null) {
                    val delta = (currentValue - previousValue).coerceAtLeast(0)
                    repeat(delta) { onStep() }
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values)
            }
        }
    }

    private fun processAccelerometer(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        // Simple low-pass
        smoothedMag = smoothedMag * 0.9f + magnitude * 0.1f

        // Update sliding window for adaptive threshold
        window[winIdx] = smoothedMag
        winIdx = (winIdx + 1) % WINDOW_SIZE
        if (winIdx == 0) winFull = true

        if (winFull) {
            val avg = window.average().toFloat()
            adaptiveThreshold = avg + 0.5f // sensitivity offset
        }

        val now = System.currentTimeMillis()
        if (smoothedMag > adaptiveThreshold) {
            if (!isRising && (now - lastStepMs > MIN_STEP_DELAY_MS)) {
                onStep()
                lastStepMs = now
            }
            isRising = true
        } else {
            isRising = false
        }
    }
}
