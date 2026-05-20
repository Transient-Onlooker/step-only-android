package com.example.walkers.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepEntity::class,
        WalkingSessionEntity::class,
        AppStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStepDao(): DailyStepDao
    abstract fun walkingSessionDao(): WalkingSessionDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "step_only.db"
                ).build().also { instance = it }
            }
        }
    }
}
