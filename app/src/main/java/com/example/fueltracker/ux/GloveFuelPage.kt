package com.example.fueltracker.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fueltracker.R
import kotlinx.coroutines.delay

// Dimension constants (from design spec; not colors, safe to inline)
private val StepperButtonSize  = 64.dp   // min touch target for ± buttons
private val OdometerNudgeSize  = 52.dp   // small ± on odometer row
private val PresetChipHeight   = 42.dp
private val SegmentedHeight    = 46.dp
private val SaveBarHeight      = 66.dp

@Composable
internal fun GloveFuelPage(
    viewModel: FuelViewModel,
    distanceKm: Double = 0.0,
    onDismiss: () -> Unit
) {
    val entries         by viewModel.currentEntries.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val lastOdometer = viewModel.getLastOdometer(entries) ?: 0.0

    val petrolLabel      = stringResource(R.string.fuel_type_petrol)
    val powerPetrolLabel = stringResource(R.string.fuel_type_power_petrol)

    var selectedFuelType by remember { mutableStateOf(petrolLabel) }
    var fuelLitres       by remember { mutableStateOf(0.0) }
    var odoKm            by remember { mutableStateOf(lastOdometer) }
    var odoSynced        by remember { mutableStateOf(lastOdometer > 0.0) }
    var fullTank         by remember { mutableStateOf(true) }

    // Sync odo once the DB load completes (currentEntries starts as emptyList)
    LaunchedEffect(lastOdometer) {
        if (!odoSynced && lastOdometer > 0.0) {
            odoKm = lastOdometer
            odoSynced = true
        }
    }

    val petrolPrice      = viewModel.getFuelPrice(petrolLabel)
    val powerPetrolPrice = viewModel.getFuelPrice(powerPetrolLabel)
    val price            = viewModel.getFuelPrice(selectedFuelType)
    val runningCost      = fuelLitres * price   // always live, even when 0

    // Quick-fill presets derived from vehicle tank capacity
    val tankCap = selectedVehicle?.tankCapacity ?: 13.0
    val quickFillPresets: List<Pair<String, Double>> = remember(tankCap) {
        when {
            tankCap <= 10.0 -> listOf("2L" to 2.0,  "5L" to 5.0,   "Full" to tankCap)
            tankCap <= 15.0 -> listOf("5L" to 5.0,  "10L" to 10.0, "Full" to tankCap)
            tankCap <= 25.0 -> listOf("7L" to 7.0,  "15L" to 15.0, "Full" to tankCap)
            else            -> listOf("10L" to 10.0, "20L" to 20.0, "Full" to tankCap)
        }
    }

    // Theme tokens — no hardcoded colors
    val primary          = MaterialTheme.colorScheme.primary
    val onPrimary        = MaterialTheme.colorScheme.onPrimary
    val surface          = MaterialTheme.colorScheme.surface
    val surfaceVariant   = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant   = MaterialTheme.colorScheme.outlineVariant
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val configuration = LocalConfiguration.current
    val isLandscape   = configuration.screenWidthDp > configuration.screenHeightDp

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

        // ── Header ───────────────────────────────────────────────────────────
        val header: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Add Fill-up",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    when {
                        distanceKm > 0.0 -> Text(
                            "Ride: +${"%.1f".format(distanceKm)} km  ·  Odo: ${"%.0f".format(lastOdometer)} km",
                            style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant
                        )
                        lastOdometer > 0.0 -> Text(
                            "Current odo: ${"%.0f".format(lastOdometer)} km",
                            style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant
                        )
                    }
                }
                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = surfaceVariant)
                ) { Icon(Icons.Default.ArrowBack, "Back") }
            }
        }

        // ── Fuel type tiles (accent border on selected only) ─────────────────
        val fuelTypeRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GloveFuelTypeTile(
                    name = petrolLabel, price = petrolPrice,
                    selected = selectedFuelType == petrolLabel,
                    icon = Icons.Default.LocalGasStation,
                    onClick = { selectedFuelType = petrolLabel },
                    modifier = Modifier.weight(1f)
                )
                GloveFuelTypeTile(
                    name = powerPetrolLabel, price = powerPetrolPrice,
                    selected = selectedFuelType == powerPetrolLabel,
                    icon = Icons.Default.Star,
                    onClick = { selectedFuelType = powerPetrolLabel },
                    modifier = Modifier.weight(1f),
                    isPremium = true
                )
            }
        }

        // ── Litres stepper — symmetric: [-1][-0.5][value][+0.5][+1] ─────────
        val fuelStepper: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(surface)
                    .border(1.dp, outlineVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "LITRES",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                // Symmetric stepper row — buttons weight(1f), value weight(2f)
                // so on 360dp phones each button ≈ 44dp × 64dp (meets 44dp floor)
                Row(
                    modifier = Modifier.fillMaxWidth().height(StepperButtonSize),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlobeStepButton(
                        label = "−1",
                        enabled = fuelLitres >= 1.0,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) { fuelLitres = (fuelLitres - 1.0).coerceAtLeast(0.0) }

                    GlobeStepButton(
                        label = "−0.1",
                        enabled = fuelLitres >= 0.1,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) { fuelLitres = ((fuelLitres - 0.1) * 10).toLong() / 10.0 }

                    Text(
                        text = "%.1f L".format(fuelLitres),
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = onSurface,
                        maxLines = 1
                    )

                    GlobeStepButton(
                        label = "+0.1",
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) { fuelLitres = ((fuelLitres + 0.1) * 10).toLong() / 10.0 }

                    GlobeStepButton(
                        label = "+1",
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) { fuelLitres += 1.0 }
                }

                // Quick-fill preset chips from vehicle tank capacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickFillPresets.forEach { (label, value) ->
                        val sel = fuelLitres == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(PresetChipHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (sel) primary.copy(alpha = 0.15f) else surfaceVariant
                                )
                                .border(
                                    width = if (sel) 2.dp else 1.dp,
                                    color = if (sel) primary else outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { fuelLitres = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) primary else onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Odometer stepper ─────────────────────────────────────────────────
        val odoStepper: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(surface)
                    .border(1.dp, outlineVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "ODOMETER",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant, letterSpacing = 1.5.sp
                        )
                        Text(
                            "%.0f km".format(odoKm),
                            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = onSurface
                        )
                        if (distanceKm > 0.0) {
                            Text(
                                "ride: +${"%.1f".format(distanceKm)} km",
                                style = MaterialTheme.typography.labelSmall, color = primary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlobeStepButton(
                            label = "−1",
                            enabled = odoKm > lastOdometer,
                            modifier = Modifier.size(OdometerNudgeSize)
                        ) { odoSynced = true; odoKm = (odoKm - 1.0).coerceAtLeast(lastOdometer) }
                        GlobeStepButton(
                            label = "+1",
                            modifier = Modifier.size(OdometerNudgeSize)
                        ) { odoSynced = true; odoKm += 1.0 }
                    }
                }
                // Quick-add chips — weight(1f) so +200 never overflows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("+10" to 10, "+50" to 50, "+100" to 100, "+200" to 200).forEach { (label, delta) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(surfaceVariant)
                                .border(1.dp, outlineVariant, RoundedCornerShape(10.dp))
                                .clickable { odoSynced = true; odoKm += delta },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Full / Partial segmented toggle — filled, no glowing frame ────────
        val fullPartialToggle: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SegmentedHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, outlineVariant, RoundedCornerShape(14.dp))
            ) {
                listOf("Full" to true, "Partial" to false).forEach { (label, isFull) ->
                    val sel = fullTank == isFull
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (sel) primary else surface)
                            .clickable { fullTank = isFull },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) onPrimary else onSurface
                        )
                    }
                }
            }
        }

        // ── Save bar — always primary, always shows live cost ─────────────────
        val saveBar: @Composable () -> Unit = {
            Button(
                onClick = {
                    if (fuelLitres > 0) viewModel.addEntry(
                        odoKm.coerceAtLeast(lastOdometer), fuelLitres, fullTank, selectedFuelType
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(SaveBarHeight),
                shape = RoundedCornerShape(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = onPrimary
                )
            ) {
                Text(
                    text = "Save · ₹${"%.0f".format(runningCost)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        }

        // ── Landscape layout ─────────────────────────────────────────────────
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                header()
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // LEFT: fuel type + litre stepper (scrollable if needed)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        fuelTypeRow()
                        fuelStepper()
                    }
                    // RIGHT: odo → toggle → [spacer] → save — FIXED, save pinned to bottom
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        odoStepper()
                        Spacer(Modifier.height(12.dp))
                        fullPartialToggle()
                        Spacer(Modifier.weight(1f))
                        saveBar()
                    }
                }
            }
        } else {
            // ── Portrait layout — scrollable content, save pinned to bottom ───
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    header()
                    fuelTypeRow()
                    fuelStepper()
                    odoStepper()
                    fullPartialToggle()
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(12.dp))
                saveBar()
                Spacer(Modifier.height(16.dp))
            }
        }
    } // Surface
}

// ── Step button — uniform look for all ± operations ──────────────────────────

@Composable
internal fun GlobeStepButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.size(StepperButtonSize),
    onClick: () -> Unit
) {
    var isHeld by remember { mutableStateOf(false) }

    LaunchedEffect(isHeld) {
        if (isHeld && enabled) {
            delay(350L)
            while (isHeld && enabled) {
                onClick()
                delay(80L)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    onClick()
                    isHeld = true
                    tryAwaitRelease()
                    isHeld = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f),
            maxLines = 1
        )
    }
}

// ── Fuel type tile — accent border on SELECTED only ──────────────────────────

@Composable
internal fun GloveFuelTypeTile(
    name: String,
    price: Double,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPremium: Boolean = false
) {
    val primary      = MaterialTheme.colorScheme.primary
    val surface      = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) primary.copy(alpha = 0.12f) else surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary else outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon, null,
            tint = if (selected) primary else onSurface,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            if (price > 0) {
                Text(
                    "₹${"%.2f".format(price)}/L",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) primary else onSurfaceVar
                )
            }
            if (isPremium) {
                Text(
                    "PREMIUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
