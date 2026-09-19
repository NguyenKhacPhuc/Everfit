# Spec: Training Calendar

Status: **Draft** · Date: 2026-09-19 · Stage 2 (Design)
Reads: [`intent.md`](intent.md) and [`intents/`](intents/)

## 0. What this document is

> **Everything that was decided and must stay true, plus the reason.**

The test for what belongs:

> If changing *how* something is implemented — without changing *what was
> decided* — would make a section wrong, it does not belong.

A spec that drifts from the code is worse than none, because it is trusted.

| In | Out |
|---|---|
| Layer boundaries | How a function is written |
| Contracts and persisted shapes | Wiring and configuration |
| Domain rules, with their truth tables | Exhaustive file listings |
| Decisions, rejected alternatives, reasons | Anything a refactor invalidates |

Defined by the *kind* of statement, not by topic. Domain rules belong here (§4)
even though they are neither technology nor architecture — they are decisions
the implementation must satisfy, and the most expensive to get wrong.

| Content | Home |
|---|---|
| Why a decision was taken | This document |
| How an intent is executed, estimates, risks | `plans/` |
| Rules an agent must not break | `CLAUDE.md` |
| Definition of done for a rung | `intents/` |
| Exact wiring | The code |

### Source map

Where each contract lives. The spec states the **rule**; the file holds the
**declaration**. Once a file exists, this document points at it rather than
restating it — that is what stops the two drifting.

| Contract | File | Rule in |
|---|---|---|
| MVI store, `flatMapFirst` | `core/mvi/` — adopted from `mvi-search`, not reinvented | §2.8 |
| Screen contract (State, Intent, Effect) | `feature/calendar/CalendarContract.kt` | §5.1 |
| Results + reducer | `feature/calendar/CalendarReducer.kt` | §4.2, §5.2 |
| Repository boundary | `core/domain/WorkoutRepository.kt` | §2.3 |
| Outcome type | `core/model/Result.kt` (no imports); builders in `data/base/ResultExt.kt` | §2.7 |
| Persisted shape | `data/local/entity/` | §3.2 |
| Week rule | `core/domain/WeekProvider.kt` | §4.1 |
| Design tokens | `core/ui/theme/` — `EverfitTheme` wraps `MaterialTheme` | §2.8 |
| Status rule | `core/domain/StatusResolver.kt` | §4.2 |

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

```
core/      depended on by everything; depends on nothing in the app
  model/     Result, domain models — no imports at all
  domain/    rules + repository contracts — no framework types at all
  mvi/       the store
  ui/theme/  design tokens
  ext/       small helpers
data/      Ktor, Room, mappers, repository implementation
feature/   one package per screen: contract, reducer, ViewModel, components
di/        Koin module
```

Two rules make the direction checkable rather than aspirational:

- **`core` may not import `data`, `feature` or `di`.** It is depended on by all
  of them, so anything reachable from `core` is reachable from everywhere.
- **`data` may not import `feature`, `core.ui` or `core.mvi`.**

Screen-specific composables live under `feature/<screen>/components`, not in
`core/ui`. A component that takes a feature's UI model is not shared, and
putting it in `core` inverts the dependency — `core/ui` holds the theme and
anything genuinely reused.

### 2.3 Layer contracts

| Layer | May depend on | Must not contain |
|---|---|---|
| `feature` | `core` | Ktor, Room, DTOs, entities |
| `core/domain` | *nothing* | Any Android or library type |
| `core/model` | *nothing* | Android or Ktor types |
| `data` | `core` | Compose, ViewModel, feature models |

The rule that matters: **`domain` imports nothing.** No `android.*`, no Room,
no Ktor, not even `Context`. That is what makes the JVM-only test story real.

`WorkoutRepository` is declared in `domain` and implemented in `data`, so the
arrow points inward at the boundary that would otherwise invert. Three methods:
observe the week as a `Flow`, refresh (returning `Result.Success` or
`Result.Error`), and toggle a completion by id.

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
DTO→Entity→Domain, `feature/mapper` owns Domain→UI.

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

Typed at the boundary, not thrown across it. A failure becomes a
`Result.Error(code, message)`; the code is an HTTP status where there was one,
otherwise an `ApiError` constant:

| Code | Means |
|---|---|
| HTTP status (e.g. `500`) | Reached the server, which refused |
| `ApiError.NETWORK` | No connectivity, DNS failure, TLS, timeout |
| `ApiError.SERVER` | Connection refused |
| `ApiError.UNKNOWN` | Anything else, including an unparseable body |

Mapping lives in `data/base/ResultExt.kt`, so nothing above `data` sees a Ktor
or serialization exception and replacing the HTTP client touches one file.

