package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telemetry_records",
    indices = [Index(value = ["timestamp"])]
)
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val cpuTemp: Double,
    val gpuTemp: Double,
    val cpuUsage: Double,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val diskUsedGb: Double,
    val diskTotalGb: Double,
    val fanSpeedRpm: Int,
    val voltageVolts: Double,
    val netRxKbps: Double,
    val netTxKbps: Double,
    val isThrottled: Boolean
)
