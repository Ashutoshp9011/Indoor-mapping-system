package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

private val CardBg = Color(0xFF141A26)
private val Border = Color(0xFF222B3D)
private val Red = Color(0xFFFF3B30)
private val Gray = Color(0xFF7E8798)

@Composable
fun CompassView(angle: Float, modifier: Modifier = Modifier) {

    Card(
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {

        Canvas(
            modifier = Modifier
                .size(72.dp)
                .background(CardBg)
        ) {

            val c = center

            // outer circle
            drawCircle(
                color = Border,
                radius = size.minDimension / 2.1f
            )

            rotate(angle, c) {

                // north arrow
                drawLine(
                    color = Red,
                    start = Offset(c.x, c.y),
                    end = Offset(c.x, 12f),
                    strokeWidth = 8f
                )

                // south arrow
                drawLine(
                    color = Gray,
                    start = Offset(c.x, c.y),
                    end = Offset(c.x, size.height - 12f),
                    strokeWidth = 8f
                )
            }
        }
    }
}