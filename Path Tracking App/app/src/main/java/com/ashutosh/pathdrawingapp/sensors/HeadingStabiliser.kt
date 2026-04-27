package com.ashutosh.pathdrawingapp.sensors

import kotlin.math.*

/**
 * HeadingStabiliser
 * ─────────────────────────────────────────────────────────────────────────────
 * Three-stage pipeline that produces a reliable indoor heading from noisy
 * azimuth readings, without ever rigidly "locking" the path to straight.
 *
 * Stage 1 — Spike Rejection (3-sample median)
 *   Removes sudden jumps caused by magnetic interference or sensor glitches.
 *   A single bad reading cannot affect the output because the median of
 *   [prev, current, next-candidate] is used instead of the raw value.
 *
 * Stage 2 — Adaptive Low-Pass Filter
 *   The filter strength (alpha) adjusts automatically:
 *     • Small delta  (<  8°) → heavy smoothing (alpha 0.10) — kills jitter
 *     • Medium delta (8–35°) → moderate blend  (alpha 0.25) — normal walk turn
 *     • Large delta  (> 35°) → light smoothing  (alpha 0.55) — intentional turn
 *   This means tiny phone wiggles are absorbed, real turns are tracked quickly.
 *
 * Stage 3 — Map-Based Heading Hint (soft corridor snap)
 *   When the user is walking in a stable direction (heading variance < threshold)
 *   the output is GENTLY nudged toward the nearest likely corridor axis
 *   (every 45°: N, NE, E, SE, S, SW, W, NW).
 *
 *   Key design choice — NO RIGID LOCK:
 *     snapStrength = 0.0 … 0.35 (never reaches 1.0)
 *   So the heading is always a weighted blend of sensor + hint.
 *   The user can still walk at any angle; the hint just reduces accumulated
 *   drift on long straight corridors.
 *
 * Usage:
 *   val stabiliser = HeadingStabiliser()
 *   val heading = stabiliser.process(rawAzimuthDegrees)
 */
class HeadingStabiliser {

    // ── Tuning constants ──────────────────────────────────────────────────────

    /** Corridor grid spacing in degrees. 45 = N/NE/E/SE/S/SW/W/NW. */
    private val SNAP_GRID_DEG = 45f

    /**
     * Maximum soft-snap strength. Range 0.0–1.0.
     * 0.35 means the hint can nudge at most 35% toward the grid axis.
     * Increase for straighter corridors; decrease for free-form movement.
     */
    private val MAX_SNAP_STRENGTH = 0.30f

    /**
     * How many consecutive readings must be stable before snap activates.
     * Higher = more inertia before hint kicks in (prevents snap on brief pauses).
     */
    private val STABILITY_WINDOW = 12

    /**
     * Heading variance threshold (degrees²) below which the path is considered
     * "stable enough" to apply corridor hints.
     */
    private val STABILITY_THRESHOLD_DEG2 = 18f

    // ── Internal state ────────────────────────────────────────────────────────

    // Stage 1 — median buffer (3 samples)
    private val medianBuf = FloatArray(3) { 0f }
    private var medIdx    = 0
    private var medFull   = false

    // Stage 2 — smoothed output
    private var smoothed: Float? = null

    // Stage 3 — stability tracker
    private val varianceWindow = FloatArray(STABILITY_WINDOW)
    private var varIdx  = 0
    private var varFull = false

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Feed one raw azimuth reading (degrees, –180…+180 or 0…360).
     * Returns the stabilised heading in degrees (–180…+180).
     */
    fun process(rawDeg: Float): Float {

        // ── Stage 1: spike rejection ──────────────────────────
        medianBuf[medIdx % 3] = rawDeg
        medIdx++
        medFull = medFull || medIdx >= 3
        val afterMedian = if (medFull) median3(medianBuf) else rawDeg

        // ── Stage 2: adaptive low-pass ────────────────────────
        val current = smoothed
        if (current == null) {
            smoothed = afterMedian
            return afterMedian
        }

        val delta = shortestArc(afterMedian, current)
        val alpha = adaptiveAlpha(abs(delta))

        var stage2 = current + alpha * delta
        stage2 = normalise(stage2)
        smoothed = stage2

        // ── Stage 3: soft corridor hint ───────────────────────
        varianceWindow[varIdx % STABILITY_WINDOW] = stage2
        varIdx++
        varFull = varFull || varIdx >= STABILITY_WINDOW

        val output = if (varFull) {
            val variance = headingVariance(varianceWindow)
            if (variance < STABILITY_THRESHOLD_DEG2) {
                applySoftSnap(stage2, variance)
            } else {
                stage2
            }
        } else {
            stage2
        }

        return normalise(output)
    }

