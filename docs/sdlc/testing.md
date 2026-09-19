# Testing strategy

Reads: [`spec.md`](spec.md) for the decisions being tested.

A separate document because it answers a different question. `spec.md` records
what was decided; this records how it is proven, and the specific cases that
must pass. The two age at different rates — a decision is stable, a test list
grows with every rung.


## Tiers

From the reference implementation — the split that makes MVI cheap to test.

**Tier 1 — the reducer.** Plain function calls. No dispatcher, no fake, no
`runTest`. `reduceCalendar(state, result)` is a top-level pure function, so a
transition test is two lines and runs in microseconds. **Most rules belong
here**, including every stale-result guard and the whole status matrix.

**Tier 2 — the ViewModel.** Needs `MainDispatcherRule` and a fake repository.
Reserved for what Tier 1 cannot reach: pipeline behaviour — that a double
`Refresh` starts one refresh (`flatMapFirst`), that two rapid toggles both land
(`flatMapConcat`), that effects fire in order with the state that caused them.

**Tier 3 — instrumented.** DAOs and Compose.

The point of the split: a rule tested at Tier 1 costs nothing to run and cannot
flake. Pushing logic into the reducer is therefore also a testing decision.

## Where tests run

The architecture in §2.3 exists largely to make this table lopsided: almost
everything is a plain JVM test, so the suite runs in seconds with no emulator.

| Subject | Kind | Device |
|---|---|---|
| `WeekProvider` | JVM unit, fixed `Clock` | No |
| `reduceCalendar` | **Tier 1** — plain calls, no coroutines | No |
| `StatusResolver` | JVM unit, table-driven | No |
| DTO parsing / mappers | JVM unit, committed fixture | No |
| `WorkoutApi` | JVM unit, Ktor `MockEngine` | No |
| `WorkoutRepositoryImpl` | JVM unit, fake DAOs | No |
| `CalendarViewModel` | **Tier 2** — fake repo + `MainDispatcherRule` | No |
| Koin graph | JVM unit, `verify()` | No |
| DAO / migrations | Instrumented, in-memory Room | Yes |
| Composables | Instrumented, Compose UI test | Yes |
| Visual fidelity | Screenshot vs design | Yes |

## Libraries

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

## Naming

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

## The matrices

**This is a test list, not a summary of one.** Each row is written before the
code it covers (see [`WORKFLOW.md`](WORKFLOW.md)). Transcribe these rows; do not
re-derive them at the keyboard.

Everything not listed here is incidental.

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

**`reduceCalendar` — Tier 1.** Plain calls, no coroutines. Most of the suite.

| Result in | From state | Expect |
|---|---|---|
| `CachedLoaded(days)` | any | `days` replaced, `load` untouched |
| `RefreshStarted` | `Idle` | `load = Refreshing` |
| `RefreshSucceeded` | `Refreshing` | `load = Idle`, **`days` untouched** |
| `RefreshFailed(msg)` | `Refreshing` | `load = Failed(msg)`, **`days` untouched** |
| `RefreshFailed` | `days` populated | `showsFullScreenError == false` |
| `RefreshFailed` | `days` all empty | `showsFullScreenError == true` |
| any | any | `weekDates` never changes after construction |

The two "`days` untouched" rows are rung 3.4 expressed as a transition: no
reducer branch clears content on failure, so it cannot happen.

**`CalendarViewModel` — Tier 2.** Only what Tier 1 cannot reach.

- `weekDates` populated in the **initial** state, before any emission
- **double `Refresh` starts one refresh** — `flatMapFirst`, and the second must
  not cancel the first
- **two rapid `ToggleCompletion` both land** — `flatMapConcat`, neither cancels
  the other
- `RefreshFailed` over populated `days` emits `ShowSnackbar`; over empty `days`
  it does not (that is full-screen state instead)
- effects arrive ordered with the state change that caused them

**Koin** — `verify()` / `checkModules()` in the JVM suite, converting Koin's
runtime resolution back into a build-time failure (§2.5).

## What is deliberately not tested

Named so their absence reads as a decision:

- Generated Room and serialization code
- Compose layout that a screenshot check already covers
- Getters, `data class` equality, enum mapping with no branching
- The live endpoint. Tests use the committed fixture; a third party being down
  must never turn the suite red.

No coverage percentage is targeted. The matrices above are the target; a
percentage would reward testing the list above.

## The gate

`./gradlew build` runs compilation, unit tests and lint, and exits non-zero on
any failure. That single command is the feedback loop — an agent or a CI step
can verify its own work without interpreting output.

Instrumented tests are **not** in the gate: they need a device and would make
the loop too slow to run after every change. They run before a rung involving
UI is marked done.

