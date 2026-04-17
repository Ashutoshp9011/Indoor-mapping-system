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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────
//  Enums
// ─────────────────────────────────────────────────────────────

enum class TurnInstruction { LEFT, STRAIGHT, RIGHT }

enum class NodeType { ROOM, HALL, TOILET, CUSTOM }

enum class RoomSide { LEFT, RIGHT, BOTH }

// ─────────────────────────────────────────────────────────────
//  Data Models
// ─────────────────────────────────────────────────────────────

data class MapNode(
    val position: Offset,
    val label: String     = "Node",
    val type: NodeType    = NodeType.ROOM,
    val doors: Int        = 1,
    val side: RoomSide    = RoomSide.BOTH,
    val floor: String     = "G",
    val customType: String = "",
)

data class MapUiState(
    // ── Path ─────────────────────────────────────────────────
    val pathPoints: List<Offset> = listOf(Offset(200f, 480f)),
    val currentPos: Offset       = Offset(200f, 480f),

    // ── Steps ────────────────────────────────────────────────
    val stepCount: Int     = 0,
    val distanceMetres: Float = 0f,

    // ── Status ───────────────────────────────────────────────
    val isWalking: Boolean   = false,
    val isRecording: Boolean = false,
    val showStopDialog: Boolean = false,

    // ── Sensor ───────────────────────────────────────────────
    val angle: Float = 0f,

    // ── Navigation ───────────────────────────────────────────
    val instruction: TurnInstruction = TurnInstruction.STRAIGHT,

    // ── Nodes ────────────────────────────────────────────────
    val nodes: List<MapNode> = emptyList(),

    // ── Map UI ───────────────────────────────────────────────
    val currentFloor: String = "G",
    val zoom: Float          = 1f,
    val panOffset: Offset    = Offset.Zero,

    // ── Gates ────────────────────────────────────────────────
    val entrancePos: Offset? = null,
    val exitPos: Offset?     = null,

    // ── Theme ────────────────────────────────────────────────
    val isDarkMode: Boolean = false,
)

// ─────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────

class IndoorMapViewModel : ViewModel() {

    private val baseStepLengthPx = 22f
    private val baseStrideMetres = 0.762f
    private val minStrideFactor = 0.72f
    private val maxStrideFactor = 1.28f
    private val headingBlendAlpha = 0.22f
    private val mapHintBlend = 0.32f
    private val mapHintMaxDelta = 38f

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var stopTimerJob: Job? = null
    private var stabilizedHeading = 0f
    private var lastStepRealtimeMs: Long? = null

    // ── Sensor ───────────────────────────────────────────────

    fun updateAngle(angle: Float) {
        stabilizedHeading = blendAngles(stabilizedHeading, angle, headingBlendAlpha)
        _uiState.update { it.copy(angle = stabilizedHeading) }
    }

    // ── Recording ────────────────────────────────────────────

    fun toggleRecording() {
        val recording = !_uiState.value.isRecording
        _uiState.update { it.copy(isRecording = recording) }
        if (!recording) {
            stopTimerJob?.cancel()
            _uiState.update { it.copy(isWalking = false) }
        }
    }

    // ── Steps ────────────────────────────────────────────────

    fun onStepDetected(angleDeg: Float) {
        if (!_uiState.value.isRecording) return

        val nowMs = System.currentTimeMillis()
        val stepIntervalMs = lastStepRealtimeMs?.let { nowMs - it }
        lastStepRealtimeMs = nowMs

        val strideFactor = computeAdaptiveStrideFactor(stepIntervalMs)
        val resolvedHeading = computeResolvedHeading(angleDeg)
        val stepLengthPx = baseStepLengthPx * strideFactor
        val strideMetres = baseStrideMetres * strideFactor

        _uiState.update { state ->
            val prev     = state.currentPos
            val angleRad = Math.toRadians(resolvedHeading.toDouble())
            val newX     = prev.x + stepLengthPx * cos(angleRad).toFloat()
            val newY     = prev.y - stepLengthPx * sin(angleRad).toFloat()
            val newPos   = Offset(newX, newY)
            val newSteps = state.stepCount + 1

            state.copy(
                currentPos     = newPos,
                pathPoints     = state.pathPoints + newPos,
                stepCount      = newSteps,
                distanceMetres = state.distanceMetres + strideMetres,
                isWalking      = true,
                showStopDialog = false,
                angle          = resolvedHeading,
            )
        }
        restartStopTimer()
    }

    // ── Turn Instructions ─────────────────────────────────────

