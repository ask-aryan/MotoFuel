package com.example.fuletracker.ux

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuletracker.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FuelStats(
    val avgEfficiency: Double,
    val bestEfficiency: Double,
    val worstEfficiency: Double,
    val totalDistance: Double,
    val totalFuel: Double,
    val totalCost: Double,
    val lastOdometer: Double,
    val costPerKm: Double,
    val entryCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fuel_prefs", 0)
    private val repository: FuelRepository = FuelRepository(
        FuelDatabase.getDatabase(application).fuelDao(),
        FuelDatabase.getDatabase(application).tripDao()
    )
    
    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedVehicleId = MutableStateFlow<Int?>(null)
    val selectedVehicleId = _selectedVehicleId.asStateFlow()

    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else allVehicles.map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentEntries: StateFlow<List<FuelEntry>> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<FuelEntry>> = repository.allEntries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // ── Trip flows ────────────────────────────────────────────────────────────
    val currentTrips: StateFlow<List<Trip>> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getTripsForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrip: StateFlow<Trip?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getActiveTrip(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTripA: StateFlow<Trip?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getActiveTripByType(id, "QUICK_A")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeTripB: StateFlow<Trip?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getActiveTripByType(id, "QUICK_B")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var petrolPrice: Double
        get() = prefs.getFloat("petrol_price", 0f).toDouble()
        set(value) {
            prefs.edit().putFloat("petrol_price", value.toFloat()).apply()
            // advance onboarding if on step 0
            if (getOnboardingStep() == 0) setOnboardingStep(1)
        }

    init {
        viewModelScope.launch {
            allVehicles.collectLatest { list ->
                if (list.isNotEmpty() && _selectedVehicleId.value == null) {
                    _selectedVehicleId.value = list.first().id
                }
            }
        }
    }

    fun selectVehicle(vehicleId: Int) {
        _selectedVehicleId.value = vehicleId
    }

    fun addVehicle(name: String, make: String, model: String, licensePlate: String) {
        viewModelScope.launch {
            val id = repository.insertVehicle(
                Vehicle(name = name, make = make, model = model, licensePlate = licensePlate)
            )
            if (_selectedVehicleId.value == null) {
                _selectedVehicleId.value = id.toInt()
            }
            // advance onboarding if on step 1
            if (getOnboardingStep() == 1) setOnboardingStep(2)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            if (_selectedVehicleId.value == vehicle.id) {
                _selectedVehicleId.value = allVehicles.value.firstOrNull { it.id != vehicle.id }?.id
            }
        }
    }

    fun addEntry(odometer: Double, fuelAmount: Double, fullTank: Boolean, fuelType: String) {
        val vehicleId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            repository.insert(
                FuelEntry(
                    vehicleId = vehicleId,
                    odometer = odometer,
                    fuelAmount = fuelAmount,
                    pricePerLiter = petrolPrice,
                    fullTank = fullTank,
                    fuelType = fuelType
                )
            )
        }
    }

    fun deleteEntry(entry: FuelEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    fun getLastOdometer(entries: List<FuelEntry>): Double? {
        return entries.maxByOrNull { it.odometer }?.odometer
    }

    fun computeStats(entries: List<FuelEntry>): FuelStats? {
        if (entries.isEmpty()) return null
        val sorted = entries.sortedBy { it.odometer }

        val fullTankEntries = sorted.filter { it.fullTank }
        val segments = mutableListOf<Double>()
        for (i in 1 until fullTankEntries.size) {
            val dist = fullTankEntries[i].odometer - fullTankEntries[i - 1].odometer
            val fuel = fullTankEntries[i].fuelAmount
            if (dist > 0 && fuel > 0) segments.add(dist / fuel)
        }

        val totalDistance = if (sorted.size > 1) sorted.last().odometer - sorted.first().odometer else 0.0
        val totalCost = entries.sumOf { it.fuelAmount * it.pricePerLiter }

        return FuelStats(
            avgEfficiency = if (segments.isEmpty()) 0.0 else segments.average(),
            bestEfficiency = segments.maxOrNull() ?: 0.0,
            worstEfficiency = segments.minOrNull() ?: 0.0,
            totalDistance = totalDistance,
            totalFuel = entries.sumOf { it.fuelAmount },
            totalCost = totalCost,
            lastOdometer = sorted.last().odometer,
            costPerKm = if (totalDistance > 0) totalCost / totalDistance else 0.0,
            entryCount = entries.size
        )
    }

    // ── Trip operations ───────────────────────────────────────────────────────
    fun startNamedTrip(name: String, startOdometer: Double, notes: String = "") {
        val vehicleId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            repository.insertTrip(
                Trip(
                    vehicleId = vehicleId,
                    name = name,
                    type = "NAMED",
                    startOdometer = startOdometer,
                    notes = notes
                )
            )
        }
    }

    fun startQuickTrip(type: String, startOdometer: Double) {
        val vehicleId = _selectedVehicleId.value ?: return
        val label = if (type == "QUICK_A") "Trip A" else "Trip B"
        viewModelScope.launch {
            repository.insertTrip(
                Trip(
                    vehicleId = vehicleId,
                    name = label,
                    type = type,
                    startOdometer = startOdometer
                )
            )
        }
    }

    fun endTrip(trip: Trip, endOdometer: Double) {
        viewModelScope.launch {
            repository.updateTrip(
                trip.copy(
                    endOdometer = endOdometer,
                    endDate = System.currentTimeMillis(),
                    isActive = false
                )
            )
        }
    }

    fun resetQuickTrip(trip: Trip, currentOdometer: Double) {
        viewModelScope.launch {
            // end current
            repository.updateTrip(
                trip.copy(
                    endOdometer = currentOdometer,
                    endDate = System.currentTimeMillis(),
                    isActive = false
                )
            )
            // start fresh
            repository.insertTrip(
                Trip(
                    vehicleId = trip.vehicleId,
                    name = trip.name,
                    type = trip.type,
                    startOdometer = currentOdometer
                )
            )
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { repository.deleteTrip(trip) }
    }
    fun getOnboardingStep(): Int {
        return prefs.getInt("onboarding_step", 0)
        // 0 = not started
        // 1 = price set, need vehicle
        // 2 = vehicle added, show settings tip
        // 3 = completed
    }

    fun setOnboardingStep(step: Int) {
        prefs.edit().putInt("onboarding_step", step).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getInt("onboarding_step", 0) >= 3
    }
    fun computeTripStats(trip: Trip, entries: List<FuelEntry>): TripStats? {
        val end = trip.endOdometer ?: return null
        val distance = end - trip.startOdometer
        if (distance <= 0) return null

        val tripEntries = entries.filter {
            it.odometer >= trip.startOdometer &&
                    it.odometer <= end &&
                    it.fullTank
        }
        val fuelUsed = tripEntries.sumOf { it.fuelAmount }
        val cost = tripEntries.sumOf { it.fuelAmount * it.pricePerLiter }
        val efficiency = if (fuelUsed > 0) distance / fuelUsed else 0.0

        return TripStats(distance, fuelUsed, cost, efficiency)
    }

    data class TripStats(
        val distance: Double,
        val fuelUsed: Double,
        val cost: Double,
        val efficiency: Double
    )

    data class FuelInsight(
        val emoji: String,
        val message: String,
        val type: InsightType = InsightType.INFO
    )

    enum class InsightType { INFO, POSITIVE, WARNING }

    fun generateInsights(entries: List<FuelEntry>): List<FuelInsight> {
        val insights = mutableListOf<FuelInsight>()
        if (entries.size < 2) return insights

        val sorted = entries.sortedBy { it.date }
        val now = System.currentTimeMillis()
        val oneMonthAgo = now - 30L * 24 * 60 * 60 * 1000
        val twoMonthsAgo = now - 60L * 24 * 60 * 60 * 1000

        val thisMonth = sorted.filter { it.date >= oneMonthAgo }
        val lastMonth = sorted.filter { it.date in twoMonthsAgo until oneMonthAgo }

        // 1. Mileage comparison
        val thisMonthEff = computeEfficiencyForList(thisMonth)
        val lastMonthEff = computeEfficiencyForList(lastMonth)
        if (thisMonthEff > 0 && lastMonthEff > 0) {
            val change = ((thisMonthEff - lastMonthEff) / lastMonthEff) * 100
            when {
                change >= 5 -> insights.add(FuelInsight("🚀",
                    "Mileage improved ${"%.1f".format(change)}% compared to last month!",
                    InsightType.POSITIVE))
                change <= -5 -> insights.add(FuelInsight("⚠️",
                    "Mileage dropped ${"%.1f".format(-change)}% compared to last month",
                    InsightType.WARNING))
                else -> insights.add(FuelInsight("✅",
                    "Mileage is consistent with last month",
                    InsightType.INFO))
            }
        }

        // 2. Fuel price change
        if (sorted.size >= 2) {
            val firstPrice = sorted.first().pricePerLiter
            val lastPrice = sorted.last().pricePerLiter
            val priceDiff = lastPrice - firstPrice
            if (priceDiff > 1.0) {
                insights.add(FuelInsight("📈",
                    "Fuel price increased ₹${"%.2f".format(priceDiff)}/L since your first entry",
                    InsightType.WARNING))
            } else if (priceDiff < -1.0) {
                insights.add(FuelInsight("📉",
                    "Fuel price decreased ₹${"%.2f".format(-priceDiff)}/L since your first entry",
                    InsightType.POSITIVE))
            }
        }

        // 3. Best fill-up
        val fullTankSorted = entries.filter { it.fullTank }.sortedBy { it.odometer }
        val segmentEfficiencies = mutableListOf<Pair<FuelEntry, Double>>()
        for (i in 1 until fullTankSorted.size) {
            val dist = fullTankSorted[i].odometer - fullTankSorted[i - 1].odometer
            val fuel = fullTankSorted[i].fuelAmount
            if (dist > 0 && fuel > 0) {
                segmentEfficiencies.add(Pair(fullTankSorted[i], dist / fuel))
            }
        }
        val bestSegment = segmentEfficiencies.maxByOrNull { it.second }
        val lastSegment = segmentEfficiencies.lastOrNull()

        if (bestSegment != null && lastSegment != null) {
            if (lastSegment == bestSegment) {
                insights.add(FuelInsight("🏆",
                    "Your last fill-up was your most efficient ever at ${"%.1f".format(bestSegment.second)} km/L!",
                    InsightType.POSITIVE))
            } else {
                insights.add(FuelInsight("🏆",
                    "Best fill-up was ${"%.1f".format(bestSegment.second)} km/L",
                    InsightType.INFO))
            }
        }

        // 4. Monthly spend
        if (thisMonth.isNotEmpty()) {
            val spend = thisMonth.sumOf { it.fuelAmount * it.pricePerLiter }
            insights.add(FuelInsight("💰",
                "You've spent ₹${"%.0f".format(spend)} on fuel this month",
                InsightType.INFO))
        }

        // 5. Frequent fill-up warning
        if (thisMonth.size >= 6) {
            insights.add(FuelInsight("⛽",
                "You've filled up ${thisMonth.size} times this month — quite frequent!",
                InsightType.WARNING))
        }

        return insights
    }

    private fun computeEfficiencyForList(entries: List<FuelEntry>): Double {
        val full = entries.filter { it.fullTank }.sortedBy { it.odometer }
        val segments = mutableListOf<Double>()
        for (i in 1 until full.size) {
            val dist = full[i].odometer - full[i - 1].odometer
            val fuel = full[i].fuelAmount
            if (dist > 0 && fuel > 0) segments.add(dist / fuel)
        }
        return if (segments.isEmpty()) 0.0 else segments.average()
    }

    private val backupManager = BackupManager(application, FuelDatabase.getDatabase(application))

    suspend fun exportBackup(): android.net.Uri {
        return backupManager.exportBackup(petrolPrice)
    }

    suspend fun readBackup(uri: android.net.Uri): AppBackup? {
        return backupManager.readBackupFromUri(uri)
    }

    suspend fun importBackup(backup: AppBackup, mergeMode: Boolean) {
        backupManager.importBackup(backup, mergeMode) { price ->
            petrolPrice = price
        }
    }
}
