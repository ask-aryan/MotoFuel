package com.example.fuletracker.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleWeeklyReminder(context: Context) {
        val data = workDataOf("type" to "weekly")

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(7, TimeUnit.DAYS)
            .addTag("weekly_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleInactivityReminder(context: Context) {
        val data = workDataOf("type" to "inactivity")

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(10, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(10, TimeUnit.DAYS)
            .addTag("inactivity_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "inactivity_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun schedulePriceReminder(context: Context) {
        val data = workDataOf("type" to "price")

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(30, TimeUnit.DAYS)
            .addTag("price_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "price_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    fun cancelByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
}