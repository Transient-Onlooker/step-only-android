package com.example.walkers.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkingSessionDao {
    @Query("SELECT * FROM walking_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WalkingSessionEntity>>

    @Query("SELECT * FROM walking_sessions WHERE isActive = 0 ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WalkingSessionEntity>>

    @Query("SELECT * FROM walking_sessions WHERE isActive = 1 ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WalkingSessionEntity?>

    @Query("SELECT * FROM walking_sessions WHERE isActive = 1 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): WalkingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WalkingSessionEntity): Long

    @Query(
        """
        UPDATE walking_sessions
        SET steps = steps + :deltaSteps,
            distanceKm = distanceKm + :deltaDistanceKm
        WHERE isActive = 1 AND isPaused = 0
        """
    )
    suspend fun addStepsToActive(deltaSteps: Long, deltaDistanceKm: Double)

    @Query("UPDATE walking_sessions SET isPaused = 1 WHERE isActive = 1")
    suspend fun pauseActive()

    @Query("UPDATE walking_sessions SET isPaused = 0 WHERE isActive = 1")
    suspend fun resumeActive()

    @Query(
        """
        UPDATE walking_sessions
        SET endedAt = :endedAt,
            isActive = 0,
            isPaused = 0
        WHERE isActive = 1
        """
    )
    suspend fun endActive(endedAt: Long)
}
