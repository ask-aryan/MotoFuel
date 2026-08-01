package com.example.fueltracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context, private val database: FuelDatabase) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private suspend fun buildBackup(petrolPrice: Double): AppBackup {
        val dao = database.fuelDao()
        val serviceDao = database.serviceDao()

        val vehicles = dao.getAllVehicles().first()
        val entries = dao.getAllEntries().first()
        val serviceEntries = serviceDao.getAllServiceEntriesOnce()

        return AppBackup(
            petrolPrice = petrolPrice,
            vehicles = vehicles.map {
                VehicleBackup(it.id, it.name, it.make, it.model, it.licensePlate, it.imageUrl)
            },
            fuelEntries = entries.map {
                FuelEntryBackup(it.id, it.vehicleId, it.odometer, it.fuelAmount,
                    it.pricePerLiter, it.fullTank, it.fuelType, it.date)
            },
            serviceEntries = serviceEntries.map {
                ServiceEntryBackup(it.id, it.vehicleId, it.date, it.title, it.category, it.odometer, it.cost, it.notes)
            }
        )
    }

    private fun backupFileName(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "motofuel_backup_$dateStr.json"
    }

    // ── Export ─────────────────────────────────────────────────────────────
    suspend fun exportBackup(petrolPrice: Double): Uri {
        val json = gson.toJson(buildBackup(petrolPrice))
        val file = File(context.cacheDir, backupFileName())
        file.writeText(json)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    /**
     * Writes a backup as a new file inside the user-chosen folder (SAF tree Uri).
     * Returns false if the folder is no longer accessible or the write fails.
     */
    suspend fun exportBackupToFolder(treeUri: Uri, petrolPrice: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            if (!folder.exists() || !folder.canWrite()) return@withContext false
            val json = gson.toJson(buildBackup(petrolPrice))
            val newFile = folder.createFile("application/json", backupFileName()) ?: return@withContext false
            context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(json.toByteArray())
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
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
        val serviceDao = database.serviceDao()

        if (!mergeMode) {
            // Clear all existing data
            dao.deleteAllEntries()
            serviceDao.deleteAllServiceEntries()
            dao.deleteAllVehicles()
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

        // Restore service entries with remapped vehicleIds
        backup.serviceEntries.forEach { s ->
            val newVehicleId = vehicleIdMap[s.vehicleId] ?: return@forEach
            serviceDao.insert(
                ServiceEntry(
                    vehicleId = newVehicleId,
                    date = s.date,
                    title = s.title,
                    category = s.category,
                    odometer = s.odometer,
                    cost = s.cost,
                    notes = s.notes
                )
            )
        }
    }
}
