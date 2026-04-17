package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.pathdrawingapp.NodeType
import com.ashutosh.pathdrawingapp.RoomSide
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun OptionsSheet(
    onSave: (label: String, type: NodeType, doors: Int, side: RoomSide, customType: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current

    var selectedType by remember { mutableStateOf(NodeType.ROOM) }
    var name         by remember { mutableStateOf("") }
    var doors        by remember { mutableIntStateOf(1) }
    var side         by remember { mutableStateOf(RoomSide.BOTH) }
    var customName   by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.surface)
                .clickable(enabled = false) {}   // Prevent dismiss tap pass-through
                .padding(bottom = 28.dp),
        ) {
            // ── Handle ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.border, RoundedCornerShape(2.dp))
            )

            // ── Title row ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Node Options",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.ink,
                    letterSpacing = (-0.3).sp,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceHigh)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 13.sp, color = colors.inkSub)
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Type selector ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeOptions.forEach { opt ->
                    val active = selectedType == opt.type
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) opt.softColor(colors) else colors.surfaceHigh)
                            .border(
                                width = 1.5.dp,
                                color = if (active) opt.color(colors) else colors.border,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { selectedType = opt.type }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(opt.icon, fontSize = 20.sp)
                        Text(
                            opt.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) opt.color(colors) else colors.inkSub,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Custom type name (only when CUSTOM selected) ──
            if (selectedType == NodeType.CUSTOM) {
                SheetInput(
                    value       = customName,
                    onValue     = { customName = it },
                    placeholder = "Custom type name (e.g. Lab, Cafeteria)…",
                    modifier    = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(10.dp))
            }

            // ── Node name ─────────────────────────────────────
            SheetInput(
                value       = name,
                onValue     = { name = it },
                placeholder = "Node name (e.g. Room 101, Main Hall)…",
                modifier    = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(14.dp))

            // ── Doors + Side row ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Doors counter
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        "DOORS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkLight,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CounterBtn("-", colors.border, colors.ink) {
                            doors = (doors - 1).coerceAtLeast(0)
                        }
                        Text(
                            doors.toString(),
                            modifier = Modifier.weight(1f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.ink,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        CounterBtn("+", colors.accent, Color.White) {
                            doors = (doors + 1).coerceAtMost(10)
                        }
                    }
                }

                // Side selector
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        "ROOM SIDE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkLight,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sideOptions.forEach { opt ->
                            val active = side == opt.side
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) colors.accentSoft else colors.surface)
                                    .border(
                                        1.5.dp,
                                        if (active) colors.accent else colors.border,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { side = opt.side }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    opt.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) colors.accent else colors.inkSub,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Save button ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.accent)
                    .clickable {
                        onSave(name, selectedType, doors, side, customName)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Save Node",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun SheetInput(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    BasicTextField(
        value = value,
        onValueChange = onValue,
        cursorBrush = SolidColor(colors.accent),
        textStyle = TextStyle(color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceHigh)
            .border(1.5.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 13.sp, color = colors.inkLight)
            }
            inner()
        }
    )
}

@Composable
private fun CounterBtn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ─────────────────────────────────────────────────────────────
//  Type + Side option configs
// ─────────────────────────────────────────────────────────────

private data class TypeOption(
    val type: NodeType,
    val icon: String,
    val label: String,
    val color: (com.ashutosh.pathdrawingapp.ui.theme.AppColors) -> Color,
    val softColor: (com.ashutosh.pathdrawingapp.ui.theme.AppColors) -> Color,
)

private val typeOptions = listOf(
    TypeOption(NodeType.ROOM,   "🚪", "Room",   { it.accent },         { it.accentSoft }),
    TypeOption(NodeType.HALL,   "🏛️", "Hall",   { Color(0xFF8B5CF6) }, { Color(0xFFF3EEFF) }),
    TypeOption(NodeType.TOILET, "🚻", "Toilet", { it.amber },          { it.amberSoft }),
    TypeOption(NodeType.CUSTOM, "✏️", "Custom", { it.green },          { it.greenSoft }),
)

private data class SideOption(val side: RoomSide, val label: String)
private val sideOptions = listOf(
    SideOption(RoomSide.LEFT,  "◀ L"),
    SideOption(RoomSide.BOTH,  "◀▶"),
    SideOption(RoomSide.RIGHT, "R ▶"),
)

