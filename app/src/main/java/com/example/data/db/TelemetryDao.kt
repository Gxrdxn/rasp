package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TelemetryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getRecordsSince(sinceTimestamp: Long): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getRecordsSinceDirect(sinceTimestamp: Long): List<TelemetryEntity>

    @Query("SELECT * FROM telemetry_records ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int): List<TelemetryEntity>

    @Query("SELECT COUNT(*) FROM telemetry_records")
    suspend fun getCount(): Int

    @Query("DELETE FROM telemetry_records WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
