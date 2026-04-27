package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.ashutosh.pathdrawingapp.MapNode
import com.ashutosh.pathdrawingapp.MapUiState
import com.ashutosh.pathdrawingapp.NodeType
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    uiState: MapUiState
) {
    val colors = LocalAppColors.current

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val floorNodes = uiState.nodes.filter { it.floor == uiState.currentFloor }
        val floorPath = uiState.pathPoints

        clipRect {
            translate(
                left = uiState.panOffset.x,
                top = uiState.panOffset.y,
            ) {
                scale(
                    scale = uiState.zoom,
                    pivot = center,
                ) {
                    drawGrid(w, h, colors.mapGrid)

                    if (floorPath.size > 1) {
                        for (index in 0 until floorPath.lastIndex) {
                            drawLine(
                                color = colors.pathBlue,
                                start = floorPath[index],
                                end = floorPath[index + 1],
                                strokeWidth = 8f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    floorNodes.forEach { node ->
                        drawNode(node, colors)
                    }

                    uiState.entrancePos?.let { drawMarker(it, colors.entrance) }
                    uiState.exitPos?.let { drawMarker(it, colors.exit) }
                    
                    drawCurrentPosition(uiState.currentPos, uiState.angle, colors.accent, Color.Gray)
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(w: Float, h: Float, color: Color) {
    val gap = 60f
    var x = 0f
    while (x < w) {
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
        x += gap
    }
    var y = 0f
    while (y < h) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        y += gap
    }
}

private fun DrawScope.drawNode(node: MapNode, colors: com.ashutosh.pathdrawingapp.ui.theme.AppColors) {
    val color = when (node.type) {
        NodeType.ROOM -> colors.accent
        NodeType.HALL -> colors.purple
        NodeType.TOILET -> colors.amber
        NodeType.LAB -> colors.green
        NodeType.CUSTOM -> colors.green
    }

    drawCircle(color = color, radius = 20f, center = node.position)
    drawCircle(color = colors.surface, radius = 10f, center = node.position)
}

// Extension property for purple since it's missing in AppColors but used in code
private val com.ashutosh.pathdrawingapp.ui.theme.AppColors.purple: Color get() = Color(0xFFE25BFF)

private fun DrawScope.drawMarker(position: Offset, color: Color) {
    drawCircle(
        color = color,
        radius = 20f,
        center = position,
        style = Stroke(width = 5f, pathEffect = PathEffect.cornerPathEffect(12f)),
    )
}

private fun DrawScope.drawCurrentPosition(
    position: Offset,
    angle: Float,
    accentColor: Color,
    dotColor: Color
) {
    drawCircle(color = dotColor, radius = 13f, center = position)
    drawCircle(color = accentColor.copy(alpha = 0.22f), radius = 22f, center = position)

    val arrowLength = 42f
    val angleRad = Math.toRadians(angle.toDouble())
    val tip = Offset(
        x = position.x + arrowLength * cos(angleRad).toFloat(),
        y = position.y - arrowLength * sin(angleRad).toFloat(),
    )

    drawLine(color = accentColor, start = position, end = tip, strokeWidth = 6f, cap = StrokeCap.Round)

    rotate(degrees = -angle, pivot = tip) {
        drawLine(color = accentColor, start = tip, end = Offset(tip.x - 14f, tip.y - 10f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color = accentColor, start = tip, end = Offset(tip.x - 14f, tip.y + 10f), strokeWidth = 4f, cap = StrokeCap.Round)
    }
}
