package com.example.walkers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walking_sessions")
data class WalkingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long?,
    val steps: Long,
    val distanceKm: Double,
    val isActive: Boolean,
    val isPaused: Boolean = false
)
