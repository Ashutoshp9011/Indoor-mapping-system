package com.ashutosh.pathdrawingapp

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
//  Enums
// ─────────────────────────────────────────────────────────────

enum class TurnInstruction { LEFT, STRAIGHT, RIGHT }
enum class NodeType         { ROOM, HALL, TOILET, LAB, CUSTOM }
enum class RoomSide         { LEFT, RIGHT, BOTH }

// ─────────────────────────────────────────────────────────────
//  Data models
// ─────────────────────────────────────────────────────────────

data class MapNode(
    val position:   Offset,
    val label:      String    = "Node",
    val type:       NodeType  = NodeType.ROOM,
    val doors:      Int       = 1,
    val side:       RoomSide  = RoomSide.BOTH,
    val floor:      String    = "G",
    val customType: String    = "",
)

data class MapUiState(
    // ── Path ──────────────────────────────────────────────────
    val pathPoints:     List<Offset> = listOf(Offset(200f, 480f)),
    val currentPos:     Offset       = Offset(200f, 480f),

    // ── Steps ─────────────────────────────────────────────────
    val stepCount:      Int   = 0,
    val distanceMetres: Float = 0f,

    // ── Status ────────────────────────────────────────────────
    val isWalking:      Boolean = false,
    val isRecording:    Boolean = false,
    val showStopDialog: Boolean = false,

    // ── Sensor ────────────────────────────────────────────────
    val angle:          Float   = 0f,

    // ── Navigation ────────────────────────────────────────────
    val instruction:    TurnInstruction = TurnInstruction.STRAIGHT,

    // ── Nodes ─────────────────────────────────────────────────
    val nodes:          List<MapNode> = emptyList(),

    // ── Map UI ────────────────────────────────────────────────
    val currentFloor:   String  = "G",
    val zoom:           Float   = 1f,
    val panOffset:      Offset  = Offset.Zero,

    // ── Gates ─────────────────────────────────────────────────
    val entrancePos:    Offset? = null,
    val exitPos:        Offset? = null,

    // ── Theme ─────────────────────────────────────────────────
    val isDarkMode:     Boolean = false,
)

// ─────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────

/**
 * IndoorMapViewModel
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * WHAT CHANGED vs the old version
 * ────────────────────────────────
 *
 * OLD:
 *   • Fixed 18 px step length every time
 *   • Heading directly from sensor → ViewModel (raw noise reached the map)
 *   • No path history awareness
 *
 * NEW — three improvements that work together:
 *
 * 1. Adaptive step length
 *      StepDetector now calls onStepDetected(stepLengthPx, angleDeg)
 *      where stepLengthPx is the Weinberg per-step estimate.
 *      The ViewModel just uses whatever the detector measured.
 *
 * 2. Heading stabilisation (done inside DirectionSensor / HeadingStabiliser)
 *      By the time a heading arrives here it has already passed through
 *      median filter → adaptive LPF → soft corridor hint.
 *      The ViewModel stores the clean heading and uses it directly.
 *
 * 3. Path-history heading hint (map-based, inside ViewModel)
 *      After each step we look at the last PATH_HINT_WINDOW path points.
 *      If the recent path direction (from first to last of that window)
 *      is very close to the new sensor heading, we blend the two slightly.
 *      This smooths out micro-wobbles that survive the sensor pipeline.
 *
 *      Again — NO RIGID LOCK. The blend weight is at most PATH_HINT_MAX (0.25),
 *      so the sensor always dominates. If the user genuinely turns, the
 *      path history quickly becomes stale and the sensor takes over fully.
 */
class IndoorMapViewModel : ViewModel() {

    // ── Tuning ────────────────────────────────────────────────────────────────

    /**
     * How many recent path points to look at when computing the
     * "recent path direction" hint. Larger = more inertia on straights.
     */
    private val PATH_HINT_WINDOW = 6

    /**
     * Maximum weight given to the path-history hint. Must be < 0.5.
     * 0.25 means sensor still contributes ≥ 75% of the final heading.
     */
    private val PATH_HINT_MAX = 0.25f

    /**
     * Only apply path hint when sensor and path directions agree within
     * this many degrees. If they disagree more → user is turning → skip hint.
     */
    private val PATH_HINT_AGREE_DEG = 30f

    /**
     * Metres per step used for distance calculation.
     * The actual canvas movement comes from StepDetector (Weinberg model).
     */
    private val STRIDE_METRES = 0.762f

    // ── State ─────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var stopTimerJob: Job? = null

    // ── Sensor inputs ─────────────────────────────────────────────────────────

