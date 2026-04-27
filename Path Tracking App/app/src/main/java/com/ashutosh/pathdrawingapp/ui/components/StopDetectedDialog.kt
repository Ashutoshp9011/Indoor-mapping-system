package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.pathdrawingapp.NodeType
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun StopDetectedDialog(
    visible: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onLeftNode: () -> Unit,
    onRightNode: () -> Unit,
    onMoreToggle: () -> Unit,
    onQuickType: (NodeType) -> Unit,
    onModify: () -> Unit,
) {
    val colors = LocalAppColors.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "You stopped walking",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink,
                )

                Text(
                    text = "Add a quick left/right node, or open more place options.",
                    fontSize = 12.sp,
                    color = colors.inkSub,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickNodeButton(
                        label = "Left",
                        color = colors.accent,
                        modifier = Modifier.weight(1f),
                        onClick = onLeftNode,
                    )
                    Button(
                        onClick = onMoreToggle,
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceHigh,
                            contentColor = colors.ink,
                        ),
                    ) {
                        Text(if (expanded) "Less" else "More", fontWeight = FontWeight.SemiBold)
                    }
                    QuickNodeButton(
                        label = "Right",
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                        onClick = onRightNode,
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MoreChip("Room", onClick = { onQuickType(NodeType.ROOM) })
                            MoreChip("Toilet", onClick = { onQuickType(NodeType.TOILET) })
                            MoreChip("Hall", onClick = { onQuickType(NodeType.HALL) })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MoreChip("Lab", onClick = { onQuickType(NodeType.LAB) })
                            MoreChip("Modify", onClick = onModify)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNodeButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoreChip(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Medium) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceHigh,
            labelColor = colors.ink,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = colors.borderMid,
            selectedBorderColor = colors.accent,
        ),
    )
}
