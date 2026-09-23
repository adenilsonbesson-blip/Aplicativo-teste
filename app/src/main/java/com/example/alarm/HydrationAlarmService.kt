package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HydrationAlarmService : Service() {

    private lateinit var soundPlayer: AlarmSoundPlayer
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "hydration_alarm_channel_high_v2"
        const val NOTIFICATION_ID = 7001

        const val ACTION_START_ALARM = "com.example.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_ALARM"

        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_VIBRATE = "extra_vibrate"

        private val _isRingingFlow = MutableStateFlow(false)
        val isRingingFlow: StateFlow<Boolean> = _isRingingFlow.asStateFlow()

        private val _activeAlarmMessage = MutableStateFlow("Hora de beber água!")
        val activeAlarmMessage: StateFlow<String> = _activeAlarmMessage.asStateFlow()

        fun start(context: Context, message: String = "Hora de beber água!", volume: Float = 1.0f, vibrate: Boolean = true) {
            val intent = Intent(context, HydrationAlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_VIBRATE, vibrate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HydrationAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AlarmSoundPlayer(this)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AquaAlerta:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM

        if (action == ACTION_STOP_ALARM) {
            stopAlarmAndService()
            return START_NOT_STICKY
        }

        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Seu corpo precisa de hidratação agora! Beba água para desligar."
        val volume = intent?.getFloatExtra(EXTRA_VOLUME, 1.0f) ?: 1.0f
        val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true

        _activeAlarmMessage.value = message
        _isRingingFlow.value = true

        wakeLock?.acquire(3 * 60 * 1000L) // 3 minutes timeout safety

        val notification = buildPersistentNotification(message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        soundPlayer.startLoudAlarm(volume = volume, enableVibration = vibrate)

        return START_STICKY
    }

    private fun stopAlarmAndService() {
        _isRingingFlow.value = false
        soundPlayer.stopAlarm()
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            // ignore
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarme Sonoro de Hidratação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações com som contínuo e persistente para garantir sua hidratação."
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(message: String): Notification {
        // Tap notification -> opens full screen app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.example.ACTION_OPEN_ALARM_RINGING"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Confirm Hydration (Bebi 250ml) -> automatically shuts off alarm & logs water!
        val confirmIntent = Intent(this, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_CONFIRM_HYDRATION
            putExtra(HydrationAlarmReceiver.EXTRA_AMOUNT_ML, 250)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            this,
            101,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10m
        val snoozeIntent = Intent(this, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_fg)
            .setContentTitle("🚨 ALARME: HORA DE BEBER ÁGUA!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nToque em 'Bebi 250ml' para confirmar e desligar o alarme sonoro."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_add,
                "💧 BEBI 250ml (Desligar)",
                confirmPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "⏰ Adiar 10 min",
                snoozePendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        stopAlarmAndService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
