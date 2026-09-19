# Spec: Training Calendar

Status: **Draft** · Date: 2026-09-19 · Stage 2 (Design)
Reads: [`intent.md`](intent.md) and [`intents/`](intents/)

## 0. What this document is

In one sentence:

> **Everything that was decided and must stay true, plus the reason it was
> decided that way.**

It is the artefact Stage 3 reads before writing code, and the artefact a
reviewer reads to understand why the code looks the way it does.

Note that this is defined by the *kind* of statement, not by topic. Technology,
patterns and architecture are the obvious residents, but **domain rules belong
here too** (§4) — the status truth table is neither a technology nor a pattern,
yet it is a decision the implementation must satisfy, and it is the section most
expensive to get wrong.

### The test for what belongs here

> If changing *how* something is implemented — without changing *what was
> decided* — would make a section wrong, that section does not belong.

A spec that drifts from the code is worse than no spec, because it is trusted.
Keeping it to decisions is what keeps it true.

### In scope

- Layer boundaries, and what may cross them
- Contracts: interface signatures, persisted shapes, error taxonomy, UI state shape
- Domain rules, with the truth tables that define them
- Decisions taken, alternatives rejected, and the reason
- Constraints the implementation must satisfy

### Out of scope

- How a function is written
- Wiring and configuration that the code expresses more precisely than prose
- Exhaustive file listings
- Anything a refactor would invalidate

### Where the rest lives

| Content | Home |
|---|---|
| Why a decision was taken, options weighed | This document |
| How an intent will be executed, estimates, risks | `plans/NN-*.md` (drills) |
| Rules an agent must not break | `CLAUDE.md` |
| What must be true for a rung to be done | `intents/NN-*.md` |
| Exact wiring | The code |

### On code samples below

Where code appears, it is the **contract** — a signature, a shape, a taxonomy.
Blocks marked *illustrative* show intent only; the implementation is
authoritative and no one should update this document when it changes.

## 1. Technology choices

| Concern | Choice | Why |
|---|---|---|
| UI | **Jetpack Compose** | Declarative; previews give the tight visual loop that pixel-perfect work needs |
| Concurrency | **Coroutines + Flow** | Structured concurrency; `suspend` at every async boundary |
| Persistence | **Room** | First-party relational store with an observable query API (fallback in Drill 03 §4) |
| Networking | **Ktor Client** | Kotlin-native, coroutine-first HTTP |
| Serialization | **kotlinx.serialization** | Ktor-native, no KSP (Drill 02 §2) |
| DI | **Koin** | Runtime DSL, so no KSP or annotation processing (§2.5) |
| Pattern | **MVI** | Pure reducer, unidirectional; §2.8 |
| Dates | **`java.time` + desugaring** | minSdk 24 makes desugaring mandatory (Drill 01 §2) |

## 2. Architecture

### 2.0 What this architecture is, and what it is not

The brief asks for "a modern, clean architecture that you are confident in" —
using *clean* in the ordinary sense of tidy and well-separated, not as a
citation of a specific book.

This build is therefore described precisely rather than branded:

> **Layered MVI, applying the Clean Architecture dependency rule.**

What is adopted from Clean Architecture:

- **The dependency rule.** Source dependencies point inward only. `domain`
  depends on nothing; `ui` and `data` both depend on `domain` (§2.3).
- **Dependency inversion at the data boundary.** The `WorkoutRepository`
  interface is owned by `domain`; `data` supplies the implementation. The
  high-level policy does not depend on the low-level detail.
- **Framework independence of business rules.** Week calculation and status
  derivation are plain Kotlin, testable with no Android runtime (§6).

What is deliberately **not** adopted:

- **No use-case / interactor layer.** Three operations (observe, refresh,
  toggle) would become three pass-through classes. That logic lives in
  `WeekProvider` and `StatusResolver` instead, where it is directly testable.
- **No fifth model** beyond §2.4's four.
- **No module-per-layer split.** See §2.1.

Naming the omissions is the point: claiming "Clean Architecture" without its
defining structure invites the follow-up question the brief warns about.

### 2.1 Module strategy

**One Gradle module (`:app`).** A multi-module split (`:core`, `:data`,
`:feature-calendar`) is the textbook answer to "scalable and modular", but for
a single screen it buys enforced layer boundaries at the cost of build-file
setup that a 24-hour budget does not repay.

Boundaries are instead enforced by **package structure and inward-pointing
dependencies** (§2.3). The split remains mechanical later: each package below
maps 1:1 onto a module.

