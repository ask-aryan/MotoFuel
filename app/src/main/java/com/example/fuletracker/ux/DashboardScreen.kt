package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fuletracker.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

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
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thisMonthFuelSpent = entries
        .filter { it.date >= monthStart }
        .sumOf { it.fuelAmount * it.pricePerLiter }

    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    var onboardingStep by remember { mutableStateOf(viewModel.getOnboardingStep()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch strings at the top level of the composable
    val manageBikeMessage = stringResource(R.string.onboarding_manage_bike_snackbar)
    val gotItLabel = stringResource(R.string.got_it)
    val bikeImageDesc = stringResource(R.string.bike_image_description)
    val avgEfficiencyLabel = stringResource(R.string.average_efficiency_label)
    val unitKmL = stringResource(R.string.unit_km_l)
    val costPerKmLabel = stringResource(R.string.cost_per_km_label)
    val totalDistanceLabel = stringResource(R.string.total_distance_label)
    val totalSpentLabel = stringResource(R.string.total_spent_label)
    val thisMonthFuelSpentLabel = stringResource(R.string.this_month_fuel_spent_label)
    val fuelPriceLabel = stringResource(R.string.fuel_price_label)
    val notSetLabel = stringResource(R.string.not_set)
    val recentFillUpsHeader = stringResource(R.string.recent_fill_ups_header)
    val welcomeMessage = stringResource(R.string.welcome_message)
    val tooltipPriceTitle = stringResource(R.string.tooltip_price_title)
    val tooltipPriceMessage = stringResource(R.string.tooltip_price_message)
    val tooltipBikeTitle = stringResource(R.string.tooltip_bike_title)
    val tooltipBikeMessage = stringResource(R.string.tooltip_bike_message)
    val fabSetPriceLabel = stringResource(R.string.fab_set_price)
    val fabAddBikeLabel = stringResource(R.string.fab_add_bike)
    val fabAddFillupLabel = stringResource(R.string.fab_add_fillup)
    val petrolTypeLabel = stringResource(R.string.fuel_type_petrol)
    val agoLabel = stringResource(R.string.ago)

    // Listen for Quick Add event from Widget
    LaunchedEffect(Unit) {
        viewModel.showAddEntryEvent.collectLatest {
            if (vehicles.isNotEmpty() && price > 0) {
                showAddEntrySheet = true
            }
        }
    }

    LaunchedEffect(vehicles.size, price) { onboardingStep = viewModel.getOnboardingStep() }
    LaunchedEffect(onboardingStep) {
        if (onboardingStep == 2) {
            snackbarHostState.showSnackbar(
                message = manageBikeMessage,
                actionLabel = gotItLabel,
                duration = SnackbarDuration.Long
            )
            viewModel.setOnboardingStep(3)
            onboardingStep = 3
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Box(modifier = modifier.fillMaxSize().padding(scaffoldPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Bike Image Banner ──────────────────────────────────────
                selectedVehicle?.imageUrl?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = bikeImageDesc,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // ── Hero Stats Card ────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            avgEfficiencyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                stats?.let { "%.1f".format(it.avgEfficiency) } ?: "—",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 52.sp
                            )
                            Text(
                                " $unitKmL",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(costPerKmLabel, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp)
                                Text(stats?.let { "₹${"%.2f".format(it.costPerKm)}" } ?: "₹—",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(totalDistanceLabel, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp)
                                Text(stats?.let { "${"%.0f".format(it.totalDistance)} km" } ?: "0 km",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // ── Quick Stats Row ────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickStatCard(
                        label = totalSpentLabel,
                        value = stats?.let { "₹${"%.0f".format(it.totalCost)}" } ?: "₹0",
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        label = fuelPriceLabel,
                        value = if (price > 0) "₹${"%.2f".format(price)}/L" else notSetLabel,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickStatCard(
                        label = thisMonthFuelSpentLabel,
                        value = "₹${"%.0f".format(thisMonthFuelSpent)}",
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        label = stringResource(R.string.stat_total_fuel),
                        value = stats?.let { "${"%.1f".format(it.totalFuel)} L" } ?: "0.0 L",
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Last Service Banner ────────────────────────────────────
                val serviceEntries by viewModel.currentServiceEntries.collectAsState()
                serviceEntries.firstOrNull()?.let { lastService ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(categoryEmoji(lastService.category), fontSize = 22.sp)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.last_service_title, lastService.title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(stringResource(R.string.at_odometer_reading, "%.0f".format(lastService.odometer)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            }
                            // Distance since last service
                            stats?.let { s ->
                                val kmSince = s.lastOdometer - lastService.odometer
                                if (kmSince > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${"%.0f".format(kmSince)} km", fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text(agoLabel, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Recent Activity ────────────────────────────────────────
                if (entries.isNotEmpty()) {
                    Text(recentFillUpsHeader, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline, letterSpacing = 1.2.sp)
                    entries.take(3).forEach { entry ->
                        EntryCard(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                    }
                } else if (vehicles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏍️", fontSize = 48.sp)
                        Text(welcomeMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                // ── Insights Section ───────────────────────────────────
                val insights = viewModel.generateInsights(entries)
                InsightsSection(insights = insights)

                Spacer(Modifier.height(80.dp))
            }

            // ── Onboarding tooltips ────────────────────────────────────────
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 96.dp),
                horizontalAlignment = Alignment.End
            ) {
                OnboardingTooltip(
                    visible = onboardingStep == 0,
                    icon = Icons.Default.CurrencyRupee,
                    title = tooltipPriceTitle,
                    message = tooltipPriceMessage
                )
                OnboardingTooltip(
                    visible = onboardingStep == 1,
                    icon = Icons.Default.DirectionsBike,
                    title = tooltipBikeTitle,
                    message = tooltipBikeMessage
                )
            }

            val fabState = when {
                price <= 0 || onboardingStep == 0 -> FabState.Price
                vehicles.isEmpty() || onboardingStep == 1 -> FabState.Vehicle
                else -> FabState.Fillup
            }
            FloatingActionButton(
                onClick = {
                    when (fabState) {
                        FabState.Price -> showPriceDialog = true
                        FabState.Vehicle -> showVehicleDialog = true
                        FabState.Fillup -> showAddEntrySheet = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = when (fabState) {
                    FabState.Price -> MaterialTheme.colorScheme.error
                    FabState.Vehicle -> MaterialTheme.colorScheme.secondary
                    FabState.Fillup -> MaterialTheme.colorScheme.primary
                }
            ) {
                when (fabState) {
                    FabState.Price -> Icon(Icons.Default.CurrencyRupee, fabSetPriceLabel)
                    FabState.Vehicle -> Icon(Icons.Default.DirectionsBike, fabAddBikeLabel)
                    FabState.Fillup -> Icon(Icons.Default.LocalGasStation, fabAddFillupLabel)
                }
            }
        }
    }

    if (showAddEntrySheet) {
        ModalBottomSheet(onDismissRequest = { showAddEntrySheet = false }) {
            AddEntryScreen(
                viewModel = viewModel,
                onEntryAdded = { showAddEntrySheet = false },
                onDismiss = { showAddEntrySheet = false }
            )
        }
    }

    if (showPriceDialog) {
        QuickPriceDialog(
            fuelType = petrolTypeLabel,
            currentPrice = viewModel.getFuelPrice(petrolTypeLabel),
            onDismiss = { showPriceDialog = false },
            onSave = { p ->
                viewModel.setFuelPrice(petrolTypeLabel, p)
                showPriceDialog = false
            }
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

enum class FabState { Price, Vehicle, Fillup }
