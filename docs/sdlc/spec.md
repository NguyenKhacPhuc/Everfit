# Spec: Training Calendar

Status: **Draft** · Date: 2026-09-19 · Stage 2 (Design)
Reads: [`intent.md`](intent.md) and [`intents/`](intents/)

## 1. Platform mapping

The brief specifies iOS technologies; this is an Android submission. Each
requirement is mapped to its closest Android equivalent, and the mapping itself
is a deliverable — it should be stated in the README so the reviewer sees a
deliberate translation rather than a spec that was ignored.

| Brief (iOS) | This build (Android) | Why |
|---|---|---|
| SwiftUI / UIKit | **Jetpack Compose** | Declarative equivalent of SwiftUI; previews give the tight visual loop that pixel-perfect work needs |
| async/await | **Coroutines + Flow** | Kotlin's structured concurrency; `suspend` is the direct analogue |
| CoreData / SwiftData / Realm | **Room** | First-party relational store with an observable query API |
| URLSession | **Ktor Client** | Kotlin-native, coroutine-first HTTP |
| MVVM-C | **MVVM** | Single screen; a coordinator layer would be ceremony here |

## 2. Architecture

### 2.1 Module strategy

**One Gradle module (`:app`).** A multi-module split (`:core`, `:data`,
`:feature-calendar`) is the textbook answer to "scalable and modular", but for
a single screen it buys enforced layer boundaries at the cost of build-file
setup that a 24-hour budget does not repay.

Boundaries are instead enforced by **package structure and inward-pointing
dependencies** (§2.3). The split remains mechanical later: each package below
maps 1:1 onto a module.

### 2.2 Package structure

```
com.example.everfit.assignment
├── EverfitApplication.kt          Application; owns the DI container
├── di/
│   └── AppContainer.kt            Manual DI graph (§2.5)
├── ui/
│   ├── MainActivity.kt
│   ├── theme/                     Colour, type, shape tokens from Figma
│   ├── calendar/
│   │   ├── CalendarScreen.kt      Stateless composable
│   │   ├── CalendarViewModel.kt   State holder
│   │   ├── CalendarUiState.kt     UI state + UI models
│   │   ├── CalendarEvent.kt       User intents
│   │   └── components/            DayCell, WorkoutCell, StatusIndicator
│   └── mapper/
│       └── UiMappers.kt           Domain -> UI model
├── domain/
│   ├── model/                     WorkoutAssignment, StoredStatus, DisplayStatus, WeekDay
│   ├── WeekProvider.kt            Clock -> Mon..Sun dates
│   └── StatusResolver.kt          (stored, override, position) -> DisplayStatus
└── data/
    ├── remote/
    │   ├── WorkoutApi.kt          Ktor client
    │   └── dto/                   WorkoutsResponseDto, DayDto, AssignmentDto
    ├── local/
    │   ├── EverfitDatabase.kt
    │   ├── entity/                WorkoutAssignmentEntity, CompletionOverrideEntity
    │   └── dao/                   WorkoutDao, CompletionDao
    ├── mapper/
    │   └── DataMappers.kt         DTO -> Entity, Entity -> Domain
    └── WorkoutRepositoryImpl.kt
```

### 2.3 Layer contracts

| Layer | May depend on | Must not contain |
|---|---|---|
| `ui` | `domain` | Ktor, Room, DTOs, entities |
| `domain` | *nothing* | Any Android or library type |
| `data` | `domain` | Compose, ViewModel |

The rule that matters: **`domain` imports nothing.** No `android.*`, no Room,
no Ktor, not even `Context`. That is what makes §6's "no device, no
Robolectric" column true — every rule in §4 runs as a plain JVM test in
milliseconds.

The repository interface lives in `domain`, its implementation in `data`, so
the dependency arrow points inward at the boundary that would otherwise invert:

```kotlin
// domain
interface WorkoutRepository {
    fun observeWeek(): Flow<List<WorkoutAssignment>>
    suspend fun refresh(): Result<Unit>
    suspend fun toggleCompletion(assignmentId: String)
}
```

