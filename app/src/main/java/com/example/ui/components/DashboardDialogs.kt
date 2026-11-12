package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertRule
import com.example.data.model.DashboardWidget
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RaspiRed
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExportDialog(
    recordCount: Int,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("PDF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Text(
                text = "Export Performance Report",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Generate and share an executive diagnostic report with $recordCount recorded telemetry samples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Format choices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormatCard(
                        title = "PDF Report",
                        desc = "Formatted multi-page diagnostic summary with KPI cards & tables",
                        icon = Icons.Default.Description,
                        isSelected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        modifier = Modifier.weight(1f)
                    )
                    FormatCard(
                        title = "CSV Data",
                        desc = "Raw comma-separated dataset for Excel, Python & Pandas analytics",
                        icon = Icons.Default.TableChart,
                        isSelected = selectedFormat == "CSV",
                        onClick = { selectedFormat = "CSV" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedFormat == "PDF") onExportPdf() else onExportCsv()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaspiRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Generate & Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun FormatCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RaspiRed.copy(alpha = 0.15f) else DarkSurfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RaspiRed else DarkBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) RaspiRed else TelemetryCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 13.sp),
                color = TextMuted
            )
        }
    }
}

@Composable
fun AlertSettingsDialog(
    currentRule: AlertRule,
    onDismiss: () -> Unit,
    onSaveRule: (AlertRule) -> Unit
) {
    var tempThreshold by remember { mutableDoubleStateOf(currentRule.tempWarningThreshold) }
    var cpuThreshold by remember { mutableDoubleStateOf(currentRule.cpuWarningThreshold) }
    var notifEnabled by remember { mutableStateOf(currentRule.notificationsEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alerts",
                    tint = RaspiRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Telemetry Alert Thresholds",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Configure trip thresholds that trigger system notifications and dashboard warnings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Temp Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Temperature Warning", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text(
                        text = "%.0f °C".format(tempThreshold),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = RaspiRed
                    )
                }
                Slider(
                    value = tempThreshold.toFloat(),
                    onValueChange = { tempThreshold = it.toDouble() },
                    valueRange = 50f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = RaspiRed,
                        activeTrackColor = RaspiRed,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // CPU Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CPU Load Warning", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text(
                        text = "%.0f %%".format(cpuThreshold),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TelemetryCyan
                    )
                }
                Slider(
                    value = cpuThreshold.toFloat(),
                    onValueChange = { cpuThreshold = it.toDouble() },
                    valueRange = 50f..98f,
                    colors = SliderDefaults.colors(
                        thumbColor = TelemetryCyan,
                        activeTrackColor = TelemetryCyan,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notifications Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Android System Notifications", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text(text = "Post alerts to device notification bar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = { notifEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RaspiRed
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveRule(
                        currentRule.copy(
                            tempWarningThreshold = tempThreshold,
                            cpuWarningThreshold = cpuThreshold,
                            notificationsEnabled = notifEnabled
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaspiRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Thresholds")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun ConnectionDialog(
    currentUrl: String,
    isSimulating: Boolean,
    onDismiss: () -> Unit,
    onConnectRemote: (String) -> Unit,
    onSwitchToSimulator: () -> Unit
) {
    var wsUrl by remember { mutableStateOf(if (currentUrl.isNotBlank()) currentUrl else "ws://192.168.1.100:8765") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "WebSocket",
                    tint = TelemetryCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Raspberry Pi Connection",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Connect directly to a Raspberry Pi WebSocket server daemon or use the interactive live telemetry simulator.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    label = { Text("WebSocket URL", color = TextMuted) },
                    placeholder = { Text("ws://192.168.1.x:8765") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        onSwitchToSimulator()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSimulating) EmeraldGreen else TextSecondary
                    )
                ) {
                    Text(text = if (isSimulating) "✓ Currently in Live Simulator Mode" else "Switch to Live Pi 5 Simulator")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConnectRemote(wsUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaspiRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Connect WebSocket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        }
    )
}

@Composable
fun WidgetCustomizerDialog(
    widgets: List<DashboardWidget>,
    onDismiss: () -> Unit,
    onToggleWidget: (DashboardWidget) -> Unit,
    onMoveWidget: (fromIndex: Int, toIndex: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Text(
                text = "Customize Dashboard Layout",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Show/hide and reorder widgets to tailor your monitoring workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    widgets.forEachIndexed { index, widget ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleWidget(widget) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (widget.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = if (widget.isVisible) EmeraldGreen else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = widget.title,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (widget.isVisible) TextPrimary else TextMuted
                                )
                            }

                            Row {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveWidget(index, index - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move up",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (index < widgets.size - 1) {
                                    IconButton(
                                        onClick = { onMoveWidget(index, index + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move down",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RaspiRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done")
            }
        }
    )
}
