package com.example.fuletracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY startDate DESC")
    fun getTripsForVehicle(vehicleId: Int): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId AND isActive = 1 LIMIT 1")
    fun getActiveTrip(vehicleId: Int): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId AND type = :type AND isActive = 1 LIMIT 1")
    fun getActiveTripByType(vehicleId: Int, type: String): Flow<Trip?>

    @Query("SELECT * FROM trips")
    suspend fun getAllTripsOnce(): List<Trip>

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()
}