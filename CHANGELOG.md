# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[Unreleased]: https://github.com/diiafyra/motoman-android/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/diiafyra/motoman-android/releases/tag/v0.1.0
