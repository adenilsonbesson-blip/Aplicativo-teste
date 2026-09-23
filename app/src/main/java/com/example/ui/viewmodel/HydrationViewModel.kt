package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.HydrationAlarmScheduler
import com.example.alarm.HydrationAlarmService
import com.example.alarm.HydrationPaceInfo
import com.example.alarm.PaceStatus
import com.example.alarm.SmartHydrationCalculator
import com.example.data.db.AppDatabase
import com.example.data.model.DrinkType
import com.example.data.model.HydrationLog
import com.example.data.model.UserSettings
import com.example.data.repository.HydrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HydrationViewModel(
    application: Application,
    private val repository: HydrationRepository
) : AndroidViewModel(application) {

    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    val todayLogs: StateFlow<List<HydrationLog>> = repository.getTodayLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<HydrationLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isAlarmRinging: StateFlow<Boolean> = HydrationAlarmService.isRingingFlow

    val activeAlarmMessage: StateFlow<String> = HydrationAlarmService.activeAlarmMessage

    val paceInfo: StateFlow<HydrationPaceInfo> = combine(
        todayLogs,
        userSettings
    ) { logs, settings ->
        val totalMl = logs.sumOf { it.amountMl }
        SmartHydrationCalculator.calculatePace(
            todayTotalMl = totalMl,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HydrationPaceInfo(
            currentTotalMl = 0,
            dailyGoalMl = 2500,
            percentage = 0,
            expectedMlAtThisTime = 0,
            paceStatus = PaceStatus.ON_TRACK,
            deficitMl = 0,
            recommendedNextIntervalMinutes = 90,
            isBedtime = false,
            nextAlarmTimeMillis = System.currentTimeMillis() + 90 * 60 * 1000L,
            message = "Carregando status de hidratação..."
        )
    )

    fun logWater(amountMl: Int, drinkType: DrinkType = DrinkType.WATER, note: String = "") {
        viewModelScope.launch {
            repository.logWater(amountMl, drinkType, note)
            // If the alarm is ringing, stop it immediately!
            if (isAlarmRinging.value) {
                HydrationAlarmService.stop(getApplication())
            }
            HydrationAlarmScheduler.recalculateAndScheduleNext(getApplication())
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLog(id)
            HydrationAlarmScheduler.recalculateAndScheduleNext(getApplication())
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            HydrationAlarmScheduler.recalculateAndScheduleNext(getApplication())
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            HydrationAlarmScheduler.recalculateAndScheduleNext(getApplication())
        }
    }

    fun triggerAlarmTestNow() {
        val settings = userSettings.value
        HydrationAlarmService.start(
            context = getApplication(),
            message = "⚠️ TESTE DE ALARME: Este som continuará tocando até você confirmar que bebeu água!",
            volume = settings.soundVolume,
            vibrate = settings.vibrationEnabled
        )
    }

    fun stopAlarmDirectly() {
        HydrationAlarmService.stop(getApplication())
        HydrationAlarmScheduler.recalculateAndScheduleNext(getApplication())
    }

    fun snoozeAlarm(minutes: Int = 10) {
        HydrationAlarmService.stop(getApplication())
        HydrationAlarmScheduler.snoozeAlarm(getApplication(), minutes)
    }

    fun calculateRecommendedWaterGoal(weightKg: Float, activityLevel: String): Int {
        val baseMlPerKg = when (activityLevel) {
            "SEDENTARY" -> 30f
            "INTENSE" -> 40f
            else -> 35f // MODERATE
        }
        return (weightKg * baseMlPerKg).toInt()
    }
}

class HydrationViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HydrationViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val repository = HydrationRepository(db.hydrationDao())
            return HydrationViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
