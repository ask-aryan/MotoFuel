package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fuletracker.data.FuelEntry
import com.example.fuletracker.data.Vehicle
import com.example.fuletracker.ux.FuelViewModel.FuelInsight
import com.example.fuletracker.ux.FuelViewModel.InsightType
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EntryCard(entry: FuelEntry, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val partialFillLabel = stringResource(R.string.partial_fill)
    val deleteLabel = stringResource(R.string.delete_button)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("%,.0f km".format(entry.odometer),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    "${"%.1f".format(entry.fuelAmount)} L · ${entry.fuelType} · ${dateFormat.format(Date(entry.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (!entry.fullTank) {
                    Text(partialFillLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${"%.0f".format(entry.fuelAmount * entry.pricePerLiter)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
                Text("₹${"%.1f".format(entry.pricePerLiter)}/L",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = deleteLabel,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_confirmation_title),
            message = stringResource(R.string.delete_fuel_entry_message),
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun QuickStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun ChartPlaceholder(message: String) {
    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun QuickPriceDialog(
    fuelType: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(if (currentPrice > 0) "%.2f".format(currentPrice) else "") }
    val title = stringResource(R.string.set_fuel_price_dialog_title, fuelType)
    val desc = stringResource(R.string.set_fuel_price_dialog_desc, fuelType)
    val fieldLabel = stringResource(R.string.fuel_price_field_label, fuelType)
    val saveButtonLabel = stringResource(R.string.save_button)
    val cancelButtonLabel = stringResource(R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(desc, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(fieldLabel) },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { priceText.toDoubleOrNull()?.let { onSave(it) } }) {
                Text(saveButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelButtonLabel) }
        }
    )
}

@Composable
fun InsightsSection(insights: List<FuelInsight>) {
    if (insights.isEmpty()) return
    val header = stringResource(R.string.smart_insights_header)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            header,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.2.sp
        )
        insights.forEach { insight ->
            val (bgColor, textColor) = when (insight.type) {
                InsightType.POSITIVE -> Pair(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
                InsightType.WARNING -> Pair(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer
                )
                InsightType.INFO -> Pair(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(insight.emoji, style = MaterialTheme.typography.titleMedium)
                    Text(
                        insight.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(vehicle.name) }
    var make by remember { mutableStateOf(vehicle.make) }
    var model by remember { mutableStateOf(vehicle.model) }
    var plate by remember { mutableStateOf(vehicle.licensePlate) }
    var fuelType by remember { mutableStateOf(vehicle.fuelType) }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    
    val petrolType = stringResource(R.string.fuel_type_petrol)
    val dieselType = stringResource(R.string.fuel_type_diesel)
    val cngType = stringResource(R.string.fuel_type_cng)
    val electricType = stringResource(R.string.fuel_type_electric)
    val fuelTypes = listOf(petrolType, dieselType, cngType, electricType)

    val editTitle = stringResource(R.string.edit_vehicle_title)
    val nameLabel = stringResource(R.string.vehicle_name_label)
    val makeLabel = stringResource(R.string.make_label)
    val modelLabel = stringResource(R.string.model_label)
    val plateLabel = stringResource(R.string.license_plate_label)
    val fuelTypeLabel = stringResource(R.string.fuel_type_label)
    val saveLabel = stringResource(R.string.save_button)
    val cancelLabel = stringResource(R.string.cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(editTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text(makeLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(modelLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it },
                    label = { Text(plateLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = fuelTypeExpanded,
                    onExpandedChange = { fuelTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = fuelType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(fuelTypeLabel) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelTypeExpanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = fuelTypeExpanded,
                        onDismissRequest = { fuelTypeExpanded = false }
                    ) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { fuelType = type; fuelTypeExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, make, model, plate, fuelType) },
                enabled = name.isNotBlank()
            ) { Text(saveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        }
    )
}
