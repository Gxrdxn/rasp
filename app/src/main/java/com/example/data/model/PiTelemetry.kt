package com.example.data.model

data class PiTelemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val hostname: String = "raspberrypi",
    val modelName: String = "Raspberry Pi 5 Model B (8GB)",
    val osName: String = "Debian 12 (bookworm) 64-bit",
    val kernel: String = "Linux 6.6.20+rpt-rpi-2712",
    val cpuTemp: Double = 48.5,
    val gpuTemp: Double = 47.8,
    val cpuUsage: Double = 18.4,
    val cpuCores: List<Double> = listOf(14.0, 22.0, 19.5, 18.0),
    val cpuFreqMhz: Int = 2400,
    val ramUsedMb: Long = 2140,
    val ramTotalMb: Long = 8192,
    val swapUsedMb: Long = 128,
    val swapTotalMb: Long = 2048,
    val diskUsedGb: Double = 14.8,
    val diskTotalGb: Double = 64.0,
    val uptimeSeconds: Long = 345600, // 4 days
    val fanSpeedRpm: Int = 2150,
    val voltageVolts: Double = 5.08,
    val isUndervoltage: Boolean = false,
    val isThrottled: Boolean = false,
    val netRxKbps: Double = 142.5,
    val netTxKbps: Double = 88.2,
    val activeProcesses: Int = 114
) {
    val ramPercent: Double
        get() = if (ramTotalMb > 0) (ramUsedMb.toDouble() / ramTotalMb.toDouble()) * 100.0 else 0.0

    val diskPercent: Double
        get() = if (diskTotalGb > 0) (diskUsedGb / diskTotalGb) * 100.0 else 0.0

    val formattedUptime: String
        get() {
            val days = uptimeSeconds / 86400
            val hours = (uptimeSeconds % 86400) / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            return "${days}d ${hours}h ${minutes}m"
        }
}
