package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveGauge(
    value: Double,
    maxValue: Double,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    isTemperature: Boolean = false
) {
    val progress = (value / maxValue).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "gauge_progress"
    )

    // Dynamic color based on thresholds
    val activeColor = when {
        if (isTemperature) value >= 80.0 else value >= 85.0 -> CriticalRed
        if (isTemperature) value >= 68.0 else value >= 65.0 -> AmberWarning
        else -> EmeraldGreen
    }

    val animatedColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(durationMillis = 400),
        label = "gauge_color"
    )

    val statusText = when {
        if (isTemperature) value >= 80.0 else value >= 85.0 -> "HIGH"
        if (isTemperature) value >= 68.0 else value >= 65.0 -> "WARM"
        else -> "OPTIMAL"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.toPx() - strokeWidth
            val startAngle = 140f
            val sweepAngle = 260f

            // Track arc
            drawArc(
                color = DarkSurfaceVariant,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active sweep
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            EmeraldGreen,
                            if (progress > 0.6f) AmberWarning else EmeraldGreen,
                            animatedColor
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Draw tick marks
            val tickRadius = (diameter / 2) - strokeWidth
            val centerOffset = Offset(size.toPx() / 2, size.toPx() / 2)
            for (i in 0..10) {
                val tickAngle = Math.toRadians((startAngle + (sweepAngle * (i / 10f))).toDouble())
                val startX = centerOffset.x + ((tickRadius - 4.dp.toPx()) * cos(tickAngle)).toFloat()
                val startY = centerOffset.y + ((tickRadius - 4.dp.toPx()) * sin(tickAngle)).toFloat()
                val endX = centerOffset.x + (tickRadius * cos(tickAngle)).toFloat()
                val endY = centerOffset.y + (tickRadius * sin(tickAngle)).toFloat()
                drawLine(
                    color = if (i.toFloat() / 10f <= animatedProgress) animatedColor.copy(alpha = 0.7f) else DarkBorder,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Value text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "%.1f".format(value),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = animatedColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextMuted
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = animatedColor
            )
        }
    }
}
