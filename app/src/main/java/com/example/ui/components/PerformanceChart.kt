package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TelemetryEntity
import com.example.data.model.TimeRange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RaspiRed
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChartMetric(val label: String, val unit: String, val color: Color) {
    CPU("CPU Load", "%", RaspiRed),
    TEMP("CPU Temp", "°C", EmeraldGreen),
    RAM("RAM Usage", "MB", TelemetryCyan),
    NET("Network I/O", "KB/s", Color(0xFFFFB300))
}

@Composable
fun PerformanceChart(
    records: List<TelemetryEntity>,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(ChartMetric.CPU) }
    var scrubIndex by remember { mutableStateOf<Int?>(null) }

    val dataPoints = remember(records, selectedMetric) {
        records.map { r ->
            val value = when (selectedMetric) {
                ChartMetric.CPU -> r.cpuUsage
                ChartMetric.TEMP -> r.cpuTemp
                ChartMetric.RAM -> r.ramUsedMb.toDouble()
                ChartMetric.NET -> r.netRxKbps + r.netTxKbps
            }
            Pair(r.timestamp, value)
        }
    }

    val minValue = remember(dataPoints, selectedMetric) {
        if (dataPoints.isEmpty()) 0.0 else {
            val min = dataPoints.minOf { it.second }
            if (selectedMetric == ChartMetric.CPU) 0.0 else (min * 0.9).coerceAtLeast(0.0)
        }
    }

    val maxValue = remember(dataPoints, selectedMetric) {
        if (dataPoints.isEmpty()) 100.0 else {
            val max = dataPoints.maxOf { it.second }
            if (selectedMetric == ChartMetric.CPU) 100.0 else (max * 1.1).coerceAtLeast(minValue + 5.0)
        }
    }

    val avgValue = remember(dataPoints) {
        if (dataPoints.isEmpty()) 0.0 else dataPoints.map { it.second }.average()
    }

    val currentValue = dataPoints.lastOrNull()?.second ?: 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Metric Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historical Trends",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Interactive telemetry analysis",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Time Range Segmented Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeRange.values().forEach { range ->
                        val isSelected = range == selectedTimeRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) RaspiRed else DarkSurfaceVariant)
                                .pointerInput(Unit) {
                                    detectTapGestures { onTimeRangeSelected(range) }
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = range.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                ),
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartMetric.values().forEach { metric ->
                    val isSelected = metric == selectedMetric
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMetric = metric
                            scrubIndex = null
                        },
                        label = {
                            Text(
                                text = metric.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(metric.color)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = metric.color.copy(alpha = 0.2f),
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DarkBorder,
                            selectedBorderColor = metric.color
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statistics Bar (Current, Min, Max, Avg, Scrubber value)
            val scrubbedPoint = scrubIndex?.let { dataPoints.getOrNull(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (scrubbedPoint != null) {
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(scrubbedPoint.first))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Scrubber ($timeStr): ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "%.1f %s".format(scrubbedPoint.second, selectedMetric.unit),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = selectedMetric.color
                        )
                    }
                } else {
                    MetricStat("Current", "%.1f %s".format(currentValue, selectedMetric.unit), selectedMetric.color)
                    MetricStat("Avg", "%.1f %s".format(avgValue, selectedMetric.unit), TextSecondary)
                    MetricStat("Min", "%.1f".format(if (dataPoints.isNotEmpty()) dataPoints.minOf { it.second } else 0.0), TextMuted)
                    MetricStat("Peak", "%.1f".format(if (dataPoints.isNotEmpty()) dataPoints.maxOf { it.second } else 0.0), RaspiRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                if (dataPoints.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Accumulating real-time telemetry points...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .pointerInput(dataPoints) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val idx = ((offset.x / size.width) * (dataPoints.size - 1))
                                            .toInt()
                                            .coerceIn(0, dataPoints.size - 1)
                                        scrubIndex = idx
                                    }
                                )
                            }
                            .pointerInput(dataPoints) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val idx = ((offset.x / size.width) * (dataPoints.size - 1))
                                            .toInt()
                                            .coerceIn(0, dataPoints.size - 1)
                                        scrubIndex = idx
                                    },
                                    onDragEnd = { scrubIndex = null },
                                    onDragCancel = { scrubIndex = null },
                                    onDrag = { change, _ ->
                                        val idx = ((change.position.x / size.width) * (dataPoints.size - 1))
                                            .toInt()
                                            .coerceIn(0, dataPoints.size - 1)
                                        scrubIndex = idx
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val range = (maxValue - minValue).coerceAtLeast(1.0)

                        // Draw Grid Lines (4 horizontal bands)
                        for (i in 0..4) {
                            val y = height - (height * (i / 4f))
                            drawLine(
                                color = DarkBorder.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Compute points
                        val points = dataPoints.mapIndexed { index, pair ->
                            val x = (index.toFloat() / (dataPoints.size - 1)) * width
                            val normY = ((pair.second - minValue) / range).coerceIn(0.0, 1.0).toFloat()
                            val y = height - (normY * height)
                            Offset(x, y)
                        }

                        // Build smooth Bézier line path and fill path
                        val strokePath = Path()
                        val fillPath = Path()

                        if (points.isNotEmpty()) {
                            strokePath.moveTo(points[0].x, points[0].y)
                            fillPath.moveTo(points[0].x, height)
                            fillPath.lineTo(points[0].x, points[0].y)

                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val midX = (prev.x + curr.x) / 2f
                                strokePath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }

                            fillPath.lineTo(points.last().x, height)
                            fillPath.close()

                            // Fill with gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        selectedMetric.color.copy(alpha = 0.35f),
                                        selectedMetric.color.copy(alpha = 0.02f)
                                    ),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            // Draw line
                            drawPath(
                                path = strokePath,
                                color = selectedMetric.color,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Draw Scrubber crosshair if touching
                        scrubIndex?.let { idx ->
                            if (idx in points.indices) {
                                val pt = points[idx]
                                // Vertical line
                                drawLine(
                                    color = Color.White.copy(alpha = 0.7f),
                                    start = Offset(pt.x, 0f),
                                    end = Offset(pt.x, height),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                // Point highlight
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = selectedMetric.color,
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val startDt = dataPoints.firstOrNull()?.first
                val endDt = dataPoints.lastOrNull()?.first
                val fmt = SimpleDateFormat("HH:mm", Locale.US)
                Text(
                    text = if (startDt != null) fmt.format(Date(startDt)) else "--:--",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted
                )
                Text(
                    text = "Drag finger on chart to inspect exact values",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted.copy(alpha = 0.7f)
                )
                Text(
                    text = if (endDt != null) fmt.format(Date(endDt)) else "--:--",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun MetricStat(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = color
        )
    }
}
