package com.example.fuletracker.ux

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

    Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF05070A)) {
        if (isLandscape) {
            Box(Modifier.fillMaxSize().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RideMapPanel(currentLocation, Modifier.weight(1.65f).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) { dashboard() }
                }
                CloseRideButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
            }
        } else {
            Box(Modifier.fillMaxSize().padding(10.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RideMapPanel(currentLocation, Modifier.weight(1f).fillMaxWidth())
                    Box(Modifier.fillMaxWidth()) { dashboard() }
                }
                CloseRideButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }

    if (showAddEntry) {
        ModalBottomSheet(onDismissRequest = { showAddEntry = false }) {
            AddEntryScreen(
                viewModel = viewModel,
                onEntryAdded = { showAddEntry = false },
                onDismiss = { showAddEntry = false }
            )
        }
    }
}

@Composable
private fun RideMapPanel(location: Location?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101418))
    ) {
        AndroidView(
            factory = { context ->
                try {
                    Configuration.getInstance().userAgentValue = context.packageName
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(26.9124, 75.7873))
                        overlays.add(
                            Marker(this).apply {
                                id = "current_location"
                                title = "Current location"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                isEnabled = false
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e(RIDE_MODE_TAG, "Failed to create osmdroid map", e)
                    android.widget.FrameLayout(context).apply {
                        setBackgroundColor(android.graphics.Color.rgb(16, 20, 24))
                    }
                }
            },
            update = { map ->
                if (map is MapView) {
                    location?.let {
                        val point = GeoPoint(it.latitude, it.longitude)
                        val marker = map.overlays
                            .filterIsInstance<Marker>()
                            .firstOrNull { marker -> marker.id == "current_location" }
                        marker?.position = point
                        map.controller.animateTo(point)
                        map.invalidate()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
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
            RideActionButton(
                icon = Icons.Default.Navigation,
                label = "Maps",
                color = Color(0xFF4CE38A),
                modifier = Modifier.weight(1f),
                onClick = onOpenMaps
            )
            RideActionButton(
                icon = Icons.Default.LocalGasStation,
                label = "Log Fuel",
                color = Color(0xFFFFC857),
                modifier = Modifier.weight(1f),
                onClick = onLogFuel
            )
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
            containerColor = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White
        )
    ) {
        Icon(Icons.Default.Close, contentDescription = "Close ride mode")
    }
}

@Composable
private fun RideSpeedCard(speed: Float, distanceKm: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111820)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = speed.toInt().coerceAtLeast(0).toString(),
                        fontSize = 72.sp,
                        lineHeight = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = " km/h",
                        modifier = Modifier.padding(bottom = 10.dp),
                        color = Color(0xFF9CA7B2),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Ride distance",
                    color = Color(0xFF75808B),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "%.2f km".format(distanceKm),
                color = Color(0xFF4CE38A),
                fontSize = 24.sp,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151A20)),
        shape = RoundedCornerShape(10.dp)
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        mediaState.artist,
                        color = Color(0xFF9CA7B2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh media", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, enabled = mediaState.controller != null) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                FilledIconButton(onClick = onPlayPause, enabled = mediaState.controller != null) {
                    Icon(
                        if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play pause",
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(onClick = onNext, enabled = mediaState.controller != null) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                if (!hasMediaAccess) {
                    TextButton(onClick = onSettings) {
                        Text("Access")
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF251B12)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = Color(0xFFFFC857))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(message, color = Color(0xFFD2C2A8), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun RideActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontWeight = FontWeight.Bold)
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
        RideMediaState(title = "Media access needed", artist = "Tap Access once and allow Fule Tracker")
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
