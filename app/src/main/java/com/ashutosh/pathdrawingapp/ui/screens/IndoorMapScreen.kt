package com.ashutosh.pathdrawingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.pathdrawingapp.IndoorMapViewModel
import com.ashutosh.pathdrawingapp.NodeType
import com.ashutosh.pathdrawingapp.RoomSide
import com.ashutosh.pathdrawingapp.ui.components.CompassView
import com.ashutosh.pathdrawingapp.ui.components.FloorSelector
import com.ashutosh.pathdrawingapp.ui.components.MapCanvas
import com.ashutosh.pathdrawingapp.ui.components.OptionsSheet
import com.ashutosh.pathdrawingapp.ui.components.StopDetectedDialog
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun IndoorMapScreen(viewModel: IndoorMapViewModel) {
    val ui = viewModel.uiState.collectAsStateWithLifecycle().value
    val colors = LocalAppColors.current

    var showOptions by remember { mutableStateOf(false) }
    var showMoreStopOptions by remember { mutableStateOf(false) }

    if (showOptions) {
        OptionsSheet(
            onSave = { label, type, doors, side, customType ->
                viewModel.addLocationNode(label, type, doors, side, customType)
            },
            onDismiss = { showOptions = false },
        )
    }

    LaunchedEffect(ui.showStopDialog) {
        if (!ui.showStopDialog) {
            showMoreStopOptions = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            StatsHeader(
                steps = ui.stepCount,
                nodes = ui.nodes.count { it.floor == ui.currentFloor },
                floor = ui.currentFloor,
                walking = ui.isWalking,
                distanceMetres = ui.distanceMetres,
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search rooms, halls, toilets...", color = colors.inkSub)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surface,
                    focusedContainerColor = colors.surface,
                    unfocusedTextColor = colors.ink,
                    focusedTextColor = colors.ink,
                ),
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.mapBg,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, colors.border, RoundedCornerShape(26.dp)),
                ) {
                    MapCanvas(Modifier.fillMaxSize().padding(8.dp), ui)
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(72.dp),
                    shape = RoundedCornerShape(40.dp),
                    shadowElevation = 8.dp,
                    color = colors.surface,
                ) {
                    CompassView(ui.angle, Modifier.fillMaxSize().padding(8.dp))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FloorSelector(
                        selectedFloor = ui.currentFloor,
                        onFloorSelected = viewModel::setFloor,
                    )
                    ZoomPanel(
                        onZoomIn = viewModel::zoomIn,
                        onZoomOut = viewModel::zoomOut,
                    )
                    LegendPanel()
                }
            }

            Spacer(Modifier.height(10.dp))

            BottomBar(
                isRecording = ui.isRecording,
                onToggleRecording = viewModel::toggleRecording,
                onClear = viewModel::clearMap,
                onAddNode = { showOptions = true },
                onCaptureEntrance = viewModel::captureEntrance,
                onCaptureExit = viewModel::captureExit,
            )
        }

        StopDetectedDialog(
            visible = ui.showStopDialog && !showOptions,
            expanded = showMoreStopOptions,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            onLeftNode = {
                viewModel.addLocationNode("", NodeType.ROOM, 1, RoomSide.LEFT)
                viewModel.dismissStopDialog()
            },
            onRightNode = {
                viewModel.addLocationNode("", NodeType.ROOM, 1, RoomSide.RIGHT)
                viewModel.dismissStopDialog()
            },
            onMoreToggle = { showMoreStopOptions = !showMoreStopOptions },
            onQuickType = { type ->
                viewModel.addLocationNode("", type, 1, RoomSide.BOTH)
                viewModel.dismissStopDialog()
            },
            onModify = {
                viewModel.dismissStopDialog()
                showOptions = true
            },
        )
    }
}

@Composable
fun StatsHeader(
    steps: Int,
    nodes: Int,
    floor: String,
    walking: Boolean,
    distanceMetres: Float,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Chip("STEPS", steps.toString(), colors.accent)
        Chip("DIST", "%.1f m".format(distanceMetres), colors.accent)
        Chip("NODES", nodes.toString(), Color(0xFFE25BFF))
        Chip("FLOOR", floor, colors.amber)
        Chip(
            "STATUS",
            if (walking) "Walking" else "Stopped",
            if (walking) colors.green else colors.amber,
        )
    }
}

@Composable
fun Chip(t: String, v: String, c: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t, color = colors.inkSub, fontSize = 10.sp)
        Text(v, color = c, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ZoomPanel(onZoomIn: () -> Unit, onZoomOut: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassBtn("+", onZoomIn)
        GlassBtn("-", onZoomOut)
    }
}

@Composable
fun GlassBtn(t: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        modifier = Modifier
            .size(58.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(t, color = colors.accent, fontSize = 28.sp)
        }
    }
}

@Composable
fun LegendPanel() {
    val colors = LocalAppColors.current
    Surface(shape = RoundedCornerShape(18.dp), color = colors.surface) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Dot(colors.green)
            Dot(colors.red)
            Dot(colors.amber)
        }
    }
}

@Composable
fun Dot(c: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(c, RoundedCornerShape(50)),
    )
}

@Composable
fun BottomBar(
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onClear: () -> Unit,
    onAddNode: () -> Unit,
    onCaptureEntrance: () -> Unit,
    onCaptureExit: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onToggleRecording,
            modifier = Modifier
                .weight(1.4f)
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) colors.amber else colors.green,
                contentColor = Color.White,
            ),
        ) {
            Text(if (isRecording) "Stop" else "Start")
        }

        val actions = listOf(
            "Clear" to onClear,
            "Node" to onAddNode,
            "Entry" to onCaptureEntrance,
            "Exit" to onCaptureExit,
        )

        actions.forEach { (label, action) ->
            OutlinedButton(
                onClick = action,
                modifier = Modifier.height(72.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
            ) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}