### 2.4 The model chain

Four representations, each earning its place:

```
AssignmentDto  ──>  WorkoutAssignmentEntity  ──>  WorkoutAssignment  ──>  WorkoutUiModel
   (wire)              (Room row)                   (domain)              (rendered)
      │                     │                            │                      │
  _id, status         id, server_status          id, storedStatus        id, title,
  total_exercise      day_index                  isCompleted             subtitle,
  nested in DayDto    flat                       dayIndex                displayStatus,
                                                                         showCheckmark
```

Collapsing any pair is tempting and each collapse costs something:

- **DTO = Entity** couples the database schema to the wire format. A payload
  change then becomes a Room migration.
- **Entity = Domain** leaks `server_status` as an `Int` into domain logic and
  makes the override merge (§4.2) a database concern.
- **Domain = UI** puts `DisplayStatus` — which depends on *today* — inside the
  domain model, so the same object means different things at different times.

Mapping is one-directional and lives at the boundary it crosses:
`data/mapper` owns DTO→Entity→Domain, `ui/mapper` owns Domain→UI.

### 2.5 Dependency injection

**Manual constructor injection via a single `AppContainer`**, not Hilt.

```kotlin
class AppContainer(context: Context) {
    private val database by lazy { EverfitDatabase.build(context) }
    private val httpClient by lazy { HttpClient(/* ... */) }
    private val api by lazy { WorkoutApi(httpClient) }

    val clock: Clock = Clock.systemDefaultZone()
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepositoryImpl(api, database.workoutDao(), database.completionDao(), ioDispatcher)
    }
}
```

Rationale, stated plainly because a reviewer may expect Hilt:

- The graph is **six objects with no scoping beyond singleton**. Hilt's value
  is managing graphs that outgrow comprehension; this one fits on a screen.
- Hilt adds KSP, two Gradle plugins and annotation processing to a toolchain
  already on a brand-new AGP 9.1.1. That is a real schedule risk for zero
  architectural gain at this size.
- Every class already takes its dependencies through its constructor, which is
  what makes the tests in §6 possible. Migrating to Hilt is then mechanical —
  annotate and delete `AppContainer`. Constructor injection is the decision
  that matters; the container is an implementation detail.

The `ViewModel` is built by an explicit `ViewModelProvider.Factory` reading
from `AppContainer`.

> **Reversible.** If Hilt is preferred for signalling reasons, it is a
> contained change — see §7.

### 2.6 Concurrency

- `Dispatchers.IO` is **injected**, never referenced directly, so tests
  substitute a test dispatcher and run deterministically.
- The repository exposes `Flow`; Room produces it and it is never collected
  inside the data layer.
- `refresh()` is `suspend` and returns `Result<Unit>`; it writes to Room and
  never returns data. The UI observes Room, so refresh and render are decoupled
  — which is what makes the §5.1 ordering guarantee structural rather than
  timing-dependent.
- The ViewModel collects in `viewModelScope`, surviving configuration change.
- `stateIn(WhileSubscribed(5_000))` keeps the flow warm across rotation without
  leaking it.

### 2.7 Error model

Typed at the boundary, not thrown across it:

```kotlin
sealed interface DataError {
    data object Network : DataError      // no connectivity, timeout
    data object Server : DataError       // non-2xx
    data object Parsing : DataError      // malformed body
    data class Unknown(val cause: Throwable) : DataError
}
```

Ktor and kotlinx.serialization exceptions are caught in `WorkoutApi` and mapped
to `DataError`. Nothing above `data` sees a library exception type, so swapping
Ktor for anything else touches one file.

Errors are **non-fatal by design**: `refresh()` returning a failure sets
`error` in UI state and leaves cached content untouched (rung 3.4).

### 2.8 UI architecture

Unidirectional data flow:

```
CalendarViewModel ──StateFlow<CalendarUiState>──> CalendarScreen
        ▲                                               │
        └──────────── CalendarEvent ────────────────────┘
```

