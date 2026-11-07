# 0001. Three modules instead of one module per feature

Status: accepted

## Context

The app has four features (fuel log, maintenance, overview, nearby) and one developer. I wanted the
layer boundaries checked by the compiler, not by discipline: a ViewModel must not reach a Room DAO, and
the fuel economy calculation must not depend on Android.

The common large-app layout splits every feature into its own Gradle module, often with separate API
and implementation modules. That gives fast incremental builds and clear ownership in a big team.

## Decision

Three modules:

- `:core:domain`, plain Kotlin/JVM: models, repository interfaces, calculations.
- `:core:data`, Android library: Room, DataStore, Retrofit, repository implementations.
- `:app`, Android application: Compose screens, ViewModels, navigation, WorkManager.

Inside `:app`, features are packages (`feature/fuel`, `feature/maintenance`), not modules. The data
module uses Kotlin explicit API mode and keeps implementations `internal`; only its Hilt module binds
them to the domain interfaces.

## Alternatives

- One module. Rejected: nothing would stop `:app` code from importing Room entities, and the domain
  tests would need Robolectric or an emulator.
- One module per feature. Rejected for now: four features do not pay for the extra build files,
  navigation contracts and shared-dependency wiring. The package layout keeps that split cheap later.

## Consequences

- Domain tests run on the plain JVM in seconds.
- Breaking a layer rule is a compile error, not a review comment.
- A change in `:core:data` rebuilds `:app`; acceptable at this size.