### 2.2 Package structure

Target shape, indicative rather than exhaustive — the decision is the *grouping*
and the direction of dependencies, not the file list:

```
com.example.everfit.assignment
├── EverfitApplication.kt          Application; owns the DI container
├── di/
│   └── AppModule.kt               Koin module (§2.5)
├── mvi/
│   ├── MviViewModel.kt            Store: I/R/S/E, sync region, effects
│   └── FlowOperators.kt           flatMapFirst
├── ui/
│   ├── MainActivity.kt
│   ├── theme/                     Colour, type, shape tokens from Figma
│   ├── calendar/
│   │   ├── CalendarContract.kt    State, Intent, Effect, UI models (public)
│   │   ├── CalendarReducer.kt     Result + reduceCalendar (internal, pure)
│   │   ├── CalendarViewModel.kt   Pipelines only — cannot write state
│   │   ├── CalendarScreen.kt      Takes (state, onIntent)
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

The cost of collapsing each adjacent pair:

| Collapse | Cost |
|---|---|
| DTO = Entity | A payload change becomes a Room migration |
| Entity = Domain | `server_status` leaks into domain logic as an `Int`; the override merge becomes a database concern |
| Domain = UI | `DisplayStatus` depends on *today*, so the model would mean different things at different times |

Mapping is one-directional, at the boundary it crosses: `data/mapper` owns
DTO→Entity→Domain, `ui/mapper` owns Domain→UI.

### 2.5 Dependency injection

**Koin**, declared as a single module graph.

What is bound, and with what lifetime — this is the contract; the DSL that
expresses it lives in the code:

| Binding | Lifetime | Notes |
|---|---|---|
| `EverfitDatabase` and its DAOs | singleton | |
| `HttpClient`, `WorkoutApi` | singleton | |
| `Clock` | singleton | **injected**, never `systemDefaultZone()` inline (§4.1) |
| IO `CoroutineDispatcher` | singleton, named | **injected**, never bare `Dispatchers.IO` (§2.6) |
| `WorkoutRepository` | singleton | bound to the interface, not the implementation |
| `CalendarViewModel` | per screen | |

The two that matter are `Clock` and the dispatcher. Both exist as bindings
purely so tests can replace them; inlining either makes §6.4's matrices
untestable.

Started in `EverfitApplication`; the screen resolves its state holder with
`koinViewModel()`.

Why Koin:

- **No annotation processing** — plain Kotlin DSL, no KSP, no Gradle plugin.
  (Koin *Annotations* would reintroduce KSP; the plain DSL is deliberate.)
- **Recognised approach**, without Hilt's setup cost.
- **Constructor injection preserved** — tests build subjects directly and never
  start Koin.

Trade-off: runtime resolution makes a missing binding a crash, not a compile
error. Mitigated by a `verify()` test (see `testing.md`).

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
- The state flow stays warm briefly after the last collector, so a rotation
  does not re-query or re-fetch. *(Illustrative: `stateIn(WhileSubscribed(5_000))`.)*

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

### 2.8 UI architecture — MVI

Following the store in `/Users/phucnguyen/Documents/mvi-search` (`MviViewModel`,
`flatMapFirst`), adopted rather than reinvented.

```
        onIntent(I)                    Result R           reduce(S, R) -> S
Screen ─────────────> pipelines ──────────────> sync region ──────────> StateFlow<S> ──> Screen
                                                      │
                                                      └── effectFor() ──> Channel<E>
```

Four type parameters: **I**ntent, **R**esult, **S**tate, **E**ffect.

**Intent ≠ Result.** Intents are what the UI *asks for*; Results are *facts*
produced by work that has already happened. This is the central discipline, not
bookkeeping — a Result carries the request it belongs to, which is what lets the
reducer drop stale work synchronously. A coroutine that writes state itself
cannot do that: by the time its continuation resumes there is no reliable "is
this still current".

Structural invariants, from the reference implementation:

| Invariant | Enforced by |
|---|---|
| The UI cannot write state | `onIntent` is the only public door in |
| No torn transitions | One collector, one assignment, nothing suspends in the sync region |
| The reducer cannot stop being pure | It is a **top-level function** passed as `::reduceCalendar`, so it has no `this` |
| Every effect coroutine is cancellable | All live in `viewModelScope` via `pipeToState()` |
| Effects do not replay on rotation | `Channel`, never `StateFlow` |

- `CalendarScreen` takes `(state, onIntent)` and never sees the ViewModel, so
  every state is previewable and screenshot-testable with no database or
  network.
- Intents are a sealed interface, so a new interaction is a compile error until
  a pipeline claims it.
- Design tokens live in `ui/theme`, referenced by name, never inline hex.

#### Flattening strategy per pipeline

| Pipeline | Operator | Why |
|---|---|---|
| Refresh | `flatMapFirst` | A double tap must not start two refreshes, and must not cancel the first |
| Toggle completion | `flatMapConcat` | Two rapid toggles are independent facts; neither may cancel the other |
| Cache observation | plain `map` | A Room `Flow`, not a triggered request |

`flatMapFirst` is not in the standard library and comes from the reference
implementation.

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
CalendarState(
    weekDates: List<LocalDate>,   // always present, even while loading
    days: List<DayUiModel>,       // content
    load: Load,                   // status, orthogonal to content
)

sealed interface Load { Idle; Refreshing; Failed(message) }
```

