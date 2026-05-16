package com.example.fuletracker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fuletracker.data.FuelDatabase
import com.example.fuletracker.data.FuelEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences("fuel_prefs", 0)
        val vehicleId = prefs.getInt("selected_vehicle_id", -1)

        when (action) {
            "ACTION_ADD_FUEL" -> {
                val amount = intent.getDoubleExtra("AMOUNT", 0.0)
                if (vehicleId != -1 && amount > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = FuelDatabase.getDatabase(context)
                            val dao = db.fuelDao()
                            val entries = dao.getEntriesForVehicleOnce(vehicleId)
                            val lastEntry = entries.maxByOrNull { it.odometer }
                            
                            val price = prefs.getFloat("price_Petrol", 105.51f).toDouble()
                            
                            val newEntry = FuelEntry(
                                vehicleId = vehicleId,
                                odometer = lastEntry?.odometer ?: 0.0,
                                fuelAmount = amount,
                                pricePerLiter = price,
                                fullTank = false,
                                fuelType = "Petrol",
                                date = System.currentTimeMillis()
                            )
                            dao.insert(newEntry)
                            FuelWidgetProvider.triggerUpdate(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            "ACTION_NEXT_PAGE" -> {
                updatePageIndex(context, 1)
            }
            "ACTION_PREV_PAGE" -> {
                updatePageIndex(context, -1)
            }
        }
    }

    private fun updatePageIndex(context: Context, delta: Int) {
        val prefs = context.getSharedPreferences("fuel_prefs", 0)
        var index = prefs.getInt("widget_page_index", 0)
        index = (index + delta + 2) % 2
        prefs.edit().putInt("widget_page_index", index).apply()
        FuelWidgetProvider.triggerUpdate(context)
    }
}
