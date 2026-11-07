# Sổ Xe

An Android app for motorbike and car owners in Vietnam: log every fill-up, see real fuel economy and
cost per kilometre, and get reminded before maintenance is due. Everything works offline; the network
only adds things on top, such as nearby fuel stations and garages.

> Status: early development. The project skeleton is in place; features land one pull request at a time.
> See [CHANGELOG.md](CHANGELOG.md).

## Features

Planned for v1.0:

- Several vehicles, one selected at a time
- Fuel log with full-tank and missed-fill flags
- Fuel economy (L/100 km), cost per km and monthly charts
- Maintenance items due by distance or by time, with reminders
- Nearby fuel stations and garages on a map
- Vietnamese and English

## Architecture

Three Gradle modules, so the compiler enforces the boundaries:

```
:app  ──▶  :core:data  ──▶  :core:domain
  └─────────────────────────────▲
```

- `:core:domain` is plain Kotlin with no Android dependency. Models, repository interfaces and the
  calculations that are easiest to get wrong live here and are tested on the JVM.
- `:core:data` implements the repositories with Room, DataStore and Retrofit.
- `:app` holds the Compose UI, ViewModels and Hilt wiring. ViewModels never see Room or Retrofit.

Details in [docs/architecture.md](docs/architecture.md).

## Tech stack

Each library is here because a feature needs it.

| Area | Choice |
| --- | --- |
| Language | Kotlin, Coroutines |
| UI | Jetpack Compose, Material 3, single activity |
| Dependency injection | Hilt |
| Build | Gradle version catalog, KSP |
| Code quality | ktlint, Android Lint, GitHub Actions on every pull request |

Room, DataStore, Navigation Compose, Retrofit, OkHttp, WorkManager, Maps Compose and Firebase are added
by the features that use them.

## Build

Requirements: JDK 17 and the Android SDK with platform 37.

```sh
./gradlew assembleDebug          # build the debug APK
./gradlew test                   # unit tests
./gradlew ktlintCheck lint       # style and static checks, same as CI
```

No secrets are needed yet. See [docs/setup.md](docs/setup.md) for local configuration.

## License

[Apache License 2.0](LICENSE)
