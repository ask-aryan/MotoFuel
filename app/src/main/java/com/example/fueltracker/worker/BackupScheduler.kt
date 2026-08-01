package com.example.fueltracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "auto_backup"

    fun scheduleDailyBackup(context: Context, folderUri: String) {
        val data = workDataOf(BackupWorker.KEY_FOLDER_URI to folderUri)

        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(WORK_NAME)
            .build()

        // UPDATE (not KEEP) so a changed folder URI actually takes effect on an already-scheduled job.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
