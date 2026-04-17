package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Local colours (using theme tokens where possible)
private val DialogBg      = Color(0xFFFFFFFF) // Changed to white for dark fonts
private val AccentCyan    = Color(0xFF1A6BFF)
private val AccentAmber   = Color(0xFFF59E0B)
private val TextPrimary   = Color(0xFF0F1923)
private val TextSecondary = Color(0xFF6B7A99)
private val DividerColor  = Color(0xFFE4E8F0)

/**
 * StopDetectedDialog — shown when the step-detection timeout fires.
 *
 * Gives the user two options:
 *  • "Add Location" — pins a node at the current position.
 *  • "Cancel"       — dismisses the dialog with no action.
 *
 * @param onAddLocation  Callback for the "Add Location" button.
 * @param onCancel       Callback for the "Cancel" button.
 */
@Composable
fun StopDetectedDialog(
    onAddLocation: () -> Unit,
    onCancel: () -> Unit
) {
    // Gentle scale-in animation on the dialog icon
    val infiniteTransition = rememberInfiniteTransition(label = "stopIcon")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dialog card
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DialogBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Icon ──────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AccentAmber.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = "Stopped",
                        tint = AccentAmber,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── Title ─────────────────────────────────────
                Text(
                    text = "You've Stopped",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Subtitle ──────────────────────────────────
                Text(
                    text = "No movement detected for 2 seconds.\nWould you like to save this location?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // ── Add Location button ───────────────────────
                Button(
                    onClick = onAddLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan.copy(alpha = 0.2f),
                        contentColor   = AccentCyan
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Add Location",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Cancel button ─────────────────────────────
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
