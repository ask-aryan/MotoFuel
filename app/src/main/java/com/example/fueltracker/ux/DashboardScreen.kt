package com.example.fueltracker.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fueltracker.R
import androidx.compose.ui.res.stringResource
import com.example.fueltracker.data.FuelEntry
import com.example.fueltracker.ui.theme.AppTheme
import com.example.fueltracker.ui.theme.LocalAppTheme
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val vehicles by viewModel.allVehicles.collectAsState()
    val stats = viewModel.computeStats(entries)
    val price = viewModel.petrolPrice

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thisMonthSpent = entries.filter { it.date >= monthStart }
        .sumOf { it.fuelAmount * it.pricePerLiter }

    var showAddEntrySheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FuelEntry?>(null) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableStateOf(viewModel.getOnboardingStep()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val manageBikeMessage = stringResource(R.string.onboarding_manage_bike_snackbar)
    val gotItLabel = stringResource(R.string.got_it)
    val petrolTypeLabel = stringResource(R.string.fuel_type_petrol)

    LaunchedEffect(Unit) {
        viewModel.showAddEntryEvent.collectLatest {
            if (vehicles.isNotEmpty() && price > 0) showAddEntrySheet = true
        }
    }
    LaunchedEffect(vehicles.size, price) { onboardingStep = viewModel.getOnboardingStep() }
    LaunchedEffect(onboardingStep) {
        if (onboardingStep == 2) {
            snackbarHostState.showSnackbar(manageBikeMessage, gotItLabel, duration = SnackbarDuration.Long)
            viewModel.setOnboardingStep(3); onboardingStep = 3
        }
    }

    // ── Range estimate ─────────────────────────────────────────────────────
    val lastEntry = entries.maxByOrNull { it.odometer }
    val estimatedRange: Double? = if (stats != null && lastEntry != null && stats.avgEfficiency > 0) {
        val fullRange = stats.avgEfficiency * lastEntry.fuelAmount
        val distanceSince = stats.lastOdometer - lastEntry.odometer
        (fullRange - distanceSince).coerceAtLeast(0.0)
    } else null
    val maxRange: Double? = if (stats != null && lastEntry != null && stats.avgEfficiency > 0)
        stats.avgEfficiency * lastEntry.fuelAmount else null
    val rangePercent = if (estimatedRange != null && maxRange != null && maxRange > 0)
        (estimatedRange / maxRange).coerceIn(0.0, 1.0).toFloat() else 0f

    // ── Last fill label ────────────────────────────────────────────────────
    val lastFillLabel = lastEntry?.let {
        val days = ((System.currentTimeMillis() - it.date) / 86_400_000).toInt()
        val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.date))
        val daysStr = when (days) { 0 -> "today"; 1 -> "yesterday"; else -> "$days days ago" }
        "Last fill $daysStr · ${"%.1f".format(it.fuelAmount)} L · $dateStr"
    }

    // ── Last service ───────────────────────────────────────────────────────
    val serviceEntries by viewModel.currentServiceEntries.collectAsState()
    val lastService = serviceEntries.firstOrNull()

    // ── Insights ───────────────────────────────────────────────────────────
    val insights = viewModel.generateInsights(entries)
    val topInsight = insights.firstOrNull()

    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    val fabState = when {
        price <= 0 -> FabState.Price
        vehicles.isEmpty() -> FabState.Vehicle
        else -> FabState.Fillup
    }
    val onFabClick: () -> Unit = {
        when (fabState) {
            FabState.Price -> showPriceDialog = true
            FabState.Vehicle -> showVehicleDialog = true
            FabState.Fillup -> showAddEntrySheet = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        if (isLandscape) {
            // ── Landscape 3-column layout ──────────────────────────────────
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT: Hero card only — no FAB overlay
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroOdometerCard(
                        odometer = stats?.lastOdometer,
                        lastFillLabel = lastFillLabel,
                        estimatedRange = estimatedRange,
                        rangePercent = rangePercent
                    )
                }

                // MIDDLE: 3 stat chips + optional service/insight — fits viewport height
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (stats != null) {
                        StatChip(label = "AVG", value = "${"%.1f".format(stats.avgEfficiency)}", unit = "km/L")
                        StatChip(label = "COST/KM", value = "₹${"%.2f".format(stats.costPerKm)}")
                        StatChip(label = "THIS MO.", value = "₹${"%.0f".format(thisMonthSpent)}")
                    }
                    lastService?.let { svc ->
                        MiniChip(
                            icon = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) },
                            title = svc.title,
                            subtitle = "${"%.0f".format(svc.odometer)} km",
                            accentBg = true
                        )
                    }
                    topInsight?.let { insight ->
                        MiniChip(
                            icon = { Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) },
                            title = insight.message.take(28),
                            subtitle = "",
                            accentBg = false
                        )
                    }
                }

                // RIGHT: Recent fill-ups + Log Fill-up button pinned at bottom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        "RECENT FILL-UPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    // Scrollable list takes all remaining height
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        if (entries.isNotEmpty()) {
                            entries.take(5).forEach { entry ->
                                LandscapeEntryRow(
                                    entry = entry,
                                    onDelete = { viewModel.deleteEntry(entry) }
                                )
                            }
                        } else if (vehicles.isEmpty()) {
                            EmptyStateCard()
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Log Fill-up button — full-width, pinned to bottom, never overlaps hero
                    Button(
                        onClick = onFabClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (fabState) {
                                FabState.Price -> MaterialTheme.colorScheme.error
                                FabState.Vehicle -> MaterialTheme.colorScheme.secondary
                                FabState.Fillup -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = when (fabState) {
                                FabState.Price -> MaterialTheme.colorScheme.onError
                                FabState.Vehicle -> MaterialTheme.colorScheme.onSecondary
                                FabState.Fillup -> MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    ) {
                        Icon(
                            when (fabState) {
                                FabState.Price -> Icons.Default.CurrencyRupee
                                FabState.Vehicle -> Icons.Default.DirectionsBike
                                FabState.Fillup -> Icons.Default.LocalGasStation
                            }, contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (fabState) {
                                FabState.Price -> "Set Price"
                                FabState.Vehicle -> "Add Bike"
                                FabState.Fillup -> "Log Fill-up"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // ── Portrait layout ────────────────────────────────────────────
            Box(modifier = modifier.fillMaxSize().padding(scaffoldPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Home", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                        if (stats != null) {
                            Text("${stats.entryCount} fill-ups", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HeroOdometerCard(
                        odometer = stats?.lastOdometer,
                        lastFillLabel = lastFillLabel,
                        estimatedRange = estimatedRange,
                        rangePercent = rangePercent
                    )

                    if (stats != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            StatChip(label = "AVG", value = "${"%.1f".format(stats.avgEfficiency)}", unit = "km/L", modifier = Modifier.weight(1f))
                            StatChip(label = "COST/KM", value = "₹${"%.2f".format(stats.costPerKm)}", modifier = Modifier.weight(1f))
                            StatChip(label = "THIS MO.", value = "₹${"%.0f".format(thisMonthSpent)}", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            StatChip(label = "TOTAL FUEL", value = "${"%.1f".format(stats.totalFuel)}", unit = "L", modifier = Modifier.weight(1f))
                            StatChip(label = "TOTAL SPENT", value = "₹${"%.0f".format(stats.totalCost)}", modifier = Modifier.weight(1f))
                        }
                    }

                    if (lastService != null || topInsight != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            lastService?.let { svc ->
                                MiniChip(
                                    icon = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) },
                                    title = svc.title, subtitle = "${"%.0f".format(svc.odometer)} km",
                                    accentBg = true, modifier = Modifier.weight(1f)
                                )
                            }
                            topInsight?.let { insight ->
                                MiniChip(
                                    icon = { Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) },
                                    title = insight.message.take(28), subtitle = "",
                                    accentBg = false, modifier = Modifier.weight(1f)
                                )
                            } ?: if (lastService != null) Spacer(Modifier.weight(1f)) else Unit
                        }
                    }

                    if (entries.isNotEmpty()) {
                        Text("RECENT FILL-UPS", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
                        entries.take(3).forEach { entry ->
                            EntryCard(
                                entry = entry,
                                onEdit = { editingEntry = entry; showAddEntrySheet = true },
                                onDelete = { viewModel.deleteEntry(entry) }
                            )
                        }
                    } else if (vehicles.isEmpty()) {
                        EmptyStateCard()
                    }

                    if (insights.size > 1) {
                        InsightsSection(insights = insights.drop(1))
                    }

                    Spacer(Modifier.height(140.dp))
                }

                ExtendedFloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 100.dp),
                    containerColor = when (fabState) {
                        FabState.Price -> MaterialTheme.colorScheme.error
                        FabState.Vehicle -> MaterialTheme.colorScheme.secondary
                        FabState.Fillup -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when (fabState) {
                        FabState.Price -> MaterialTheme.colorScheme.onError
                        FabState.Vehicle -> MaterialTheme.colorScheme.onSecondary
                        FabState.Fillup -> MaterialTheme.colorScheme.onPrimary
                    },
                    icon = {
                        Icon(when (fabState) {
                            FabState.Price -> Icons.Default.CurrencyRupee
                            FabState.Vehicle -> Icons.Default.DirectionsBike
                            FabState.Fillup -> Icons.Default.LocalGasStation
                        }, contentDescription = null)
                    },
                    text = {
                        Text(when (fabState) {
                            FabState.Price -> "Set Price"
                            FabState.Vehicle -> "Add Bike"
                            FabState.Fillup -> "Log Fill-up"
                        }, fontWeight = FontWeight.Bold)
                    }
                )
            }
        }
    }

    if (showAddEntrySheet && isLandscape) {
        Dialog(
            onDismissRequest = { showAddEntrySheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            GloveFuelPage(viewModel = viewModel, onDismiss = { showAddEntrySheet = false })
        }
    }

    if (showAddEntrySheet && !isLandscape) {
        val addEntrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddEntrySheet = false; editingEntry = null },
            sheetState = addEntrySheetState
        ) {
            AddEntryScreen(
                viewModel = viewModel,
                editingEntry = editingEntry,
                onEntryAdded = { showAddEntrySheet = false; editingEntry = null },
                onDismiss = { showAddEntrySheet = false; editingEntry = null },
                modifier = Modifier.imePadding()
            )
        }
    }

    if (showPriceDialog) {
        QuickPriceDialog(
            fuelType = petrolTypeLabel,
            currentPrice = viewModel.getFuelPrice(petrolTypeLabel),
            onDismiss = { showPriceDialog = false },
            onSave = { p -> viewModel.setFuelPrice(petrolTypeLabel, p); showPriceDialog = false }
        )
    }

    if (showVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showVehicleDialog = false },
            onConfirm = { name, make, model, plate, fuelType ->
                viewModel.addVehicle(name, make, model, plate, fuelType)
                showVehicleDialog = false
            }
        )
    }
}

