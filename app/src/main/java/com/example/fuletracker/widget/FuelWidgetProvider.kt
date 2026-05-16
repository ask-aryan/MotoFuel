package com.example.fuletracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import com.example.fuletracker.MainActivity
import com.example.fuletracker.R
import com.example.fuletracker.data.FuelDatabase
import com.example.fuletracker.data.FuelEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

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
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FuelWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { widgetId ->
                updateWidget(context, appWidgetManager, widgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.fuel_widget)
            val prefs = context.getSharedPreferences("fuel_prefs", 0)
            val selectedVehicleId = prefs.getInt("selected_vehicle_id", -1)
            val pageIndex = prefs.getInt("widget_page_index", 0)

            views.setDisplayedChild(R.id.widget_flipper, pageIndex)
            updateDots(views, pageIndex)

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
                        val sorted = entries.sortedBy { it.odometer }

                        // PAGE 1 Stats
                        val fullTankEntries = sorted.filter { it.fullTank }
                        val segments = mutableListOf<Double>()
                        for (i in 1 until fullTankEntries.size) {
                            val dist = fullTankEntries[i].odometer - fullTankEntries[i - 1].odometer
                            val fuel = fullTankEntries[i].fuelAmount
                            if (dist > 0 && fuel > 0) segments.add(dist / fuel)
                        }
                        val avgEff = if (segments.isEmpty()) 0.0 else segments.average()
                        val range = avgEff * vehicle.tankCapacity
                        val lastOdo = sorted.lastOrNull()?.odometer ?: 0.0
                        val totalDistance = if (sorted.size > 1) sorted.last().odometer - sorted.first().odometer else 0.0
                        val totalCost = entries.sumOf { it.fuelAmount * it.pricePerLiter }
                        val costPerKm = if (totalDistance > 0) totalCost / totalDistance else 0.0
                        val lastFillDate = sorted.lastOrNull()?.date

                        views.setTextViewText(R.id.widget_vehicle_name, vehicle.name)
                        views.setTextViewText(R.id.widget_efficiency, if (avgEff > 0) "%.1f".format(avgEff) else "—")
                        views.setTextViewText(R.id.widget_range, if (range > 0) "~${range.toInt()}" else "—")
                        views.setTextViewText(R.id.widget_odometer, "%,.0f km".format(lastOdo))
                        views.setTextViewText(R.id.widget_cost_per_km, if (costPerKm > 0) "₹%.2f".format(costPerKm) else "—")
                        views.setTextViewText(R.id.widget_last_fill, lastFillDate?.let { 
                            DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS) 
                        } ?: "—")

                        // PAGE 2: Monthly Stats
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        val startOfMonth = calendar.timeInMillis
                        
                        val thisMonthEntries = entries.filter { it.date >= startOfMonth }
                        val monthSpent = thisMonthEntries.sumOf { it.fuelAmount * it.pricePerLiter }
                        val monthFuel = thisMonthEntries.sumOf { it.fuelAmount }
                        val monthDist = if (thisMonthEntries.size >= 2) {
                            thisMonthEntries.maxBy { it.odometer }.odometer - thisMonthEntries.minBy { it.odometer }.odometer
                        } else 0.0

                        views.setTextViewText(R.id.widget_month_spent, "₹%.0f".format(monthSpent))
                        views.setTextViewText(R.id.widget_month_dist, "%.0f km".format(monthDist))
                        views.setTextViewText(R.id.widget_month_fuel, "%.1f L".format(monthFuel))

                    } else {
                        views.setTextViewText(R.id.widget_vehicle_name, "No vehicle")
                    }

                    //Deep links
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("ACTION_QUICK_ADD", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    views.setOnClickPendingIntent(R.id.widget_add_button, PendingIntent.getActivity(context, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    views.setOnClickPendingIntent(R.id.widget_add_full, PendingIntent.getActivity(context, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    
                    views.setOnClickPendingIntent(R.id.widget_add_2l, getActionIntent(context, "ACTION_ADD_FUEL", 2.0))
                    views.setOnClickPendingIntent(R.id.widget_add_5l, getActionIntent(context, "ACTION_ADD_FUEL", 5.0))
                    views.setOnClickPendingIntent(R.id.widget_next, getActionIntent(context, "ACTION_NEXT_PAGE"))
                    views.setOnClickPendingIntent(R.id.widget_prev, getActionIntent(context, "ACTION_PREV_PAGE"))

                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        private fun updateDots(views: RemoteViews, index: Int) {
            // Using setInt with "setColorFilter" is safer than "setBackgroundTint"
            // Dot 1
            views.setInt(R.id.dot1, "setColorFilter", if (index == 0) 0xFF21005D.toInt() else 0xFFCAC4D0.toInt())
            // Dot 2
            views.setInt(R.id.dot2, "setColorFilter", if (index == 1) 0xFF21005D.toInt() else 0xFFCAC4D0.toInt())
        }

        private fun getActionIntent(context: Context, action: String, amount: Double? = null): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                this.action = action
                amount?.let { putExtra("AMOUNT", it) }
            }
            return PendingIntent.getBroadcast(context, (action + amount).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
