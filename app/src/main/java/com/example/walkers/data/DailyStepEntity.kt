package com.example.walkers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepEntity(
    @PrimaryKey val date: String,
    val steps: Long,
    val distanceKm: Double,
    val updatedAt: Long
)