    fun setInstruction(instruction: TurnInstruction) {
        _uiState.update { it.copy(instruction = instruction) }
    }

    // ── Map Controls ─────────────────────────────────────────

    fun zoomIn()  = _uiState.update { it.copy(zoom = (it.zoom + 0.2f).coerceAtMost(3.0f)) }
    fun zoomOut() = _uiState.update { it.copy(zoom = (it.zoom - 0.2f).coerceAtLeast(0.4f)) }

    fun setFloor(floor: String) {
        _uiState.update { it.copy(currentFloor = floor) }
    }

    /** Recenters the pan offset so the current position is visible. */
    fun centerOnCurrentPos() {
        _uiState.update { it.copy(panOffset = Offset.Zero) }
    }

    // ── Gates ────────────────────────────────────────────────

    fun captureEntrance() {
        _uiState.update { it.copy(entrancePos = it.currentPos) }
    }

    fun captureExit() {
        _uiState.update { it.copy(exitPos = it.currentPos) }
    }

    // ── Nodes ────────────────────────────────────────────────

    fun addLocationNode(
        label: String = "",
        type: NodeType = NodeType.ROOM,
        doors: Int = 1,
        side: RoomSide = RoomSide.BOTH,
        customType: String = "",
    ) {
        _uiState.update { state ->
            val newNode = MapNode(
                position   = state.currentPos,
                label      = label.ifBlank { "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${state.nodes.size + 1}" },
                type       = type,
                doors      = doors,
                side       = side,
                floor      = state.currentFloor,
                customType = customType,
            )
            state.copy(
                nodes         = state.nodes + newNode,
                showStopDialog = false,
            )
        }
    }

    fun clearMap() {
        stopTimerJob?.cancel()
        stabilizedHeading = 0f
        lastStepRealtimeMs = null
        _uiState.value = MapUiState(isDarkMode = _uiState.value.isDarkMode)
    }

    fun dismissStopDialog() {
        _uiState.update { it.copy(showStopDialog = false) }
    }

    // ── Theme ────────────────────────────────────────────────

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    // ── Stop Detection ────────────────────────────────────────

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

    private fun computeAdaptiveStrideFactor(stepIntervalMs: Long?): Float {
        if (stepIntervalMs == null) return 1f

        return when {
            stepIntervalMs <= 430L -> 1.22f
            stepIntervalMs <= 520L -> 1.12f
            stepIntervalMs <= 650L -> 1.00f
            stepIntervalMs <= 820L -> 0.90f
            else -> 0.80f
        }.coerceIn(minStrideFactor, maxStrideFactor)
    }

    private fun computeResolvedHeading(rawHeading: Float): Float {
        val state = _uiState.value
        val smoothedHeading = blendAngles(stabilizedHeading, rawHeading, headingBlendAlpha)
        val hintedHeading = computeMapHintHeading(state)
        val resolvedHeading = if (hintedHeading == null) {
            smoothedHeading
        } else {
            val delta = signedAngleDelta(smoothedHeading, hintedHeading)
            if (abs(delta) <= mapHintMaxDelta) {
                normalizeAngle(smoothedHeading + delta * mapHintBlend)
            } else {
                smoothedHeading
            }
        }

        stabilizedHeading = resolvedHeading
        return resolvedHeading
    }

    private fun computeMapHintHeading(state: MapUiState): Float? {
        if (state.pathPoints.size >= 2) {
            val from = state.pathPoints[state.pathPoints.lastIndex - 1]
            val to = state.pathPoints.last()
            val dx = to.x - from.x
            val dy = from.y - to.y

            if (abs(dx) > 1f || abs(dy) > 1f) {
                val travelHeading = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                return snapToNearestCorridor(travelHeading)
            }
        }

        return snapToNearestCorridor(state.angle)
    }

    private fun snapToNearestCorridor(angle: Float): Float {
        val candidates = listOf(0f, 90f, 180f, -90f)
        return candidates.minByOrNull { abs(signedAngleDelta(angle, it)) } ?: angle
    }

    private fun blendAngles(current: Float, target: Float, alpha: Float): Float {
        val delta = signedAngleDelta(current, target)
        return normalizeAngle(current + delta * alpha)
    }

    private fun signedAngleDelta(from: Float, to: Float): Float {
        var delta = normalizeAngle(to) - normalizeAngle(from)
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        return delta
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle
        while (normalized > 180f) normalized -= 360f
        while (normalized < -180f) normalized += 360f
        return normalized
    }
}
