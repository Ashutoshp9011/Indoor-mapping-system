package com.ashutosh.pathdrawingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.pathdrawingapp.IndoorMapViewModel
import com.ashutosh.pathdrawingapp.ui.components.CompassView
import com.ashutosh.pathdrawingapp.ui.components.FloorSelector
import com.ashutosh.pathdrawingapp.ui.components.MapCanvas
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun IndoorMapScreen(viewModel: IndoorMapViewModel) {
    val ui = viewModel.uiState.collectAsStateWithLifecycle().value
    val colors = LocalAppColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(10.dp)
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
                placeholder = { Text("Search rooms, halls, toilets...", color = colors.inkSub) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surface,
                    focusedContainerColor = colors.surface,
                    unfocusedTextColor = colors.ink,
                    focusedTextColor = colors.ink
                )
            )
        
        Spacer(Modifier.height(10.dp))
        
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colors.mapBg,
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, colors.border, RoundedCornerShape(26.dp))
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
                color = colors.surface
            ) {
                CompassView(ui.angle, Modifier.fillMaxSize().padding(8.dp))
            }
            
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
            onAddNode = { viewModel.addLocationNode() },
            onCaptureEntrance = viewModel::captureEntrance,
            onCaptureExit = viewModel::captureExit,
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Chip("STEPS", steps.toString(), colors.accent)
        Chip("DIST", "%.1f m".format(distanceMetres), colors.accent)
        Chip("NODES", nodes.toString(), Color(0xFFE25BFF))
        Chip("FLOOR", floor, colors.amber)
        Chip("STATUS", if (walking) "Walking" else "Stopped", if (walking) colors.green else colors.amber)
    }
}

@Composable
fun Chip(t: String, v: String, c: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t, color = colors.inkSub, fontSize = 10.sp)
        Text(v, color = colors.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
            .shadow(8.dp, RoundedCornerShape(18.dp))
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
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Dot(colors.green)
            Dot(colors.red)
            Dot(colors.amber)
        }
    }
}

@Composable
fun Dot(c: Color) {
    Box(
        Modifier
            .size(18.dp)
            .background(c, RoundedCornerShape(50))
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onToggleRecording,
            modifier = Modifier
                .weight(1.4f)
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) colors.amber else colors.green,
                contentColor = Color.White
            )
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink)
            ) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}
