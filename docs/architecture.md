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
└── model/, repository/, usecase/, util/

core/data/src/main/kotlin/io/github/diiafyra/motoman/core/data/
└── database/, network/, datastore/, repository/
```

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
