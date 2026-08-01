package com.example.fueltracker.ux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.fueltracker.data.AppBackup
import kotlinx.coroutines.launch
import com.example.fueltracker.data.Vehicle
import com.example.fueltracker.ui.theme.AppTheme
import com.example.fueltracker.worker.ReminderScheduler
import com.example.fueltracker.R
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val vehicles by viewModel.allVehicles.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showAddVehicle by remember { mutableStateOf(false) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    val currentTheme by viewModel.appTheme.collectAsState()
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {

        // ── Look & Feel ────────────────────────────────────────────────────
        item {
            SettingsSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.Palette, null, tint = primary, modifier = Modifier.size(18.dp))
                    Text(
                        "Look & Feel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Choose a visual style for the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AppTheme.entries.forEach { theme ->
                    val isSelected = theme == currentTheme
                    val accentColor = when (theme) {
                        AppTheme.PIT_LANE -> Color(0xFFC2F53F)
                        AppTheme.DAYLIGHT -> Color(0xFF6643E0)
                    }
                    val swatchBg = when (theme) {
                        AppTheme.PIT_LANE -> Color(0xFF15171A)
                        AppTheme.DAYLIGHT -> Color(0xFFF4EFE7)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                if (isSelected) BorderStroke(2.dp, accentColor)
                                else BorderStroke(1.dp, outlineVariant),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setAppTheme(theme) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(swatchBg)
                                .border(BorderStroke(1.5.dp, accentColor), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(accentColor)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(theme.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setAppTheme(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                    }
                }
            }
        }

        // ── Fuel Prices ────────────────────────────────────────────────────
        item {
            val fuelTypes = listOf(
                stringResource(R.string.fuel_type_petrol),
                stringResource(R.string.fuel_type_power_petrol),
                "Diesel", "CNG", "Electric"
            )
            val priceTexts = remember {
                mutableStateMapOf<String, String>().apply {
                    fuelTypes.forEach { type ->
                        put(type, viewModel.getFuelPrice(type).let {
                            if (it > 0) "%.2f".format(it) else ""
                        })
                    }
                }
            }
            val recentlySaved = remember { mutableStateMapOf<String, Boolean>() }
            val priceScope = rememberCoroutineScope()

            SettingsSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, null, tint = primary, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.fuel_prices_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Set current price per litre for each fuel you use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                fuelTypes.forEach { fuelType ->
                    val savedPrice = viewModel.getFuelPrice(fuelType)
                    val hasPrice = savedPrice > 0
                    val isEditing = (priceTexts[fuelType] ?: "") != (if (hasPrice) "%.2f".format(savedPrice) else "")

                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Price field card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (hasPrice) primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    if (hasPrice) BorderStroke(1.5.dp, primary.copy(alpha = 0.5f))
                                    else BorderStroke(1.dp, outlineVariant),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Column {
                                Text(
                                    fuelType.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasPrice) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "₹",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedTextField(
                                        value = priceTexts[fuelType] ?: "",
                                        onValueChange = { priceTexts[fuelType] = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        placeholder = {
                                            Text(
                                                if (hasPrice) "%.2f".format(savedPrice) else "Not set",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "/L",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Save button
                        val justSaved = recentlySaved[fuelType] == true
                        val btnActive = isEditing || !hasPrice
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    when {
                                        justSaved -> MaterialTheme.colorScheme.tertiary
                                        btnActive -> primary
                                        else -> primary.copy(alpha = 0.12f)
                                    }
                                )
                                .clickable {
                                    priceTexts[fuelType]?.toDoubleOrNull()?.let { newPrice ->
                                        viewModel.setFuelPrice(fuelType, newPrice)
                                        priceTexts[fuelType] = "%.2f".format(newPrice)
                                        recentlySaved[fuelType] = true
                                        priceScope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            recentlySaved[fuelType] = false
                                        }
                                    }
                                }
                                .padding(horizontal = 17.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (justSaved) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        "Saved",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            } else {
                                Text(
                                    stringResource(R.string.save_button),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (btnActive) MaterialTheme.colorScheme.onPrimary else primary
                                )
                            }
                        }
                    }
                }

                // Price History
                if (allEntries.isNotEmpty()) {
                    val recentPriceEntries = allEntries
                        .filter { it.pricePerLiter > 0 }
                        .sortedByDescending { it.date }
                        .distinctBy { "${it.fuelType}_${"%.2f".format(it.pricePerLiter)}" }
                        .take(8)

                    if (recentPriceEntries.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.price_history_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    recentPriceEntries.firstOrNull()?.fuelType?.uppercase() ?: "RECENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        recentPriceEntries.forEachIndexed { idx, entry ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            val vehicle = vehicles.find { it.id == entry.vehicleId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            .format(Date(entry.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (vehicle != null) {
                                        Text(
                                            vehicle.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "₹${"%.2f".format(entry.pricePerLiter)}/L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Vehicles ───────────────────────────────────────────────────────
        item {
            SettingsSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsBike, null, tint = primary, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.my_vehicles_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(primary.copy(alpha = 0.12f))
                            .clickable { showAddVehicle = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_vehicle_desc), tint = primary, modifier = Modifier.size(18.dp))
                    }
                }

                if (vehicles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_vehicles_yet), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    vehicles.forEach { vehicle ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DirectionsBike, null, tint = primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(vehicle.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    if (vehicle.make.isNotEmpty() || vehicle.model.isNotEmpty()) {
                                        Text("${vehicle.make} ${vehicle.model}".trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (vehicle.licensePlate.isNotEmpty()) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(primary.copy(alpha = 0.08f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(vehicle.licensePlate, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = primary)
                                        }
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = { editingVehicle = vehicle }) {
                                    Icon(Icons.Default.Edit, null, tint = primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { vehicleToDelete = vehicle }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Backup & Restore ───────────────────────────────────────────────
        item {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var showImportDialog by remember { mutableStateOf(false) }
            var pendingBackup by remember { mutableStateOf<AppBackup?>(null) }
            var importError by remember { mutableStateOf("") }

            val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    scope.launch {
                        val backup = viewModel.readBackup(it)
                        if (backup != null) { pendingBackup = backup; showImportDialog = true }
                        else importError = "Invalid backup file"
                    }
                }
            }

            SettingsSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, null, tint = primary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.backup_restore_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.backup_restore_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

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
                            context.startActivity(Intent.createChooser(intent, "Save Backup"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.export_backup_button))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { filePicker.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.import_backup_button))
                }

                if (importError.isNotEmpty()) {
                    Text(importError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (showImportDialog && pendingBackup != null) {
                val backup = pendingBackup!!
                val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text(stringResource(R.string.import_backup_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.backup_from_label, dateFmt.format(Date(backup.exportDate))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.contains_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("• ${backup.vehicles.size} ${stringResource(R.string.vehicles_count_label)}", style = MaterialTheme.typography.bodySmall)
                            Text("• ${backup.fuelEntries.size} ${stringResource(R.string.fuel_entries_count_label)}", style = MaterialTheme.typography.bodySmall)
                            Text("• ${backup.serviceEntries.size} ${stringResource(R.string.service_entries_count_label)}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Text(stringResource(R.string.import_mode_question), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {
                        Button(onClick = { scope.launch { viewModel.importBackup(backup, false); showImportDialog = false; pendingBackup = null } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.replace_all_button)) }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.cancel)) }
                            FilledTonalButton(onClick = { scope.launch { viewModel.importBackup(backup, true); showImportDialog = false; pendingBackup = null } }) { Text(stringResource(R.string.merge_button)) }
                        }
                    }
                )
            }
        }

        // ── Auto Backup ────────────────────────────────────────────────────
        item {
            val context = LocalContext.current
            var folderUriString by remember { mutableStateOf(viewModel.getBackupFolderUri()) }
            var autoBackupEnabled by remember { mutableStateOf(viewModel.isAutoBackupEnabled()) }
            val folderName = folderUriString?.let { uriStr ->
                androidx.documentfile.provider.DocumentFile.fromTreeUri(context, android.net.Uri.parse(uriStr))?.name
            }

            val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    viewModel.setBackupFolder(uri)
                    folderUriString = uri.toString()
                }
            }

            SettingsSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, null, tint = primary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.auto_backup_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.auto_backup_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                Text(
                    text = folderName?.let { stringResource(R.string.backup_folder_selected_label, it) }
                        ?: stringResource(R.string.backup_folder_none_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (folderUriString == null) stringResource(R.string.choose_backup_folder_button)
                        else stringResource(R.string.change_backup_folder_button)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.enable_auto_backup_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = autoBackupEnabled,
                        enabled = folderUriString != null,
                        onCheckedChange = { enabled ->
                            autoBackupEnabled = enabled
                            viewModel.setAutoBackupEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = primary)
                    )
                }
            }
        }

        // ── Reminders ──────────────────────────────────────────────────────
        item {
            val context = LocalContext.current
            var notificationsEnabled by remember { mutableStateOf(viewModel.areNotificationsEnabled()) }

            SettingsSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, null, tint = primary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.enable_reminders_label), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.reminders_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = primary)
                    )
                }

                if (notificationsEnabled) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.active_reminders_header), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    listOf(R.string.weekly_reminder_label, R.string.inactivity_reminder_label, R.string.price_check_reminder_label).forEach {
                        Text("• ${stringResource(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
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

    if (vehicleToDelete != null) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_confirmation_title),
            message = stringResource(R.string.delete_vehicle_message),
            onConfirm = { viewModel.deleteVehicle(vehicleToDelete!!); vehicleToDelete = null },
            onDismiss = { vehicleToDelete = null }
        )
    }
}

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}