    /** Reset all internal state (call when recording stops). */
    fun reset() {
        medianBuf.fill(0f);  medIdx = 0;  medFull = false
        varianceWindow.fill(0f); varIdx = 0; varFull = false
        smoothed = null
    }

    // ── Stage helpers ─────────────────────────────────────────────────────────

    /**
     * Adaptive alpha: big rotation → higher alpha (faster tracking),
     * tiny rotation → low alpha (heavier smoothing = jitter killed).
     */
    private fun adaptiveAlpha(absDeltaDeg: Float): Float = when {
        absDeltaDeg <  8f  -> 0.10f   // micro-jitter: very smooth
        absDeltaDeg < 35f  -> 0.25f   // normal walking turn: balanced
        else               -> 0.55f   // intentional sharp turn: responsive
    }

    /**
     * Softly nudge [heading] toward the nearest corridor axis (every 45°).
     * snapStrength grows with stability (lower variance → stronger hint)
     * but never exceeds MAX_SNAP_STRENGTH so the lock is never rigid.
     */
    private fun applySoftSnap(heading: Float, variance: Float): Float {
        // Find nearest corridor axis
        val gridCount  = (360f / SNAP_GRID_DEG).roundToInt()
        val normalized = ((heading % 360f) + 360f) % 360f
        val nearest    = (normalized / SNAP_GRID_DEG).roundToInt() % gridCount * SNAP_GRID_DEG

        // Snap strength: 0 at threshold, MAX at variance = 0
        val stabilityRatio  = (1f - variance / STABILITY_THRESHOLD_DEG2).coerceIn(0f, 1f)
        val snapStrength    = stabilityRatio * MAX_SNAP_STRENGTH

        // Blend heading toward the hint
        val arc = shortestArc(nearest, heading)
        return normalise(heading + snapStrength * arc)
    }

    // ── Math utilities ────────────────────────────────────────────────────────

    /** Shortest signed arc from [from] to [to] in degrees (–180…+180). */
    private fun shortestArc(to: Float, from: Float): Float {
        var d = to - from
        if (d >  180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    /** Wrap angle to –180…+180 range. */
    private fun normalise(deg: Float): Float {
        var d = deg % 360f
        if (d >  180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    /** Median of a 3-element float array (sort-free). */
    private fun median3(a: FloatArray): Float {
        val x = a[0]; val y = a[1]; val z = a[2]
        return if (x <= y) {
            if (y <= z) y else if (x <= z) z else x
        } else {
            if (x <= z) x else if (y <= z) z else y
        }
    }

    /**
     * Circular variance of heading values in degrees.
     * Uses mean of squared angular deviations from circular mean.
     */
    private fun headingVariance(angles: FloatArray): Float {
        val n    = angles.size.toFloat()
        val sinM = angles.sumOf { sin(Math.toRadians(it.toDouble())) }.toFloat() / n
        val cosM = angles.sumOf { cos(Math.toRadians(it.toDouble())) }.toFloat() / n
        val mean = Math.toDegrees(atan2(sinM.toDouble(), cosM.toDouble())).toFloat()

        return angles.sumOf {
            var d = (it - mean).toDouble()
            if (d >  180.0) d -= 360.0
            if (d < -180.0) d += 360.0
            d * d
        }.toFloat() / n
    }
}
