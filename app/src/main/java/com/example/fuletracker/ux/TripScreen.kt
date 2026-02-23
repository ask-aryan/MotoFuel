package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fuletracker.data.Trip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val trips by viewModel.currentTrips.collectAsState()
    val tripA by viewModel.activeTripA.collectAsState()
    val tripB by viewModel.activeTripB.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()

    var showStartNamedTrip by remember { mutableStateOf(false) }
    var showEndTripDialog by remember { mutableStateOf<Trip?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Trip Mode", style = MaterialTheme.typography.headlineMedium) }

        // ── Quick Trips ────────────────────────────────────────────────────
        item {
            Text("QUICK TRIPS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.2.sp)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTripCard(
                    label = "Trip A",
                    trip = tripA,
                    modifier = Modifier.weight(1f),
                    onStart = { odo -> viewModel.startQuickTrip("QUICK_A", odo) },
                    onReset = { odo -> tripA?.let { viewModel.resetQuickTrip(it, odo) } },
                    onEnd = { showEndTripDialog = tripA }
                )
                QuickTripCard(
                    label = "Trip B",
                    trip = tripB,
                    modifier = Modifier.weight(1f),
                    onStart = { odo -> viewModel.startQuickTrip("QUICK_B", odo) },
                    onReset = { odo -> tripB?.let { viewModel.resetQuickTrip(it, odo) } },
                    onEnd = { showEndTripDialog = tripB }
                )
            }
        }

        // ── Named Trips ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NAMED TRIPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.2.sp)
                if (activeTrip == null || activeTrip?.type != "NAMED") {
                    FilledTonalButton(
                        onClick = { showStartNamedTrip = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Trip", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Active named trip
        if (activeTrip != null && activeTrip?.type == "NAMED") {
            item {
                ActiveNamedTripCard(
                    trip = activeTrip!!,
                    onEnd = { showEndTripDialog = activeTrip }
                )
            }
        }

        // Completed trips history
        val completedTrips = trips.filter { !it.isActive && it.type == "NAMED" }
        if (completedTrips.isEmpty() && activeTrip == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("No trips yet. Start your first trip!",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(completedTrips) { trip ->
                val stats = viewModel.computeTripStats(trip, entries)
                CompletedTripCard(
                    trip = trip,
                    stats = stats,
                    onDelete = { viewModel.deleteTrip(trip) }
                )
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
    if (showStartNamedTrip) {
        StartNamedTripDialog(
            onDismiss = { showStartNamedTrip = false },
            onStart = { name, odo, notes ->
                viewModel.startNamedTrip(name, odo, notes)
                showStartNamedTrip = false
            }
        )
    }

    showEndTripDialog?.let { trip ->
        EndTripDialog(
            trip = trip,
            onDismiss = { showEndTripDialog = null },
            onEnd = { odo ->
                viewModel.endTrip(trip, odo)
                showEndTripDialog = null
            }
        )
    }
}

// ── Quick Trip Card ────────────────────────────────────────────────────────────
@Composable
fun QuickTripCard(
    label: String,
    trip: Trip?,
    modifier: Modifier = Modifier,
    onStart: (Double) -> Unit,
    onReset: (Double) -> Unit,
    onEnd: () -> Unit
) {
    var showOdoInput by remember { mutableStateOf(false) }
    var odoText by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (trip != null)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            if (trip != null) {
                Text("Started", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                Text("%.0f km".format(trip.startOdometer),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Reset button
                    FilledTonalIconButton(
                        onClick = { action = "reset"; showOdoInput = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    }
                    // Stop button
                    FilledTonalIconButton(
                        onClick = { action = "stop"; showOdoInput = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                    }
                }
            } else {
                Text("Not started", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                FilledTonalButton(
                    onClick = { action = "start"; showOdoInput = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showOdoInput) {
        AlertDialog(
            onDismissRequest = { showOdoInput = false; odoText = "" },
            title = { Text("Enter Odometer (km)") },
            text = {
                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it },
                    label = { Text("Odometer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    odoText.toDoubleOrNull()?.let { odo ->
                        when (action) {
                            "start" -> onStart(odo)
                            "reset" -> onReset(odo)
                            "stop" -> onEnd()
                        }
                    }
                    showOdoInput = false
                    odoText = ""
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showOdoInput = false; odoText = "" }) { Text("Cancel") }
            }
        )
    }
}

// ── Active Named Trip Card ─────────────────────────────────────────────────────
@Composable
fun ActiveNamedTripCard(trip: Trip, onEnd: () -> Unit) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Flag, null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text(trip.name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("ACTIVE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold)
                }
            }
            Text("Started: ${dateFmt.format(Date(trip.startDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Text("Start odometer: ${"%.0f".format(trip.startOdometer)} km",
                style = MaterialTheme.typography.bodyMedium)
            if (trip.notes.isNotEmpty()) {
                Text(trip.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            Button(
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("End Trip")
            }
        }
    }
}

// ── Completed Trip Card ────────────────────────────────────────────────────────
@Composable
fun CompletedTripCard(trip: Trip, stats: FuelViewModel.TripStats?, onDelete: () -> Unit) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trip.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    "${dateFmt.format(Date(trip.startDate))} → ${trip.endDate?.let { dateFmt.format(Date(it)) } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (stats != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${"%.0f".format(stats.distance)} km",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        if (stats.efficiency > 0) {
                            Text("${"%.1f".format(stats.efficiency)} km/L",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        if (stats.cost > 0) {
                            Text("₹${"%.0f".format(stats.cost)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────
@Composable
fun StartNamedTripDialog(onDismiss: () -> Unit, onStart: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var odo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Named Trip") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip Name") },
                    placeholder = { Text("e.g. Jaipur to Delhi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = odo,
                    onValueChange = { odo = it },
                    label = { Text("Start Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val o = odo.toDoubleOrNull()
                if (name.isNotBlank() && o != null) onStart(name, o, notes)
            }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EndTripDialog(trip: Trip, onDismiss: () -> Unit, onEnd: (Double) -> Unit) {
    var odo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Trip: ${trip.name}") },
        text = {
            OutlinedTextField(
                value = odo,
                onValueChange = { odo = it },
                label = { Text("Current Odometer (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                odo.toDoubleOrNull()?.let { onEnd(it) }
            }) { Text("End Trip") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}