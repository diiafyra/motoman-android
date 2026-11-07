# Architecture

## Modules

```
:app  ──▶  :core:data  ──▶  :core:domain
  └─────────────────────────────▲
```

Arrows point from a module to what it depends on.

| Module | Type | Holds | Depends on |
| --- | --- | --- | --- |
| `:core:domain` | Kotlin/JVM | Models, repository interfaces, use cases, calculations | Nothing Android |
| `:core:data` | Android library | Room, DataStore, Retrofit, repository implementations | `:core:domain` |
| `:app` | Android application | Compose screens, ViewModels, navigation, Hilt modules, WorkManager | Both core modules |

`:core:domain` uses Kotlin explicit API mode, so anything the other modules can see is marked `public`
on purpose. `:core:data` does the same and exposes only what Hilt needs to bind.

## Package layout

```
app/src/main/java/io/github/diiafyra/motoman/
├── ui/designsystem/     theme and shared components
├── ui/navigation/       type-safe routes, bottom bar
├── feature/<name>/      Screen + ViewModel + UiState
└── work/                reminder worker

core/domain/src/main/kotlin/io/github/diiafyra/motoman/core/domain/
├── model/               Vehicle, FuelEntry, MaintenanceItem, MaintenanceRecord
├── fuel/                fuel economy (full-tank method), fill-up validation
├── maintenance/         current odometer, maintenance status
└── util/                Clock, IdGenerator

core/data/src/main/kotlin/io/github/diiafyra/motoman/core/data/
└── database/, network/, datastore/, repository/
```

## Domain rules

**Units.** Volume is stored in millilitres and money in dong, both as `Long`; odometer readings are
whole kilometres as `Int`. Sums over hundreds of fill-ups stay exact, and values become decimals only
when shown.

**Fuel economy** uses the full-tank method. A segment runs from one full fill-up to the next; its fuel
is every fill-up after the first, up to and including the second. A segment is dropped if any fill-up
in it is marked "missed previous", because some fuel was never logged. With no valid segment the app
asks for one more full tank instead of showing a wrong figure. The average is total fuel over total
distance, weighted by distance.

**Maintenance status.** The current odometer is the highest reading the app knows (initial reading,
fill-ups, maintenance records). An item is due at its last service plus its interval, by distance or by
months, whichever comes first:

| Status | When |
| --- | --- |
| No baseline | No last-done value for any interval the item has |
| Overdue | Past the due reading or the due date |
| Due soon | Within the threshold (default 500 km or 14 days) |
| Ok | Otherwise |

**Time and ids** come from the `Clock` and `IdGenerator` interfaces, so tests never depend on the real
clock or on random UUIDs.

## Rules that hold everywhere

- **Room is the source of truth.** The UI reads `Flow`s from Room. Network results are written to Room,
  or, for short-lived data such as nearby stations, returned directly but never pushed to the UI
  behind the database's back.
- **Errors are a type, not exceptions.** Repositories return a result with a classified error branch;
  ViewModels map it to a message through one shared mapper.
- **One immutable `UiState` per screen**, exposed as `StateFlow`. User actions are functions on the
  ViewModel.

Decisions that are hard to reverse are recorded as ADRs in `docs/adr/`, starting with the module split
and the storage of money and volume.
