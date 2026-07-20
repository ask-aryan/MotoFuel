package com.example.fueltracker.ux

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fueltracker.data.Vehicle
import java.io.File
import java.io.FileOutputStream

// ── Copy image to internal storage so URI stays valid ─────────────────────────
fun copyImageToInternal(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val dir = File(context.filesDir, "bike_images").apply { mkdirs() }
        val file = File(dir, "bike_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output -> input.copyTo(output) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun VehicleManagementScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val vehicles by viewModel.allVehicles.collectAsState()
    val selectedId by viewModel.selectedVehicleId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Bikes", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vehicles) { vehicle ->
                VehicleItem(
                    vehicle = vehicle,
                    isSelected = vehicle.id == selectedId,
                    onSelect = { viewModel.selectVehicle(vehicle.id) },
                    onImagePicked = { uri -> viewModel.updateVehicleImage(vehicle, uri) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, make, model, plate, fuelType ->
                viewModel.addVehicle(name, make, model, plate, fuelType)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun VehicleItem(
    vehicle: Vehicle,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onImagePicked: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val savedPath = copyImageToInternal(context, it)
            if (savedPath != null) onImagePicked(savedPath)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Bike image banner (full width if set)
            if (!vehicle.imageUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    AsyncImage(
                        model = vehicle.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Change photo button overlay
                    SmallFloatingActionButton(
                        onClick = { imageLauncher.launch("image/*") },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, "Change photo",
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon or image thumbnail (when no full banner)
                if (vehicle.imageUrl.isNullOrEmpty()) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(vehicle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (vehicle.make.isNotEmpty() || vehicle.model.isNotEmpty()) {
                        Text("${vehicle.make} ${vehicle.model}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (vehicle.licensePlate.isNotEmpty()) {
                        Surface(shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(vehicle.licensePlate,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Add photo button when no image yet
                if (vehicle.imageUrl.isNullOrEmpty()) {
                    FilledTonalButton(
                        onClick = { imageLauncher.launch("image/*") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Photo", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("Petrol") }
    var fuelTypeExpanded by remember { mutableStateOf(false) }
    val fuelTypes = listOf("Petrol", "Diesel", "CNG", "Electric")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Bike / Vehicle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name (e.g. My Xpulse)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = make, onValueChange = { make = it },
                    label = { Text("Brand (e.g. Hero)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it },
                    label = { Text("Model (e.g. Xpulse 210)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plate, onValueChange = { plate = it },
                    label = { Text("License Plate") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = fuelTypeExpanded, onExpandedChange = { fuelTypeExpanded = it }) {
                    OutlinedTextField(
                        value = fuelType, onValueChange = {}, readOnly = true,
                        label = { Text("Fuel Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelTypeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = fuelTypeExpanded, onDismissRequest = { fuelTypeExpanded = false }) {
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { fuelType = type; fuelTypeExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, make, model, plate, fuelType) },
                enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
