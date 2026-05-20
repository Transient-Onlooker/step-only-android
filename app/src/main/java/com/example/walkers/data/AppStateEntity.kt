package com.example.walkers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastRawSteps: Long? = null,
    val totalSteps: Long = 0,
    val strideMeters: Double = DEFAULT_STRIDE_METERS,
    val trackingEnabled: Boolean = true
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_STRIDE_METERS = 0.70
    }
}
