package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HydrationLog
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {

    @Query("SELECT * FROM hydration_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<HydrationLog>>

    @Query("SELECT * FROM hydration_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getLogsBetween(startTime: Long, endTime: Long): Flow<List<HydrationLog>>

    @Query("SELECT * FROM hydration_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getLogsBetweenSync(startTime: Long, endTime: Long): List<HydrationLog>

    @Query("SELECT * FROM hydration_logs WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getLogsForDayFlow(startTime: Long, endTime: Long): Flow<List<HydrationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HydrationLog): Long

    @Query("DELETE FROM hydration_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM hydration_logs")
    suspend fun clearAllLogs()

    // Settings
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettingsFlow(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSettings(settings: UserSettings)
}