    /** Called by DirectionSensor on every stabilised heading reading. */
    fun updateAngle(angle: Float) {
        _uiState.update { it.copy(angle = angle) }
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    fun toggleRecording() {
        val nowRecording = !_uiState.value.isRecording
        _uiState.update { it.copy(isRecording = nowRecording) }
        if (!nowRecording) {
            stopTimerJob?.cancel()
            _uiState.update { it.copy(isWalking = false) }
        }
    }

    // ── Step received from StepDetector ───────────────────────────────────────

    /**
     * Called by StepDetector after each confirmed step.
     *
     * @param stepLengthPx  Canvas pixels for this step (Weinberg adaptive value).
     * @param sensorAngleDeg  Stabilised heading from DirectionSensor (degrees).
     *
     * Processing:
     *   1. Apply path-history heading hint (soft, non-locking).
     *   2. Compute new canvas position using final heading.
     *   3. Append to path.
     *   4. Restart stop timer.
     */
    fun onStepDetected(stepLengthPx: Float, sensorAngleDeg: Float) {
        if (!_uiState.value.isRecording) return

        _uiState.update { state ->
            // ── Step 1: path-history heading hint ─────────────
            val finalAngle = applyPathHint(
                sensorAngle = sensorAngleDeg,
                pathPoints  = state.pathPoints,
            )

            // ── Step 2: compute new canvas position ───────────
            val prev     = state.currentPos
            val rad      = Math.toRadians(finalAngle.toDouble())
            val newX     = prev.x + stepLengthPx * cos(rad).toFloat()
            val newY     = prev.y - stepLengthPx * sin(rad).toFloat()
            val newPos   = Offset(newX, newY)
            val newSteps = state.stepCount + 1

            state.copy(
                currentPos     = newPos,
                pathPoints     = state.pathPoints + newPos,
                stepCount      = newSteps,
                distanceMetres = newSteps * STRIDE_METRES,
                isWalking      = true,
                showStopDialog = false,
                angle          = finalAngle,
            )
        }

        restartStopTimer()
    }

    // ── Path-history heading hint ─────────────────────────────────────────────

    /**
     * Looks at the most recent [PATH_HINT_WINDOW] path points to compute
     * a "recent path direction". If that direction agrees with the sensor
     * heading within [PATH_HINT_AGREE_DEG], blends them softly.
     *
     * This eliminates residual micro-wobbles on long straight corridors
     * without ever locking the direction or overriding genuine turns.
     */
    private fun applyPathHint(
        sensorAngle: Float,
        pathPoints:  List<Offset>,
    ): Float {
        // Need at least PATH_HINT_WINDOW points to form a history vector
        if (pathPoints.size < PATH_HINT_WINDOW) return sensorAngle

        val tail  = pathPoints.takeLast(PATH_HINT_WINDOW)
        val first = tail.first()
        val last  = tail.last()

        val dx = last.x - first.x
        val dy = last.y - first.y

        // If the last N steps barely moved, skip the hint (user was still)
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist < 2f) return sensorAngle

        // Canvas → compass angle (canvas y is inverted)
        val pathAngleDeg = Math.toDegrees(
            Math.atan2(-dy.toDouble(), dx.toDouble())
        ).toFloat()

        // Shortest arc between sensor and path direction
        var diff = pathAngleDeg - sensorAngle
        if (diff >  180f) diff -= 360f
        if (diff < -180f) diff += 360f

        // If they disagree by more than the threshold → user is turning → no hint
        if (Math.abs(diff) > PATH_HINT_AGREE_DEG) return sensorAngle

        // Soft blend: weight grows as agreement improves, capped at PATH_HINT_MAX
        val agreement   = 1f - Math.abs(diff) / PATH_HINT_AGREE_DEG   // 0…1
        val hintWeight  = agreement * PATH_HINT_MAX                    // 0…0.25

        var result = sensorAngle + hintWeight * diff
        if (result >  180f) result -= 360f
        if (result < -180f) result += 360f
        return result
    }

    // ── Turn instructions ─────────────────────────────────────────────────────

    fun setInstruction(instruction: TurnInstruction) {
        _uiState.update { it.copy(instruction = instruction) }
    }

    // ── Map controls ──────────────────────────────────────────────────────────

    fun zoomIn()  = _uiState.update { it.copy(zoom = (it.zoom + 0.2f).coerceAtMost(3.0f)) }
    fun zoomOut() = _uiState.update { it.copy(zoom = (it.zoom - 0.2f).coerceAtLeast(0.4f)) }

    fun setFloor(floor: String) {
        _uiState.update { it.copy(currentFloor = floor) }
    }

    fun centerOnCurrentPos() {
        _uiState.update { it.copy(panOffset = Offset.Zero) }
    }

    // ── Gates ─────────────────────────────────────────────────────────────────

    fun captureEntrance() = _uiState.update { it.copy(entrancePos = it.currentPos) }
    fun captureExit()     = _uiState.update { it.copy(exitPos     = it.currentPos) }

    // ── Nodes ─────────────────────────────────────────────────────────────────

    fun addLocationNode(
        label:      String,
        type:       NodeType,
        doors:      Int,
        side:       RoomSide,
        customType: String = "",
    ) {
        _uiState.update { state ->
            val fallback = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${state.nodes.size + 1}"
            val newNode  = MapNode(
                position   = state.currentPos,
                label      = label.ifBlank { fallback },
                type       = type,
                doors      = doors,
                side       = side,
                floor      = state.currentFloor,
                customType = customType,
            )
            state.copy(
                nodes          = state.nodes + newNode,
                showStopDialog = false,
            )
        }
    }

    fun clearMap() {
        stopTimerJob?.cancel()
        _uiState.value = MapUiState(isDarkMode = _uiState.value.isDarkMode)
    }

    fun dismissStopDialog() = _uiState.update { it.copy(showStopDialog = false) }

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun toggleDarkMode() = _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }

    // ── Stop detection ────────────────────────────────────────────────────────

    private fun restartStopTimer() {
        stopTimerJob?.cancel()
        stopTimerJob = viewModelScope.launch {
            delay(2_000L)
            _uiState.update { it.copy(isWalking = false, showStopDialog = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerJob?.cancel()
    }
}
