package com.example.fuletracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context, private val database: FuelDatabase) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ── Export ─────────────────────────────────────────────────────────────
    suspend fun exportBackup(petrolPrice: Double): Uri {
        val dao = database.fuelDao()
        val tripDao = database.tripDao()

        val vehicles = dao.getAllVehicles().first()
        val entries = dao.getAllEntries().first()
        val trips = tripDao.getAllTripsOnce()

        val backup = AppBackup(
            petrolPrice = petrolPrice,
            vehicles = vehicles.map {
                VehicleBackup(it.id, it.name, it.make, it.model, it.licensePlate, it.imageUrl)
            },
            fuelEntries = entries.map {
                FuelEntryBackup(it.id, it.vehicleId, it.odometer, it.fuelAmount,
                    it.pricePerLiter, it.fullTank, it.fuelType, it.date)
            },
            trips = trips.map {
                TripBackup(it.id, it.vehicleId, it.name, it.type, it.startOdometer,
                    it.endOdometer, it.startDate, it.endDate, it.notes, it.isActive)
            }
        )

        val json = gson.toJson(backup)
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val fileName = "motofuel_backup_$dateStr.json"

        val file = File(context.cacheDir, fileName)
        file.writeText(json)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun shareBackup(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MotoFuel Backup")
            putExtra(Intent.EXTRA_TEXT, "MotoFuel data backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Import ─────────────────────────────────────────────────────────────
    suspend fun readBackupFromUri(uri: Uri): AppBackup? {
        return try {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return null
            gson.fromJson(json, AppBackup::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importBackup(
        backup: AppBackup,
        mergeMode: Boolean,
        onPetrolPrice: (Double) -> Unit
    ) {
        val dao = database.fuelDao()
        val tripDao = database.tripDao()

        if (!mergeMode) {
            // Clear all existing data
            dao.deleteAllEntries()
            dao.deleteAllVehicles()
            tripDao.deleteAllTrips()
        }

        // Restore petrol price
        onPetrolPrice(backup.petrolPrice)

        // Restore vehicles
        val vehicleIdMap = mutableMapOf<Int, Int>() // old id → new id
        backup.vehicles.forEach { v ->
            val newId = dao.insertVehicle(
                Vehicle(
                    name = v.name,
                    make = v.make,
                    model = v.model,
                    licensePlate = v.licensePlate,
                    imageUrl = v.imageUrl
                )
            )
            vehicleIdMap[v.id] = newId.toInt()
        }

        // Restore fuel entries with remapped vehicleIds
        backup.fuelEntries.forEach { e ->
            val newVehicleId = vehicleIdMap[e.vehicleId] ?: return@forEach
            dao.insert(
                FuelEntry(
                    vehicleId = newVehicleId,
                    odometer = e.odometer,
                    fuelAmount = e.fuelAmount,
                    pricePerLiter = e.pricePerLiter,
                    fullTank = e.fullTank,
                    fuelType = e.fuelType,
                    date = e.date
                )
            )
        }

        // Restore trips with remapped vehicleIds
        backup.trips.forEach { t ->
            val newVehicleId = vehicleIdMap[t.vehicleId] ?: return@forEach
            tripDao.insert(
                Trip(
                    vehicleId = newVehicleId,
                    name = t.name,
                    type = t.type,
                    startOdometer = t.startOdometer,
                    endOdometer = t.endOdometer,
                    startDate = t.startDate,
                    endDate = t.endDate,
                    notes = t.notes,
                    isActive = t.isActive
                )
            )
        }
    }
}