package com.example.fueltracker.ux

data class FuelStats(
    val avgEfficiency: Double,
    val bestEfficiency: Double,
    val worstEfficiency: Double,
    val totalDistance: Double,
    val totalFuel: Double,
    val totalCost: Double,
    val lastOdometer: Double,
    val costPerKm: Double,
    val entryCount: Int
)