Known sharp edge, recorded rather than patched: the shared `toResult()` has no
branch for deserialization failures, so a malformed body lands on `UNKNOWN`
rather than a distinct parsing code. Adding `ApiError.PARSING` would change a
convention shared with other projects.

Errors are **non-fatal by design**: a failed refresh sets `load = Failed` and
leaves cached content untouched (rung 3.4).

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
- Design tokens live in `core/ui/theme`, referenced by name, never inline hex.
  Status colours are **semantic tokens** on an extended theme object
  (`LocalEverfitColors`), not Material `ColorScheme` slots — `error` meaning
  "missed" would be a lie every reader has to decode. See Drill 00 §5b.

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
| Future days show a stale label (rung 2.3) | `DisplayStatus` computed in `feature/mapper` against today, not stored (§2.4) |

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
StoredStatus   = ASSIGNED(0) | MISSED(1) | COMPLETED(2)    // confirmed from the design
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

Declared in `feature/calendar/CalendarContract.kt`.

| Field | Purpose |
|---|---|
| `weekDates` | Seven dates, from `WeekProvider` **at construction** — never from the network |
| `days` | Content |
| `load` | Status: `Idle` · `Refreshing` · `Failed(message)` — orthogonal to content |

Derived, never stored:

| Property | Definition |
|---|---|
| `showsFullScreenError` | `load is Failed` **and** every day empty |
| `isRefreshing` | `load is Refreshing` |

`weekDates` being populated synchronously is what makes the brief's loading
requirement — correct dates, empty data — fall out rather than be retrofitted.

**Revision.** This previously specified flat `isLoading: Boolean` +
`error: ErrorType?`, rejecting a sealed type because a failed refresh over good
cache is both content-bearing and errored. That rejected the wrong thing: what
fails is collapsing *content and status into one* hierarchy. Separate `days` and
`load` fields express the awkward case exactly, while making
`Refreshing && Failed` unrepresentable — which two booleans cannot.

### 5.2 Sequences

```
                    ┌──────────────── Room Flow ──────────────┐
                    │                                          ▼
Screen ─onIntent─> pipelines ─Result─> reduce(S,R) ─> StateFlow ─> Screen
                       │                    │
                    Ktor/DAO            effectFor ─> Channel<E>
```

| Sequence | Results, in order | Net effect |
|---|---|---|
| **Cold start** | `CachedLoaded([])` · `RefreshStarted` · `RefreshSucceeded` · `CachedLoaded(days)` | Dates first, then content |
| **Warm start** | `CachedLoaded(days)` · `RefreshStarted` · `RefreshSucceeded` | Content before any network call |
| **Toggle** | *(write)* · `CachedLoaded(days)` | Checkmark; no Result of its own |
| **Refresh fails** | `RefreshStarted` · `RefreshFailed` | `load = Failed`, `days` untouched, snackbar |

Two properties fall out of the reducer rather than being enforced by care: no
branch clears `days` on failure (rung 3.4), and the toggle writes a different
table from refresh, so they cannot race (rung 5.5).

## 6. Testing strategy

Moved to [`testing.md`](testing.md) — it answers a different question and grows
at a different rate.

The decision it records: domain, data and ViewModel logic is JVM-testable with
no device and no Robolectric. That is a constraint on §2.3, not a testing
preference — it is why `domain` may import nothing.

## 7. Open decisions

- [x] **Status enum semantics — RESOLVED 2026-09-19 from the design.**
      `0 = ASSIGNED, 1 = MISSED, 2 = COMPLETED`. Note this is **not** the
      assumption previously recorded here, which had 1 and 2 reversed: the
      fixture's `status=1` items render as "Missed" and its single `status=2`
      item renders as "Completed". Had the assumption shipped, every past
      workout would have shown the opposite state.
- [x] **Design tokens — RESOLVED.** Extracted from `docs/design/training.png`:
      purple `#7470EF`, card `#F7F8FC`, text `#1E0A3C`, secondary `#7B7E91`,
      missed `#FF5E5E`, divider `#F1F1F1`. Spacing refined in Intent 04.
- [ ] **DataStore.** Not adopted. Room covers both cached workouts and the
      completion overlay, and a sync timestamp is a column rather than a reason
      for a second persistence library. Revisit only if genuine user
      preferences appear.
- [ ] **Compose plugin wiring.** AGP 9 compiles Kotlin with no separate Kotlin
      plugin applied (verified in PR #1), but enabling Compose still requires
      the Compose compiler plugin. Exact wiring is verified in rung 0.2 rather
      than assumed here.
