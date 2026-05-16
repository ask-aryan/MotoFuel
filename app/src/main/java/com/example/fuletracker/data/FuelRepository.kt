package com.example.fuletracker.data

import kotlinx.coroutines.flow.Flow

class FuelRepository(
    private val dao: FuelDao,
    private val serviceDao: ServiceDao
) {
    // Fuel Entries
    fun getEntriesForVehicle(vehicleId: Int): Flow<List<FuelEntry>> = dao.getEntriesForVehicle(vehicleId)
    suspend fun getEntriesForVehicleOnce(vehicleId: Int): List<FuelEntry> = dao.getEntriesForVehicleOnce(vehicleId)
    val allEntries: Flow<List<FuelEntry>> = dao.getAllEntries()
    suspend fun insert(entry: FuelEntry) = dao.insert(entry)
    suspend fun delete(entry: FuelEntry) = dao.delete(entry)

    // Vehicles
    val allVehicles: Flow<List<Vehicle>> = dao.getAllVehicles()
    suspend fun insertVehicle(vehicle: Vehicle): Long = dao.insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = dao.updateVehicle(vehicle)
    suspend fun deleteVehicle(vehicle: Vehicle) = dao.deleteVehicle(vehicle)
    suspend fun getVehicleById(id: Int): Vehicle? = dao.getVehicleById(id)

    // Service Entries
    fun getServiceEntriesForVehicle(vehicleId: Int): Flow<List<ServiceEntry>> =
        serviceDao.getServiceEntriesForVehicle(vehicleId)
    suspend fun insertServiceEntry(entry: ServiceEntry) = serviceDao.insert(entry)
    suspend fun updateServiceEntry(entry: ServiceEntry) = serviceDao.update(entry)
    suspend fun deleteServiceEntry(entry: ServiceEntry) = serviceDao.delete(entry)
    suspend fun getLastServiceEntry(vehicleId: Int): ServiceEntry? = serviceDao.getLastServiceEntry(vehicleId)
}
