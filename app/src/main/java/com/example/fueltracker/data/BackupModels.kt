package com.example.fueltracker.data

import com.google.gson.annotations.SerializedName

data class AppBackup(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportDate") val exportDate: Long = System.currentTimeMillis(),
    @SerializedName("petrolPrice") val petrolPrice: Double,
    @SerializedName("vehicles") val vehicles: List<VehicleBackup>,
    @SerializedName("fuelEntries") val fuelEntries: List<FuelEntryBackup>,
    @SerializedName("serviceEntries") val serviceEntries: List<ServiceEntryBackup> = emptyList()
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

data class ServiceEntryBackup(
    @SerializedName("id") val id: Int,
    @SerializedName("vehicleId") val vehicleId: Int,
    @SerializedName("date") val date: Long,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("odometer") val odometer: Double,
    @SerializedName("cost") val cost: Double,
    @SerializedName("notes") val notes: String
)
