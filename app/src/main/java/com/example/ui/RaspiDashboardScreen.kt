package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PiTelemetry
import com.example.data.model.WidgetType
import com.example.data.network.ConnectionState
import com.example.ui.components.AiAdvisorView
import com.example.ui.components.AlertSettingsDialog
import com.example.ui.components.CliTerminalView
import com.example.ui.components.ConnectedDevicesView
import com.example.ui.components.ConnectionDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.LiveGauge
import com.example.ui.components.PerformanceChart
import com.example.ui.components.WidgetCustomizerDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RaspiRed
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaspiDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showWidgetCustomizer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(RaspiRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "π",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RaspiDash",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${uiState.telemetry.modelName} • Uptime: ${uiState.telemetry.formattedUptime}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    // WebSocket / Simulation status pill
                    ConnectionStatusPill(
                        connectionState = uiState.connectionState,
                        onClick = { showConnectionDialog = true },
                        modifier = Modifier.testTag("connection_status_button")
                    )

                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Report (PDF/CSV)",
                            tint = TelemetryCyan
                        )
                    }

                    IconButton(
                        onClick = { showAlertsDialog = true },
                        modifier = Modifier.testTag("alert_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert Settings",
                            tint = if (uiState.alertEvents.isNotEmpty()) AmberWarning else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { showWidgetCustomizer = true },
                        modifier = Modifier.testTag("customize_widgets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = "Customize Dashboard Layout",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Alert Banner (if triggered)
            uiState.activeAlertBanner?.let { alert ->
                item(key = "active_alert_banner") {
                    ActiveAlertBanner(
                        alert = alert,
                        onDismiss = { viewModel.dismissAlertBanner() }
                    )
                }
            }

            // Render visible widgets in ordered sequence
            val sortedWidgets = uiState.widgets.filter { it.isVisible }.sortedBy { it.order }

            sortedWidgets.forEach { widget ->
                when (widget.type) {
                    WidgetType.OVERVIEW_GAUGES -> {
                        item(key = "widget_overview_gauges") {
                            OverviewGaugesWidget(telemetry = uiState.telemetry)
                        }
                    }
                    WidgetType.REALTIME_CHART -> {
                        item(key = "widget_performance_chart") {
                            PerformanceChart(
                                records = uiState.historicalRecords,
                                selectedTimeRange = uiState.selectedTimeRange,
                                onTimeRangeSelected = { viewModel.setTimeRange(it) }
                            )
                        }
                    }
                    WidgetType.AI_OPTIMIZER -> {
                        item(key = "widget_ai_optimizer") {
                            AiAdvisorView(
                                summary = uiState.aiSummary,
                                suggestions = uiState.aiSuggestions,
                                isLoading = uiState.isAiLoading,
                                onRunAnalysis = { viewModel.runAiAnalysis() },
                                onExecuteCommand = { viewModel.executeCliCommand(it) }
                            )
                        }
                    }
                    WidgetType.MEMORY_STORAGE -> {
                        item(key = "widget_memory_storage") {
                            MemoryStorageWidget(telemetry = uiState.telemetry)
                        }
                    }
                    WidgetType.CONNECTED_DEVICES -> {
                        item(key = "widget_connected_devices") {
                            ConnectedDevicesView(
                                usbDevices = uiState.usbDevices,
                                gpioPins = uiState.gpioPins,
                                networkInterfaces = uiState.networkInterfaces,
                                bluetoothDevices = uiState.bluetoothDevices,
                                onToggleGpio = { viewModel.toggleGpioPin(it) }
                            )
                        }
                    }
                    WidgetType.CLI_TERMINAL -> {
                        item(key = "widget_cli_terminal") {
                            CliTerminalView(
                                history = uiState.terminalHistory,
                                onExecuteCommand = { viewModel.executeCliCommand(it) },
                                onClearTerminal = { viewModel.clearTerminal() }
                            )
                        }
                    }
                    else -> Unit
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Dialogs
    if (showExportDialog) {
        ExportDialog(
            recordCount = uiState.historicalRecords.size,
            onDismiss = { showExportDialog = false },
            onExportPdf = {
                val file = viewModel.exportPdfReport()
                viewModel.shareExportFile(file, "application/pdf", "RaspiDash Diagnostic PDF Report")
            },
            onExportCsv = {
                val file = viewModel.exportCsvData()
                viewModel.shareExportFile(file, "text/csv", "RaspiDash Performance CSV Data")
            }
        )
    }

    if (showAlertsDialog) {
        AlertSettingsDialog(
            currentRule = uiState.alertRule,
            onDismiss = { showAlertsDialog = false },
            onSaveRule = { viewModel.updateAlertRule(it) }
        )
    }

    if (showConnectionDialog) {
        val currentUrl = when (val c = uiState.connectionState) {
            is ConnectionState.Connected -> c.url
            else -> ""
        }
        val isSim = uiState.connectionState is ConnectionState.SimulatedLive

        ConnectionDialog(
            currentUrl = currentUrl,
            isSimulating = isSim,
            onDismiss = { showConnectionDialog = false },
            onConnectRemote = { viewModel.connectWebSocket(it) },
            onSwitchToSimulator = { viewModel.switchToSimulator() }
        )
    }

    if (showWidgetCustomizer) {
        WidgetCustomizerDialog(
            widgets = uiState.widgets,
            onDismiss = { showWidgetCustomizer = false },
            onToggleWidget = { viewModel.toggleWidgetVisibility(it) },
            onMoveWidget = { from, to -> viewModel.moveWidget(from, to) }
        )
    }
}

@Composable
private fun ConnectionStatusPill(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (connectionState) {
        is ConnectionState.SimulatedLive -> Pair("LIVE SIM", EmeraldGreen)
        is ConnectionState.Connected -> Pair("WS ONLINE", TelemetryCyan)
        is ConnectionState.Connecting -> Pair("CONNECTING", AmberWarning)
        is ConnectionState.Disconnected -> Pair("OFFLINE", TextMuted)
        is ConnectionState.Error -> Pair("ERROR", CriticalRed)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            ),
            color = color
        )
    }
}

@Composable
private fun ActiveAlertBanner(
    alert: com.example.data.model.AlertEvent,
    onDismiss: () -> Unit
) {
    val isCritical = alert.severity == com.example.data.model.AlertSeverity.CRITICAL
    val bannerColor = if (isCritical) CriticalRed else AmberWarning

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bannerColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = bannerColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = bannerColor
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextPrimary
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OverviewGaugesWidget(telemetry: PiTelemetry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "CPU & Thermals",
                        tint = RaspiRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Telemetry & Thermal Load",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Voltage / Fan RPM Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Fan: ${telemetry.fanSpeedRpm} RPM",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (telemetry.fanSpeedRpm > 3000) AmberWarning else EmeraldGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two circular gauges: CPU Usage and Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveGauge(
                    value = telemetry.cpuUsage,
                    maxValue = 100.0,
                    label = "CPU Load",
                    unit = "%",
                    isTemperature = false
                )

                LiveGauge(
                    value = telemetry.cpuTemp,
                    maxValue = 90.0,
                    label = "SoC Temp",
                    unit = "°C",
                    isTemperature = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Quad-Core Horizontal Progress Bars
            Text(
                text = "QUAD-CORE LOAD DISTRIBUTION (${telemetry.cpuFreqMhz} MHz)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            telemetry.cpuCores.forEachIndexed { index, load ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Core $index",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextSecondary,
                        modifier = Modifier.width(48.dp)
                    )

                    val coreProgress = (load / 100.0).toFloat().coerceIn(0f, 1f)
                    val barColor = when {
                        load > 85.0 -> CriticalRed
                        load > 65.0 -> AmberWarning
                        else -> TelemetryCyan
                    }

                    LinearProgressIndicator(
                        progress = { coreProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = barColor,
                        trackColor = DarkSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "%.0f%%".format(load),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextPrimary,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Status Badges (Undervoltage, Throttling, Frequency)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    title = "Rail Voltage",
                    value = "%.2f V".format(telemetry.voltageVolts),
                    isWarning = telemetry.isUndervoltage,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    title = "GPU Core",
                    value = "%.1f °C".format(telemetry.gpuTemp),
                    isWarning = telemetry.gpuTemp > 75.0,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    title = "Throttle Status",
                    value = if (telemetry.isThrottled) "ACTIVE" else "0x0 (OK)",
                    isWarning = telemetry.isThrottled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    title: String,
    value: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isWarning) CriticalRed.copy(alpha = 0.15f) else DarkSurfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, if (isWarning) CriticalRed else DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = if (isWarning) CriticalRed else EmeraldGreen
            )
        }
    }
}

@Composable
private fun MemoryStorageWidget(telemetry: PiTelemetry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Memory & Storage",
                    tint = TelemetryCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Memory & Storage Utilization",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM Allocation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RAM (LPDDR4X)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "${telemetry.ramUsedMb} MB / ${telemetry.ramTotalMb} MB (%.1f%%)".format(telemetry.ramPercent),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TelemetryCyan
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (telemetry.ramPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = TelemetryCyan,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Storage / MicroSD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Root Filesystem (/dev/mmcblk0p2)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "%.1f GB / %.1f GB (%.1f%%)".format(telemetry.diskUsedGb, telemetry.diskTotalGb, telemetry.diskPercent),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldGreen
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (telemetry.diskPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = EmeraldGreen,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Swap and Processes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = "SWAP ALLOCATION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                        Text(
                            text = "${telemetry.swapUsedMb} MB / ${telemetry.swapTotalMb} MB",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(text = "ACTIVE PROCESSES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                        Text(
                            text = "${telemetry.activeProcesses} tasks",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }
    }
}