- `CalendarScreen` is **stateless** — it takes `CalendarUiState` and an
  `(CalendarEvent) -> Unit`. This is what makes every state previewable and
  screenshot-testable without a ViewModel, a database or a network.
- All state is hoisted to the ViewModel. Composables hold no `remember`ed
  business state.
- Events are a sealed interface (`ToggleCompletion(id)`, `Retry`) rather than
  loose lambdas, so adding an interaction is a compile error until handled.
- Design tokens live in `ui/theme` and are referenced by name, never as inline
  hex. Pixel-perfect work then happens in one place.

### 2.9 Why this survives the fixture's traps

The architecture is shaped by three specific failure modes from Stage 1, not by
a generic template:

| Trap | Structural defence |
|---|---|
| Refresh reverts a local mark (rung 5.5) | Separate override table; refresh never writes it (§3.2) |
| Wrong row toggles (rung 5.2) | Events carry `id`; no list-position or title addressing (§2.8) |
| Future days show a stale label (rung 2.3) | `DisplayStatus` computed in `ui/mapper` against today, not stored (§2.4) |

## 3. Data model

### 3.1 Wire format

```json
{ "data": [ { "_id": "...", "day": 0,
    "assignments": [ { "_id": "...", "title": "Legs day",
                       "status": 1, "total_exercise": 5 } ] } ] }
```

DTOs mirror this exactly and do nothing else. Renaming and reshaping happen at
the mapping boundary, so a change in the payload touches one file.

### 3.2 Two tables, deliberately

```
workout_assignments          completion_overrides
  id            TEXT PK        assignment_id  TEXT PK
  day_index     INT            is_completed   BOOL
  title         TEXT           updated_at     INT
  server_status INT
  total_exercise INT
```

**This split is the answer to rung 5.5.** A refresh replaces
`workout_assignments` wholesale; it never touches `completion_overrides`. A
user's local mark therefore cannot be destroyed by a server response that
disagrees with it.

The alternative — one table with a mutable status column — is simpler to write
and silently reverts the user's tap on the next refresh. That failure only
appears *after* a refresh, so it survives casual testing and shows up in front
of the reviewer.

## 4. Domain rules

### 4.1 The week

```
WeekProvider(clock: Clock, zone: ZoneId) -> List<LocalDate>   // Mon..Sun, size 7
dayIndex 0..6 -> weekDates[dayIndex]
```

`Clock` is injected, never `LocalDate.now()`. Without this, "today" is
untestable and the purple highlight can only be checked by changing the device
date. The API carries **no dates at all**, so this mapping is the only thing
tying `day: 0..6` to a real calendar.

### 4.2 Stored status vs displayed status

Two distinct types, because the brief defines status by temporal position:

```
StoredStatus   = ASSIGNED(0) | COMPLETED(1) | MISSED(2)    // assumption, see §7
DisplayStatus  = COMPLETED | MISSED | ASSIGNED | UPCOMING
```

Completion is resolved before display:

```
isCompleted = override?.isCompleted ?: (storedStatus == COMPLETED)
```

Then, given the day's position relative to today:

| Day | isCompleted | DisplayStatus |
|---|---|---|
| Past | true | COMPLETED |
| Past | false | MISSED |
| Today | true | COMPLETED |
| Today | false | ASSIGNED |
| Future | *either* | UPCOMING (greyed) |

Future days are greyed **regardless of stored status**. Collapsing these two
types into one is the mistake this table exists to prevent.

## 5. Data flow

```
Room (cache)  ──emit immediately──┐
                                  ├─> Repository ──> ViewModel ──> Compose
Ktor (network) ──refresh──> Room ─┘
```

The repository exposes an observable query over Room, and refresh is a separate
write into Room. Cached data therefore reaches the UI without waiting on the
network — the ordering guarantee rung 3.3 asserts — and a failed refresh is
non-destructive by construction (rung 3.4), because nothing clears the cache on
error.

### 5.1 Screen state

