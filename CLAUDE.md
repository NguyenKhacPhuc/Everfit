# CLAUDE.md

Android training-calendar app.

**Start with [`docs/sdlc/WORKFLOW.md`](docs/sdlc/WORKFLOW.md)** — the loop, the
reading order, and current status. This file is rules only; it does not tell you
what to do next.

Design record: [`spec.md`](docs/sdlc/spec.md) ·
Tests: [`testing.md`](docs/sdlc/testing.md) ·
Rungs: [`intents/`](docs/sdlc/intents/) ·
Drills: [`plans/`](docs/sdlc/plans/)

## Commands

```bash
./gradlew build     # compile + unit tests + lint  (the gate)
./gradlew test      # unit tests only
./gradlew lint      # Android lint only
```

Healthy output ends with `BUILD SUCCESSFUL` and exit code 0.

**Never pipe a build to another command when checking whether it passed.**
`./gradlew build | tail` returns *tail's* exit status and reports green over a
failing build. Redirect to a file and check `$?`:

```bash
./gradlew build > /tmp/b.log 2>&1; echo "exit=$?"
```

This already produced one false pass in this repo.

## Execution gate

**Do not write production code, build files or dependency changes until the
drill for that intent has been explicitly approved.**

Protocol: [`docs/sdlc/drill-protocol.md`](docs/sdlc/drill-protocol.md). A drill
covers options with pros and cons, a recommendation, what it affects, blocking
vs non-blocking open questions, an estimate with confidence, risks, and
priority. Approval means clear agreement — a question or an unrelated reply is
not approval.

Investigating, reading and writing documents need no gate.

If execution shows the approved approach is wrong, stop and re-drill rather
than silently substituting a different design.

## Test first

**Write the failing test before the implementation, and run it to see it fail.**
Scope and rationale: [`docs/sdlc/WORKFLOW.md`](docs/sdlc/WORKFLOW.md).

- The cases are already specified. `docs/sdlc/testing.md` is the test list, not
  a summary of one — transcribe it, do not invent cases.
- A test that passes before the implementation exists asserts nothing and looks
  exactly like one that works. Seeing red is the only thing that distinguishes
  them.
- Test and implementation land in the **same commit**; the rung is the unit.
- Exceptions, and only these: build wiring (nothing to assert beyond the gate),
  visual fidelity (not expressible as an assertion), instrumented DAO tests
  (device loop too slow) — test-after is acceptable there.
- Fixing a bug? Write the failing test first, always. No exceptions.

## Architecture rules

Layered MVVM with the Clean Architecture dependency rule. Dependencies point
inward: `ui -> domain <- data`.

- `domain/` imports **no Android and no library types** — no `android.*`, no
  Room, no Ktor, no Compose, no `Context`. Kotlin stdlib and plain JDK types
  (`java.time`) are fine: they are what keep domain tests on the JVM.
- Never `LocalDate.now()` — take a `Clock`. Never `WeekFields.of(locale)` for
  the week start; it yields Sunday-first in some locales, which is correct
  generally and wrong here. Use `previousOrSame(MONDAY)`.
- `ui/` must never reference DTOs or Room entities.
- `data/` must never reference Compose or ViewModel types.
- The `WorkoutRepository` interface lives in `domain`; its implementation lives
  in `data`.
- Model chain is `DTO -> Entity -> Domain -> UI`. Do not collapse a pair
  without reading spec §2.4 — each collapse has a named cost.
- Dispatchers are injected, never referenced directly. No bare
  `Dispatchers.IO` outside the Koin module.
- `Clock` is injected. Never call `LocalDate.now()` — it makes "today"
  untestable.

## Invariants that protect known traps

These come from real properties of the fixture. Breaking one produces a bug
that still demos correctly.

- **Never write to `workout_assignments` during a completion toggle.** Local
  marks live in `completion_overrides`. A refresh replaces the former and must
  never touch the latter, or the user's tap silently reverts.
- **Address workouts by `id`, never by list position or title.** "Legs day"
  appears on two different days and day 4 holds two workouts.
- **Future days render greyed regardless of stored status.** `DisplayStatus` is
  derived against today, not read from the server.
- **Never clear the cache on a refresh failure.** Cached content stays on
  screen; the error is surfaced alongside it, not instead of it.

