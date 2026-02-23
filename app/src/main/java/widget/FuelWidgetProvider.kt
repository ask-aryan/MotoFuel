package com.example.fuletracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.fuletracker.MainActivity
import com.example.fuletracker.R
import com.example.fuletracker.data.FuelDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FuelWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.fuel_widget)

            // Load data from DB
            CoroutineScope(Dispatchers.IO).launch {
                val dao = FuelDatabase.getDatabase(context).fuelDao()
                val entries = dao.getAllEntries().first()
                val sorted = entries.sortedBy { it.odometer }

                // Calculate efficiency
                val fullTankEntries = sorted.filter { it.fullTank }
                val segments = mutableListOf<Double>()
                for (i in 1 until fullTankEntries.size) {
                    val dist = fullTankEntries[i].odometer - fullTankEntries[i - 1].odometer
                    val fuel = fullTankEntries[i].fuelAmount
                    if (dist > 0 && fuel > 0) segments.add(dist / fuel)
                }
                val avgEff = if (segments.isEmpty()) null else segments.average()
                val lastOdo = sorted.lastOrNull()?.odometer

                // Update views
                views.setTextViewText(
                    R.id.widget_efficiency,
                    avgEff?.let { "%.1f".format(it) } ?: "—"
                )
                views.setTextViewText(
                    R.id.widget_odometer,
                    lastOdo?.let { "%,.0f km".format(it) } ?: "—"
                )

                // Button click → open app on Add tab
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("openTab", 2)  // 2 = Add Entry tab
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_add_button, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}