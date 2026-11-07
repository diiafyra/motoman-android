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
├── ui/                  MotoManApp shell (top bar, bottom bar), AppViewModel
├── ui/designsystem/     theme; MotoManTextField, EmptyState, LoadingState, ConfirmDialog
├── ui/navigation/       type-safe routes, the four tabs, NavHost
├── ui/format/           number formatting and labels for domain enums
├── feature/<name>/      Screen + ViewModel + UiState
└── work/                reminder worker

core/domain/src/main/kotlin/io/github/diiafyra/motoman/core/domain/
├── model/               Vehicle, FuelEntry, MaintenanceItem, MaintenanceRecord
├── fuel/                fuel economy (full-tank method), fill-up validation
├── maintenance/         current odometer, maintenance status, recording a service
├── vehicle/             vehicle form validation
├── usecase/             ObserveSelectedVehicle
├── repository/          interfaces implemented by :core:data
└── util/                Clock, IdGenerator

core/data/src/main/kotlin/io/github/diiafyra/motoman/core/data/
├── database/            Room database, entities, mappers, DAOs
├── datastore/           preferences (selected vehicle)
├── repository/          repository implementations
├── time/                SystemClock, UuidIdGenerator
└── di/                  Hilt bindings from implementations to domain interfaces
core/data/schemas/       exported Room schemas, one JSON file per version
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

## Screens and navigation

One activity hosts `MotoManApp`: a top bar with the selected vehicle, a bottom bar with four tabs
(Overview, Fuel, Maintenance, Nearby) and a `NavHost`. Routes are `@Serializable` classes, so
arguments are typed (`VehicleEditRoute(vehicleId)`), never assembled as strings. Screens outside the
tabs (vehicle list and form) draw their own top bar; the shell hides its bars there.

Every tab works on one vehicle. `ObserveSelectedVehicle` combines the vehicle list with the id stored
in DataStore and falls back to the oldest vehicle when nothing was chosen or the chosen one was
deleted. With no vehicle at all, each tab shows the same "add your first vehicle" state.

Each screen has an immutable `UiState` exposed as `StateFlow`; user actions are ViewModel functions.
A screen that closes after an action (saving a form, choosing a vehicle) waits for an `isDone` flag
in its state rather than closing right away, because closing cancels the ViewModel's scope and with it
any write still in flight.

## Storage

Room holds four tables: `vehicle`, `fuel_entry`, `maintenance_item` and `maintenance_record`.

- Every row has a client-generated UUID `id` and `createdAt`, `updatedAt`, `deletedAt` in epoch
  millis. The UI does not use them; they prepare for sync in v1.1, which will still add its own
  bookkeeping (what has been uploaded).
- Rows are soft-deleted: `deletedAt` is set and DAOs filter them out. Undo is one update, and sync
  can upload deletions. Deleting a vehicle marks its fill-ups, items and records in the same
  transaction.
- A save never brings a deleted row back, and nothing can be saved or restored under a deleted
  vehicle. A stale edit screen that saves after a deletion leaves the row deleted.
- `updatedAt` never moves backwards, even when the device clock does, so that "newer wins" during
  sync keeps meaning "edited later".
- Saves use `@Upsert`, which updates an existing row in place. `INSERT OR REPLACE` would delete the
  row first and cascade the delete to its children.
- Dates are epoch days, instants are epoch millis; enums are stored by name.
- An item's last-done values change only when a service is recorded: the record is stored and the
  values move in one transaction, unless the item already has a later service. Saving an edited item
  keeps them, so an edit form opened earlier cannot put an old value back.

```
vehicle 1──* fuel_entry
vehicle 1──* maintenance_item 1──* maintenance_record
vehicle 1──* maintenance_record
```

## Rules that hold everywhere

- **Room is the source of truth.** The UI reads `Flow`s from Room. Network results are written to Room,
  or, for short-lived data such as nearby stations, returned directly but never pushed to the UI
  behind the database's back.
- **Errors are a type, not exceptions.** Repositories return a result with a classified error branch;
  ViewModels map it to a message through one shared mapper.
- **One immutable `UiState` per screen**, exposed as `StateFlow`. User actions are functions on the
  ViewModel.

## Decisions

- [0001. Three modules instead of one module per feature](adr/0001-three-modules.md)
- [0002. Money and volume stored as integers](adr/0002-integer-money-and-volume.md)
- [0003. Fuel economy by the full-tank method](adr/0003-full-tank-fuel-economy.md)
