package com.example.data.ai

import android.util.Log
import com.example.data.db.TelemetryEntity
import com.example.data.model.AiCategory
import com.example.data.model.AiPriority
import com.example.data.model.AiSuggestion
import com.example.data.model.PiTelemetry
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "RaspiAiAdvisor"

class RaspiAiAdvisor {

    private val suggestionItemSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short actionable recommendation title"),
            "category" to Schema.enumeration(
                values = listOf("THERMAL", "PERFORMANCE", "MEMORY", "SECURITY", "WORKFLOW"),
                description = "Category of recommendation"
            ),
            "description" to Schema.string(description = "Detailed explanation of the issue and why this helps"),
            "impact" to Schema.string(description = "Estimated gain e.g. -8°C lower thermal, 18% CPU savings"),
            "suggestedCommand" to Schema.string(description = "Linux bash command to run in the CLI to apply this fix", nullable = true),
            "priority" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "Priority level"
            )
        )
    )

    private val rootSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "High-level diagnostic summary of the Raspberry Pi status"),
            "suggestions" to Schema.array(
                items = suggestionItemSchema,
                description = "List of prioritized actionable recommendations"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    suspend fun analyzeSystem(
        telemetry: PiTelemetry,
        recentHistory: List<TelemetryEntity>
    ): Pair<String, List<AiSuggestion>> = withContext(Dispatchers.IO) {
        val avgTemp = if (recentHistory.isNotEmpty()) recentHistory.map { it.cpuTemp }.average() else telemetry.cpuTemp
        val maxTemp = if (recentHistory.isNotEmpty()) recentHistory.maxOf { it.cpuTemp } else telemetry.cpuTemp
        val avgCpu = if (recentHistory.isNotEmpty()) recentHistory.map { it.cpuUsage }.average() else telemetry.cpuUsage
        val throttledEvents = recentHistory.count { it.isThrottled }

        val prompt = """
            You are a Linux and Raspberry Pi Systems Engineer and Performance Optimization AI.
            Analyze the following real-time telemetry and historical performance metrics from a Raspberry Pi:

            Model: ${telemetry.modelName}
            OS: ${telemetry.osName}
            Kernel: ${telemetry.kernel}
            Uptime: ${telemetry.formattedUptime}
            
            Current Snapshot:
            - CPU Temp: ${telemetry.cpuTemp} °C (Avg: %.1f °C, Peak: %.1f °C)
            - GPU Temp: ${telemetry.gpuTemp} °C
            - Total CPU Load: ${telemetry.cpuUsage} % (Avg: %.1f %)
            - Core Loads: ${telemetry.cpuCores.joinToString(", ") { "%.1f%%".format(it) }}
            - CPU Frequency: ${telemetry.cpuFreqMhz} MHz
            - RAM Usage: ${telemetry.ramUsedMb} MB / ${telemetry.ramTotalMb} MB (%.1f%%)
            - Swap Usage: ${telemetry.swapUsedMb} MB / ${telemetry.swapTotalMb} MB
            - Disk Usage: ${telemetry.diskUsedGb} GB / ${telemetry.diskTotalGb} GB
            - Fan Speed: ${telemetry.fanSpeedRpm} RPM
            - Voltage: ${telemetry.voltageVolts} V (Undervolt: ${telemetry.isUndervoltage})
            - Throttled State: ${telemetry.isThrottled} (Throttling historical incidents: $throttledEvents)
            - Network I/O: RX ${telemetry.netRxKbps} KB/s, TX ${telemetry.netTxKbps} KB/s
            - Active Process Count: ${telemetry.activeProcesses}

            Identify potential bottlenecks, thermal risks, memory pressure, background service overhead, or workflow enhancements.
            Provide a diagnostic summary and 3-5 concrete, actionable suggestions with actual Linux commands (e.g. systemctl, vcgencmd, sysctl, nice, swapfile tuning, fan curve adjustments).
        """.trimIndent().format(avgTemp, maxTemp, avgCpu, telemetry.ramPercent)

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text ?: throw IllegalStateException("Model returned empty text.")
            Log.d(TAG, "Raw AI response: $text")

            val json = JSONObject(text)
            val summary = json.optString("summary", "System performance analysis complete.")
            val suggestionsArray = json.optJSONArray("suggestions")

            val list = mutableListOf<AiSuggestion>()
            if (suggestionsArray != null) {
                for (i in 0 until suggestionsArray.length()) {
                    val item = suggestionsArray.getJSONObject(i)
                    val catStr = item.optString("category", "PERFORMANCE")
                    val prioStr = item.optString("priority", "MEDIUM")
                    list.add(
                        AiSuggestion(
                            title = item.optString("title", "Optimize Configuration"),
                            category = try { AiCategory.valueOf(catStr) } catch (_: Exception) { AiCategory.PERFORMANCE },
                            description = item.optString("description", ""),
                            impact = item.optString("impact", "Improved system stability"),
                            suggestedCommand = item.optString("suggestedCommand").takeIf { it.isNotBlank() },
                            priority = try { AiPriority.valueOf(prioStr) } catch (_: Exception) { AiPriority.MEDIUM }
                        )
                    )
                }
            }

            Pair(summary, list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed, providing intelligent heuristic fallback", e)
            generateFallbackSuggestions(telemetry, avgTemp, avgCpu, throttledEvents)
        }
    }

    private fun generateFallbackSuggestions(
        telemetry: PiTelemetry,
        avgTemp: Double,
        avgCpu: Double,
        throttledCount: Int
    ): Pair<String, List<AiSuggestion>> {
        val list = mutableListOf<AiSuggestion>()

        if (telemetry.cpuTemp >= 70.0 || avgTemp >= 68.0) {
            list.add(
                AiSuggestion(
                    title = "Aggressive Fan Curve Tuning",
                    category = AiCategory.THERMAL,
                    description = "CPU core temperature is operating near thermal trip thresholds (${telemetry.cpuTemp}°C). Enabling dynamic PWM fan scaling prevents thermal frequency capping.",
                    impact = "Lowers temperature by 6-12°C under sustained load",
                    suggestedCommand = "sudo raspi-config nonint do_fan 1 55",
                    priority = AiPriority.HIGH
                )
            )
        }

        if (telemetry.ramPercent > 75.0) {
            list.add(
                AiSuggestion(
                    title = "Tune Zram / Swappiness Ratio",
                    category = AiCategory.MEMORY,
                    description = "Memory utilization is at ${"%.1f".format(telemetry.ramPercent)}%. Lowering vm.swappiness reduces SD card/SSD write degradation while maintaining cache throughput.",
                    impact = "Preserves RAM I/O and extends SD card lifespan",
                    suggestedCommand = "sudo sysctl vm.swappiness=10",
                    priority = AiPriority.MEDIUM
                )
            )
        }

        if (telemetry.cpuUsage > 50.0 || avgCpu > 45.0) {
            list.add(
                AiSuggestion(
                    title = "Audit Background Daemon Services",
                    category = AiCategory.PERFORMANCE,
                    description = "High baseline load detected with ${telemetry.activeProcesses} processes. Disabling unused display managers or bluetooth audio daemons reclaims CPU cycles.",
                    impact = "Reclaims 10-15% CPU headroom for core tasks",
                    suggestedCommand = "systemctl list-units --type=service --state=running",
                    priority = AiPriority.MEDIUM
                )
            )
        }

        list.add(
            AiSuggestion(
                title = "Automated Cron Health Check & Log Rotation",
                category = AiCategory.WORKFLOW,
                description = "Configure journald vacuuming to prevent log files from accumulating and filling flash storage over extended runtimes.",
                impact = "Saves disk space and speeds up boot telemetry queries",
                suggestedCommand = "sudo journalctl --vacuum-time=7d",
                priority = AiPriority.LOW
            )
        )

        val summary = "System operating at ${telemetry.cpuTemp}°C and ${telemetry.cpuUsage}% CPU. Recommendations tailored for ${telemetry.modelName}."
        return Pair(summary, list)
    }
}
