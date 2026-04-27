package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.pathdrawingapp.NodeType
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun InlineStopActionBar(
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
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface.copy(alpha = 0.96f),
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CompactActionButton(
                        label = "Left",
                        color = colors.green,
                        modifier = Modifier.weight(1f),
                        onClick = onLeftNode,
                    )
                    Button(
                        onClick = onMoreToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceHigh,
                            contentColor = colors.ink,
                        ),
                    ) {
                        Text(if (expanded) "Less" else "More", fontWeight = FontWeight.SemiBold)
                    }
                    CompactActionButton(
                        label = "Right",
                        color = colors.accent,
                        modifier = Modifier.weight(1f),
                        onClick = onRightNode,
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InlineChoiceChip("Room", Modifier.weight(1f)) { onQuickType(NodeType.ROOM) }
                            InlineChoiceChip("Toilet", Modifier.weight(1f)) { onQuickType(NodeType.TOILET) }
                            InlineChoiceChip("Hall", Modifier.weight(1f)) { onQuickType(NodeType.HALL) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InlineChoiceChip("Lab", Modifier.weight(1f)) { onQuickType(NodeType.LAB) }
                            InlineChoiceChip("Modify", Modifier.weight(1f)) { onModify() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InlineChoiceChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    FilterChip(
        selected = false,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
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