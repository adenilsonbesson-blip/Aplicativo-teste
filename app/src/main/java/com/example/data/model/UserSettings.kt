package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val dailyGoalMl: Int = 2500,
    val weightKg: Float = 70.0f,
    val activityLevel: String = "MODERATE", // SEDENTARY (30ml/kg), MODERATE (35ml/kg), INTENSE (40ml/kg)
    val wakeUpHour: Int = 7,
    val wakeUpMinute: Int = 0,
    val bedHour: Int = 22,
    val bedMinute: Int = 0,
    val smartRemindersEnabled: Boolean = true,
    val loudAlarmEnabled: Boolean = true, // Persistent siren/alarm sound until user clicks confirm
    val soundVolume: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val fixedIntervalMinutes: Int = 90,
    val streakDays: Int = 1,
    val lastGoalMetDate: String = ""
) {
    val wakeUpMinutesFromMidnight: Int
        get() = wakeUpHour * 60 + wakeUpMinute

    val bedMinutesFromMidnight: Int
        get() = bedHour * 60 + bedMinute

    val awakeDurationMinutes: Int
        get() = if (bedMinutesFromMidnight > wakeUpMinutesFromMidnight) {
            bedMinutesFromMidnight - wakeUpMinutesFromMidnight
        } else {
            (24 * 60 - wakeUpMinutesFromMidnight) + bedMinutesFromMidnight
        }
}
