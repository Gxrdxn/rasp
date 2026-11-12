package com.example.data.network

import android.util.Log
import com.example.data.model.PiTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "RaspiWebSocketClient"

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val url: String) : ConnectionState
    data class SimulatedLive(val model: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

class RaspiWebSocketClient(
    private val scope: CoroutineScope
) {
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .build()

    private var activeWebSocket: WebSocket? = null
    private var simulatorJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(
        ConnectionState.SimulatedLive("Raspberry Pi 5 (8GB)")
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryStream = MutableSharedFlow<PiTelemetry>(replay = 1)
    val telemetryStream: SharedFlow<PiTelemetry> = _telemetryStream.asSharedFlow()

    private val _commandOutput = MutableSharedFlow<String>()
    val commandOutput: SharedFlow<String> = _commandOutput.asSharedFlow()

    private var currentTelemetry = PiTelemetry()

    init {
        // Start simulation by default so user has live streaming immediately
        startSimulator()
    }

    fun connect(wsUrl: String) {
        stopSimulator()
        activeWebSocket?.close(1000, "Switching connection")
        _connectionState.value = ConnectionState.Connecting

        val request = try {
            Request.Builder().url(wsUrl).build()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Invalid URL format: ${e.message}")
            return
        }

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected: $wsUrl")
                _connectionState.value = ConnectionState.Connected(wsUrl)
                // Request initial status
                webSocket.send("""{"action": "get_status"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.value = ConnectionState.Error(t.localizedMessage ?: "Connection error")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason ($code)")
                _connectionState.value = ConnectionState.Disconnected
            }
        })
    }

    fun disconnect() {
        activeWebSocket?.close(1000, "User requested disconnect")
        activeWebSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun startSimulator() {
        if (simulatorJob?.isActive == true) return
        activeWebSocket?.close(1000, "Switching to simulator")
        activeWebSocket = null
        _connectionState.value = ConnectionState.SimulatedLive("Raspberry Pi 5 (8GB)")

        simulatorJob = scope.launch(Dispatchers.Default) {
            var simUptime = 345600L
            var baseTemp = 48.0
            var baseCpu = 20.0
            var baseRam = 2150L

            while (isActive) {
                delay(1500)
                simUptime += 2

                // Dynamic fluctuations
                val cpuDelta = Random.nextDouble(-8.0, 9.0)
                val newCpu = (baseCpu + cpuDelta).coerceIn(4.0, 98.0)
                baseCpu = newCpu

                // Thermal model: higher CPU heats Pi up
                val tempTarget = 40.0 + (newCpu * 0.42)
                baseTemp += (tempTarget - baseTemp) * 0.12 + Random.nextDouble(-0.3, 0.4)
                baseTemp = baseTemp.coerceIn(38.0, 84.0)

                // Fan RPM adjusts automatically above 55°C
                val targetRpm = when {
                    baseTemp > 75.0 -> 4800
                    baseTemp > 65.0 -> 3600
                    baseTemp > 52.0 -> 2200
                    else -> 0
                }

                val ramDelta = Random.nextLong(-30, 45)
                baseRam = (baseRam + ramDelta).coerceIn(1200L, 7400L)

                val core0 = (newCpu + Random.nextDouble(-5.0, 6.0)).coerceIn(1.0, 100.0)
                val core1 = (newCpu + Random.nextDouble(-8.0, 7.0)).coerceIn(1.0, 100.0)
                val core2 = (newCpu + Random.nextDouble(-6.0, 5.0)).coerceIn(1.0, 100.0)
                val core3 = (newCpu + Random.nextDouble(-7.0, 8.0)).coerceIn(1.0, 100.0)

                val isThrottled = baseTemp > 80.0
                val isUndervolt = Random.nextInt(100) < 2 // rare anomaly

                currentTelemetry = PiTelemetry(
                    timestamp = System.currentTimeMillis(),
                    hostname = "raspberrypi-5",
                    modelName = "Raspberry Pi 5 Model B (8GB)",
                    osName = "Debian GNU/Linux 12 (bookworm)",
                    kernel = "Linux 6.6.20+rpt-rpi-2712",
                    cpuTemp = Math.round(baseTemp * 10.0) / 10.0,
                    gpuTemp = Math.round((baseTemp - 1.2) * 10.0) / 10.0,
                    cpuUsage = Math.round(newCpu * 10.0) / 10.0,
                    cpuCores = listOf(
                        Math.round(core0 * 10.0) / 10.0,
                        Math.round(core1 * 10.0) / 10.0,
                        Math.round(core2 * 10.0) / 10.0,
                        Math.round(core3 * 10.0) / 10.0
                    ),
                    cpuFreqMhz = if (baseTemp > 82.0) 1500 else 2400,
                    ramUsedMb = baseRam,
                    ramTotalMb = 8192,
                    swapUsedMb = 142,
                    swapTotalMb = 2048,
                    diskUsedGb = 14.8,
                    diskTotalGb = 64.0,
                    uptimeSeconds = simUptime,
                    fanSpeedRpm = targetRpm,
                    voltageVolts = if (isUndervolt) 4.62 else 5.08,
                    isUndervoltage = isUndervolt,
                    isThrottled = isThrottled,
                    netRxKbps = Math.round(Random.nextDouble(20.0, 480.0) * 10.0) / 10.0,
                    netTxKbps = Math.round(Random.nextDouble(10.0, 210.0) * 10.0) / 10.0,
                    activeProcesses = 112 + Random.nextInt(-4, 5)
                )

                _telemetryStream.emit(currentTelemetry)
            }
        }
    }

    private fun stopSimulator() {
        simulatorJob?.cancel()
        simulatorJob = null
    }

    fun sendCommand(command: String) {
        val trimmed = command.trim()
        if (activeWebSocket != null) {
            val json = JSONObject().apply {
                put("action", "exec")
                put("command", trimmed)
            }
            activeWebSocket?.send(json.toString())
        } else {
            // Emulate local CLI command output in simulator mode
            simulateCommandResponse(trimmed)
        }
    }

    private fun simulateCommandResponse(cmd: String) {
        scope.launch {
            delay(150)
            val output = when {
                cmd.startsWith("vcgencmd measure_temp") -> {
                    "temp=${currentTelemetry.cpuTemp}'C"
                }
                cmd.startsWith("vcgencmd get_throttled") -> {
                    if (currentTelemetry.isThrottled) "throttled=0x20000 (throttled has occurred)" else "throttled=0x0"
                }
                cmd.startsWith("free") -> {
                    """
                           total        used        free      shared  buff/cache   available
                    Mem:        7.8Gi       2.1Gi       4.2Gi        64Mi       1.5Gi       5.4Gi
                    Swap:       2.0Gi       140Mi       1.8Gi
                    """.trimIndent()
                }
                cmd.startsWith("df") -> {
                    """
                    Filesystem     1K-blocks     Used Available Use% Mounted on
                    /dev/root       61247832 15518928  43187216  27% /
                    /dev/mmcblk0p1    524288    68214    456074  13% /boot/firmware
                    """.trimIndent()
                }
                cmd.startsWith("top") || cmd.startsWith("htop") -> {
                    """
                    top - 12:44:02 up 4 days,  load average: 0.42, 0.38, 0.31
                    Tasks: 114 total,   1 running, 113 sleeping,   0 stopped
                    %Cpu(s): ${currentTelemetry.cpuUsage} us,  1.2 sy,  0.0 ni, 80.4 id,  0.2 wa
                    MiB Mem :   7984.4 total,   4210.2 free,   2150.0 used,   1624.2 buff/cache
                    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
                    1842 pi        20   0  324812  48120  24110 S   8.2   0.6   4:18.22 python3 app.py
                    2109 root      20   0  112800  28190  12140 S   4.5   0.4   1:22.04 raspi-telemetry
                    1024 systemd   20   0   28410   8410   6120 S   0.2   0.1   0:45.10 systemd
                    """.trimIndent()
                }
                cmd.startsWith("lsusb") -> {
                    """
                    Bus 002 Device 002: ID 0781:5583 SanDisk Corp. Ultra Fit
                    Bus 002 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
                    Bus 001 Device 003: ID 046d:0825 Logitech, Inc. Webcam C270
                    Bus 001 Device 002: ID 046d:c534 Logitech, Inc. Unifying Receiver
                    Bus 001 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub
                    """.trimIndent()
                }
                cmd.startsWith("pinout") -> {
                    """
                    ,--------------------------------.
                    | oooooooooooooooooooo J8   +======
                    | 1ooooooooooooooooooo      | USB   
                    |                           +======
                    |      Pi Model 5-B                 
                    |      1.0 8GB              +======
                    |                           | USB   
                    |  [] DSI    [PWR]  [HDMI]  +======
                    `--------------------------------'
                    """.trimIndent()
                }
                cmd.startsWith("uname -a") -> {
                    "Linux raspberrypi 6.6.20+rpt-rpi-2712 #1 SMP PREEMPT Debian 1:6.6.20-1+rpt1 (2024-03-07) aarch64 GNU/Linux"
                }
                cmd.startsWith("help") -> {
                    """
                    RaspiDash CLI Commands:
                      vcgencmd measure_temp      - Print SoC core temperature
                      vcgencmd get_throttled     - Print throttling / undervolt bitmask
                      top / htop                 - Show system processes and loads
                      free -h                    - Memory and swap allocation
                      df -h                      - Disk space usage
                      lsusb                      - List connected USB peripherals
                      pinout                     - Diagram of GPIO pin header
                      uname -a                   - Kernel architecture and release
                      set-fan <rpm>              - Adjust PWM fan curve target
                      set-alert-temp <deg>       - Update thermal warning threshold
                      clear                      - Clear terminal output
                    """.trimIndent()
                }
                cmd.startsWith("set-fan") -> {
                    val target = cmd.substringAfter("set-fan").trim().toIntOrNull() ?: 2400
                    "OK: Fan PWM target set to ${target} RPM"
                }
                cmd.startsWith("set-alert-temp") -> {
                    val deg = cmd.substringAfter("set-alert-temp").trim().toDoubleOrNull() ?: 75.0
                    "OK: Alert threshold set to $deg °C"
                }
                else -> {
                    "bash: $cmd: command executed successfully (simulated response code 0)"
                }
            }
            _commandOutput.emit(output)
        }
    }

    private fun parseIncomingMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            if (json.has("type") && json.getString("type") == "cmd_output") {
                val out = json.optString("output", "")
                scope.launch { _commandOutput.emit(out) }
                return
            }

            val telemetry = PiTelemetry(
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                hostname = json.optString("hostname", currentTelemetry.hostname),
                modelName = json.optString("model", currentTelemetry.modelName),
                osName = json.optString("os", currentTelemetry.osName),
                kernel = json.optString("kernel", currentTelemetry.kernel),
                cpuTemp = json.optDouble("cpu_temp", currentTelemetry.cpuTemp),
                gpuTemp = json.optDouble("gpu_temp", currentTelemetry.gpuTemp),
                cpuUsage = json.optDouble("cpu_usage", currentTelemetry.cpuUsage),
                cpuFreqMhz = json.optInt("cpu_freq_mhz", currentTelemetry.cpuFreqMhz),
                ramUsedMb = json.optLong("ram_used_mb", currentTelemetry.ramUsedMb),
                ramTotalMb = json.optLong("ram_total_mb", currentTelemetry.ramTotalMb),
                swapUsedMb = json.optLong("swap_used_mb", currentTelemetry.swapUsedMb),
                swapTotalMb = json.optLong("swap_total_mb", currentTelemetry.swapTotalMb),
                diskUsedGb = json.optDouble("disk_used_gb", currentTelemetry.diskUsedGb),
                diskTotalGb = json.optDouble("disk_total_gb", currentTelemetry.diskTotalGb),
                uptimeSeconds = json.optLong("uptime_sec", currentTelemetry.uptimeSeconds),
                fanSpeedRpm = json.optInt("fan_rpm", currentTelemetry.fanSpeedRpm),
                voltageVolts = json.optDouble("voltage", currentTelemetry.voltageVolts),
                isUndervoltage = json.optBoolean("undervoltage", false),
                isThrottled = json.optBoolean("throttled", false),
                netRxKbps = json.optDouble("net_rx_kbps", currentTelemetry.netRxKbps),
                netTxKbps = json.optDouble("net_tx_kbps", currentTelemetry.netTxKbps),
                activeProcesses = json.optInt("processes", currentTelemetry.activeProcesses)
            )
            currentTelemetry = telemetry
            scope.launch { _telemetryStream.emit(telemetry) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse telemetry JSON: $jsonText", e)
        }
    }
}
