package com.example.fuletracker.ux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.WavingHand
import com.example.fuletracker.ux.OnboardingTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FuelViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val vehicles by viewModel.allVehicles.collectAsState()
    
    val stats = viewModel.computeStats(entries)
    val price = viewModel.petrolPrice
    
    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }

    var onboardingStep by remember { mutableStateOf(viewModel.getOnboardingStep()) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vehicles.size, price) {
        onboardingStep = viewModel.getOnboardingStep()
    }

    LaunchedEffect(onboardingStep) {
        if (onboardingStep == 2) {
            snackbarHostState.showSnackbar(
                message = "⚙ Manage your garage & fuel price in Settings",
                actionLabel = "Got it",
                duration = SnackbarDuration.Long
            )
            viewModel.setOnboardingStep(3)
            onboardingStep = 3
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Box(modifier = modifier.fillMaxSize().padding(scaffoldPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Selector and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vehicle Selector
                    if (vehicles.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                    Text(
                                        text = selectedVehicle?.name ?: "Select Vehicle",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                vehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text(vehicle.name) },
                                        onClick = {
                                            viewModel.selectVehicle(vehicle.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }


                }

                // Hero card with Efficiency and Cost per KM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "AVERAGE EFFICIENCY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                stats?.let { "%.1f".format(it.avgEfficiency) } ?: "—",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 52.sp
                            )
                            Text(
                                " km/L",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "COST PER KM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    stats?.let { "₹${"%.2f".format(it.costPerKm)}" } ?: "₹—",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "TOTAL DISTANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    stats?.let { "${"%.0f".format(it.totalDistance)} km" } ?: "0 km",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Quick stats grid
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickStatCard(
                        label = "Total Spent",
                        value = stats?.let { "₹${"%.0f".format(it.totalCost)}" } ?: "₹0",
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        label = "Fuel Price",
                        value = if (price > 0) "₹${"%.2f".format(price)}/L" else "—",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Recent activity header
                if (entries.isNotEmpty()) {
                    Text(
                        "RECENT ACTIVITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.2.sp
                    )
                    entries.take(3).forEach { entry ->
                        EntryCard(entry = entry, onDelete = { viewModel.deleteEntry(entry) })
                    }
                } else if (vehicles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome! Set up your profile to start tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                // Smart Insights
                val insights = viewModel.generateInsights(entries)
                InsightsSection(insights = insights)

                Spacer(Modifier.height(80.dp))
            }
// Onboarding tooltips
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp),
                horizontalAlignment = Alignment.End
            ) {
                OnboardingTooltip(
                    visible = onboardingStep == 0,
                    icon = Icons.Default.CurrencyRupee,
                    title = "Set Fuel Price First",
                    message = "Tap the button below to enter current petrol price"
                )
                OnboardingTooltip(
                    visible = onboardingStep == 1,
                    icon = Icons.Default.DirectionsCar,
                    title = "Add Your Vehicle",
                    message = "Now tap below to add your bike or car"
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
                    FabState.Price -> Icon(Icons.Default.CurrencyRupee, "Set Price")
                    FabState.Vehicle -> Icon(Icons.Default.DirectionsCar, "Add Vehicle")
                    FabState.Fillup -> Icon(Icons.Default.LocalGasStation, "Add Fill-up")
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
            currentPrice = price,
            onDismiss = { showPriceDialog = false },
            onSave = { 
                viewModel.petrolPrice = it
                showPriceDialog = false
            }
        )
    }
    if (showVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showVehicleDialog = false },
            onConfirm = { name: String, make: String, model: String, plate: String ->
                viewModel.addVehicle(name, make, model, plate)
                showVehicleDialog = false
            }
        )
    }
}
enum class FabState { Price, Vehicle, Fillup }