// ── Hero Card ────────────────────────────────────────────────────────────────

@Composable
private fun HeroOdometerCard(
    odometer: Double?,
    lastFillLabel: String?,
    estimatedRange: Double?,
    rangePercent: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val appTheme = LocalAppTheme.current
    val isDaylight = appTheme == AppTheme.DAYLIGHT

    // Daylight: bold violet gradient, white text. Pit Lane: dark surface gradient, themed text.
    val heroBg: Brush
    val heroText: Color
    val heroMuted: Color
    val heroGaugeBg: Color
    val heroGaugeFill: Brush

    if (isDaylight) {
        heroBg = Brush.linearGradient(listOf(Color(0xFF6643E0), Color(0xFF8B5CF0)))
        heroText = Color.White
        heroMuted = Color.White.copy(alpha = 0.72f)
        heroGaugeBg = Color.White.copy(alpha = 0.2f)
        heroGaugeFill = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White))
    } else {
        heroBg = Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
        )
        heroText = MaterialTheme.colorScheme.onSurface
        heroMuted = MaterialTheme.colorScheme.onSurfaceVariant
        heroGaugeBg = outlineVariant
        heroGaugeFill = Brush.horizontalGradient(listOf(primary.copy(alpha = 0.7f), primary))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(heroBg)
            .then(if (!isDaylight) Modifier.border(1.dp, outlineVariant, RoundedCornerShape(22.dp)) else Modifier)
            .padding(20.dp)
    ) {
        // Corner decoration
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            (if (isDaylight) Color.White else primary).copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ODOMETER",
                    style = MaterialTheme.typography.labelSmall,
                    color = heroMuted,
                    letterSpacing = 2.sp
                )
                if (odometer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(if (isDaylight) Color.White else primary, RoundedCornerShape(3.dp)))
                        Text("LIVE", style = MaterialTheme.typography.labelSmall, color = if (isDaylight) Color.White else primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    odometer?.let { "%,.0f".format(it) } ?: "—",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = heroText,
                    lineHeight = 52.sp
                )
                Text("km", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = heroMuted, modifier = Modifier.padding(bottom = 6.dp))
            }

            if (lastFillLabel != null) {
                Text(lastFillLabel, style = MaterialTheme.typography.bodySmall, color = heroMuted)
            }

            Spacer(Modifier.height(14.dp))

            // Range gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDaylight) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 13.dp, vertical = 11.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("RANGE LEFT", style = MaterialTheme.typography.labelSmall, color = heroMuted, letterSpacing = 1.5.sp)
                        Text(
                            estimatedRange?.let { "~${it.roundToInt()} km" } ?: "—",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroText
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(heroGaugeBg)
                    ) {
                        Box(Modifier.fillMaxWidth(rangePercent).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(heroGaugeFill))
                    }
                }
            }
        }
    }
}

