package com.example.walkers.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepDao {
    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyStepEntity>>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyStepEntity?>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyStep: DailyStepEntity)

    @Transaction
    suspend fun addSteps(date: String, deltaSteps: Long, deltaDistanceKm: Double, updatedAt: Long) {
        val current = getByDate(date)
        if (current == null) {
            upsert(
                DailyStepEntity(
                    date = date,
                    steps = deltaSteps,
                    distanceKm = deltaDistanceKm,
                    updatedAt = updatedAt
                )
            )
        } else {
            upsert(
                current.copy(
                    steps = current.steps + deltaSteps,
                    distanceKm = current.distanceKm + deltaDistanceKm,
                    updatedAt = updatedAt
                )
            )
        }
    }
}
