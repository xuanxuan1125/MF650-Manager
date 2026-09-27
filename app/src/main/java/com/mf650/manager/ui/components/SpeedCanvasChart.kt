package com.mf650.manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.ui.theme.SpeedDownload
import com.mf650.manager.ui.theme.SpeedUpload
import java.util.Locale

@Composable
fun SpeedCanvasChart(
    downloadSpeeds: List<Float>, // in KB/s
    uploadSpeeds: List<Float>,   // in KB/s
    modifier: Modifier = Modifier,
    heightDp: Int = 120
) {
    val currentDown = downloadSpeeds.lastOrNull() ?: 0f
    val currentUp = uploadSpeeds.lastOrNull() ?: 0f

    val maxDown = downloadSpeeds.maxOrNull() ?: 100f
    val maxUp = uploadSpeeds.maxOrNull() ?: 100f
    val maxY = (maxOf(maxDown, maxUp, 100f) * 1.2f).coerceAtLeast(100f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "↓ ${formatSpeed(currentDown)}",
                    color = SpeedDownload,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "↑ ${formatSpeed(currentUp)}",
                    color = SpeedUpload,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "峰值: ${formatSpeed(maxY)}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            val width = size.width
            val height = size.height

            // Draw horizontal guide lines (0%, 50%, 100%)
            val gridColor = Color.Gray.copy(alpha = 0.15f)
            drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

            if (downloadSpeeds.size > 1) {
                val stepX = width / (downloadSpeeds.size - 1).coerceAtLeast(1)

                // Download path
                val downPath = Path()
                downloadSpeeds.forEachIndexed { i, speed ->
                    val x = i * stepX
                    val y = height - ((speed / maxY) * height).coerceIn(0f, height)
                    if (i == 0) downPath.moveTo(x, y) else downPath.lineTo(x, y)
                }
                drawPath(
                    path = downPath,
                    color = SpeedDownload,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Upload path
                val upPath = Path()
                uploadSpeeds.forEachIndexed { i, speed ->
                    val x = i * stepX
                    val y = height - ((speed / maxY) * height).coerceIn(0f, height)
                    if (i == 0) upPath.moveTo(x, y) else upPath.lineTo(x, y)
                }
                drawPath(
                    path = upPath,
                    color = SpeedUpload,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun formatSpeed(kbs: Float): String {
    return if (kbs >= 1024f) {
        String.format(Locale.US, "%.1f MB/s", kbs / 1024f)
    } else {
        String.format(Locale.US, "%.0f KB/s", kbs)
    }
}
