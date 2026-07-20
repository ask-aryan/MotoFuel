package com.example.fueltracker.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.fueltracker.ui.theme.AppTheme
import com.example.fueltracker.ui.theme.LocalAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fueltracker.data.ServiceEntry
import com.example.fueltracker.R
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

    val grouped = entries.groupBy { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.date }
        "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${cal.get(Calendar.YEAR)}"
    }

    val totalSpent = entries.sumOf { it.cost }
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.log_service), fontWeight = FontWeight.Bold) },
                containerColor = primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 88.dp)
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
                // ── Page Header ───────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Diary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${entries.size} services",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Summary hero card ─────────────────────────────────────────
                item {
                    val appTheme = LocalAppTheme.current
                    val isDaylight = appTheme == AppTheme.DAYLIGHT
                    val heroPrimary = MaterialTheme.colorScheme.primary
                    val heroBg: Brush = if (isDaylight)
                        Brush.linearGradient(listOf(Color(0xFF6643E0), Color(0xFF8B5CF0)))
                    else
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))
                    val heroText = if (isDaylight) Color.White else MaterialTheme.colorScheme.onSurface
                    val heroMuted = if (isDaylight) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(heroBg)
                            .then(if (!isDaylight) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)) else Modifier)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Corner decoration — matchParentSize so it doesn't add to card height
                        val decorColor = (if (isDaylight) Color.White else heroPrimary).copy(alpha = 0.12f)
                        Spacer(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    val radius = 180.dp.toPx()
                                    val cx = size.width + 16.dp.toPx()
                                    val cy = size.height + 16.dp.toPx()
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(decorColor, Color.Transparent),
                                            center = Offset(cx, cy),
                                            radius = radius
                                        ),
                                        radius = radius,
                                        center = Offset(cx, cy)
                                    )
                                }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    stringResource(R.string.total_services_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = heroMuted,
                                    letterSpacing = 1.5.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "${entries.size}",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = heroText,
                                        lineHeight = 44.sp
                                    )
                                    Text(
                                        "jobs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = heroMuted,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    stringResource(R.string.total_spent_label).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = heroMuted,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    "₹${"%.0f".format(totalSpent)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = heroText
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                grouped.forEach { (month, monthEntries) ->
                    item {
                        Text(
                            month.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(monthEntries) { entry ->
                        ServiceEntryCard(entry = entry, onDelete = { viewModel.deleteServiceEntry(entry) })
                    }
                }

                item { Spacer(Modifier.height(140.dp)) }
            }
        }
    }

    if (showAddSheet) {
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState
        ) {
            AddServiceEntrySheet(
                viewModel = viewModel,
                onDone = { showAddSheet = false },
                onDismiss = { showAddSheet = false },
                modifier = Modifier.imePadding()
            )
        }
    }
}

@Composable
fun ServiceEntryCard(entry: ServiceEntry, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(1.dp, outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category emoji tile — distinct per service type
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoryEmoji(entry.category), fontSize = 20.sp)
        }

        // Middle: title + odometer · date + optional notes
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${"%.0f".format(entry.odometer)} km · ${dateFormat.format(Date(entry.date))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.notes.isNotBlank()) {
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Right: cost + delete
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (entry.cost > 0) {
                Text(
                    "₹${"%.0f".format(entry.cost)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primary
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_confirmation_title),
            message = stringResource(R.string.delete_service_entry_message),
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceEntrySheet(viewModel: FuelViewModel, onDone: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val lastOdometer = viewModel.getLastOdometer(entries)

    val categories = listOf("Chain", "Oil", "Service", "Tyres", "Brakes", "Wash", "Other")
    var selectedCategory by remember { mutableStateOf("Service") }
    var title by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val suggestions = mapOf(
        "Chain" to listOf("Chain Lube", "Chain Clean", "Chain Replace"),
        "Oil" to listOf("Engine Oil Change", "Gear Oil Change"),
        "Service" to listOf("Full Service", "Minor Service", "Spark Plug"),
        "Tyres" to listOf("Front Tyre", "Rear Tyre", "Tyre Puncture"),
        "Brakes" to listOf("Brake Pads", "Brake Fluid", "Brake Adjustment"),
        "Wash" to listOf("Bike Wash", "Engine Cleaning"),
        "Other" to listOf("Air Filter", "Battery", "Clutch Cable")
    )

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.log_maintenance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = primary, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "CATEGORY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp
        )

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat; title = "" },
                    label = { Text("${categoryEmoji(cat)} $cat") }
                )
            }
        }

        val currentSuggestions = suggestions[selectedCategory] ?: emptyList()
        if (currentSuggestions.isNotEmpty()) {
            Text(
                stringResource(R.string.quick_select_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currentSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { title = suggestion },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

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
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    stringResource(R.string.title_field_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(primary),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) Text(
                                stringResource(R.string.title_field_placeholder),
                                style = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
            DiaryInputRow(
                label = stringResource(R.string.odometer_field_label),
                value = odometer,
                onValueChange = { odometer = it },
                suffix = "km",
                supporting = lastOdometer?.let { "Current: ${"%.0f".format(it)} km" }
            )
            HorizontalDivider(color = outlineVariant, thickness = 0.5.dp)
            DiaryInputRow(
                label = stringResource(R.string.cost_field_label),
                value = cost,
                onValueChange = { cost = it },
                suffix = "₹"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.dp, outlineVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                stringResource(R.string.notes_field_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(4.dp))
            BasicTextField(
                value = notes,
                onValueChange = { notes = it },
                textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(primary),
                minLines = 2,
                maxLines = 3,
                decorationBox = { inner ->
                    Box {
                        if (notes.isEmpty()) Text(
                            "Anything to note…",
                            style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

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
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "✓ ${stringResource(R.string.save_to_diary)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DiaryInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    supporting: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
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
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(primary),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (value.isEmpty()) Text(
                                "—",
                                style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            inner()
                        }
                    },
                    modifier = Modifier.width(100.dp)
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
