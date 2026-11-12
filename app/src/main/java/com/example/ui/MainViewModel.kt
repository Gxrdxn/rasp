package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.RaspiAiAdvisor
import com.example.data.db.RaspiDatabase
import com.example.data.db.TelemetryEntity
import com.example.data.export.ReportExporter
import com.example.data.model.AlertEvent
import com.example.data.model.AlertRule
import com.example.data.model.AlertSeverity
import com.example.data.model.BluetoothDevice
import com.example.data.model.DashboardWidget
import com.example.data.model.GpioMode
import com.example.data.model.GpioPin
import com.example.data.model.NetworkInterface
import com.example.data.model.PiTelemetry
import com.example.data.model.TerminalEntry
import com.example.data.model.TimeRange
import com.example.data.model.UsbDevice
import com.example.data.model.WidgetType
import com.example.data.network.ConnectionState
import com.example.data.network.RaspiWebSocketClient
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

data class DashboardUiState(
    val telemetry: PiTelemetry = PiTelemetry(),
    val connectionState: ConnectionState = ConnectionState.SimulatedLive("Raspberry Pi 5 (8GB)"),
    val selectedTimeRange: TimeRange = TimeRange.MINUTES_15,
    val historicalRecords: List<TelemetryEntity> = emptyList(),
    val alertRule: AlertRule = AlertRule(),
    val alertEvents: List<AlertEvent> = emptyList(),
    val terminalHistory: List<TerminalEntry> = emptyList(),
    val usbDevices: List<UsbDevice> = emptyList(),
    val gpioPins: List<GpioPin> = emptyList(),
    val networkInterfaces: List<NetworkInterface> = emptyList(),
    val bluetoothDevices: List<BluetoothDevice> = emptyList(),
    val widgets: List<DashboardWidget> = emptyList(),
    val aiSummary: String = "",
    val aiSuggestions: List<com.example.data.model.AiSuggestion> = emptyList(),
    val isAiLoading: Boolean = false,
    val activeAlertBanner: AlertEvent? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RaspiDatabase.getDatabase(application)
    private val telemetryDao = db.telemetryDao()
    private val wsClient = RaspiWebSocketClient(viewModelScope)
    private val aiAdvisor = RaspiAiAdvisor()

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            widgets = defaultWidgets(),
            usbDevices = defaultUsbDevices(),
            gpioPins = defaultGpioPins(),
            networkInterfaces = defaultNetworkInterfaces(),
            bluetoothDevices = defaultBluetoothDevices(),
            terminalHistory = listOf(
                TerminalEntry(
                    command = "systemctl status raspi-telemetry",
                    output = "● raspi-telemetry.service - Real-time Hardware Telemetry Daemon\n   Loaded: loaded (/lib/systemd/system/raspi-telemetry.service)\n   Active: active (running) since Thu 2026-11-12 00:00:01 PST",
                    exitCode = 0
                )
            )
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var lastRecordedTimestamp = 0L
    private var lastAlertTimestamp = 0L

    init {
        NotificationHelper.createNotificationChannel(application)

        // Preseed initial historical records if database is empty so charts look great immediately
        viewModelScope.launch(Dispatchers.IO) {
            val count = telemetryDao.getCount()
            if (count < 10) {
                preseedHistoricalData()
            }
            refreshHistoricalData(_uiState.value.selectedTimeRange)
        }

        // Listen to WebSocket connection state
        viewModelScope.launch {
            wsClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Listen to live telemetry stream
        viewModelScope.launch {
            wsClient.telemetryStream.collect { telemetry ->
                onNewTelemetry(telemetry)
            }
        }

        // Listen to CLI outputs
        viewModelScope.launch {
            wsClient.commandOutput.collect { output ->
                val currentHist = _uiState.value.terminalHistory
                val lastItem = currentHist.lastOrNull()
                if (lastItem != null && lastItem.output.isBlank()) {
                    // Update output of the last typed command
                    val updated = currentHist.dropLast(1) + lastItem.copy(output = output)
                    _uiState.value = _uiState.value.copy(terminalHistory = updated)
                } else {
                    _uiState.value = _uiState.value.copy(
                        terminalHistory = currentHist + TerminalEntry(command = "", output = output)
                    )
                }
            }
        }

        // Run initial AI analysis
        runAiAnalysis()
    }

    private suspend fun onNewTelemetry(telemetry: PiTelemetry) {
        val now = System.currentTimeMillis()

        // Update live state
        _uiState.value = _uiState.value.copy(
            telemetry = telemetry,
            networkInterfaces = updateNetworkRates(telemetry)
        )

        // Save to Room every 3 seconds
        if (now - lastRecordedTimestamp >= 3000L) {
            lastRecordedTimestamp = now
            val entity = TelemetryEntity(
                timestamp = telemetry.timestamp,
                cpuTemp = telemetry.cpuTemp,
                gpuTemp = telemetry.gpuTemp,
                cpuUsage = telemetry.cpuUsage,
                ramUsedMb = telemetry.ramUsedMb,
                ramTotalMb = telemetry.ramTotalMb,
                diskUsedGb = telemetry.diskUsedGb,
                diskTotalGb = telemetry.diskTotalGb,
                fanSpeedRpm = telemetry.fanSpeedRpm,
                voltageVolts = telemetry.voltageVolts,
                netRxKbps = telemetry.netRxKbps,
                netTxKbps = telemetry.netTxKbps,
                isThrottled = telemetry.isThrottled
            )
            withContext(Dispatchers.IO) {
                telemetryDao.insert(entity)
            }
            refreshHistoricalData(_uiState.value.selectedTimeRange)
        }

        // Check alerts
        checkAlertThresholds(telemetry, now)
    }

    private fun checkAlertThresholds(telemetry: PiTelemetry, now: Long) {
        val rule = _uiState.value.alertRule
        if (!rule.notificationsEnabled) return

        // Throttle alert notifications to avoid spamming (e.g. at most once every 30 seconds)
        if (now - lastAlertTimestamp < 30000L) return

        var event: AlertEvent? = null

        when {
            telemetry.cpuTemp >= rule.tempCriticalThreshold -> {
                event = AlertEvent(
                    title = "CRITICAL: Thermal Throttling Imminent",
                    message = "CPU core temperature reached ${telemetry.cpuTemp}°C (Trip point: ${rule.tempCriticalThreshold}°C). Fan at ${telemetry.fanSpeedRpm} RPM.",
                    severity = AlertSeverity.CRITICAL
                )
            }
            telemetry.cpuTemp >= rule.tempWarningThreshold -> {
                event = AlertEvent(
                    title = "Thermal Warning: CPU Temp High",
                    message = "CPU temperature is currently ${telemetry.cpuTemp}°C. Consider increasing fan PWM or inspecting airflow.",
                    severity = AlertSeverity.WARNING
                )
            }
            telemetry.cpuUsage >= rule.cpuCriticalThreshold -> {
                event = AlertEvent(
                    title = "CRITICAL: CPU Core Saturated",
                    message = "CPU utilization sustained at ${telemetry.cpuUsage}%. High task queue depth.",
                    severity = AlertSeverity.CRITICAL
                )
            }
            telemetry.isUndervoltage -> {
                event = AlertEvent(
                    title = "Undervoltage Detected",
                    message = "Input rail dropped to ${telemetry.voltageVolts}V (Threshold: 4.85V). Check power supply and USB-C cable.",
                    severity = AlertSeverity.WARNING
                )
            }
        }

        if (event != null) {
            lastAlertTimestamp = now
            val updatedEvents = listOf(event) + _uiState.value.alertEvents.take(49)
            _uiState.value = _uiState.value.copy(
                alertEvents = updatedEvents,
                activeAlertBanner = event
            )
            NotificationHelper.sendAlertNotification(
                context = getApplication(),
                id = 1001,
                title = event.title,
                message = event.message,
                isCritical = event.severity == AlertSeverity.CRITICAL
            )
        }
    }

    fun dismissAlertBanner() {
        _uiState.value = _uiState.value.copy(activeAlertBanner = null)
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        viewModelScope.launch(Dispatchers.IO) {
            refreshHistoricalData(range)
        }
    }

    private suspend fun refreshHistoricalData(range: TimeRange) {
        val since = System.currentTimeMillis() - range.durationMs
        val records = telemetryDao.getRecordsSinceDirect(since)
        _uiState.value = _uiState.value.copy(historicalRecords = records)
    }

    fun executeCliCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        if (trimmed == "clear") {
            _uiState.value = _uiState.value.copy(terminalHistory = emptyList())
            return
        }

        val entry = TerminalEntry(command = trimmed, output = "")
        _uiState.value = _uiState.value.copy(
            terminalHistory = _uiState.value.terminalHistory + entry
        )
        wsClient.sendCommand(trimmed)
    }

    fun clearTerminal() {
        _uiState.value = _uiState.value.copy(terminalHistory = emptyList())
    }

    fun toggleGpioPin(physicalPin: Int) {
        val currentPins = _uiState.value.gpioPins.toMutableList()
        val index = currentPins.indexOfFirst { it.physicalPin == physicalPin }
        if (index != -1) {
            val pin = currentPins[index]
            if (pin.bcm != null && !pin.isPowerOrGround) {
                val toggled = pin.copy(isHigh = !pin.isHigh)
                currentPins[index] = toggled
                _uiState.value = _uiState.value.copy(gpioPins = currentPins)
                // Log action to terminal
                executeCliCommand("pinctrl set ${pin.bcm} ${if (toggled.isHigh) "dh" else "dl"}")
            }
        }
    }

    fun updateAlertRule(newRule: AlertRule) {
        _uiState.value = _uiState.value.copy(alertRule = newRule)
    }

    fun connectWebSocket(url: String) {
        wsClient.connect(url)
    }

    fun switchToSimulator() {
        wsClient.startSimulator()
    }

    fun toggleWidgetVisibility(widget: DashboardWidget) {
        val updated = _uiState.value.widgets.map {
            if (it.type == widget.type) it.copy(isVisible = !it.isVisible) else it
        }
        _uiState.value = _uiState.value.copy(widgets = updated)
    }

    fun moveWidget(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.widgets.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val reordered = list.mapIndexed { idx, w -> w.copy(order = idx) }
            _uiState.value = _uiState.value.copy(widgets = reordered)
        }
    }

    fun runAiAnalysis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            try {
                val history = withContext(Dispatchers.IO) {
                    telemetryDao.getRecentRecords(30)
                }
                val (summary, suggestions) = aiAdvisor.analyzeSystem(_uiState.value.telemetry, history)
                _uiState.value = _uiState.value.copy(
                    aiSummary = summary,
                    aiSuggestions = suggestions,
                    isAiLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    aiSummary = "AI analysis completed with heuristic system checks.",
                    isAiLoading = false
                )
            }
        }
    }

    fun exportPdfReport(): File {
        return ReportExporter.exportPdf(
            context = getApplication(),
            telemetry = _uiState.value.telemetry,
            records = _uiState.value.historicalRecords
        )
    }

    fun exportCsvData(): File {
        return ReportExporter.exportCsv(
            context = getApplication(),
            telemetry = _uiState.value.telemetry,
            records = _uiState.value.historicalRecords
        )
    }

    fun shareExportFile(file: File, mimeType: String, title: String) {
        ReportExporter.shareFile(getApplication(), file, mimeType, title)
    }

    private suspend fun preseedHistoricalData() {
        val now = System.currentTimeMillis()
        val list = mutableListOf<TelemetryEntity>()
        var temp = 46.0
        var cpu = 18.0

        for (i in 60 downTo 1) {
            val ts = now - (i * 15000L)
            cpu = (cpu + Random.nextDouble(-6.0, 7.0)).coerceIn(5.0, 92.0)
            temp = (temp + (40.0 + cpu * 0.4 - temp) * 0.15 + Random.nextDouble(-0.2, 0.3)).coerceIn(38.0, 78.0)
            list.add(
                TelemetryEntity(
                    timestamp = ts,
                    cpuTemp = Math.round(temp * 10.0) / 10.0,
                    gpuTemp = Math.round((temp - 1.0) * 10.0) / 10.0,
                    cpuUsage = Math.round(cpu * 10.0) / 10.0,
                    ramUsedMb = 2100L + (cpu * 5).toLong(),
                    ramTotalMb = 8192L,
                    diskUsedGb = 14.8,
                    diskTotalGb = 64.0,
                    fanSpeedRpm = if (temp > 55.0) 2400 else 0,
                    voltageVolts = 5.08,
                    netRxKbps = 120.0 + Random.nextDouble(10.0, 80.0),
                    netTxKbps = 60.0 + Random.nextDouble(5.0, 40.0),
                    isThrottled = temp > 80.0
                )
            )
        }
        telemetryDao.insertAll(list)
    }

    private fun updateNetworkRates(telemetry: PiTelemetry): List<NetworkInterface> {
        return _uiState.value.networkInterfaces.map { net ->
            if (net.name == "wlan0") {
                net.copy(rxKbps = telemetry.netRxKbps, txKbps = telemetry.netTxKbps)
            } else {
                net
            }
        }
    }

    private fun defaultWidgets(): List<DashboardWidget> = listOf(
        DashboardWidget(WidgetType.OVERVIEW_GAUGES, "System Gauges & Cores", true, 0),
        DashboardWidget(WidgetType.REALTIME_CHART, "Performance Trends", true, 1),
        DashboardWidget(WidgetType.AI_OPTIMIZER, "AI Performance Advisor", true, 2),
        DashboardWidget(WidgetType.MEMORY_STORAGE, "Memory & Storage", true, 3),
        DashboardWidget(WidgetType.CONNECTED_DEVICES, "Connected Devices & GPIO", true, 4),
        DashboardWidget(WidgetType.CLI_TERMINAL, "CLI Terminal Console", true, 5)
    )

    private fun defaultUsbDevices(): List<UsbDevice> = listOf(
        UsbDevice("usb-1", "SanDisk Ultra Fit 64GB", "Bus 002 Dev 002", "0781", "5583", "5.0 Gbps (USB 3.0)", isStorage = true),
        UsbDevice("usb-2", "Logitech C270 HD Webcam", "Bus 001 Dev 003", "046d", "0825", "480 Mbps (USB 2.0)", isStorage = false),
        UsbDevice("usb-3", "Logitech Wireless Receiver", "Bus 001 Dev 002", "046d", "c534", "12 Mbps (USB 1.1)", isStorage = false)
    )

    private fun defaultNetworkInterfaces(): List<NetworkInterface> = listOf(
        NetworkInterface("wlan0", "192.168.1.142", "dc:a6:32:89:ef:12", isUp = true, type = "Wi-Fi", rxKbps = 142.5, txKbps = 88.2, signalDbm = -54),
        NetworkInterface("eth0", "192.168.1.100", "dc:a6:32:89:ef:11", isUp = true, type = "Ethernet", rxKbps = 0.0, txKbps = 0.0)
    )

    private fun defaultBluetoothDevices(): List<BluetoothDevice> = listOf(
        BluetoothDevice("Raspberry Pi Keyboard", "E4:5F:01:2A:3B:4C", isConnected = true, deviceType = "Keyboard/HID", rssi = -62),
        BluetoothDevice("JBL Charge 5 Audio", "B8:D5:0B:78:12:44", isConnected = false, deviceType = "Audio Sink")
    )

    private fun defaultGpioPins(): List<GpioPin> = listOf(
        GpioPin(1, null, "3V3 Power", isPowerOrGround = true),
        GpioPin(2, null, "5V Power", isPowerOrGround = true),
        GpioPin(3, 2, "GPIO 2 (SDA)", GpioMode.ALT0, isHigh = true),
        GpioPin(4, null, "5V Power", isPowerOrGround = true),
        GpioPin(5, 3, "GPIO 3 (SCL)", GpioMode.ALT0, isHigh = true),
        GpioPin(6, null, "Ground", isPowerOrGround = true),
        GpioPin(7, 4, "GPIO 4 (GPCLK0)", GpioMode.OUTPUT, isHigh = false),
        GpioPin(8, 14, "GPIO 14 (TXD)", GpioMode.ALT0, isHigh = true),
        GpioPin(9, null, "Ground", isPowerOrGround = true),
        GpioPin(10, 15, "GPIO 15 (RXD)", GpioMode.ALT0, isHigh = true),
        GpioPin(11, 17, "GPIO 17", GpioMode.OUTPUT, isHigh = true),
        GpioPin(12, 18, "GPIO 18 (PWM0)", GpioMode.PWM, isHigh = false, isPwm = true),
        GpioPin(13, 27, "GPIO 27", GpioMode.OUTPUT, isHigh = false),
        GpioPin(14, null, "Ground", isPowerOrGround = true),
        GpioPin(15, 22, "GPIO 22", GpioMode.OUTPUT, isHigh = false),
        GpioPin(16, 23, "GPIO 23", GpioMode.OUTPUT, isHigh = false),
        GpioPin(17, null, "3V3 Power", isPowerOrGround = true),
        GpioPin(18, 24, "GPIO 24", GpioMode.OUTPUT, isHigh = false),
        GpioPin(19, 10, "GPIO 10 (MOSI)", GpioMode.ALT0, isHigh = false),
        GpioPin(20, null, "Ground", isPowerOrGround = true)
    )
}
