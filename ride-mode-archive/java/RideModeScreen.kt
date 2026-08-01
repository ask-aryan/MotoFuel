package com.example.fueltracker.ux

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fueltracker.R
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RideMediaNotificationListener : NotificationListenerService()

private data class RideMediaState(
    val title: String = "No media playing",
    val artist: String = "Open Spotify or YT Music",
    val packageName: String? = null,
    val isPlaying: Boolean = false,
    val controller: MediaController? = null
)

private const val RIDE_MODE_TAG = "RideMode"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideModeScreen(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var speed by remember { mutableStateOf(0f) }
    var distanceMeters by remember { mutableStateOf(0.0) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var showAddEntry by remember { mutableStateOf(false) }
    var hasLocationAccess by remember { mutableStateOf(hasLocationPermission(context)) }
    var locationDenied by remember { mutableStateOf(false) }
    var hasMediaAccess by remember { mutableStateOf(hasNotificationListenerAccess(context)) }
    var mediaState by remember { mutableStateOf(loadMediaState(context)) }

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                currentLocation = location
                speed = if (location.hasSpeed()) location.speed * 3.6f else 0f
                lastLocation?.let { previous ->
                    val distance = previous.distanceTo(location)
                    if (distance > 1.0) distanceMeters += distance
                }
                lastLocation = location
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationAccess = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationDenied = !hasLocationAccess
        if (hasLocationAccess) {
            startLocationUpdates(locationClient, locationCallback)
        }
    }

    DisposableEffect(Unit) {
        if (hasLocationAccess) {
            startLocationUpdates(locationClient, locationCallback)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        onDispose { locationClient.removeLocationUpdates(locationCallback) }
    }

    DisposableEffect(mediaState.controller) {
        val controller = mediaState.controller ?: return@DisposableEffect onDispose {}
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                mediaState = mediaState.copy(isPlaying = state?.state == PlaybackState.STATE_PLAYING)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaState = mediaState.copy(
                    title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: mediaState.title,
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: mediaState.artist
                )
            }
        }
        controller.registerCallback(callback)
        onDispose { controller.unregisterCallback(callback) }
    }

    val dashboard = @Composable {
        RideDashboardPanel(
            speed = speed,
            distanceKm = distanceMeters / 1000.0,
            hasLocationAccess = hasLocationAccess,
            locationDenied = locationDenied,
            mediaState = mediaState,
            hasMediaAccess = hasMediaAccess,
            onMediaRefresh = {
                hasMediaAccess = hasNotificationListenerAccess(context)
                mediaState = loadMediaState(context)
            },
            onMediaSettings = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onLocationSettings = { openAppSettings(context) },
            onPlayPause = {
                val controls = mediaState.controller?.transportControls
                if (mediaState.isPlaying) controls?.pause() else controls?.play()
                mediaState = loadMediaState(context)
            },
            onPrevious = {
                mediaState.controller?.transportControls?.skipToPrevious()
                mediaState = loadMediaState(context)
            },
            onNext = {
                mediaState.controller?.transportControls?.skipToNext()
                mediaState = loadMediaState(context)
            },
            onOpenMaps = { openMaps(context, currentLocation) },
            onLogFuel = { showAddEntry = true }
        )
    }

    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (isLandscape) {
            Box(Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RideMapPanel(currentLocation, Modifier.weight(1.65f).fillMaxHeight())
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RideStatusPill(vehicleName = selectedVehicle?.name)
                        dashboard()
                    }
                }
                CloseRideButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
            }
        } else {
            Box(Modifier.fillMaxSize().padding(10.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RideStatusPill(vehicleName = selectedVehicle?.name)
                    RideMapPanel(currentLocation, Modifier.weight(1f).fillMaxWidth())
                    // Dashboard shares equal weight with map so it always gets bounded height;
                    // verticalScroll lets it scroll on small screens instead of clipping the button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) { dashboard() }
                }
                CloseRideButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    } // Surface

    AnimatedVisibility(
        visible = showAddEntry,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        GloveFuelPage(
            viewModel = viewModel,
            distanceKm = distanceMeters / 1000.0,
            onDismiss = { showAddEntry = false }
        )
    }
    } // Box
}

