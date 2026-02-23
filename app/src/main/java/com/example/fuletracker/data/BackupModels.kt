package com.example.fuletracker.data

import com.google.gson.annotations.SerializedName

data class AppBackup(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportDate") val exportDate: Long = System.currentTimeMillis(),
    @SerializedName("petrolPrice") val petrolPrice: Double,
    @SerializedName("vehicles") val vehicles: List<VehicleBackup>,
    @SerializedName("fuelEntries") val fuelEntries: List<FuelEntryBackup>,
    @SerializedName("trips") val trips: List<TripBackup>
)

data class VehicleBackup(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("licensePlate") val licensePlate: String,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

data class FuelEntryBackup(
    @SerializedName("id") val id: Int,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("odometer") val odometer: Double,
    @SerializedName("fuelAmount") val fuelAmount: Double,
    @SerializedName("pricePerLiter") val pricePerLiter: Double,
    @SerializedName("fullTank") val fullTank: Boolean,
    @SerializedName("fuelType") val fuelType: String,
    @SerializedName("date") val date: Long
)

data class TripBackup(
    @SerializedName("id") val id: Int,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("startOdometer") val startOdometer: Double,
    @SerializedName("endOdometer") val endOdometer: Double?,
    @SerializedName("startDate") val startDate: Long,
    @SerializedName("endDate") val endDate: Long?,
    @SerializedName("notes") val notes: String,
    @SerializedName("isActive") val isActive: Boolean
)