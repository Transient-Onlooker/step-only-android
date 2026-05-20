package com.example.walkers.domain

object DistanceCalculator {
    fun stepsToKm(steps: Long, strideMeters: Double): Double {
        return steps * strideMeters / 1000.0
    }
}
