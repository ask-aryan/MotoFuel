package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.platform.LocalContext
import com.example.fuletracker.data.AppBackup
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import com.example.fuletracker.data.Vehicle
import com.example.fuletracker.worker.ReminderScheduler

@Composable
fun SettingsScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val vehicles by viewModel.allVehicles.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var priceText by remember {
        mutableStateOf(
            if (viewModel.petrolPrice > 0) "%.2f".format(viewModel.petrolPrice) else ""
        )
    }

    // Price history — group by month
    val priceHistory = allEntries
        .sortedByDescending { it.date }
        .distinctBy {
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(java.util.Date(it.date))
        }
        .take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // ── Fuel Price Card ────────────────────────────────────────────────
        item {
            val fuelTypes = listOf("Petrol", "Diesel", "CNG", "Electric")
            val priceTexts = remember {
                mutableStateMapOf<String, String>().apply {
                    fuelTypes.forEach { type ->
                        put(type, viewModel.getFuelPrice(type).let {
                            if (it > 0) "%.2f".format(it) else ""
                        })
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Fuel Prices",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Set current prices per litre for each fuel type you use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // One row per fuel type
                    fuelTypes.forEach { fuelType ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = priceTexts[fuelType] ?: "",
                                onValueChange = { priceTexts[fuelType] = it },
                                label = { Text(fuelType) },
                                prefix = { Text("₹") },
                                suffix = { Text("/L") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedLabelColor = when (fuelType) {
                                        "Diesel" -> MaterialTheme.colorScheme.tertiary
                                        "CNG" -> MaterialTheme.colorScheme.secondary
                                        "Electric" -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            )
                            FilledTonalButton(
                                onClick = {
                                    priceTexts[fuelType]?.toDoubleOrNull()?.let {
                                        viewModel.setFuelPrice(fuelType, it)
                                    }
                                },
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Save")
                            }
                        }

                        // Show current saved price
                        val savedPrice = viewModel.getFuelPrice(fuelType)
                        if (savedPrice > 0) {
                            Text(
                                "Current $fuelType price: ₹${"%.2f".format(savedPrice)}/L",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Price history
                    if (allEntries.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            "Price History",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp
                        )

                        val byFuelType = allEntries
                            .sortedByDescending { it.date }
                            .groupBy { it.fuelType }

                        byFuelType.forEach { (fuelType, fuelEntries) ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    fuelType,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            fuelEntries.distinctBy { it.pricePerLiter }.take(10).forEach { entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                                .format(java.util.Date(entry.date)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        val vehicle = vehicles.find { it.id == entry.vehicleId }
                                        if (vehicle != null) {
                                            Text(
                                                vehicle.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    Text(
                                        "₹${"%.2f".format(entry.pricePerLiter)}/L",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // ── Vehicles Card ──────────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text("My Vehicles",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                        }
                        FilledTonalIconButton(onClick = { showAddVehicle = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                        }
                    }

                    if (vehicles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No vehicles added yet",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        vehicles.forEach { vehicle ->
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            modifier = Modifier.padding(8.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Column {
                                        Text(vehicle.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold)
                                        if (vehicle.make.isNotEmpty() || vehicle.model.isNotEmpty()) {
                                            Text(
                                                "${vehicle.make} ${vehicle.model}".trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (vehicle.licensePlate.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    vehicle.licensePlate,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editingVehicle = vehicle }) {
                                        Icon(
                                            Icons.Default.Edit, null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteVehicle(vehicle) }) {
                                        Icon(
                                            Icons.Default.Delete, null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Backup Card ────────────────────────────────────────────────────────
        item {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var showImportDialog by remember { mutableStateOf(false) }
            var pendingBackup by remember { mutableStateOf<AppBackup?>(null) }
            var importError by remember { mutableStateOf("") }

            // File picker launcher
            val filePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    scope.launch {
                        val backup = viewModel.readBackup(it)
                        if (backup != null) {
                            pendingBackup = backup
                            showImportDialog = true
                        } else {
                            importError = "Invalid backup file"
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Backup & Restore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Export your data to a file and share it anywhere — Google Drive, WhatsApp, email. Import it later to restore everything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Export button
                    Button(
                        onClick = {
                            scope.launch {
                                val uri = viewModel.exportBackup()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "MotoFuel Backup")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Save Backup")
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export Backup")
                    }

                    // Import button
                    OutlinedButton(
                        onClick = { filePicker.launch("application/json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Backup")
                    }

                    if (importError.isNotEmpty()) {
                        Text(importError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Import mode dialog
            if (showImportDialog && pendingBackup != null) {
                val backup = pendingBackup!!
                val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Import Backup") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Backup from: ${dateFmt.format(Date(backup.exportDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                            Text("Contains:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold)
                            Text("• ${backup.vehicles.size} vehicle(s)",
                                style = MaterialTheme.typography.bodySmall)
                            Text("• ${backup.fuelEntries.size} fuel entries",
                                style = MaterialTheme.typography.bodySmall)
                            Text("• ${backup.trips.size} trips",
                                style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Text("How do you want to import?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            scope.launch {
                                viewModel.importBackup(backup, mergeMode = false)
                                showImportDialog = false
                                pendingBackup = null
                            }
                        },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Replace All") }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Cancel")
                            }
                            FilledTonalButton(onClick = {
                                scope.launch {
                                    viewModel.importBackup(backup, mergeMode = true)
                                    showImportDialog = false
                                    pendingBackup = null
                                }
                            }) { Text("Merge") }
                        }
                    }
                )
            }
        }
        item {
            val context = LocalContext.current
            var notificationsEnabled by remember {
                mutableStateOf(
                    viewModel.areNotificationsEnabled()
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Reminders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Reminders",
                                style = MaterialTheme.typography.bodyLarge)
                            Text("Weekly fill-up, inactivity & price alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                viewModel.setNotificationsEnabled(enabled)
                                if (enabled) {
                                    ReminderScheduler.scheduleWeeklyReminder(context)
                                    ReminderScheduler.scheduleInactivityReminder(context)
                                    ReminderScheduler.schedulePriceReminder(context)
                                } else {
                                    ReminderScheduler.cancelAll(context)
                                }
                            }
                        )
                    }

                    if (notificationsEnabled) {
                        HorizontalDivider()
                        Text("Active reminders:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text("• Weekly fill-up reminder",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• 10-day inactivity reminder",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• Monthly fuel price check",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showAddVehicle) {
        AddVehicleDialog(
            onDismiss = { showAddVehicle = false },
            onConfirm = { name: String, make: String, model: String, plate: String, fuelType: String ->
                viewModel.addVehicle(name, make, model, plate, fuelType)
                showAddVehicle = false
            }
        )
    }

    editingVehicle?.let { vehicle ->
        EditVehicleDialog(
            vehicle = vehicle,
            onDismiss = { editingVehicle = null },
            onConfirm = { name: String, make: String, model: String, plate: String, fuelType: String ->
                viewModel.editVehicle(vehicle, name, make, model, plate, fuelType)
                editingVehicle = null
            }
        )
    }
}