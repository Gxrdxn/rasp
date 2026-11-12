package com.example.data.model

enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

data class AlertRule(
    val tempWarningThreshold: Double = 70.0,
    val tempCriticalThreshold: Double = 80.0,
    val cpuWarningThreshold: Double = 85.0,
    val cpuCriticalThreshold: Double = 95.0,
    val ramWarningThreshold: Double = 90.0,
    val notificationsEnabled: Boolean = true,
    val alertSoundEnabled: Boolean = true
)

data class AlertEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val isAcknowledged: Boolean = false
)

data class TerminalEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val output: String,
    val exitCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemNotice: Boolean = false
)

enum class AiCategory {
    THERMAL, PERFORMANCE, MEMORY, SECURITY, WORKFLOW
}

enum class AiPriority {
    HIGH, MEDIUM, LOW
}

data class AiSuggestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: AiCategory,
    val description: String,
    val impact: String,
    val suggestedCommand: String? = null,
    val priority: AiPriority = AiPriority.MEDIUM,
    val isApplied: Boolean = false
)

enum class WidgetType {
    OVERVIEW_GAUGES,
    THERMAL_FAN,
    CPU_CORES,
    MEMORY_STORAGE,
    REALTIME_CHART,
    CONNECTED_DEVICES,
    GPIO_MATRIX,
    NETWORK_STATUS,
    CLI_TERMINAL,
    AI_OPTIMIZER
}

data class DashboardWidget(
    val type: WidgetType,
    val title: String,
    val isVisible: Boolean = true,
    val order: Int
)

enum class TimeRange(val label: String, val durationMs: Long) {
    MINUTES_15("15m", 15 * 60 * 1000L),
    HOUR_1("1h", 60 * 60 * 1000L),
    HOURS_6("6h", 6 * 60 * 60 * 1000L),
    HOURS_24("24h", 24 * 60 * 60 * 1000L),
    DAYS_7("7d", 7 * 24 * 60 * 60 * 1000L)
}