Derived, never stored — computing them removes the risk of a stale copy:

```
showsFullScreenError = load is Failed && days.all { it.workouts.isEmpty() }
isRefreshing         = load is Refreshing
```

`weekDates` is populated synchronously from `WeekProvider` at construction,
never from the network. That is what makes the brief's loading requirement —
correct dates, empty data — fall out rather than be retrofitted.

**Revision.** This section previously argued for flat `isLoading: Boolean` plus
`error: ErrorType?`, on the grounds that a failed refresh over good cache is
simultaneously content-bearing and errored, which a sealed
`Loading | Content | Error` hierarchy cannot express.

That reasoning rejected the wrong thing. What fails is collapsing *content and
status into one* hierarchy. Keeping `days` and `load` as **separate fields**,
with `load` sealed, expresses the awkward case exactly — `Load.Failed` alongside
a populated `days` — while still making `Refreshing && Failed` unrepresentable.
Two booleans cannot do that; they permit illegal combinations that then need a
test to rule out.

### 5.2 Walkthroughs

Each sequence as Intent → Result → State.

**Cold start — empty cache**
```
(init)              -> WeekProvider              State(weekDates, days=[], Idle)
Room Flow emits []  -> CachedLoaded([])          unchanged
Refresh             -> RefreshStarted            load = Refreshing
                    -> RefreshSucceeded          load = Idle
Room Flow re-emits  -> CachedLoaded(days)        days populated
```

**Warm start — populated cache**
```
Room Flow emits     -> CachedLoaded(days)        content, immediately
Refresh (background)-> RefreshStarted/Succeeded  load only; days untouched
```
Nothing awaits the network before first render.

**Toggle completion**
```
ToggleCompletion(id) -> upsert completion_overrides   (assignments untouched)
                     -> Room re-emits -> CachedLoaded  checkmark appears
```
The toggle produces no state-changing Result of its own — Room is the source of
truth, so the write returns through the cache pipeline. Two writes touch
different tables, so a concurrent refresh cannot race it.

**Refresh fails over good cache**
```
Refresh -> RefreshStarted            load = Refreshing
        -> RefreshFailed(message)    load = Failed; days untouched
                                     effectFor -> ShowSnackbar
```
The reducer never clears `days`, so this holds by construction. A full-screen
error is *state* (`showsFullScreenError`); a failed refresh over usable content
is a one-shot *effect* — the same split the reference implementation makes
between a failed search and a failed page load.

## 6. Testing strategy

Moved to [`testing.md`](testing.md) — it answers a different question and grows
at a different rate.

The decision it records: domain, data and ViewModel logic is JVM-testable with
no device and no Robolectric. That is a constraint on §2.3, not a testing
preference — it is why `domain` may import nothing.

## 7. Open decisions

- [ ] **Status enum semantics.** `0=ASSIGNED, 1=COMPLETED, 2=MISSED` is an
      assumption. Every status colour depends on it. Confirm against the design
      before rung 2.2 is closed.
- [ ] **Design tokens.** Purple value, status colours, radii, spacing, type
      scale, checkmark icon — pending PNG exports into `docs/design/`.
- [ ] **DataStore.** Not adopted. Room covers both cached workouts and the
      completion overlay, and a sync timestamp is a column rather than a reason
      for a second persistence library. Revisit only if genuine user
      preferences appear.
- [ ] **Compose plugin wiring.** AGP 9 compiles Kotlin with no separate Kotlin
      plugin applied (verified in PR #1), but enabling Compose still requires
      the Compose compiler plugin. Exact wiring is verified in rung 0.2 rather
      than assumed here.
