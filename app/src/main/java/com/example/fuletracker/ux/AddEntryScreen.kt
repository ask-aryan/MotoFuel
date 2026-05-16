package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource

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

    val petrolLabel = stringResource(R.string.fuel_type_petrol)
    val powerPetrolLabel = stringResource(R.string.fuel_type_power_petrol)

    // Fuel type selection — Petrol or Power Petrol
    var selectedFuelType by remember { mutableStateOf(petrolLabel) }
    val petrolPrice = viewModel.getFuelPrice(petrolLabel)
    val powerPetrolPrice = viewModel.getFuelPrice(powerPetrolLabel)
    val price = viewModel.getFuelPrice(selectedFuelType)

    var odometer by remember { mutableStateOf("") }
    var fuelAmount by remember { mutableStateOf("") }
    var fullTank by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.add_fillup_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }

        if (lastOdometer != null) {
            Text(
                stringResource(R.string.last_recorded_odometer, "%.0f".format(lastOdometer)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // ── Fuel Type Selector ─────────────────────────────────────────────
        Text(stringResource(R.string.fuel_type_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Petrol option
            val petrolSelected = selectedFuelType == petrolLabel
            Card(
                onClick = { selectedFuelType = petrolLabel },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (petrolSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (petrolSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = if (petrolSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        petrolLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (petrolSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (petrolPrice > 0) {
                        Text(
                            "₹${"%.2f".format(petrolPrice)}/L",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (petrolSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(stringResource(R.string.price_not_set), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Power Petrol option
            val powerSelected = selectedFuelType == powerPetrolLabel
            Card(
                onClick = { selectedFuelType = powerPetrolLabel },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (powerSelected) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (powerSelected) CardDefaults.outlinedCardBorder() else null
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (powerSelected) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        powerPetrolLabel,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (powerSelected) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (powerPetrolPrice > 0) {
                        Text(
                            "₹${"%.2f".format(powerPetrolPrice)}/L",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (powerSelected) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(stringResource(R.string.price_not_set), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            stringResource(R.string.premium_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (price <= 0) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    stringResource(R.string.no_price_set_warning, selectedFuelType),
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Odometer
        OutlinedTextField(
            value = odometer,
            onValueChange = { odometer = it },
            label = { Text(stringResource(R.string.current_odometer_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = distanceSinceLast?.let { { Text(stringResource(R.string.distance_since_last_fill, "%.1f".format(it))) } }
        )

        // Fuel Amount
        OutlinedTextField(
            value = fuelAmount,
            onValueChange = { fuelAmount = it },
            label = { Text(stringResource(R.string.fuel_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Live preview card
        if (estimatedCost != null || estimatedEfficiency != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.preview_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (estimatedCost != null) {
                        Text(stringResource(R.string.estimated_cost_preview, "%.2f".format(estimatedCost)), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (estimatedEfficiency != null) {
                        Text(stringResource(R.string.estimated_efficiency_preview, "%.1f".format(estimatedEfficiency)), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (distanceSinceLast != null) {
                        Text(stringResource(R.string.distance_covered_preview, "%.1f".format(distanceSinceLast)), style = MaterialTheme.typography.bodyMedium)
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
                    Text(stringResource(R.string.filled_to_full_tank_question), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (fullTank) stringResource(R.string.used_in_efficiency_desc) else stringResource(R.string.excluded_from_efficiency_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fullTank) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Switch(checked = fullTank, onCheckedChange = { fullTank = it })
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

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
                        viewModel.addEntry(odo, fuel, fullTank, selectedFuelType)
                        onEntryAdded()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_entry_button), fontWeight = FontWeight.Bold)
        }
    }
}
