# Everfit — Training Calendar

A one-week training calendar for Android. Shows a Monday–Sunday week of workouts
with their status, and lets you mark a workout complete locally.

| | |
|---|---|
| **Video walkthrough** | [Watch (4 min)](https://drive.google.com/file/d/12o4v8Sqv-My0xsBSauT60NYmf-dz9Zfi/view?usp=sharing) |
| **Design** | [Figma](https://www.figma.com/design/APChc5i8CKTSn7CZg7Gg36/Everfit?node-id=23776-49540) · exports in [`docs/design/`](docs/design/) |
| **Tests** | 58 JVM unit tests — no emulator required |

## Build and run

Requires JDK 17+ and the Android SDK (API 37 installed).

```bash
git clone https://github.com/NguyenKhacPhuc/Everfit.git
cd Everfit
./gradlew installDebug          # build + install on a running device/emulator
```

Other useful targets:

```bash
./gradlew build     # compile + unit tests + lint — the gate, non-zero on any failure
./gradlew test      # unit tests only
```

`local.properties` is not committed. Android Studio writes it on first open; from
the CLI, set `ANDROID_HOME` or create it with `sdk.dir=/path/to/Android/sdk`.

**Note on `./gradlew build`** — do not pipe it when checking whether it passed.
`./gradlew build | tail` returns *tail's* exit status and will report green over a
failing build. Redirect and check `$?`.

## Architecture

**Layered MVI, applying the Clean Architecture dependency rule.** Described
precisely rather than branded: the dependency rule and dependency inversion are
adopted; a use-case/interactor layer is deliberately not, because three
operations would become three pass-through classes. Full reasoning and the
rejected alternatives are in [`docs/sdlc/spec.md`](docs/sdlc/spec.md).

```
Screen ─onIntent─> pipelines ─Result─> reduce(S,R) ─> StateFlow ─> Screen
                       │                    │
                    Ktor/Room           effectFor ─> Channel<Effect>
```

| Concern | Choice |
|---|---|
| UI | Jetpack Compose |
| Presentation | MVI — pure top-level reducer, effects via `Channel` |
| Persistence | Room (two tables — see below) |
| Networking | Ktor + kotlinx.serialization |
| DI | Koin (plain DSL — no KSP beyond Room's) |
| Dates | `java.time` + core library desugaring (minSdk 24) |

The MVI store (`mvi/MviViewModel.kt`, `flatMapFirst`) is ported from a sample I
wrote previously; the reducer is a top-level function so it has no `this` and
cannot read a repository even by accident.

### Three decisions worth explaining

**Two tables, not one.** `workout_assignments` holds server truth;
`completion_overrides` holds the user's marks. A refresh replaces the former and
structurally cannot touch the latter. The one-table version — a mutable
`is_completed` column — is less code and silently reverts the user's tap on the
next refresh, a bug that only appears *after* a refresh. Verified on device: mark
a workout complete, restart the app, and the mark survives a refresh in which the
server still reports it as missed.

**Stored status is not displayed status.** The brief defines status by temporal
position, so the same value renders differently depending on the day. A future
day greys out *even when marked complete*. These are two separate tested steps;
folding them into one passes every other case and fails exactly there.

**Content and load status are separate fields.** A failed refresh over good
cached data is simultaneously content-bearing and errored, which a single
`Loading | Content | Error` hierarchy cannot express. `days` and a sealed `load`
express it exactly, while still making "refreshing and failed at once"
unrepresentable.

### Testing

58 JVM tests, no emulator required — the domain layer imports no Android types.

- **Tier 1 (reducer, domain rules, parsing):** plain function calls. No
  dispatcher, no fakes, no `runTest`.
- **Tier 2 (ViewModel, repository):** fakes plus a `MainDispatcherRule`. Reserved
  for what a pure function cannot reach — chiefly operator choice.

Two tests are load-bearing and were each written to fail first:

- **Cache emits before the network returns.** The fake remote is held open by the
  test. A fake that returned instantly would pass even against an implementation
  that awaits the network first.
- **A future day is upcoming even when completed.** A single-step status
  implementation passes every other row.

Tests read a committed capture of the API ([`workouts.json`](app/src/test/resources/workouts.json)),
never the live endpoint, so a third party being down cannot turn the suite red.

## AI Collaboration

**Tools:** Claude Code (Opus 5) in the terminal, driving the whole session —
planning, implementation and on-device verification. Android Studio for the
emulator and previews.

**How I worked.** I did not ask for the app in one prompt. I set up a process
first, then drove it: requirements were captured as intents, each intent was
"drilled" (options, trade-offs, estimate, risks) and I approved it before any
code was written, and every rung landed test-first with a green build. The
artefacts are in [`docs/sdlc/`](docs/sdlc/) and the git history follows them.

### Prompts that did the most work

**1 — Establishing the process instead of asking for code**

> "before we start I want to design this into AI SDLC following by Claude
> https://claude.com/blog/the-ai-native-sdlc-playbook"

and then, when the plan was too coarse to review:

> "we needed drill the intent before execution, meaning discuss about solution,
> pros and cons of each solution, affection, open questions, remaining questions,
> estimation then prioritize base on what we discuss. Only start execute after my
> agreements"

This is the prompt I would keep. It converted the assistant from a code generator
into something I could review *before* it spent my time. The drill for the
foundation predicted that Room's KSP would clash with AGP 9 and pre-agreed a
fallback with a 45-minute trigger. It did clash, exactly there — and because the
decision was already made, it cost about ten minutes instead of an afternoon.

**2 — Imposing my own architecture rather than accepting the default**

> "for this we apply MVI, not MVVM, I have sample which I've done on my own here,
> you can check for reference" (with a path to my `mvi-search` project)

Pointing at working code of my own was far more effective than describing the
pattern. It also corrected an earlier decision of mine: I had specified flat
`isLoading` + `error` fields, and reading my own sample back showed that a sealed
`Load` *alongside* content expresses "failed refresh over good cache" exactly,
while making impossible combinations unrepresentable.

**3 — Auditing the process for what was missing**

> "I saw the flow, no TDD found"

The workflow said "implement → verify", which is test-after. Making it red/green
turned out to matter twice: the cache-ordering test initially passed for the
wrong reason, and two error-mapping bugs in the Ktor layer (a non-2xx reaching
the deserializer, and Ktor wrapping serialization failures in its own exception)
only surfaced because the failure cases were written before the implementation.

### What I did not delegate

Architecture decisions, the MVI store design, and the review at every gate. Where
AI drafted non-trivial logic — the week/date maths, the SQL merge — there is a
comment in the code saying so and what I changed. I can explain every file in
this repository.

## End-to-end process (bonus)

What I actually do when a feature request arrives, and what I did here:

1. **Capture intent before design.** Write down the problem, the outcome, the
   constraints and the open questions. Ambiguities surface here, cheaply. Doing
   this caught both brief findings above before any code existed.
2. **Design, and record the rejected options.** A decision without its
   alternatives gets relitigated later. `spec.md` keeps the reasoning; `CLAUDE.md`
   keeps the resulting rules in enforceable form.
3. **Break work into rungs small enough to verify.** A rung is done when the
   build is green, its test passes, and it is committed alone — so a regression
   bisects to one step.
4. **Estimate with a named failure mode.** "2–3h unless KSP will not resolve
   against AGP 9" is useful; "2–3h" is not.
5. **Test first where the loop is fast**, and be honest that it does not apply
   uniformly — build wiring and visual fidelity are verified differently.
6. **Verify on the real thing.** Unit tests did not catch the system-bar overlap,
   the dead emulator DNS, or the lint error on `Scaffold`'s padding. A device did.
7. **Leave the reasoning behind.** The commit messages say *why*; the diff already
   says what.

## Repository layout

```
app/src/main/java/com/example/everfit/assignment/
├── mvi/        MviViewModel, flatMapFirst
├── ui/         Compose screen, contract, reducer, theme tokens
├── domain/     Week and status rules — no Android types
├── data/       Ktor, Room, mappers, repository
└── di/         Koin module

docs/sdlc/      intents, spec, testing strategy, drills (the audit trail)
docs/design/    Figma exports
```
