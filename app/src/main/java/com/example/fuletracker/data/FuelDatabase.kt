package com.example.fuletracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FuelEntry::class, Vehicle::class, ServiceEntry::class], version = 15, exportSchema = false)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun serviceDao(): ServiceDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        fun getDatabase(context: Context): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
