package com.example.fueltracker

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fueltracker.ui.theme.AppTheme
import com.example.fueltracker.ui.theme.FuelTrackerTheme
import com.example.fueltracker.data.Vehicle
import com.example.fueltracker.ux.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.fueltracker.worker.ReminderScheduler
import com.example.fueltracker.widget.FuelWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: FuelViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scheduleReminders()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> scheduleReminders()
                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleReminders()
        }
    }

    private fun scheduleReminders() {
        ReminderScheduler.scheduleWeeklyReminder(this)
        ReminderScheduler.scheduleInactivityReminder(this)
        ReminderScheduler.schedulePriceReminder(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        
        // Ensure widget is up to date when app starts
        FuelWidgetProvider.triggerUpdate(this)
        
        // Handle widget action if the app was opened from "Log Fill-up"
        if (intent?.getBooleanExtra("ACTION_QUICK_ADD", false) == true) {
            viewModel.triggerAddEntry()
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            FuelTrackerTheme(appTheme = appTheme) {
                MainScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh widget data whenever returning to the app
        FuelWidgetProvider.triggerUpdate(this)
    }
}

@Composable
fun MainScreen(viewModel: FuelViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val vehicles by viewModel.allVehicles.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val dashboardTitle = stringResource(R.string.nav_dashboard)
    val statsTitle = stringResource(R.string.nav_stats)
    val diaryTitle = stringResource(R.string.nav_diary)
    val rideTitle = stringResource(R.string.nav_ride_mode)
    val settingsTitle = stringResource(R.string.nav_settings)
    val currentTitle = when (selectedTab) {
        1 -> statsTitle
        2 -> diaryTitle
        3 -> rideTitle
        4 -> settingsTitle
        else -> dashboardTitle
    }
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    val onRideClick = { context.startActivity(Intent(context, RidingModeActivity::class.java)) }

    if (isLandscape) {
        // Landscape: content + nav rail on right
        Row(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    AppHeader(
                        selectedTab = selectedTab,
                        title = currentTitle,
                        vehicles = vehicles,
                        selectedVehicle = selectedVehicle,
                        settingsTitle = settingsTitle,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        onSettingsClick = { selectedTab = 4 },
                        onAddVehicle = { selectedTab = 4 }
                    )
                }
            ) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)
                when (selectedTab) {
                    0 -> DashboardScreen(viewModel = viewModel, modifier = modifier)
                    1 -> StatsScreen(viewModel = viewModel, modifier = modifier)
                    2 -> DiaryScreen(viewModel = viewModel, modifier = modifier)
                    4 -> SettingsScreen(viewModel = viewModel, modifier = modifier)
                }
            }
            LandscapeNavRail(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onRideClick = onRideClick,
                onSettingsClick = { selectedTab = 4 },
                settingsTitle = settingsTitle
            )
        }
    } else {
        // Portrait: floating pill over content
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    AppHeader(
                        selectedTab = selectedTab,
                        title = currentTitle,
                        vehicles = vehicles,
                        selectedVehicle = selectedVehicle,
                        settingsTitle = settingsTitle,
                        onVehicleSelected = { viewModel.selectVehicle(it) },
                        onSettingsClick = { selectedTab = 4 },
                        onAddVehicle = { selectedTab = 4 }
                    )
                }
            ) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)
                when (selectedTab) {
                    0 -> DashboardScreen(viewModel = viewModel, modifier = modifier)
                    1 -> StatsScreen(viewModel = viewModel, modifier = modifier)
                    2 -> DiaryScreen(viewModel = viewModel, modifier = modifier)
                    4 -> SettingsScreen(viewModel = viewModel, modifier = modifier)
                }
            }

            val navBg = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, navBg.copy(alpha = 0.96f))
                        )
                    )
            )

            PillNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onRideClick = onRideClick,
                dashboardTitle = dashboardTitle,
                statsTitle = statsTitle,
                diaryTitle = diaryTitle,
                rideTitle = rideTitle,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LandscapeNavRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    settingsTitle: String
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    data class RailItem(val index: Int, val icon: ImageVector, val label: String)

    val items = listOf(
        RailItem(0, Icons.Default.Home, "Home"),
        RailItem(1, Icons.Default.BarChart, "Stats"),
        RailItem(2, Icons.Default.MenuBook, "Diary"),
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = outlineVariant.copy(alpha = 0.5f),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            val isSelected = item.index == selectedTab
            val animSpec = tween<Color>(durationMillis = 250)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) primary else Color.Transparent,
                animationSpec = animSpec,
                label = "railBg"
            )
            val iconTint by animateColorAsState(
                targetValue = if (isSelected) onPrimary else onSurfaceVariant,
                animationSpec = animSpec,
                label = "railTint"
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(item.index) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Ride mode
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surface)
                .border(1.dp, outlineVariant, RoundedCornerShape(14.dp))
                .clickable { onRideClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DirectionsBike,
                contentDescription = "Ride",
                tint = onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Settings
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selectedTab == 4) primary else Color.Transparent)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = settingsTitle,
                tint = if (selectedTab == 4) onPrimary else onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PillNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRideClick: () -> Unit,
    dashboardTitle: String,
    statsTitle: String,
    diaryTitle: String,
    rideTitle: String,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    data class NavItem(val index: Int, val icon: ImageVector, val label: String, val onClick: () -> Unit)

    val items = listOf(
        NavItem(0, Icons.Default.Home, dashboardTitle) { onTabSelected(0) },
        NavItem(1, Icons.Default.BarChart, statsTitle) { onTabSelected(1) },
        NavItem(2, Icons.Default.MenuBook, diaryTitle) { onTabSelected(2) },
        NavItem(-1, Icons.Default.DirectionsBike, rideTitle) { onRideClick() }
    )

    // Container is fully transparent — no background here at all
    // The gradient fade lives in MainScreen as a separate layer behind this
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 14.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(surface.copy(alpha = 0.94f))
                .border(1.dp, outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.index == selectedTab
                val animSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 250)
                val tabBg by animateColorAsState(
                    targetValue = if (isSelected) primary else Color.Transparent,
                    animationSpec = animSpec,
                    label = "tabBg"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) onPrimary else onSurfaceVariant,
                    animationSpec = animSpec,
                    label = "iconTint"
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(44.dp))
                        .background(tabBg)
                        .clickable { item.onClick() }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(200)) + expandHorizontally(tween(250), expandFrom = Alignment.Start),
                        exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200), shrinkTowards = Alignment.Start)
                    ) {
                        Text(
                            item.label,
                            color = onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    selectedTab: Int,
    title: String,
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    settingsTitle: String,
    onVehicleSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onAddVehicle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedTab in 0..2 && vehicles.isNotEmpty()) {
                CompactVehicleSelector(
                    vehicles = vehicles,
                    selectedVehicle = selectedVehicle,
                    onVehicleSelected = onVehicleSelected,
                    onAddVehicle = onAddVehicle,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = settingsTitle,
                    tint = if (selectedTab == 4) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactVehicleSelector(
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    onVehicleSelected: (Int) -> Unit,
    onAddVehicle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.widthIn(min = 60.dp, max = 160.dp)) {
                Text(
                    text = selectedVehicle?.name ?: stringResource(R.string.select_vehicle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                selectedVehicle?.licensePlate?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            vehicles.forEach { vehicle ->
                val isSelected = vehicle.id == selectedVehicle?.id
                DropdownMenuItem(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) primary.copy(alpha = 0.10f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        ),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                vehicle.name,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (vehicle.licensePlate.isNotBlank()) {
                                Text(
                                    vehicle.licensePlate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingIcon = if (isSelected) ({
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }) else null,
                    onClick = {
                        onVehicleSelected(vehicle.id)
                        expanded = false
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            DropdownMenuItem(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                text = {
                    Text(
                        stringResource(R.string.add_vehicle_desc),
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Add, null, tint = primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                    expanded = false
                    onAddVehicle()
                }
            )
        }
    }
}
