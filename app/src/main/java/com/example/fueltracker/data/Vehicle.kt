package com.example.fueltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val licensePlate: String = "",
    val imageUrl: String? = null,
    val fuelType: String = "Petrol",
    val tankCapacity: Double = 13.0 // Default tank capacity for range estimation
)
