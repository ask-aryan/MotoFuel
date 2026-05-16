package com.example.fuletracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val odometer: Double,
    val fuelAmount: Double,
    val pricePerLiter: Double = 0.0,
    val fullTank: Boolean = true,
    val fuelType: String = "Petrol",
    val date: Long = System.currentTimeMillis()
)
