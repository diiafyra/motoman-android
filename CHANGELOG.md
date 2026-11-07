# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
