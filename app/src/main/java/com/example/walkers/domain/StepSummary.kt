package com.example.walkers.domain

import com.example.walkers.data.WalkingSessionEntity

data class StepSummary(
    val todaySteps: Long = 0,
    val todayDistanceKm: Double = 0.0,
    val totalSteps: Long = 0,
    val totalDistanceKm: Double = 0.0,
    val activeSession: WalkingSessionEntity? = null,
    val recentSessions: List<WalkingSessionEntity> = emptyList()
)
