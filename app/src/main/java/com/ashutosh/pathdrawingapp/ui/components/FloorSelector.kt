package com.ashutosh.pathdrawingapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ashutosh.pathdrawingapp.ui.theme.LocalAppColors

@Composable
fun FloorSelector(
    selectedFloor: String,
    onFloorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier.width(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column {
            listOf("G", "1", "2", "3", "4", "5").forEach { floor ->
                val isSelected = selectedFloor == floor
                Button(
                    onClick = { onFloorSelected(floor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) colors.accent else Color.Transparent,
                        contentColor = if (isSelected) Color.White else colors.ink
                    )
                ) {
                    Text(floor)
                }
            }
        }
    }
}
