package com.example.fueltracker.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fueltracker.R
import com.example.fueltracker.data.BackupManager
import com.example.fueltracker.data.FuelDatabase

class BackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val folderUriString = inputData.getString(KEY_FOLDER_URI) ?: return Result.failure()
        val treeUri = Uri.parse(folderUriString)

        val prefs = context.getSharedPreferences("fuel_prefs", Context.MODE_PRIVATE)
        val petrolPrice = prefs
            .getFloat("price_${context.getString(R.string.fuel_type_petrol)}", 0f)
            .toDouble()

        val backupManager = BackupManager(context, FuelDatabase.getDatabase(context))
        val success = backupManager.exportBackupToFolder(treeUri, petrolPrice)
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_FOLDER_URI = "folder_uri"
    }
}
