package com.example.fuletracker

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fuletracker.ui.theme.FuleTrackerTheme
import com.example.fuletracker.data.Vehicle
import com.example.fuletracker.ux.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.fuletracker.worker.ReminderScheduler
import com.example.fuletracker.widget.FuelWidgetProvider

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
            FuleTrackerTheme {
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

    Scaffold(
        topBar = {
            AppHeader(
                selectedTab = selectedTab,
                title = currentTitle,
                vehicles = vehicles,
                selectedVehicle = selectedVehicle,
                settingsTitle = settingsTitle,
                onVehicleSelected = { viewModel.selectVehicle(it) },
                onSettingsClick = { selectedTab = 4 }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = dashboardTitle) },
                    label = { Text(dashboardTitle) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = statsTitle) },
                    label = { Text(statsTitle) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Build, contentDescription = diaryTitle) },
                    label = { Text(diaryTitle) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { context.startActivity(Intent(context, RidingModeActivity::class.java)) },
                    icon = { Icon(Icons.Default.DirectionsBike, contentDescription = rideTitle) },
                    label = { Text(rideTitle) }
                )
            }
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
}

@Composable
private fun AppHeader(
    selectedTab: Int,
    title: String,
    vehicles: List<Vehicle>,
    selectedVehicle: Vehicle?,
    settingsTitle: String,
    onVehicleSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedTab in 0..2 && vehicles.isNotEmpty()) {
                CompactVehicleSelector(
                    vehicles = vehicles,
                    selectedVehicle = selectedVehicle,
                    onVehicleSelected = onVehicleSelected,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = settingsTitle,
                    tint = if (selectedTab == 4) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    }
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.heightIn(min = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedVehicle?.name ?: stringResource(R.string.select_vehicle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    selectedVehicle?.licensePlate?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = { Text(vehicle.name) },
                    onClick = {
                        onVehicleSelected(vehicle.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
