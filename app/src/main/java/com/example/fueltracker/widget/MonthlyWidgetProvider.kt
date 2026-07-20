package com.example.fueltracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.fueltracker.R
import com.example.fueltracker.data.FuelDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MonthlyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, MonthlyWidgetProvider::class.java)
            )
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.monthly_widget)
            val prefs = context.getSharedPreferences("fuel_prefs", 0)
            val selectedVehicleId = prefs.getInt("selected_vehicle_id", -1)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FuelDatabase.getDatabase(context)
                    val dao = db.fuelDao()

                    val vehicle = if (selectedVehicleId != -1) {
                        dao.getVehicleById(selectedVehicleId)
                    } else {
                        dao.getAllVehicles().first().firstOrNull()
                    }

                    if (vehicle != null) {
                        val entries = dao.getEntriesForVehicleOnce(vehicle.id)

                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val startOfMonth = calendar.timeInMillis
                        val monthEntries = entries.filter { it.date >= startOfMonth }

                        val spent = monthEntries.sumOf { it.fuelAmount * it.pricePerLiter }
                        val fuel = monthEntries.sumOf { it.fuelAmount }
                        val dist = if (monthEntries.size >= 2) {
                            monthEntries.maxBy { it.odometer }.odometer -
                                monthEntries.minBy { it.odometer }.odometer
                        } else 0.0

                        views.setTextViewText(R.id.widget_month_spent, "₹%.0f".format(spent))
                        views.setTextViewText(R.id.widget_month_dist, "%.0f km".format(dist))
                        views.setTextViewText(R.id.widget_month_fuel, "%.1f L".format(fuel))
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
