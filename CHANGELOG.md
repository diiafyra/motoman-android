# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Network layer: OkHttp with timeouts, retry for GET only, build-type logging, and failures as values.
- Overpass client for fuel stations and garages around a point, used by the Nearby screen next.

## [0.5.0] - 2026-11-08

### Added

- Bottom navigation with Overview, Fuel, Maintenance and Nearby tabs, using type-safe routes.
- Add, edit and delete vehicles. Deleting asks for confirmation and removes the vehicle's history.
- The selected vehicle is shown in the top bar and remembered across launches; a new vehicle becomes
  the selected one.
- Design system components: text field with per-field errors, empty state, loading state, confirm
  dialog.
- Fuel log: fill-ups grouped by month with monthly cost and volume, a form with date, odometer, litres,
  amount paid, full-tank and missed-fill flags, and the price per litre shown while typing.
- Deleting a fill-up by swiping, with undo; also offered as a TalkBack action.
- Overview tab: latest and average fuel economy, cost per km, this month's spending, odometer, and the
  most urgent maintenance items with an icon and label for their status.
- Maintenance tab: items sorted by urgency, mark done with date, odometer, cost and note, add, edit
  and delete items, and the service history of each item.
- A new vehicle starts with the usual maintenance items for its type, with a note to check the
  owner's manual.
- Settings: app language (phone, Tiếng Việt, English), reminders on or off, version and privacy policy.
- Per-app language on every supported Android version.
- A UI test of the main flow, adding a vehicle and logging a fill-up, run on every pull request.
- Maintenance reminders: a daily check notifies once when an item is due soon and once more if it
  becomes overdue, for every vehicle. Android 13+ asks for notification permission from the
  Maintenance tab.
- Statistics: spending and fuel economy per month for six months, as bar charts with a table of the
  exact values.

## [0.1.0] - 2026-11-08

### Added

- Project skeleton with `:app`, `:core:domain` and `:core:data` modules.
- Material 3 theme with dynamic color and dark mode.
- ktlint, Android Lint and a GitHub Actions workflow that runs them with unit tests and a debug build
  on every pull request.
- Auto Backup of app data, so logs survive a phone change before sync exists.
- Domain models for vehicles, fill-ups and maintenance.
- Fuel economy (L/100 km and cost per km) by the full-tank method, skipping segments with a missed
  fill-up.
- Fill-up validation: odometer must stay between the fill-ups before and after it.
- Maintenance status by distance or time, whichever comes first.
- Room database with vehicles, fill-ups, maintenance items and records. Rows are soft-deleted, carry
  client-generated UUIDs and timestamps for sync, and the schema is exported to `core/data/schemas/`
  (database version 1).
- Architecture decision records for the module split, integer units and the full-tank method.

### Changed

- The project builds with JDK 21, which Robolectric needs for the Android 16 sandbox.

[Unreleased]: https://github.com/diiafyra/motoman-android/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/diiafyra/motoman-android/compare/v0.1.0...v0.5.0
[0.1.0]: https://github.com/diiafyra/motoman-android/releases/tag/v0.1.0
