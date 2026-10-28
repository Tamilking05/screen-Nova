package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DrawStroke(
    val path: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun ScreenBrushOverlay(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }
    var selectedColor by remember { mutableStateOf(Color(0xFFEF4444)) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }

    val paletteColors = listOf(
        Color(0xFFEF4444), // Neon Red
        Color(0xFFF97316), // Orange
        Color(0xFFFACC15), // Yellow
        Color(0xFF06B6D4), // Cyan
        Color(0xFF10B981), // Emerald
        Color(0xFFFFFFFF)  // White
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_brush_overlay")
    ) {
        // Drawing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedColor, strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                strokes.add(DrawStroke(currentPoints.toList(), selectedColor, strokeWidth))
                                currentPoints.clear()
                            }
                        },
                        onDragCancel = {
                            currentPoints.clear()
                        }
                    )
                }
        ) {
            // Draw finalized strokes
            for (stroke in strokes) {
                if (stroke.path.size > 1) {
                    val p = Path().apply {
                        moveTo(stroke.path.first().x, stroke.path.first().y)
                        for (i in 1 until stroke.path.size) {
                            lineTo(stroke.path[i].x, stroke.path[i].y)
                        }
                    }
                    drawPath(
                        path = p,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Draw current active stroke
            if (currentPoints.size > 1) {
                val p = Path().apply {
                    moveTo(currentPoints.first().x, currentPoints.first().y)
                    for (i in 1 until currentPoints.size) {
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(
                    path = p,
                    color = selectedColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Floating Palette Toolbar at top
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Brush",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Color choices
                paletteColors.forEach { col ->
                    val isSelected = selectedColor == col
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(col, CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = col }
                    )
                }

                // Undo
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear
                IconButton(
                    onClick = { strokes.clear() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close / Done
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF10B981), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
