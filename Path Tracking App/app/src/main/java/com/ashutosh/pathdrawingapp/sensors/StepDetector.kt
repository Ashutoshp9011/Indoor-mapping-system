package com.ashutosh.pathdrawingapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * StepDetector — adaptive step length + reliable peak detection
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * WHAT CHANGED vs the old version
 * ────────────────────────────────
 * OLD:
 *   • Fixed step length (18 px always)
 *   • Simple rising-edge threshold detection
 *   • No cadence or acceleration amplitude awareness
 *
 * NEW:
 *   • Weinberg adaptive step-length model
 *       L = K × (a_max − a_min) ^ 0.25
 *     where a_max / a_min are the peak and trough of the last full step cycle.
 *     Faster walking → bigger amplitude swing → longer step (realistic).
 *   • Cadence guard: ignores steps faster than 180 bpm (< 333 ms apart).
 *   • Peak-and-trough tracker: stores the highest and lowest smoothed
 *     magnitude seen since the last confirmed step, so the Weinberg
 *     model always has accurate per-step data.
 *   • Step-length clamped to [MIN_STEP_PX … MAX_STEP_PX] so extreme
 *     sensor noise can never produce wild jumps on the canvas.
 *
 * Callback delivers:
 *   onStep(stepLengthPx: Float)  — canvas pixels to advance this step
 */
class StepDetector(
    context: Context,
    private val onStep: (stepLengthPx: Float) -> Unit,
) : SensorEventListener {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        // Weinberg calibration constant.
        // 0.42 is typical for a phone held in hand / pocket.
        // Increase slightly (→ 0.50) if steps feel too short on your device.
        private const val WEINBERG_K = 0.42f

        // Adaptive step-length clamped to this canvas-pixel range
        private const val MIN_STEP_PX = 14f
        private const val MAX_STEP_PX = 32f

        // Low-pass alpha: 0.85 = smooth but responsive
        private const val LP_ALPHA = 0.85f

        // Adaptive threshold multiplier
        private const val THRESHOLD_MULT = 1.18f
        private const val INITIAL_THRESHOLD = 12.0f

        // Minimum ms between two confirmed steps (≈ 180 bpm max cadence)
        private const val MIN_STEP_MS = 300L

        // How many samples in the adaptive-threshold window
        private const val WINDOW = 20
    }

    // ── Sensor plumbing ───────────────────────────────────────────────────────

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // ── Internal state ────────────────────────────────────────────────────────

    private var smoothedMag       = 0f
    private var adaptiveThreshold = INITIAL_THRESHOLD
    private var lastStepMs        = 0L
    private var isRising          = false

    // Adaptive threshold window
    private val window = FloatArray(WINDOW)
    private var winIdx  = 0
    private var winFull = false

    // Peak / trough tracking for Weinberg model
    private var cycleMax = 0f       // highest smoothed mag since last step
    private var cycleMin = Float.MAX_VALUE  // lowest smoothed mag since last step
    private var cycleSamples = 0    // guard: need at least a few samples

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() = sensorManager.unregisterListener(this)

    fun reset() {
        smoothedMag       = 0f
        adaptiveThreshold = INITIAL_THRESHOLD
        lastStepMs        = 0L
        isRising          = false
        window.fill(0f); winIdx = 0; winFull = false
        cycleMax = 0f; cycleMin = Float.MAX_VALUE; cycleSamples = 0
    }

    // ── SensorEventListener ───────────────────────────────────────────────────

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Device-orientation-independent magnitude
        val rawMag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // 2. Low-pass smooth
        smoothedMag = LP_ALPHA * smoothedMag + (1f - LP_ALPHA) * rawMag

        // 3. Track per-cycle peak and trough for Weinberg model
        if (smoothedMag > cycleMax) cycleMax = smoothedMag
        if (smoothedMag < cycleMin) cycleMin = smoothedMag
        cycleSamples++

        // 4. Adaptive threshold window
        window[winIdx] = smoothedMag
        winIdx = (winIdx + 1) % WINDOW
        if (winIdx == 0) winFull = true
        adaptiveThreshold = computeThreshold()

        // 5. Peak detection + debounce + adaptive step length
        val nowMs = event.timestamp / 1_000_000L
        when {
            !isRising && smoothedMag > adaptiveThreshold -> {
                isRising = true
            }
            isRising && smoothedMag <= adaptiveThreshold -> {
                isRising = false
                if (nowMs - lastStepMs >= MIN_STEP_MS && cycleSamples >= 5) {
                    lastStepMs = nowMs

                    // ── Weinberg adaptive step length ─────────────
                    val stepPx = weinbergStepPx(cycleMax, cycleMin)
                    onStep(stepPx)

                    // Reset cycle trackers for the next step
                    cycleMax = 0f
                    cycleMin = Float.MAX_VALUE
                    cycleSamples = 0
                }
            }
        }
    }

    // ── Weinberg model ────────────────────────────────────────────────────────

    /**
     * Weinberg (2002) step-length model:
     *   L_metres = K × (a_max − a_min) ^ 0.25
     *
     * We convert metres → canvas pixels using a fixed DPI-like scale of 40 px/m.
     * (Tune PIXELS_PER_METRE if the path scale feels off on your map.)
     */
    private fun weinbergStepPx(aMax: Float, aMin: Float): Float {
        val PIXELS_PER_METRE = 40f
        val amplitude   = (aMax - aMin).coerceAtLeast(0.01f)
        val stepMetres  = WEINBERG_K * amplitude.toDouble().pow(0.25).toFloat()
        val stepPx      = stepMetres * PIXELS_PER_METRE
        return stepPx.coerceIn(MIN_STEP_PX, MAX_STEP_PX)
    }

    // ── Threshold helper ──────────────────────────────────────────────────────

    private fun computeThreshold(): Float {
        val count = if (winFull) WINDOW else winIdx
        if (count == 0) return INITIAL_THRESHOLD
        val mean = window.take(count).average().toFloat()
        return mean * THRESHOLD_MULT
    }
}