@Composable
private fun RideStatusPill(vehicleName: String?) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
        )
        Text(
            text = "RIDING",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!vehicleName.isNullOrBlank()) {
            Text(
                text = vehicleName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RideMapPanel(location: Location?, modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var destinationPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeInfo by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        if (searchQuery.isBlank() || isLoading) return
        scope.launch {
            isLoading = true
            val dest = geocodeDestination(searchQuery)
            if (dest != null) {
                destinationPoint = dest
                val fromLoc = location
                if (fromLoc != null) {
                    val result = fetchOsrmRoute(
                        GeoPoint(fromLoc.latitude, fromLoc.longitude), dest
                    )
                    if (result != null) {
                        routePoints = result.first
                        routeInfo = result.second
                    }
                }
                isSearchActive = false
            }
            isLoading = false
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    try {
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(GeoPoint(26.9124, 75.7873))
                            overlays.add(
                                Marker(this).apply {
                                    id = "current_location"
                                    title = "You"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    isEnabled = false
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(RIDE_MODE_TAG, "Failed to create osmdroid map", e)
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.rgb(21, 23, 26))
                        }
                    }
                },
                update = { view ->
                    if (view is MapView) {
                        val toRemove = view.overlays.filter {
                            it is org.osmdroid.views.overlay.Polyline ||
                                (it is Marker && it.id == "destination")
                        }
                        view.overlays.removeAll(toRemove)

                        if (routePoints.isNotEmpty()) {
                            org.osmdroid.views.overlay.Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#4285F4")
                                outlinePaint.strokeWidth = 14f
                                outlinePaint.isAntiAlias = true
                                view.overlays.add(0, this)
                            }
                        }

                        destinationPoint?.let { dest ->
                            Marker(view).apply {
                                id = "destination"
                                position = dest
                                title = "Destination"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                view.overlays.add(this)
                            }
                        }

                        location?.let { loc ->
                            val point = GeoPoint(loc.latitude, loc.longitude)
                            view.overlays.filterIsInstance<Marker>()
                                .firstOrNull { it.id == "current_location" }
                                ?.let {
                                    it.position = point
                                    it.isEnabled = true
                                }
                            if (routePoints.isEmpty()) view.controller.animateTo(point)
                        }

                        view.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Search bar / search trigger
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search destination…") },
                    singleLine = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { doSearch() }) {
                                        Icon(Icons.Default.Search, "Search",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = { isSearchActive = false }) {
                                    Icon(Icons.Default.Close, "Cancel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() })
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable { isSearchActive = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Search, "Search destination",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Search destination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Route info chip
            routeInfo?.let { info ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            info,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                destinationPoint = null
                                routePoints = emptyList()
                                routeInfo = null
                                searchQuery = ""
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, "Clear route",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RideDashboardPanel(
    speed: Float,
    distanceKm: Double,
    hasLocationAccess: Boolean,
    locationDenied: Boolean,
    mediaState: RideMediaState,
    hasMediaAccess: Boolean,
    onMediaRefresh: () -> Unit,
    onMediaSettings: () -> Unit,
    onLocationSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenMaps: () -> Unit,
    onLogFuel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasLocationAccess && locationDenied) {
            RidePermissionCard(
                icon = Icons.Default.LocationOff,
                title = "Location is off",
                message = "Enable location for live speed and map tracking.",
                action = "Settings",
                onClick = onLocationSettings
            )
        }
        RideSpeedCard(speed = speed, distanceKm = distanceKm)
        RideMediaCard(
            mediaState = mediaState,
            hasMediaAccess = hasMediaAccess,
            onRefresh = onMediaRefresh,
            onSettings = onMediaSettings,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RideMapsButton(modifier = Modifier.weight(1f), onClick = onOpenMaps)
            RideLogFuelButton(modifier = Modifier.weight(1f), onClick = onLogFuel)
        }
    }
}

@Composable
private fun CloseRideButton(onClose: (() -> Unit)?, modifier: Modifier = Modifier) {
    if (onClose == null) return
    FilledIconButton(
        onClick = onClose,
        modifier = modifier.size(44.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(Icons.Default.Close, contentDescription = "Close ride mode")
    }
}

@Composable
private fun RideSpeedCard(speed: Float, distanceKm: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = speed.toInt().coerceAtLeast(0).toString(),
                        fontSize = 76.sp,
                        lineHeight = 76.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " km/h",
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "DISTANCE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp
                )
            }
            Text(
                text = "%.2f km".format(distanceKm),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RideMediaCard(
    mediaState: RideMediaState,
    hasMediaAccess: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = Color(0xFF7DD3FC))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        mediaState.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        mediaState.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh media",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, enabled = mediaState.controller != null) {
                    Icon(Icons.Default.SkipPrevious, "Previous",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = mediaState.controller != null,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play pause",
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onNext, enabled = mediaState.controller != null) {
                    Icon(Icons.Default.SkipNext, "Next",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                }
                if (!hasMediaAccess) {
                    TextButton(onClick = onSettings) {
                        Text("Access", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RidePermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    action: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                Text(message, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClick) { Text(action, color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun RideMapsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(Icons.Default.Navigation, contentDescription = null,
            tint = Color(0xFF4CE38A), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Maps", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RideLogFuelButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(Icons.Default.LocalGasStation, contentDescription = null,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Log Fuel", fontWeight = FontWeight.ExtraBold)
    }
}

private fun loadMediaState(context: Context): RideMediaState {
    if (!hasNotificationListenerAccess(context)) {
        return RideMediaState(title = "Media access needed", artist = "Allow once in Android settings")
    }
    val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    val component = ComponentName(context, RideMediaNotificationListener::class.java)
    return try {
        val controller = manager.getActiveSessions(component).firstOrNull()
            ?: return RideMediaState()
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        RideMediaState(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: controller.packageName,
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: controller.packageName,
            packageName = controller.packageName,
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = controller
        )
    } catch (e: SecurityException) {
        RideMediaState(title = "Media access needed", artist = "Tap Access once and allow Fuel Tracker")
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

private fun hasNotificationListenerAccess(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ).orEmpty()
    return enabled.split(":").any { component ->
        ComponentName.unflattenFromString(component)?.packageName == context.packageName
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    )
}

private fun startLocationUpdates(
    client: FusedLocationProviderClient,
    callback: LocationCallback
) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(500L)
        .build()

    try {
        client.requestLocationUpdates(request, callback, null)
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

private suspend fun geocodeDestination(query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val conn = java.net.URL(
            "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
        ).openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("User-Agent", "FuelTracker/1.0 Android")
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        val arr = org.json.JSONArray(conn.inputStream.bufferedReader().readText())
        if (arr.length() > 0) {
            val obj = arr.getJSONObject(0)
            GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
        } else null
    } catch (e: Exception) {
        Log.e(RIDE_MODE_TAG, "Geocoding failed: ${e.message}")
        null
    }
}

private suspend fun fetchOsrmRoute(from: GeoPoint, to: GeoPoint): Pair<List<GeoPoint>, String>? =
    withContext(Dispatchers.IO) {
        try {
            val conn = java.net.URL(
                "https://router.project-osrm.org/route/v1/driving/" +
                    "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                    "?overview=full&geometries=geojson"
            ).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "FuelTracker/1.0 Android")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
            if (json.optString("code") != "Ok") return@withContext null
            val route = json.getJSONArray("routes").getJSONObject(0)
            val distKm = route.getDouble("distance") / 1000.0
            val durMin = route.getDouble("duration") / 60.0
            val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
            val points = (0 until coords.length()).map { i ->
                val c = coords.getJSONArray(i)
                GeoPoint(c.getDouble(1), c.getDouble(0))
            }
            val info = if (durMin >= 60)
                "%.1f km · %dh %dmin".format(distKm, (durMin / 60).toInt(), (durMin % 60).toInt())
            else
                "%.1f km · %.0f min".format(distKm, durMin)
            Pair(points, info)
        } catch (e: Exception) {
            Log.e(RIDE_MODE_TAG, "OSRM routing failed: ${e.message}")
            null
        }
    }

private fun openMaps(context: Context, location: Location?) {
    val uri = location?.let {
        "google.navigation:q=${it.latitude},${it.longitude}"
    } ?: "google.navigation:q="
    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = location?.let {
            "geo:${it.latitude},${it.longitude}?q=${it.latitude},${it.longitude}"
        } ?: "geo:0,0"
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fallback)))
    }
}

