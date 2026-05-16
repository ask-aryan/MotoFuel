package com.example.fuletracker.ux

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuletracker.R
import com.example.fuletracker.data.*
import com.example.fuletracker.widget.FuelWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fuel_prefs", 0)
    private val db = FuelDatabase.getDatabase(application)
    private val repository: FuelRepository = FuelRepository(
        db.fuelDao(),
        db.serviceDao()
    )

    private val _showAddEntryEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showAddEntryEvent = _showAddEntryEvent.asSharedFlow()

    fun triggerAddEntry() {
        _showAddEntryEvent.tryEmit(Unit)
    }

    val allVehicles: StateFlow<List<Vehicle>> = repository.allVehicles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedVehicleId = MutableStateFlow<Int?>(
        prefs.getInt("selected_vehicle_id", -1).takeIf { it != -1 }
    )
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
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val currentServiceEntries: StateFlow<List<ServiceEntry>> = _selectedVehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getServiceEntriesForVehicle(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addServiceEntry(title: String, category: String, odometer: Double, cost: Double, notes: String) {
        val vehicleId = _selectedVehicleId.value ?: return
        viewModelScope.launch {
            repository.insertServiceEntry(
                ServiceEntry(
                    vehicleId = vehicleId,
                    title = title,
                    category = category,
                    odometer = odometer,
                    cost = cost,
                    notes = notes
                )
            )
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteServiceEntry(entry: ServiceEntry) {
        viewModelScope.launch { 
            repository.deleteServiceEntry(entry)
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun getFuelPrice(fuelType: String): Double {
        return prefs.getFloat("price_$fuelType", 0f).toDouble()
    }

    fun setFuelPrice(fuelType: String, price: Double) {
        prefs.edit().putFloat("price_$fuelType", price.toFloat()).apply()
        if (getOnboardingStep() == 0) setOnboardingStep(1)
        FuelWidgetProvider.triggerUpdate(getApplication())
    }

    var petrolPrice: Double
        get() = getFuelPrice(getApplication<Application>().getString(R.string.fuel_type_petrol))
        set(value) { setFuelPrice(getApplication<Application>().getString(R.string.fuel_type_petrol), value) }

    init {
        viewModelScope.launch {
            allVehicles.collectLatest { list ->
                val selectedId = _selectedVehicleId.value
                if (list.isNotEmpty() && (selectedId == null || list.none { it.id == selectedId })) {
                    selectVehicle(list.first().id)
                }
            }
        }
    }

    fun selectVehicle(vehicleId: Int) {
        _selectedVehicleId.value = vehicleId
        prefs.edit().putInt("selected_vehicle_id", vehicleId).apply()
        FuelWidgetProvider.triggerUpdate(getApplication())
    }

    fun addVehicle(name: String, make: String, model: String, licensePlate: String, fuelType: String = getApplication<Application>().getString(R.string.fuel_type_petrol)) {
        viewModelScope.launch {
            val id = repository.insertVehicle(
                Vehicle(name = name, make = make, model = model, licensePlate = licensePlate, fuelType = fuelType)
            )
            if (_selectedVehicleId.value == null) {
                selectVehicle(id.toInt())
            }
            if (getOnboardingStep() == 1) setOnboardingStep(2)
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun editVehicle(vehicle: Vehicle, name: String, make: String, model: String, licensePlate: String, fuelType: String) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle.copy(name = name, make = make, model = model, licensePlate = licensePlate, fuelType = fuelType))
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateVehicleImage(vehicle: Vehicle, imageUri: String) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle.copy(imageUrl = imageUri))
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            if (_selectedVehicleId.value == vehicle.id) {
                val nextVehicle = allVehicles.value.firstOrNull { it.id != vehicle.id }?.id
                if (nextVehicle != null) selectVehicle(nextVehicle)
                else {
                    _selectedVehicleId.value = null
                    prefs.edit().remove("selected_vehicle_id").apply()
                }
            }
            FuelWidgetProvider.triggerUpdate(getApplication())
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
                    pricePerLiter = getFuelPrice(fuelType),
                    fullTank = fullTank,
                    fuelType = fuelType
                )
            )
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deleteEntry(entry: FuelEntry) {
        viewModelScope.launch { 
            repository.delete(entry)
            FuelWidgetProvider.triggerUpdate(getApplication())
        }
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
            lastOdometer = if (sorted.isNotEmpty()) sorted.last().odometer else 0.0,
            costPerKm = if (totalDistance > 0) totalCost / totalDistance else 0.0,
            entryCount = entries.size
        )
    }

    fun getOnboardingStep(): Int = prefs.getInt("onboarding_step", 0)
    fun setOnboardingStep(step: Int) { prefs.edit().putInt("onboarding_step", step).apply() }
    fun isOnboardingComplete(): Boolean = prefs.getInt("onboarding_step", 0) >= 3

    data class FuelInsight(val emoji: String, val message: String, val type: InsightType = InsightType.INFO)
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
        val thisMonthEff = computeEfficiencyForList(thisMonth)
        val lastMonthEff = computeEfficiencyForList(lastMonth)
        val context = getApplication<Application>()

        if (thisMonthEff > 0 && lastMonthEff > 0) {
            val change = ((thisMonthEff - lastMonthEff) / lastMonthEff) * 100
            when {
                change >= 5 -> insights.add(FuelInsight("🚀", context.getString(R.string.insight_mileage_improved, "%.1f".format(change)), InsightType.POSITIVE))
                change <= -5 -> insights.add(FuelInsight("⚠️", context.getString(R.string.insight_mileage_dropped, "%.1f".format(-change)), InsightType.WARNING))
                else -> insights.add(FuelInsight("✅", context.getString(R.string.insight_mileage_consistent), InsightType.INFO))
            }
        }
        val fullTankSorted = entries.filter { it.fullTank }.sortedBy { it.odometer }
        val segmentEfficiencies = mutableListOf<Pair<FuelEntry, Double>>()
        for (i in 1 until fullTankSorted.size) {
            val dist = fullTankSorted[i].odometer - fullTankSorted[i - 1].odometer
            val fuel = fullTankSorted[i].fuelAmount
            if (dist > 0 && fuel > 0) segmentEfficiencies.add(Pair(fullTankSorted[i], dist / fuel))
        }
        val bestSegment = segmentEfficiencies.maxByOrNull { it.second }
        val lastSegment = segmentEfficiencies.lastOrNull()
        if (bestSegment != null && lastSegment != null) {
            if (lastSegment == bestSegment) {
                insights.add(FuelInsight("🏆", context.getString(R.string.insight_last_fill_up_best, "%.1f".format(bestSegment.second)), InsightType.POSITIVE))
            } else {
                insights.add(FuelInsight("🏆", context.getString(R.string.insight_best_fill_up, "%.1f".format(bestSegment.second)), InsightType.INFO))
            }
        }
        if (thisMonth.isNotEmpty()) {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val spend = entries
                .filter { it.date >= monthStart }
                .sumOf { it.fuelAmount * it.pricePerLiter }
            insights.add(FuelInsight("💰", context.getString(R.string.insight_spent_this_month, "%.0f".format(spend)), InsightType.INFO))
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
    suspend fun exportBackup(): android.net.Uri = backupManager.exportBackup(petrolPrice)
    suspend fun readBackup(uri: android.net.Uri): AppBackup? = backupManager.readBackupFromUri(uri)
    suspend fun importBackup(backup: AppBackup, mergeMode: Boolean) {
        backupManager.importBackup(backup, mergeMode) { price -> petrolPrice = price }
        FuelWidgetProvider.triggerUpdate(getApplication())
    }

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean("notifications_enabled", true)
    fun setNotificationsEnabled(enabled: Boolean) { prefs.edit().putBoolean("notifications_enabled", enabled).apply() }
}
