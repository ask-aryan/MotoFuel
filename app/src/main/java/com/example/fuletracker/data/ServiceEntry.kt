package com.example.fuletracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_entries",
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
data class ServiceEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val date: Long = System.currentTimeMillis(),
    val title: String,                  // e.g. "Chain Lube", "Engine Oil Change"
    val category: String = "Other",     // Chain, Oil, Service, Tyres, Brakes, Other
    val odometer: Double,
    val cost: Double = 0.0,
    val notes: String = ""
)
