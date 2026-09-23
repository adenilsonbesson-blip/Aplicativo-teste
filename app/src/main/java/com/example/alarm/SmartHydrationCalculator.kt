package com.example.alarm

import com.example.data.model.UserSettings
import java.util.Calendar

data class HydrationPaceInfo(
    val currentTotalMl: Int,
    val dailyGoalMl: Int,
    val percentage: Int,
    val expectedMlAtThisTime: Int,
    val paceStatus: PaceStatus,
    val deficitMl: Int,
    val recommendedNextIntervalMinutes: Int,
    val isBedtime: Boolean,
    val nextAlarmTimeMillis: Long,
    val message: String
)

enum class PaceStatus(val title: String, val emoji: String) {
    BEHIND("Abaixo da Meta", "⚠️"),
    ON_TRACK("No Ritmo Ideal", "💧"),
    AHEAD("Adiantado", "🚀"),
    GOAL_REACHED("Meta Batida!", "🎉"),
    SLEEPING("Horário de Descanso", "🌙")
}

object SmartHydrationCalculator {

    fun calculatePace(
        todayTotalMl: Int,
        settings: UserSettings,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): HydrationPaceInfo {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentMinutesFromMidnight = currentHour * 60 + currentMinute

        val wakeMinutes = settings.wakeUpMinutesFromMidnight
        val bedMinutes = settings.bedMinutesFromMidnight
        val awakeDuration = settings.awakeDurationMinutes.coerceAtLeast(60)

        // Check if current time is within sleep window
        val isBedtime = if (bedMinutes > wakeMinutes) {
            currentMinutesFromMidnight < wakeMinutes || currentMinutesFromMidnight >= bedMinutes
        } else {
            currentMinutesFromMidnight >= bedMinutes && currentMinutesFromMidnight < wakeMinutes
        }

        val percentage = ((todayTotalMl.toFloat() / settings.dailyGoalMl.coerceAtLeast(100)) * 100).toInt()

        if (isBedtime) {
            val nextWakeTime = getNextWakeUpTimeMillis(settings, currentTimeMillis)
            return HydrationPaceInfo(
                currentTotalMl = todayTotalMl,
                dailyGoalMl = settings.dailyGoalMl,
                percentage = percentage,
                expectedMlAtThisTime = 0,
                paceStatus = PaceStatus.SLEEPING,
                deficitMl = 0,
                recommendedNextIntervalMinutes = ((nextWakeTime - currentTimeMillis) / 60000).toInt(),
                isBedtime = true,
                nextAlarmTimeMillis = nextWakeTime,
                message = "Horário de sono. O próximo alarme inteligente tocará às ${String.format("%02d:%02d", settings.wakeUpHour, settings.wakeUpMinute)}."
            )
        }

        if (percentage >= 100) {
            val nextWakeTime = getNextWakeUpTimeMillis(settings, currentTimeMillis)
            return HydrationPaceInfo(
                currentTotalMl = todayTotalMl,
                dailyGoalMl = settings.dailyGoalMl,
                percentage = percentage,
                expectedMlAtThisTime = settings.dailyGoalMl,
                paceStatus = PaceStatus.GOAL_REACHED,
                deficitMl = 0,
                recommendedNextIntervalMinutes = 180,
                isBedtime = false,
                nextAlarmTimeMillis = currentTimeMillis + 180 * 60 * 1000L,
                message = "Parabéns! Você já atingiu sua meta diária de ${settings.dailyGoalMl}ml! Continue bebendo para se refrescar."
            )
        }

        // Calculate expected hydration up to this minute of awake time
        val minutesElapsedSinceWake = if (bedMinutes > wakeMinutes) {
            (currentMinutesFromMidnight - wakeMinutes).coerceIn(0, awakeDuration)
        } else {
            if (currentMinutesFromMidnight >= wakeMinutes) {
                currentMinutesFromMidnight - wakeMinutes
            } else {
                (24 * 60 - wakeMinutes) + currentMinutesFromMidnight
            }.coerceIn(0, awakeDuration)
        }

        val awakeFraction = minutesElapsedSinceWake.toFloat() / awakeDuration.toFloat()
        val expectedMl = (settings.dailyGoalMl * awakeFraction).toInt()
        val difference = todayTotalMl - expectedMl

        val paceStatus: PaceStatus
        val intervalMinutes: Int
        val smartMessage: String

        if (!settings.smartRemindersEnabled) {
            intervalMinutes = settings.fixedIntervalMinutes
            paceStatus = if (difference < -200) PaceStatus.BEHIND else PaceStatus.ON_TRACK
            smartMessage = "Lembretes regulares a cada $intervalMinutes minutos."
        } else {
            when {
                difference < -250 -> {
                    paceStatus = PaceStatus.BEHIND
                    intervalMinutes = 45 // Urgently catch up!
                    smartMessage = "Atenção: Você está ${-difference}ml atrás da meta prevista para agora. Beba água para reidratar!"
                }
                difference < 0 -> {
                    paceStatus = PaceStatus.BEHIND
                    intervalMinutes = 60
                    smartMessage = "Você está levemente abaixo do ritmo (${-difference}ml). Um copo de água agora resolve!"
                }
                difference > 300 -> {
                    paceStatus = PaceStatus.AHEAD
                    intervalMinutes = 120 // Hydration surplus, relax interval
                    smartMessage = "Excelente! Você está $difference ml adiantado em relação ao esperado!"
                }
                else -> {
                    paceStatus = PaceStatus.ON_TRACK
                    intervalMinutes = 90
                    smartMessage = "Ótimo ritmo! Você está acompanhando certinho a meta de hidratação do dia."
                }
            }
        }

        var nextAlarm = currentTimeMillis + (intervalMinutes * 60 * 1000L)
        val bedtimeMillis = getTodayBedtimeMillis(settings, currentTimeMillis)
        if (nextAlarm > bedtimeMillis) {
            // Next alarm would fall in sleep time, schedule for next morning
            nextAlarm = getNextWakeUpTimeMillis(settings, currentTimeMillis)
        }

        return HydrationPaceInfo(
            currentTotalMl = todayTotalMl,
            dailyGoalMl = settings.dailyGoalMl,
            percentage = percentage,
            expectedMlAtThisTime = expectedMl,
            paceStatus = paceStatus,
            deficitMl = if (difference < 0) -difference else 0,
            recommendedNextIntervalMinutes = intervalMinutes,
            isBedtime = false,
            nextAlarmTimeMillis = nextAlarm,
            message = smartMessage
        )
    }

    private fun getTodayBedtimeMillis(settings: UserSettings, currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        calendar.set(Calendar.HOUR_OF_DAY, settings.bedHour)
        calendar.set(Calendar.MINUTE, settings.bedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getNextWakeUpTimeMillis(settings: UserSettings, currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        calendar.set(Calendar.HOUR_OF_DAY, settings.wakeUpHour)
        calendar.set(Calendar.MINUTE, settings.wakeUpMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= currentTimeMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
