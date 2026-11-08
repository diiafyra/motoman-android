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
├── di/                  app-level Hilt bindings (template names, notifier)
└── work/                ReminderWorker, ReminderChecker, notifications

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
├── datastore/           preferences (selected vehicle, reminders)
├── network/             Overpass API, retry interceptor, error mapping
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

### Overview and statistics

The Overview tab combines the selected vehicle's fill-ups, maintenance items and records into one
`UiState`: the latest and average figures from `calculateFuelEconomy`, this month's spending, the
current odometer and the items `rankByUrgency` puts first. When the newest stretch was dropped for a
missed fill-up, the screen says that the latest figure comes from an earlier stretch.

Statistics come from `monthlyStats`: spending counts every fill-up in its month, and fuel economy counts
the full-tank segments that ended in the month, since the fuel inside a segment cannot be placed on a
date. The charts are drawn with `Canvas` in `BarChart`: one series per chart, one hue from the theme,
bars from zero, a value label only on the selected bar, and the same numbers as a table for screen
readers.

### Maintenance

A new vehicle gets its starting items from `templateItems`: the template list and default intervals
live in the domain, the names come from string resources in the app's language at that moment. The
tab lists items with `rankByUrgency` at the current odometer. Marking an item done creates a record
through `recordDone`, which moves the item's baseline in the same transaction; editing an item never
touches its baseline.

### Monitoring and remote settings

Firebase is optional at build time. The `google-services` and Crashlytics plugins are applied only when
`app/google-services.json` exists (it is git-ignored), and `BuildConfig.FIREBASE_ENABLED` records it.
Without the file, which is what CI and contributors have, `DefaultServices` supplies no-op analytics and
the built-in settings, and Firebase classes are never touched (calling Firebase without an app would
throw). With the file, `FirebaseAnalyticsTracker` and `RemoteConfigServices` take over; both are chosen
once in `AppModule`, so nothing else knows which is in use.

Debug builds collect nothing. Release builds report crashes and exactly four events:
`vehicle_added`, `fuel_logged`, `maintenance_done` and `nearby_opened`. An event is a name and nothing
else: no parameters, no ids, no values. Editing a record does not count as creating one.

Remote Config holds `due_soon_km`, `due_soon_days` and `nearby_enabled`, with defaults in code that
apply until the first fetch and whenever it fails. Values outside a sane range (a typo in the console)
fall back to the default instead of reaching the app.

### Nearby

`NearbyViewModel` walks one path: permission, position, request, result, and each step has a state
(`NeedsPermission`, `Locating`, `LocationUnavailable`, `Loading`, `Content`, `Empty`, `Error`,
`Disabled`). `FindNearbyPlaces` asks `PlacesCache` first: results are kept in memory per grid cell of
about 1 km for 30 minutes, so panning around the same area does not call the shared Overpass service
again. Failures are never cached. The screen is switched off remotely through `FeatureFlags`.

The map is Google Maps Compose. Its key is read from `local.properties` or the `MAPS_API_KEY`
environment variable and never committed; without it the app still builds and the screen shows the
same places as a list, so the feature degrades instead of breaking.

### Network

Retrofit with kotlinx.serialization talks to Overpass; OkHttp adds a `User-Agent` that identifies the
app, timeouts, retry for `GET` and `HEAD` only, and logging that depends on the build type. Every call
goes through `safeCall`, so repositories return `AppResult<T>` and the screen chooses a message from
`DataError`. Results of the nearby query are short-lived: they are returned to the UI directly and never
written to Room. See [ADR 4](adr/0004-network-errors-and-retry.md).

### Settings and language

Settings holds the app language, the reminders switch, the version and the privacy policy link. The
language is per app: `AppCompatLanguageController` calls `AppCompatDelegate.setApplicationLocales`,
which uses the system per-app setting on Android 13+ and stores the choice itself below that (the
`autoStoreLocales` service in the manifest). That is why `MainActivity` is an `AppCompatActivity` with
an AppCompat theme. The build generates the locale config from the `values` and `values-vi` folders,
so the system settings list exactly the languages the app has. Lint fails the build on any string
missing a translation.

### Accessibility

`AccessibilityAuditTest` walks the main flow and, on each screen, checks two things in the semantics
tree: everything clickable has a name (text or content description), and its touch bounds are at
least 48 dp each way (Material buttons are drawn 40 dp tall but accept touches in 48 dp, which is why
the test reads `touchBoundsInRoot`). A failure names the screen and the element.

I first tried Android's own accessibility test framework through Compose's
`enableAccessibilityChecks()`. It passed even after I removed a button's label on purpose, so I
dropped it rather than keep a test that gives false confidence. The direct audit failed on the same
sabotage, and on its first real run it found a defect: the floating "add" buttons reached the
accessibility tree with no name. They now go through `AddFab`, which states the label.

Beyond that: errors on form fields are exposed as semantic errors, toggles and radio rows are one
touch target with a role, swipe-to-delete has the same action as a TalkBack custom action, and the
Nearby screen announces state changes with a polite live region.

### Testing the main flow

`MainFlowTest` drives the real app, Room, DataStore, navigation and Hilt included, through adding a
vehicle and logging a fill-up. It is a Compose UI test running on Robolectric with
`HiltTestApplication`, so it runs in `./gradlew test` and in CI without an emulator.

### Reminders

`ReminderWorker` is a periodic WorkManager job, first run at the next 9:00 and then daily; it only
calls `ReminderChecker`, which holds the logic and is tested with fakes. The checker looks at every
vehicle, builds reminders for overdue and due-soon items, and uses `selectReminders` so each item is
notified once per due point: once when it becomes due soon, once more if it becomes overdue, and again
only after it has been done and comes due anew. The keys already notified live in DataStore. Without
notification permission nothing is marked as sent, so the reminders appear once it is granted.

`MotoManApplication` provides WorkManager's configuration with Hilt's worker factory, which is why the
default WorkManager initializer is removed from the manifest.

### Fuel log

The Fuel tab groups fill-ups by calendar month in the user's time zone. The form validates with the
domain's `validateFuelEntry`, so the rules shown to the user are the ones the fuel economy calculation
relies on. Litres are parsed straight to millilitres by `parseVolumeMl`, never through `Double`.

The form asks for a date, not a time. A new fill-up today gets the current instant, another day gets
noon local time, and an edit keeps its original instant while the date is unchanged; this keeps the
order of fill-ups stable without making the user enter a time.

Deleting is a soft delete. The list offers undo in a snackbar, which restores the row.

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
- [0004. Network errors as values, retry only for idempotent requests](adr/0004-network-errors-and-retry.md)

What the app stores and the permissions it asks for are listed in [privacy.md](privacy.md).
