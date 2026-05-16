package com.example.fuletracker.ux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fuletracker.data.ServiceEntry
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.*

fun categoryEmoji(category: String) = when (category) {
    "Chain"   -> "🔗"
    "Oil"     -> "🛢️"
    "Service" -> "🔧"
    "Tyres"   -> "🛞"
    "Brakes"  -> "🛑"
    "Wash"    -> "🧼"
    else      -> "⚙️"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentServiceEntries.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Group entries by month
    val grouped = entries.groupBy { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.date }
        "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${cal.get(Calendar.YEAR)}"
    }

    val totalSpent = entries.sumOf { it.cost }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.log_service)) }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔧", fontSize = 64.sp)
                    Text(stringResource(R.string.diary_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.diary_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.total_services_label), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp)
                                Text("${entries.size}", style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.total_spent_label), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp)
                                Text("₹${"%.0f".format(totalSpent)}", style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                grouped.forEach { (month, monthEntries) ->
                    item {
                        Text(
                            month.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(monthEntries) { entry ->
                        ServiceEntryCard(entry = entry, onDelete = { viewModel.deleteServiceEntry(entry) })
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            AddServiceEntrySheet(
                viewModel = viewModel,
                onDone = { showAddSheet = false },
                onDismiss = { showAddSheet = false }
            )
        }
    }
}

@Composable
fun ServiceEntryCard(entry: ServiceEntry, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category emoji badge
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(categoryEmoji(entry.category), fontSize = 22.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${"%.0f".format(entry.odometer)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        dateFormat.format(Date(entry.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, maxLines = 1)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (entry.cost > 0) {
                    Text(
                        "₹${"%.0f".format(entry.cost)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_confirmation_title),
            message = stringResource(R.string.delete_service_entry_message),
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceEntrySheet(viewModel: FuelViewModel, onDone: () -> Unit, onDismiss: () -> Unit) {
    val entries by viewModel.currentEntries.collectAsState()
    val lastOdometer = viewModel.getLastOdometer(entries)

    val categories = listOf("Chain", "Oil", "Service", "Tyres", "Brakes", "Wash", "Other")
    var selectedCategory by remember { mutableStateOf("Service") }
    var title by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Quick title suggestions per category
    val suggestions = mapOf(
        "Chain" to listOf("Chain Lube", "Chain Clean", "Chain Replace"),
        "Oil" to listOf("Engine Oil Change", "Gear Oil Change"),
        "Service" to listOf("Full Service", "Minor Service", "Spark Plug"),
        "Tyres" to listOf("Front Tyre", "Rear Tyre", "Tyre Puncture"),
        "Brakes" to listOf("Brake Pads", "Brake Fluid", "Brake Adjustment"),
        "Wash" to listOf("Bike Wash", "Engine Cleaning"),
        "Other" to listOf("Air Filter", "Battery", "Clutch Cable")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.log_maintenance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }

        // Category chips
        Text(stringResource(R.string.category_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat; title = "" },
                    label = { Text("${categoryEmoji(cat)} $cat") }
                )
            }
        }

        // Quick suggestion chips
        val currentSuggestions = suggestions[selectedCategory] ?: emptyList()
        if (currentSuggestions.isNotEmpty()) {
            Text(stringResource(R.string.quick_select_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currentSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { title = suggestion },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Title field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.title_field_label)) },
            placeholder = { Text(stringResource(R.string.title_field_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Odometer field
        OutlinedTextField(
            value = odometer,
            onValueChange = { odometer = it },
            label = { Text(stringResource(R.string.odometer_field_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = lastOdometer?.let { { Text(stringResource(R.string.current_odometer_supporting, "%.0f".format(it))) } }
        )

        // Cost field
        OutlinedTextField(
            value = cost,
            onValueChange = { cost = it },
            label = { Text(stringResource(R.string.cost_field_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp)) }
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes_field_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val odo = odometer.toDoubleOrNull()
                when {
                    title.isBlank() -> error = "Enter what you did"
                    odo == null || odo <= 0 -> error = "Enter a valid odometer reading"
                    else -> {
                        viewModel.addServiceEntry(
                            title = title.trim(),
                            category = selectedCategory,
                            odometer = odo,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            notes = notes.trim()
                        )
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_to_diary), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
    }
}
