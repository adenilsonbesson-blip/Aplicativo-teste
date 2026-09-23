package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.alarm.HydrationAlarmScheduler
import com.example.alarm.HydrationAlarmService
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AquaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Bootstrap Database and schedule initial smart alarm
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(this@AquaApplication)
            HydrationAlarmScheduler.recalculateAndScheduleNext(this@AquaApplication)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                HydrationAlarmService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
                enableLights(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(alarmChannel)
        }
    }
}
