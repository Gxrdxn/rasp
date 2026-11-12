package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.db.TelemetryEntity
import com.example.data.model.PiTelemetry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun exportCsv(context: Context, telemetry: PiTelemetry, records: List<TelemetryEntity>): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "raspi_report_${fileDateFormat.format(Date())}.csv"
        val file = File(reportsDir, fileName)

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write("Timestamp,DateTime,CpuUsagePercent,CpuTempC,GpuTempC,RamUsedMb,RamTotalMb,DiskUsedGb,DiskTotalGb,FanSpeedRpm,VoltageVolts,IsThrottled,NetRxKbps,NetTxKbps\n")
            
            val allRecords = if (records.isNotEmpty()) records else listOf(
                TelemetryEntity(
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
            )

            for (r in allRecords) {
                val dt = dateFormat.format(Date(r.timestamp))
                writer.write("${r.timestamp},\"$dt\",${r.cpuUsage},${r.cpuTemp},${r.gpuTemp},${r.ramUsedMb},${r.ramTotalMb},${r.diskUsedGb},${r.diskTotalGb},${r.fanSpeedRpm},${r.voltageVolts},${r.isThrottled},${r.netRxKbps},${r.netTxKbps}\n")
            }
        }
        return file
    }

    fun exportPdf(context: Context, telemetry: PiTelemetry, records: List<TelemetryEntity>): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "raspi_system_report_${fileDateFormat.format(Date())}.pdf"
        val file = File(reportsDir, fileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(227, 11, 92) // Raspberry Red
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(25, 33, 44) // Deep Dark Navy
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(55, 65, 81)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val subtextPaint = Paint().apply {
            color = Color.rgb(120, 130, 145)
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 225, 235)
            strokeWidth = 1f
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }

        var y = 45f

        // Title Header
        canvas.drawText("RaspiDash - System Diagnostic & Analytics Report", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Generated on ${dateFormat.format(Date())} | Host: ${telemetry.hostname}", 40f, y, subtextPaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        // System Specs Box
        canvas.drawRoundRect(40f, y, 555f, y + 68f, 6f, 6f, cardBgPaint)
        canvas.drawText("System Information", 52f, y + 18f, headerPaint)
        canvas.drawText("Model: ${telemetry.modelName}", 52f, y + 36f, bodyPaint)
        canvas.drawText("OS: ${telemetry.osName}", 52f, y + 52f, bodyPaint)
        canvas.drawText("Kernel: ${telemetry.kernel}", 300f, y + 36f, bodyPaint)
        canvas.drawText("Uptime: ${telemetry.formattedUptime}", 300f, y + 52f, bodyPaint)
        y += 85f

        // Summary Analytics Metrics
        val avgCpu = if (records.isNotEmpty()) records.map { it.cpuUsage }.average() else telemetry.cpuUsage
        val maxCpu = if (records.isNotEmpty()) records.maxOf { it.cpuUsage } else telemetry.cpuUsage
        val avgTemp = if (records.isNotEmpty()) records.map { it.cpuTemp }.average() else telemetry.cpuTemp
        val maxTemp = if (records.isNotEmpty()) records.maxOf { it.cpuTemp } else telemetry.cpuTemp
        val throttledCount = if (records.isNotEmpty()) records.count { it.isThrottled } else 0

        canvas.drawText("Telemetry Summary Overview", 40f, y, headerPaint)
        y += 16f

        // Draw 4 mini metric cards
        val cardWidth = 120f
        val cardHeight = 48f
        val cards = listOf(
            Pair("Avg CPU Load", "%.1f %%".format(avgCpu)),
            Pair("Peak CPU Load", "%.1f %%".format(maxCpu)),
            Pair("Avg CPU Temp", "%.1f °C".format(avgTemp)),
            Pair("Peak CPU Temp", "%.1f °C".format(maxTemp))
        )

        for (i in cards.indices) {
            val left = 40f + (i * (cardWidth + 12f))
            canvas.drawRoundRect(left, y, left + cardWidth, y + cardHeight, 6f, 6f, cardBgPaint)
            canvas.drawText(cards[i].first, left + 8f, y + 16f, subtextPaint)
            canvas.drawText(cards[i].second, left + 8f, y + 36f, headerPaint)
        }
        y += 68f

        // Additional status
        canvas.drawText("Current Memory: ${telemetry.ramUsedMb} MB / ${telemetry.ramTotalMb} MB (%.1f%%)".format(telemetry.ramPercent), 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Current Storage: ${telemetry.diskUsedGb} GB / ${telemetry.diskTotalGb} GB (%.1f%%)".format(telemetry.diskPercent), 40f, y, bodyPaint)
        y += 16f
        canvas.drawText("Fan Speed: ${telemetry.fanSpeedRpm} RPM | Voltage: ${telemetry.voltageVolts} V | Throttle Events: $throttledCount", 40f, y, bodyPaint)
        y += 24f

        // Recent Telemetry Table
        canvas.drawText("Recorded Performance Samples (Recent)", 40f, y, headerPaint)
        y += 16f

        // Table Header
        canvas.drawRect(40f, y, 555f, y + 20f, cardBgPaint)
        canvas.drawText("Time", 48f, y + 14f, headerPaint)
        canvas.drawText("CPU %", 170f, y + 14f, headerPaint)
        canvas.drawText("Temp °C", 240f, y + 14f, headerPaint)
        canvas.drawText("RAM (MB)", 320f, y + 14f, headerPaint)
        canvas.drawText("Fan RPM", 410f, y + 14f, headerPaint)
        canvas.drawText("Status", 490f, y + 14f, headerPaint)
        y += 22f

        // Draw sample rows (up to 20 samples)
        val tableRecords = records.takeLast(20).reversed()
        for (rec in tableRecords) {
            if (y > 780f) break
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(rec.timestamp))
            canvas.drawText(timeStr, 48f, y + 13f, bodyPaint)
            canvas.drawText("%.1f%%".format(rec.cpuUsage), 170f, y + 13f, bodyPaint)
            canvas.drawText("%.1f°C".format(rec.cpuTemp), 240f, y + 13f, bodyPaint)
            canvas.drawText("${rec.ramUsedMb}", 320f, y + 13f, bodyPaint)
            canvas.drawText("${rec.fanSpeedRpm}", 410f, y + 13f, bodyPaint)
            val status = if (rec.isThrottled) "THROTTLED" else "NORMAL"
            canvas.drawText(status, 490f, y + 13f, bodyPaint)
            y += 18f
            canvas.drawLine(40f, y, 555f, y, linePaint)
        }

        // Footer
        canvas.drawText("RaspiDash Analytics - Real-time Raspberry Pi Monitoring Framework", 40f, 815f, subtextPaint)

        document.finishPage(page)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
