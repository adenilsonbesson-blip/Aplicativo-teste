package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.repository.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HydrationAlarmScheduler {

    private const val ALARM_REQUEST_CODE = 9001

    fun scheduleAlarmAt(context: Context, triggerAtMillis: Long, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(HydrationAlarmReceiver.EXTRA_MESSAGE, message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setAlarmClock or setExactAndAllowWhileIdle for reliable waking even during deep Doze mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    val showIntent = Intent(context, com.example.MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d("HydrationScheduler", "Alarm scheduled for timestamp $triggerAtMillis ($message)")
        } catch (e: Exception) {
            Log.e("HydrationScheduler", "Failed to schedule alarm", e)
        }
    }

    fun recalculateAndScheduleNext(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val repository = HydrationRepository(db.hydrationDao())
            val settings = repository.getSettingsSync()

            if (!settings.alarmsEnabled || (!settings.loudAlarmEnabled && !settings.smartRemindersEnabled)) {
                cancelAlarms(context)
                return@launch
            }

            val todayLogs = repository.getTodayLogsSync()
            val totalMl = todayLogs.sumOf { it.amountMl }

            val paceInfo = SmartHydrationCalculator.calculatePace(
                todayTotalMl = totalMl,
                settings = settings
            )

            scheduleAlarmAt(
                context = context,
                triggerAtMillis = paceInfo.nextAlarmTimeMillis,
                message = paceInfo.message
            )
        }
    }

    fun snoozeAlarm(context: Context, minutes: Int = 10) {
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        scheduleAlarmAt(context, triggerTime, "⏰ Lembrete adiado de hidratação! Tome um copo de água agora.")
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