```
CalendarUiState(
    weekDates: List<LocalDate>,   // always present, even while loading
    days: List<DayUiModel>,
    isLoading: Boolean,
    error: ErrorType?,
)
```

`weekDates` is populated synchronously from `WeekProvider` at construction,
never from the network. That is what makes the brief's loading requirement —
correct dates, empty data — fall out rather than be retrofitted.

Note this is **not** a sealed `Loading | Content | Error` hierarchy. Those
states are not mutually exclusive here: a failed refresh over good cached data
is simultaneously content-bearing and errored, which a sealed hierarchy forces
you to misrepresent.

### 5.2 Walkthroughs

The four sequences the architecture exists to get right.

**Cold start — empty cache**

```
launch -> WeekProvider gives 7 dates      UI: dates visible, cells empty, isLoading
       -> Room emits []                   UI: unchanged
       -> refresh() -> Ktor -> Room       UI: workouts appear, isLoading false
```

The grid is on screen before the network is even contacted. This is the
brief's loading requirement, satisfied by ordering rather than by a spinner.

**Warm start — populated cache**

```
launch -> WeekProvider gives 7 dates      UI: dates visible
       -> Room emits cached workouts      UI: full content, immediately
       -> refresh() in background         UI: updates in place, or doesn't
```

Nothing awaits the network before the first render. Rung 3.3 asserts exactly
this emission order.

**Toggle completion**

```
tap -> CalendarEvent.ToggleCompletion(id)
    -> repository.toggleCompletion(id)
    -> upsert into completion_overrides      (workout_assignments untouched)
    -> Room re-emits                          UI: checkmark appears
```

The write targets the override table only. A concurrent refresh cannot race it,
because the two writes touch different tables.

**Refresh fails over good cache**

```
refresh() -> Ktor throws -> mapped to DataError.Network
          -> Result.failure, nothing written to Room
          -> UI: error set, cached content still rendered
```

No code path clears the cache on failure, so rung 3.4 holds by construction
rather than by remembering to handle it. Demonstrable in the walkthrough video
with airplane mode.

## 6. Testing strategy

| Layer | Approach | Needs a device |
|---|---|---|
| Week calculation | JVM unit tests, fixed `Clock` | No |
| Status derivation | JVM unit tests over the full matrix | No |
| DTO parsing | JVM unit tests against the committed fixture | No |
| Repository | Fake DAO + stubbed HTTP engine | No |
| ViewModel | Fake repository, Turbine-style Flow assertions | No |
| Compose UI | Compose UI tests | Instrumented |
| Visual fidelity | Screenshot comparison against the design | Yes |

Ktor's `MockEngine` allows the HTTP layer to be tested without a network or a
live endpoint. The committed fixture at
[`api-sample-response.json`](api-sample-response.json) is the single source of
test data.

## 7. Open decisions

- [ ] **Status enum semantics.** `0=ASSIGNED, 1=COMPLETED, 2=MISSED` is an
      assumption. Every status colour depends on it. Confirm against the design
      before rung 2.2 is closed.
- [ ] **Design tokens.** Purple value, status colours, radii, spacing, type
      scale, checkmark icon — pending PNG exports into `docs/design/`.
- [ ] **Hilt vs manual DI.** §2.5 commits to a manual `AppContainer`. If Hilt
      is preferred for signalling familiarity to the reviewer, the change is
      contained — every class already uses constructor injection, so it is
      annotations plus deleting one file. Decide before rung 0.2 wires the
      Application class; later is more churn.
- [ ] **DataStore.** Not adopted. Room covers both cached workouts and the
      completion overlay, and a sync timestamp is a column rather than a reason
      for a second persistence library. Revisit only if genuine user
      preferences appear.
- [ ] **Compose plugin wiring.** AGP 9 compiles Kotlin with no separate Kotlin
      plugin applied (verified in PR #1), but enabling Compose still requires
      the Compose compiler plugin. Exact wiring is verified in rung 0.2 rather
      than assumed here.
