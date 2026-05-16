package com.example.fuletracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    foreignKeys = [ForeignKey(
        entity = Vehicle::class,
        parentColumns = ["id"],
        childColumns = ["vehicleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("vehicleId")]
)
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val vehicleId: Int,
    val name: String,
    val type: String = "NAMED",        // "QUICK_A", "QUICK_B", "NAMED"
    val startOdometer: Double,
    val endOdometer: Double? = null,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true
)