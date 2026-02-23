package com.example.fuletracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FuelEntry::class, Vehicle::class, Trip::class], version = 4)
abstract class FuelDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add fullTank column
                database.execSQL(
                    "ALTER TABLE fuel_entries ADD COLUMN fullTank INTEGER NOT NULL DEFAULT 1"
                )
                // Add vehicleId column
                database.execSQL(
                    "ALTER TABLE fuel_entries ADD COLUMN vehicleId INTEGER NOT NULL DEFAULT 1"
                )
                // Create the index Room expects
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fuel_entries_vehicleId ON fuel_entries(vehicleId)"
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create vehicles table
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `make` TEXT NOT NULL, `model` TEXT NOT NULL, `year` INTEGER, `licensePlate` TEXT NOT NULL, `imageUrl` TEXT)"
                )
                
                // Add default vehicle to satisfy foreign key constraint if existing data exists
                database.execSQL("INSERT INTO vehicles (name, make, model, licensePlate) VALUES ('My Vehicle', '', '', '')")
                
                // Add vehicleId column to fuel_entries
                database.execSQL(
                    "ALTER TABLE fuel_entries ADD COLUMN vehicleId INTEGER NOT NULL DEFAULT 1"
                )

                database.execSQL(
                    "ALTER TABLE fuel_entries ADD COLUMN fuelType TEXT NOT NULL DEFAULT 'Petrol'"
                )
                // Create index on vehicleId
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_entries_vehicleId` ON `fuel_entries` (`vehicleId`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                vehicleId INTEGER NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL DEFAULT 'NAMED',
                startOdometer REAL NOT NULL,
                endOdometer REAL,
                startDate INTEGER NOT NULL,
                endDate INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                isActive INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
            )
        """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trips_vehicleId ON trips(vehicleId)"
                )
            }
        }
        fun getDatabase(context: Context): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
