package com.example.fueltracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import com.example.fueltracker.MainActivity
import com.example.fueltracker.R
import com.example.fueltracker.data.FuelDatabase
import com.example.fueltracker.data.computeFuelStats
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
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FuelWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { widgetId ->
                updateWidget(context, appWidgetManager, widgetId)
            }
            // Also refresh monthly widget
            MonthlyWidgetProvider.triggerUpdate(context)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.fuel_widget)
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
                        val stats = computeFuelStats(entries)
                        val avgEff = stats?.avgEfficiency ?: 0.0
                        val range = avgEff * vehicle.tankCapacity
                        val lastOdo = stats?.lastOdometer ?: 0.0
                        val costPerKm = stats?.costPerKm ?: 0.0
                        val lastFillDate = entries.maxByOrNull { it.odometer }?.date

                        views.setTextViewText(R.id.widget_vehicle_name, vehicle.name)
                        views.setTextViewText(R.id.widget_efficiency,
                            if (avgEff > 0) "%.1f".format(avgEff) else "—")
                        views.setTextViewText(R.id.widget_range,
                            if (range > 0) "~${range.toInt()}" else "—")
                        views.setTextViewText(R.id.widget_odometer,
                            "%,.0f km".format(lastOdo))
                        views.setTextViewText(R.id.widget_cost_per_km,
                            if (costPerKm > 0) "₹%.2f".format(costPerKm) else "—")
                        views.setTextViewText(R.id.widget_last_fill,
                            lastFillDate?.let {
                                DateUtils.getRelativeTimeSpanString(
                                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                                ).toString()
                            } ?: "—")
                    } else {
                        views.setTextViewText(R.id.widget_vehicle_name, "No vehicle")
                    }

                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("ACTION_QUICK_ADD", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openAppPi = PendingIntent.getActivity(
                        context, 10, openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_add_button, openAppPi)
                    views.setOnClickPendingIntent(R.id.widget_add_full, openAppPi)
                    views.setOnClickPendingIntent(R.id.widget_add_2l,
                        getActionIntent(context, "ACTION_ADD_FUEL", 2.0))
                    views.setOnClickPendingIntent(R.id.widget_add_5l,
                        getActionIntent(context, "ACTION_ADD_FUEL", 5.0))

                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun getActionIntent(context: Context, action: String, amount: Double? = null): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                this.action = action
                amount?.let { putExtra("AMOUNT", it) }
            }
            return PendingIntent.getBroadcast(
                context, (action + amount).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
