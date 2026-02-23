package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: FuelViewModel,
    onEntryAdded: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.currentEntries.collectAsState()
    val lastOdometer = viewModel.getLastOdometer(entries)

    // Get fuel type from selected vehicle
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val vehicleFuelType = selectedVehicle?.fuelType ?: "Petrol"
    val price = viewModel.getFuelPrice(vehicleFuelType)

    var odometer by remember { mutableStateOf("") }
    var fuelAmount by remember { mutableStateOf("") }
    var fullTank by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Auto calculations
    val odoValue = odometer.toDoubleOrNull()
    val fuelValue = fuelAmount.toDoubleOrNull()
    val distanceSinceLast = if (odoValue != null && lastOdometer != null && odoValue > lastOdometer)
        odoValue - lastOdometer else null
    val estimatedEfficiency = if (distanceSinceLast != null && fuelValue != null && fuelValue > 0)
        distanceSinceLast / fuelValue else null
    val estimatedCost = if (fuelValue != null && price > 0) fuelValue * price else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Fill-up", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }

        // Last odometer hint
        if (lastOdometer != null) {
            Text(
                "Last recorded: ${"%.0f".format(lastOdometer)} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Odometer
        OutlinedTextField(
            value = odometer,
            onValueChange = { odometer = it },
            label = { Text("Current Odometer (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = distanceSinceLast?.let {
                { Text("Distance since last fill: ${"%.1f".format(it)} km") }
            }
        )

        // Fuel Amount
        OutlinedTextField(
            value = fuelAmount,
            onValueChange = { fuelAmount = it },
            label = { Text("Fuel Amount (Litres)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

// Fuel Type — read only from vehicle
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (vehicleFuelType) {
                        "Diesel" -> Icons.Default.LocalGasStation
                        "CNG" -> Icons.Default.Air
                        "Electric" -> Icons.Default.ElectricBolt
                        else -> Icons.Default.LocalGasStation
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Fuel Type: $vehicleFuelType",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // Add after the fuel type chip
        if (price <= 0) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "⚠ No price set for $vehicleFuelType — go to Settings to add it",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Live preview card
        if (estimatedCost != null || estimatedEfficiency != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Preview", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                    if (estimatedCost != null) {
                        Text("💰 Estimated cost: ₹${"%.2f".format(estimatedCost)}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (estimatedEfficiency != null) {
                        Text("⚡ Estimated efficiency: ${"%.1f".format(estimatedEfficiency)} km/L",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (distanceSinceLast != null) {
                        Text("📍 Distance covered: ${"%.1f".format(distanceSinceLast)} km",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Full tank toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Filled to full tank?",
                        style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (fullTank) "Used in efficiency calculation"
                        else "Excluded from efficiency calculation",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fullTank) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = fullTank,
                    onCheckedChange = { fullTank = it }
                )
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        // Save button
        Button(
            onClick = {
                val odo = odometer.toDoubleOrNull()
                val fuel = fuelAmount.toDoubleOrNull()
                when {
                    odo == null || odo <= 0 -> error = "Enter a valid odometer reading"
                    lastOdometer != null && odo <= lastOdometer ->
                        error = "Odometer must be greater than last reading (${lastOdometer.toInt()} km)"
                    fuel == null || fuel <= 0 -> error = "Enter a valid fuel amount"
                    else -> {
                        viewModel.addEntry(odo, fuel, fullTank, vehicleFuelType)
                        onEntryAdded()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Entry", fontWeight = FontWeight.Bold)
        }
    }
}
