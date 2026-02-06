package com.smartmemory.recall.ui.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    amplitudes: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = 4.dp.toPx()
        val spacing = 2.dp.toPx()
        val totalBarWidth = barWidth + spacing
        val maxBars = (width / totalBarWidth).toInt()
        
        val displayAmplitudes = amplitudes.takeLast(maxBars)
        
        displayAmplitudes.forEachIndexed { index, amplitude ->
            // Draw from right to left
            val x = width - (displayAmplitudes.size - index) * totalBarWidth
            
            // Normalize amplitude (max is usually 32767 for MediaRecorder)
            // We ensure a minimum height of 5% for visual consistency
            val normalizedHeight = (amplitude.toFloat() / 32767f).coerceIn(0.05f, 1f) * height
            val startY = (height - normalizedHeight) / 2
            
            drawRect(
                color = color,
                topLeft = Offset(x, startY),
                size = Size(barWidth, normalizedHeight)
            )
        }
    }
}
