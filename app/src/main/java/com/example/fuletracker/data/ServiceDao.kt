package com.example.fuletracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Insert
    suspend fun insert(entry: ServiceEntry)

    @Update
    suspend fun update(entry: ServiceEntry)

    @Delete
    suspend fun delete(entry: ServiceEntry)

    @Query("SELECT * FROM service_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getServiceEntriesForVehicle(vehicleId: Int): Flow<List<ServiceEntry>>

    @Query("SELECT * FROM service_entries WHERE vehicleId = :vehicleId ORDER BY odometer DESC LIMIT 1")
    suspend fun getLastServiceEntry(vehicleId: Int): ServiceEntry?

    @Query("SELECT * FROM service_entries")
    suspend fun getAllServiceEntriesOnce(): List<ServiceEntry>

    @Query("DELETE FROM service_entries")
    suspend fun deleteAllServiceEntries()
}
