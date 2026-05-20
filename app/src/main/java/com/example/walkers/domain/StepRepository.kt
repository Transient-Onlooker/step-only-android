package com.example.walkers.domain

import android.content.Context
import com.example.walkers.data.AppDatabase
import com.example.walkers.data.AppStateEntity
import com.example.walkers.data.WalkingSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class StepRepository private constructor(context: Context) {
    private val database = AppDatabase.get(context)
    private val dailyStepDao = database.dailyStepDao()
    private val sessionDao = database.walkingSessionDao()
    private val appStateDao = database.appStateDao()
    private val recordMutex = Mutex()

    fun observeSummary(): Flow<StepSummary> {
        return combine(
            dailyStepDao.observeAll(),
            appStateDao.observeState(),
            sessionDao.observeActive(),
            sessionDao.observeRecent(5)
        ) { dailySteps, state, activeSession, recentSessions ->
            val appState = state ?: AppStateEntity()
            val today = LocalDate.now().toString()
            val todayStep = dailySteps.firstOrNull { it.date == today }

            StepSummary(
                todaySteps = todayStep?.steps ?: 0,
                todayDistanceKm = todayStep?.distanceKm ?: 0.0,
                totalSteps = appState.totalSteps,
                totalDistanceKm = DistanceCalculator.stepsToKm(
                    appState.totalSteps,
                    appState.strideMeters
                ),
                activeSession = activeSession,
                recentSessions = recentSessions
            )
        }
    }

    fun observeDailyHistory() = dailyStepDao.observeAll()

    fun observeSessions() = sessionDao.observeAll()

    suspend fun startManualSession() {
        recordMutex.withLock {
            if (sessionDao.getActive() != null) return

            sessionDao.insert(
                WalkingSessionEntity(
                    startedAt = System.currentTimeMillis(),
                    endedAt = null,
                    steps = 0,
                    distanceKm = 0.0,
                    isActive = true
                )
            )
        }
    }

    suspend fun finishManualSession() {
        recordMutex.withLock {
            sessionDao.endActive(System.currentTimeMillis())
        }
    }

    suspend fun recordRawSteps(rawSteps: Long) {
        recordMutex.withLock {
            val state = appStateDao.getOrCreate()
            val lastRawSteps = state.lastRawSteps

            if (lastRawSteps == null || rawSteps < lastRawSteps) {
                appStateDao.upsert(state.copy(lastRawSteps = rawSteps))
                return
            }

            val deltaSteps = rawSteps - lastRawSteps
            if (deltaSteps <= 0) return

            val now = System.currentTimeMillis()
            val deltaDistanceKm = DistanceCalculator.stepsToKm(deltaSteps, state.strideMeters)
            dailyStepDao.addSteps(
                date = LocalDate.now().toString(),
                deltaSteps = deltaSteps,
                deltaDistanceKm = deltaDistanceKm,
                updatedAt = now
            )
            appStateDao.upsert(
                state.copy(
                    lastRawSteps = rawSteps,
                    totalSteps = state.totalSteps + deltaSteps
                )
            )
            sessionDao.addStepsToActive(deltaSteps, deltaDistanceKm)
        }
    }

    companion object {
        @Volatile
        private var instance: StepRepository? = null

        fun get(context: Context): StepRepository {
            return instance ?: synchronized(this) {
                instance ?: StepRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
