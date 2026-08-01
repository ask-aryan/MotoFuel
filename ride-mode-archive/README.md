# Ride Mode (parked for future use)

Pulled out of `app/src/main` on 2026-08-01. The feature was already disconnected
from the running app (no `RidingModeActivity` entry in `AndroidManifest.xml`,
no navigation wired up in `MainActivity.kt`), so removing it was low-risk.

## What's here
- `java/RidingModeActivity.kt` — hosts the ride mode Compose screen (immersive/fullscreen setup).
- `java/RideModeScreen.kt` — the ride mode UI: live speed, distance, media controls,
  osmdroid map panel, notification listener for media state.
- `res-layout/activity_riding_mode.xml` — an older XML-layout variant of the same dashboard,
  not wired to any Activity's `setContentView`.

## To bring it back
1. Move the files back into `app/src/main/java/com/example/fueltracker/` (`RidingModeActivity.kt`
   at the package root, `RideModeScreen.kt` under `ux/`) and `res/layout/` for the XML layout.
2. Re-add to `app/build.gradle.kts` dependencies:
   ```
   implementation("com.google.android.gms:play-services-location:21.3.0")
   implementation("org.osmdroid:osmdroid-android:6.1.20")
   ```
3. Re-add to `AndroidManifest.xml`: an `<activity android:name=".RidingModeActivity">` entry,
   `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` permissions, and a
   `<service>` entry for `RideMediaNotificationListener` (extends `NotificationListenerService`,
   requires `BIND_NOTIFICATION_LISTENER_SERVICE`).
4. Re-add the `Theme.FuelTracker.RidingMode` style (fullscreen, dark nav/status bar `#0D0D0D`)
   to `res/values/themes.xml`.
5. Re-add these strings to `res/values/strings.xml`:
   ```xml
   <string name="nav_ride_mode">Ride</string>
   <string name="ride_mode_title">Ride Mode</string>
   <string name="ride_mode_speed_label">km/h</string>
   <string name="ride_mode_distance_label">Distance</string>
   <string name="ride_mode_maps">Maps</string>
   <string name="ride_mode_log_fuel">Log Fuel</string>
   <string name="ride_mode_speed_unit">km/h</string>
   <string name="ride_mode_distance_unit">km</string>
   ```
6. Wire up navigation/launch from `MainActivity.kt` (there wasn't any at the time this was archived).
