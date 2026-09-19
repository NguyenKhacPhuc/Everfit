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

Three layers, one module. A multi-module split would cost setup time that a
single-screen app does not repay — noted here as a considered choice, not an
oversight.

```
ui/       Compose screen, state holder (ViewModel), UI state models
domain/   Domain models, status derivation, week calculation
data/     Ktor client + DTOs, Room entities + DAOs, repository
```

Dependencies point inward: `ui -> domain <- data`. Domain holds no Android
framework types, so every rule in section 4 is testable on the JVM with no
device and no Robolectric.

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
- [ ] **DataStore.** Not adopted. Room covers both cached workouts and the
      completion overlay, and a sync timestamp is a column rather than a reason
      for a second persistence library. Revisit only if genuine user
      preferences appear.
- [ ] **Compose plugin wiring.** AGP 9 compiles Kotlin with no separate Kotlin
      plugin applied (verified in PR #1), but enabling Compose still requires
      the Compose compiler plugin. Exact wiring is verified in rung 0.2 rather
      than assumed here.
