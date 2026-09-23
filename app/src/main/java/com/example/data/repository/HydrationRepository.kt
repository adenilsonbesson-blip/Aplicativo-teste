package com.example.data.repository

import com.example.data.db.HydrationDao
import com.example.data.model.DrinkType
import com.example.data.model.HydrationLog
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class HydrationRepository(private val dao: HydrationDao) {

    val allLogs: Flow<List<HydrationLog>> = dao.getAllLogs()

    val userSettings: Flow<UserSettings> = dao.getUserSettingsFlow().map { it ?: UserSettings() }

    fun getTodayLogs(): Flow<List<HydrationLog>> {
        val (start, end) = getDayRange(0)
        return dao.getLogsBetween(start, end)
    }

    suspend fun getTodayLogsSync(): List<HydrationLog> {
        val (start, end) = getDayRange(0)
        return dao.getLogsBetweenSync(start, end)
    }

    suspend fun getLogsForDaysAgo(daysAgo: Int): List<HydrationLog> {
        val (start, end) = getDayRange(daysAgo)
        return dao.getLogsBetweenSync(start, end)
    }

    suspend fun logWater(amountMl: Int, drinkType: DrinkType = DrinkType.WATER, note: String = ""): Long {
        val log = HydrationLog(
            amountMl = amountMl,
            drinkType = drinkType.name,
            timestamp = System.currentTimeMillis(),
            note = note
        )
        return dao.insertLog(log)
    }

    suspend fun deleteLog(id: Long) {
        dao.deleteLogById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllLogs()
    }

    suspend fun getSettingsSync(): UserSettings {
        return dao.getUserSettingsSync() ?: UserSettings()
    }

    suspend fun updateSettings(settings: UserSettings) {
        dao.saveUserSettings(settings)
    }

    companion object {
        fun getDayRange(daysAgo: Int = 0): Pair<Long, Long> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endTime = calendar.timeInMillis

            return Pair(startTime, endTime)
        }
    }
}
