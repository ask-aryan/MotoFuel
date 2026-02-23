package com.example.fuletracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    // Fuel Entries
    @Insert
    suspend fun insert(entry: FuelEntry)

    @Delete
    suspend fun delete(entry: FuelEntry)

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<FuelEntry>>

    // Vehicles
    @Insert
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?


    @Query("SELECT * FROM fuel_entries")
    suspend fun getAllEntriesOnce(): List<FuelEntry>

    @Query("DELETE FROM fuel_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM vehicles")
    suspend fun deleteAllVehicles()
}
