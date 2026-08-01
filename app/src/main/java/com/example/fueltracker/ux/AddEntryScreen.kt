package com.example.fueltracker.ux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fueltracker.R
import androidx.compose.ui.res.stringResource
import com.example.fueltracker.data.FuelEntry
import com.example.fueltracker.ui.theme.AppTheme
import com.example.fueltracker.ui.theme.LocalAppTheme

private fun formatNumber(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else value.toString()

@Composable
fun AddEntryScreen(
    viewModel: FuelViewModel,
    onEntryAdded: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    editingEntry: FuelEntry? = null
) {
    val entries by viewModel.currentEntries.collectAsState()
    val otherEntries = if (editingEntry != null) entries.filter { it.id != editingEntry.id } else entries

    // Add mode: must exceed the overall latest reading.
    // Edit mode: must stay between this entry's actual odometer neighbors, not the
    // global latest — editing an older fill-up shouldn't be compared to a later one.
    val lastOdometer = viewModel.getLastOdometer(otherEntries)
    val lowerBoundOdometer = if (editingEntry != null)
        otherEntries.filter { it.odometer < editingEntry.odometer }.maxOfOrNull { it.odometer }
        else lastOdometer
    val upperBoundOdometer = if (editingEntry != null)
        otherEntries.filter { it.odometer > editingEntry.odometer }.minOfOrNull { it.odometer }
        else null

    val petrolLabel = stringResource(R.string.fuel_type_petrol)
    val powerPetrolLabel = stringResource(R.string.fuel_type_power_petrol)

    var selectedFuelType by remember { mutableStateOf(editingEntry?.fuelType ?: petrolLabel) }
    val petrolPrice = viewModel.getFuelPrice(petrolLabel)
    val powerPetrolPrice = viewModel.getFuelPrice(powerPetrolLabel)
    val price = viewModel.getFuelPrice(selectedFuelType)

    var odometer by remember { mutableStateOf(editingEntry?.let { formatNumber(it.odometer) } ?: "") }
    var fuelAmount by remember { mutableStateOf(editingEntry?.let { formatNumber(it.fuelAmount) } ?: "") }
    var fullTank by remember { mutableStateOf(editingEntry?.fullTank ?: true) }
    var error by remember { mutableStateOf("") }

    val odoValue = odometer.toDoubleOrNull()
    val fuelValue = fuelAmount.toDoubleOrNull()
    val distanceSinceLast = if (odoValue != null && lowerBoundOdometer != null && odoValue > lowerBoundOdometer)
        odoValue - lowerBoundOdometer else null
    val estimatedCost = if (fuelValue != null && price > 0) fuelValue * price else null

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val appTheme = LocalAppTheme.current
    val isDaylight = appTheme == AppTheme.DAYLIGHT

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        if (isDaylight) {
            // Daylight: violet gradient banner behind title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF6643E0), Color(0xFF8B5CF0))))
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 28.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(if (editingEntry != null) R.string.edit_fillup_title else R.string.add_fillup_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (lastOdometer != null) {
                        Text(
                            "Last recorded · ${"%.0f".format(lastOdometer)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            // Pit Lane: plain header row
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(if (editingEntry != null) R.string.edit_fillup_title else R.string.add_fillup_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (lastOdometer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(5.dp).background(primary, RoundedCornerShape(3.dp)))
                        Text("Last recorded · ${"%.0f".format(lastOdometer)} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Scrollable body ──────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // ── Fuel Type ────────────────────────────────────────────────────────
        Text(
            "FUEL TYPE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuelTypeCard(
                onClick = { selectedFuelType = petrolLabel },
                selected = selectedFuelType == petrolLabel,
                icon = Icons.Default.LocalGasStation,
                name = petrolLabel,
                priceText = if (petrolPrice > 0) "₹${"%.2f".format(petrolPrice)}/L" else null,
                badge = null,
                modifier = Modifier.weight(1f)
            )
            FuelTypeCard(
                onClick = { selectedFuelType = powerPetrolLabel },
                selected = selectedFuelType == powerPetrolLabel,
                icon = Icons.Default.Star,
                name = powerPetrolLabel,
                priceText = if (powerPetrolPrice > 0) "₹${"%.2f".format(powerPetrolPrice)}/L" else null,
                badge = stringResource(R.string.premium_label),
                modifier = Modifier.weight(1f)
            )
        }

        if (price <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp)
            ) {
                Text(
                    stringResource(R.string.no_price_set_warning, selectedFuelType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ── Detail inputs ────────────────────────────────────────────────────
        Text(
            "DETAILS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.dp, outlineVariant, RoundedCornerShape(16.dp))
        ) {
            InputRow(
                label = stringResource(R.string.current_odometer_label),
                value = odometer,
                onValueChange = { odometer = it },
                suffix = "km",
                supporting = distanceSinceLast?.let { "${"%.1f".format(it)} km since last fill" }
            )
            HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
            InputRow(
                label = stringResource(R.string.fuel_amount_label),
                value = fuelAmount,
                onValueChange = { fuelAmount = it },
                suffix = "L"
            )
        }

        // ── Full tank toggle ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.dp, outlineVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.filled_to_full_tank_question),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (fullTank) stringResource(R.string.used_in_efficiency_desc)
                    else stringResource(R.string.excluded_from_efficiency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fullTank) primary else MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = fullTank,
                onCheckedChange = { fullTank = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = primary
                )
            )
        }

        // ── Cost preview ─────────────────────────────────────────────────────
        if (estimatedCost != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surface)
                    .border(1.dp, outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Cost this fill",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (distanceSinceLast != null) {
                        Text(
                            "${"%.1f".format(distanceSinceLast)} km covered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "₹${"%.2f".format(estimatedCost)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = primary
                )
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        // ── Save button ──────────────────────────────────────────────────────
        Button(
            onClick = {
                val odo = odometer.toDoubleOrNull()
                val fuel = fuelAmount.toDoubleOrNull()
                when {
                    odo == null || odo <= 0 -> error = "Enter a valid odometer reading"
                    lowerBoundOdometer != null && odo <= lowerBoundOdometer ->
                        error = "Odometer must be greater than ${lowerBoundOdometer.toInt()} km"
                    upperBoundOdometer != null && odo >= upperBoundOdometer ->
                        error = "Odometer must be less than ${upperBoundOdometer.toInt()} km"
                    fuel == null || fuel <= 0 -> error = "Enter a valid fuel amount"
                    else -> {
                        if (editingEntry != null) {
                            viewModel.updateEntry(
                                editingEntry.copy(
                                    odometer = odo,
                                    fuelAmount = fuel,
                                    fullTank = fullTank,
                                    fuelType = selectedFuelType,
                                    pricePerLiter = price
                                )
                            )
                        } else {
                            viewModel.addEntry(odo, fuel, fullTank, selectedFuelType)
                        }
                        onEntryAdded()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "✓ ${stringResource(if (editingEntry != null) R.string.update_entry_button else R.string.save_entry_button)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            )
        }
        } // end scrollable body Column
    } // end outer Column
}

// ── Fuel type card ────────────────────────────────────────────────────────────

@Composable
private fun FuelTypeCard(
    onClick: () -> Unit,
    selected: Boolean,
    icon: ImageVector,
    name: String,
    priceText: String?,
    badge: String?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val cardBg = if (selected) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
            Text(
                name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (priceText != null) {
                Text(
                    priceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.price_not_set),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primary.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// ── Input row (label + flat text field) ──────────────────────────────────────

@Composable
private fun InputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    supporting: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(primary),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (value.isEmpty()) {
                                Text(
                                    "—",
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.End
                                    )
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (supporting != null) {
            Text(
                supporting,
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
    }
}

