package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.HydrationLog
import com.example.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [HydrationLog::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hydrationDao(): HydrationDao

    companion object {
        private const val DATABASE_NAME = "aqua_alerta_database.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(false)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            val dao = database.hydrationDao()
                            // Initialize default persistent user settings if not present
                            if (dao.getUserSettingsSync() == null) {
                                dao.saveUserSettings(UserSettings())
                            }
                            // Initial hydration logs on first install
                            val now = System.currentTimeMillis()
                            val fourHoursAgo = now - 4 * 60 * 60 * 1000
                            val twoHoursAgo = now - 2 * 60 * 60 * 1000
                            dao.insertLog(HydrationLog(amountMl = 300, drinkType = "WATER", timestamp = fourHoursAgo, note = "Ao acordar"))
                            dao.insertLog(HydrationLog(amountMl = 250, drinkType = "TEA", timestamp = twoHoursAgo, note = "Chá verde matinal"))
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            val dao = database.hydrationDao()
                            if (dao.getUserSettingsSync() == null) {
                                dao.saveUserSettings(UserSettings())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
