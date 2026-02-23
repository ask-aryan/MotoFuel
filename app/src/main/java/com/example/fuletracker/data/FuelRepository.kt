package com.example.fuletracker.data

import kotlinx.coroutines.flow.Flow

class FuelRepository(private val dao: FuelDao, private val tripDao: TripDao) {

    // Fuel Entries
    fun getEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> = dao.getEntriesForVehicle(vehicleId)
    val allEntries: Flow<List<FuelEntry>> = dao.getAllEntries()

    suspend fun insert(entry: FuelEntry) {
        dao.insert(entry)
    }

    suspend fun delete(entry: FuelEntry) {
        dao.delete(entry)
    }

    // Vehicles
    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()

    suspend fun insertVehicle(vehicle: Vehicle): Long {
        return dao.insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: Vehicle) {
        dao.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.deleteVehicle(vehicle)
    }

    suspend fun getVehicleById(id: Int): Vehicle? {
        return dao.getVehicleById(id)
    }

    // Trips
    fun getTripsForVehicle(vehicleId: Int) = tripDao.getTripsForVehicle(vehicleId)
    fun getActiveTrip(vehicleId: Int) = tripDao.getActiveTrip(vehicleId)
    fun getActiveTripByType(vehicleId: Int, type: String) = tripDao.getActiveTripByType(vehicleId, type)
    suspend fun insertTrip(trip: Trip) = tripDao.insert(trip)
    suspend fun updateTrip(trip: Trip) = tripDao.update(trip)
    suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip)
}
