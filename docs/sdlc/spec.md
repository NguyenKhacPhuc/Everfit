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
| Pattern | **MVVM** | Single screen; see §2.0 |
| Dates | **`java.time` + desugaring** | minSdk 24 makes desugaring mandatory (Drill 01 §2) |

## 2. Architecture

### 2.0 What this architecture is, and what it is not

The brief asks for "a modern, clean architecture that you are confident in" —
using *clean* in the ordinary sense of tidy and well-separated, not as a
citation of a specific book.

This build is therefore described precisely rather than branded:

> **Layered MVVM, applying the Clean Architecture dependency rule.**

What is adopted from Clean Architecture:

- **The dependency rule.** Source dependencies point inward only. `domain`
  depends on nothing; `ui` and `data` both depend on `domain` (§2.3).
- **Dependency inversion at the data boundary.** The `WorkoutRepository`
  interface is owned by `domain`; `data` supplies the implementation. The
  high-level policy does not depend on the low-level detail.
- **Framework independence of business rules.** Week calculation and status
  derivation are plain Kotlin, testable with no Android runtime (§6).

What is deliberately **not** adopted:

- **No use-case / interactor layer.** Canonical Clean Architecture routes every
  operation through a single-method interactor. With three operations —
  observe, refresh, toggle — that layer would be pass-through classes adding
  indirection without adding a decision. The logic that *would* justify
  interactors instead lives in two named domain services, `WeekProvider` and
  `StatusResolver`, where it is directly testable.
- **No entity/model duplication beyond §2.4.** Four representations are already
  justified; a fifth for "enterprise business rules" would be ceremony.
- **No module-per-layer split.** See §2.1.

Stating this explicitly matters. Claiming "Clean Architecture" while omitting
its defining structure invites exactly the follow-up question the brief warns
about. Naming the parts adopted, and the parts skipped with reasons, is the
more defensible position — and is the honest description of the code.

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

Why Koin over the alternatives:

- **No annotation processing.** Koin's DSL is plain Kotlin resolved at runtime,
  so it adds no KSP step and no Gradle plugin. On a toolchain already running a
  brand-new AGP 9.1.1, avoiding annotation processing removes a real schedule
  risk. (Koin *Annotations* would reintroduce KSP — the plain DSL is used
  deliberately.)
- **Real DI, not a hand-rolled container.** A reviewer looking for a
  recognised approach finds one, without the setup cost of Hilt.
- **Constructor injection is preserved.** Every class still takes its
  dependencies as constructor parameters, so unit tests construct subjects
  directly and never start Koin at all. The container is wiring, not a
  test dependency.

The trade-off, recorded honestly: Koin resolves at runtime, so a missing
binding is a crash on first resolution rather than a compile error. The
mitigation is a `checkModules()` / `verify()` test in the JVM suite, which
turns the graph back into a build-time check.

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

### 6.1 Where tests run

The architecture in §2.3 exists largely to make this table lopsided: almost
everything is a plain JVM test, so the suite runs in seconds with no emulator.

| Subject | Kind | Device |
|---|---|---|
| `WeekProvider` | JVM unit, fixed `Clock` | No |
| `StatusResolver` | JVM unit, table-driven | No |
| DTO parsing / mappers | JVM unit, committed fixture | No |
| `WorkoutApi` | JVM unit, Ktor `MockEngine` | No |
| `WorkoutRepositoryImpl` | JVM unit, fake DAOs | No |
| `CalendarViewModel` | JVM unit, fake repository + test dispatcher | No |
| Koin graph | JVM unit, `verify()` | No |
| DAO / migrations | Instrumented, in-memory Room | Yes |
| Composables | Instrumented, Compose UI test | Yes |
| Visual fidelity | Screenshot vs design | Yes |

### 6.2 Libraries

`junit4`, `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`),
`turbine` (Flow assertions), `ktor-client-mock`, `koin-test`,
`androidx.room:room-testing`, `androidx.compose.ui:ui-test-junit4`.

JUnit 4, not 5: `androidx.test` rules and `createComposeRule()` are JUnit4-based,
and JUnit 5 does not run instrumented tests at all, so adopting it would mean
two frameworks in one project (Drill 00 §3).

A `MainDispatcherRule` calling `Dispatchers.setMain(...)` is required for every
ViewModel test — `Dispatchers.Main` does not exist on the JVM, and its absence
fails with a message that does not obviously point at the cause.

Test doubles are **hand-written fakes**, not a mocking framework. The
interfaces here have two or three methods; a fake is shorter than the stubbing
it replaces and does not break when a signature changes.

### 6.3 Naming