// ── Stat chip ─────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Mini chip (maintenance / insight) ────────────────────────────────────────

@Composable
private fun MiniChip(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    accentBg: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (accentBg)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.surface
    val border = if (accentBg)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(13.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        icon()
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🏍️", fontSize = 48.sp)
            Text(
                "Add your bike and first fill-up to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

enum class FabState { Price, Vehicle, Fillup }

// ── Landscape-only entry row — clean 2-part layout, no crammed delete icon ───

@Composable
private fun LandscapeEntryRow(entry: com.example.fueltracker.data.FuelEntry, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { showDeleteConfirm = true }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: "3,799 km" on one line + "8.5 L · 19 Jun"
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "%,.0f km".format(entry.odometer),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                "${"%.1f".format(entry.fuelAmount)} L · ${dateFormat.format(java.util.Date(entry.date))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        // Right: "₹968" in accent + "₹113.5/L"
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "₹${"%.0f".format(entry.fuelAmount * entry.pricePerLiter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = primary,
                maxLines = 1
            )
            Text(
                "₹${"%.1f".format(entry.pricePerLiter)}/L",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = androidx.compose.ui.res.stringResource(R.string.delete_confirmation_title),
            message = androidx.compose.ui.res.stringResource(R.string.delete_fuel_entry_message),
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