## MVI rules

Store pattern follows `/Users/phucnguyen/Documents/mvi-search` — read
`mvi/MviViewModel.kt` there before changing anything in `mvi/`.

- **Intents are requests, Results are facts.** Never send an Intent into the
  reducer, and never let a Result mean "please do something".
- **Every async Result carries its request parameters**, so the reducer can drop
  stale work synchronously. Without them a late response overwrites fresh state.
- **`reduceCalendar` is a top-level function**, passed as `::reduceCalendar`.
  Do not make it a method — having no `this` is what stops it reading a
  repository or a `var` by accident.
- **The ViewModel holds no mutable state.** It wires pipelines. If you need to
  write state, emit a Result.
- **`currentState` is for starting work, never for deciding a transition.**
  That is the reducer's job.
- Effects go through a `Channel`, never a `StateFlow`, or they replay on
  rotation.
- Pick the flattening operator deliberately: `flatMapFirst` for refresh (a
  double tap must not start two, nor cancel the first), `flatMapConcat` for
  toggles (independent facts, neither may cancel the other), `flatMapLatest`
  only where cancelling is correct.
- Derived values (`showsFullScreenError`, `isRefreshing`) are computed
  properties on state, never stored fields.

## Conventions

- Compose screens are stateless: state in, events out. No business state in
  `remember`.
- UI events are a sealed interface, so a new interaction fails to compile until
  handled.
- Colours, spacing and type come from `ui/theme`. **No inline hex, dp or sp
  literals in composables** — if a token is missing, add it to the theme.
- Status colours are semantic tokens on `LocalEverfitColors`, never Material
  `ColorScheme` slots.
- Koin uses the plain DSL. Do not add Koin Annotations — it reintroduces KSP.

## Testing

Full matrices: [`docs/sdlc/spec.md`](docs/sdlc/spec.md) §6. Rules:

- Domain, data and ViewModel logic is JVM-testable. No device, no Robolectric.
- **Never hit the live endpoint in a test.** Use the committed fixture at
  [`docs/sdlc/api-sample-response.json`](docs/sdlc/api-sample-response.json).
  A third party being down must not turn the suite red.
- Ktor is tested with `MockEngine`; Room DAOs with an in-memory database.
- **Hand-written fakes, not a mocking framework.** These interfaces have two or
  three methods.
- Test names are backtick sentences describing the behaviour, so a failure
  explains itself without opening the file — **but camelCase in
  `src/androidTest`**: method names with spaces fail below API 30 and minSdk
  is 24.
- ViewModel tests need the `MainDispatcherRule`. `Dispatchers.Main` does not
  exist on the JVM.
- Every new rung adds its tests before it is marked done. A rung is not green
  because it compiles.

Three tests are load-bearing — do not weaken them:

1. **Cache-before-network ordering.** Needs a suspended network fake the test
   resumes explicitly. A fake returning instantly passes even when the
   implementation awaits the network first.
2. **Refresh preserves overrides.** Mark complete, refresh with a server
   response saying `ASSIGNED`, assert it still reads completed.
3. **Future days grey out even when completed.** A single-step status
   implementation passes everything else and fails this.

Do not target a coverage percentage. The §6 matrices are the target.

## Project facts worth remembering

- AGP 9 compiles Kotlin with **no separate Kotlin plugin** applied. Bundled
  Kotlin is **2.2.10** — Compose and KSP plugin versions must match it.
- **KSP needs `android.disallowKotlinSourceSets=false`** in `gradle.properties`.
  AGP 9's built-in Kotlin otherwise rejects the generated source registration.
  Do not remove that line; Room stops compiling.
- The app is **Compose-only and light-only**. No AppCompat, no Material XML, no
  `values-night`. `res/values/themes.xml` is a bare window theme; all styling
  lives in `ui/theme`.
- The mock API returns **no dates** — only `day: 0..6`, mapped onto the current
  Monday–Sunday week.
- `status` values `0/1/2` are **undocumented**; the working assumption is
  `0=ASSIGNED, 1=COMPLETED, 2=MISSED`, pending confirmation from the design.
- Of the two API URLs in the brief, only `https://mock.internalef.com/workouts`
  is reachable.