```kotlin
// src/test — JVM
fun `week containing a Sunday starts on the preceding Monday`() { }

// src/androidTest — instrumented
fun longWorkoutTitleTruncatesWithEllipsis() { }
```

Backtick sentences describing behaviour, so a failure name explains the broken
rule without opening the file.

**Instrumented tests are the exception.** Method names containing spaces were
rejected by the platform before API 30, and minSdk here is 24 — a backtick name
in `src/androidTest` fails on exactly the older devices the app supports. Those
use camelCase. See Drill 00 §6.

### 6.4 The matrices

These are the tests that matter. Everything else is incidental.

**`WeekProvider`** — the API carries no dates, so this mapping is load-bearing.

| Case | Input | Expect |
|---|---|---|
| Mid-week | Sat 2026-09-19 | Mon 2026-09-14 … Sun 2026-09-20 |
| Week start | Mon 2026-09-21 | that Monday is index 0 |
| **Sunday boundary** | Sun 2026-09-20 | Mon 2026-09-14 … Sun 2026-09-20 — *not* the following week |
| Month boundary | Mon 2026-09-28 | … Sun 2026-10-04, 7 consecutive dates |
| Year boundary | Mon 2025-12-29 | … Sun 2026-01-04 |
| DST transition | Europe/London, w/c Mon 2026-03-23 | 7 dates; spring-forward Sunday does not collapse a day |
| Timezone | same `Instant`, two zones | may fall in different local weeks |
| Shape | any | exactly 7, strictly consecutive, index 0 = Monday |

The Sunday case is the one that matters: many week calculations treat Sunday as
the *first* day, which silently shows the wrong week one day in seven.

**`StatusResolver`** — two steps, tested separately.

Step 1, completion resolution (override wins):

| `override` | `storedStatus` | `isCompleted` |
|---|---|---|
| `true` | any | `true` |
| `false` | any | `false` |
| `null` | `COMPLETED` | `true` |
| `null` | `ASSIGNED` / `MISSED` | `false` |

Step 2, display derivation:

| Day position | `isCompleted` | `DisplayStatus` |
|---|---|---|
| Past | `true` | `COMPLETED` |
| Past | `false` | `MISSED` |
| Today | `true` | `COMPLETED` |
| Today | `false` | `ASSIGNED` |
| Future | `true` | `UPCOMING` |
| Future | `false` | `UPCOMING` |

The last two rows are the point: a future day greys out **even when marked
complete**. A single-step implementation passes casual testing and fails here.

**Parsing** — asserted against the committed fixture, not hand-written JSON:

- 7 day entries, indices 0–6
- 6 assignments total
- day 2 and day 5 empty; **day 4 holds two**
- `_id` → `id`, `total_exercise` → `totalExercise`
- unknown JSON fields are ignored, not fatal
- malformed body surfaces `DataError.Parsing`, never a raw exception

**`WorkoutRepositoryImpl`** — where the two hard requirements live:

| Test | Asserts |
|---|---|
| Warm start emits cache first | cached content arrives **before** the network call completes (rung 3.3) |
| Cold start | empty → content after refresh |
| Refresh failure is non-destructive | cached content still emitted; `DataError` returned; nothing cleared (rung 3.4) |
| Toggle writes only the override table | `workout_assignments` byte-identical after a toggle |
| **Refresh preserves overrides** | mark complete → refresh returns `status=ASSIGNED` → still reads as completed (rung 5.5) |
| Toggle targets one id | two assignments on day 4; only the tapped one changes (rung 5.2) |

The ordering assertion needs a controllable source — a suspended network fake
that the test resumes explicitly. A fake that returns instantly passes even
when the implementation awaits the network first, which is the exact bug.

**`CalendarViewModel`**

- `weekDates` is populated in the **initial** state, before any emission
- `isLoading` true → false across a refresh
- failed refresh: `error` set **and** `days` still populated
- `ToggleCompletion` delegates by id; no business logic in the ViewModel

**Koin** — `verify()` / `checkModules()` in the JVM suite, converting Koin's
runtime resolution back into a build-time failure (§2.5).

### 6.5 What is deliberately not tested

Named so their absence reads as a decision:

- Generated Room and serialization code
- Compose layout that a screenshot check already covers
- Getters, `data class` equality, enum mapping with no branching
- The live endpoint. Tests use the committed fixture; a third party being down
  must never turn the suite red.

No coverage percentage is targeted. The matrices above are the target; a
percentage would reward testing the list above.

### 6.6 The gate

`./gradlew build` runs compilation, unit tests and lint, and exits non-zero on
any failure. That single command is the feedback loop — an agent or a CI step
can verify its own work without interpreting output.

Instrumented tests are **not** in the gate: they need a device and would make
the loop too slow to run after every change. They run before a rung involving
UI is marked done.

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
