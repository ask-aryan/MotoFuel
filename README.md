# MotoFuel

A fuel and maintenance tracker for bikes, built with Kotlin and Jetpack Compose.

Log every fill-up, track mileage over time, keep a service diary, and see it
all on your home screen — no account, no cloud, no ads. Your data stays on
your device.

## Features

- **Fill-up logging** — odometer, fuel amount, fuel type (Petrol, Power
  Petrol, Diesel, CNG, Electric), full-tank vs. partial-fill tracking, and
  live cost/efficiency previews as you type.
- **Multi-vehicle garage** — track fuel and maintenance separately for as
  many bikes as you own, with a quick vehicle switcher in the header.
- **Stats & insights** — mileage trend, fuel price history, and monthly
  expense charts, plus smart insights like "mileage improved 8% vs last
  month."
- **Maintenance diary** — log chain lube, oil changes, services, and any
  custom maintenance entry against an odometer reading.
- **Home screen widgets** — an at-a-glance widget for spend/distance/fuel,
  plus a monthly summary widget, with a one-tap "Log Fill-up" shortcut.
- **Backup & restore** — export your data to a file (share it via Drive,
  WhatsApp, email, wherever), import it back later, and merge or replace on
  import. An optional daily auto-backup runs in the background.
- **Reminders** — weekly fill-up nudges, an inactivity check-in, and a
  monthly fuel price reminder.
- **Two themes** — **Pit Lane** (sporty, carbon black & electric lime) and
  **Daylight** (friendly, warm cream & deep violet), switchable from
  Settings.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room (local persistence)
- WorkManager (reminders, scheduled backups)
- MPAndroidChart (stats charts)
- Coil (image loading)

## Building

Open the project in Android Studio (or run `./gradlew assembleDebug` from the
CLI). Minimum SDK 24, target/compile SDK 35.

## Project layout

```
app/src/main/java/com/example/fueltracker/
├── data/       # Room entities, DAOs, repository, mileage/stats calculators
├── ux/         # Compose screens (Dashboard, Stats, Diary, Settings, etc.)
├── widget/     # Home screen widget providers
├── worker/     # WorkManager jobs (reminders, backups)
└── ui/theme/   # Pit Lane / Daylight theming
```

`ride-mode-archive/` holds a parked, work-in-progress "Ride Mode" feature
(live speed/distance/map dashboard for use while riding) that isn't wired
into the app yet — see its README for how to bring it back.

## Status

Not yet on Google Play. In the meantime, grab a build from
[Releases](https://github.com/ask-aryan/MotoFuel/releases).

## License

TBD.
