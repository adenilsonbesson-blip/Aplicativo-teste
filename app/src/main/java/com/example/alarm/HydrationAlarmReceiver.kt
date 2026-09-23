package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.DrinkType
import com.example.data.repository.HydrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HydrationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.ACTION_TRIGGER_ALARM"
        const val ACTION_CONFIRM_HYDRATION = "com.example.ACTION_CONFIRM_HYDRATION"
        const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"

        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_DRINK_TYPE = "extra_drink_type"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Hora de se hidratar!"
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val settings = db.hydrationDao().getUserSettingsSync()
                    val volume = settings?.soundVolume ?: 1.0f
                    val vibrate = settings?.vibrationEnabled ?: true

                    HydrationAlarmService.start(
                        context = context,
                        message = message,
                        volume = volume,
                        vibrate = vibrate
                    )
                }
            }

            ACTION_CONFIRM_HYDRATION -> {
                val amountMl = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                val drinkTypeName = intent.getStringExtra(EXTRA_DRINK_TYPE) ?: DrinkType.WATER.name
                val drinkType = DrinkType.fromName(drinkTypeName)

                // 1. Immediately stop the persistent alarm sound and service
                HydrationAlarmService.stop(context)

                // 2. Log water into Room Database & schedule next alarm
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val repository = HydrationRepository(db.hydrationDao())
                        repository.logWater(amountMl, drinkType, "Confirmado pelo alarme")

                        // Schedule next smart alarm
                        HydrationAlarmScheduler.recalculateAndScheduleNext(context)

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "💧 $amountMl ml de ${drinkType.displayName} registrados! Alarme desligado.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_ALARM -> {
                // Stop the active ringing alarm
                HydrationAlarmService.stop(context)
                HydrationAlarmScheduler.snoozeAlarm(context, 10)

                Toast.makeText(context, "⏰ Alarme adiado por 10 minutos.", Toast.LENGTH_SHORT).show()
            }

            ACTION_DISMISS_ALARM -> {
                HydrationAlarmService.stop(context)
                HydrationAlarmScheduler.recalculateAndScheduleNext(context)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                HydrationAlarmScheduler.recalculateAndScheduleNext(context)
            }
        }
    }
}
