# CLAUDE.md

Android training-calendar app. Design record: [`docs/sdlc/spec.md`](docs/sdlc/spec.md).
Work breakdown: [`docs/sdlc/intents/`](docs/sdlc/intents/).

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

## Architecture rules

Layered MVVM with the Clean Architecture dependency rule. Dependencies point
inward: `ui -> domain <- data`.

- `domain/` imports **nothing** outside `kotlin`/`kotlinx`. No `android.*`, no
  Room, no Ktor, no `Context`. This is what keeps domain tests on the JVM.
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

## Conventions

- Compose screens are stateless: state in, events out. No business state in
  `remember`.
- UI events are a sealed interface, so a new interaction fails to compile until
  handled.
- Colours, spacing and type come from `ui/theme`. No inline hex in composables.
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

- AGP 9 compiles Kotlin with **no separate Kotlin plugin** applied.
- The mock API returns **no dates** — only `day: 0..6`, mapped onto the current
  Monday–Sunday week.
- `status` values `0/1/2` are **undocumented**; the working assumption is
  `0=ASSIGNED, 1=COMPLETED, 2=MISSED`, pending confirmation from the design.
- Of the two API URLs in the brief, only `https://mock.internalef.com/workouts`
  is reachable.
