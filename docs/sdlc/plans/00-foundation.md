# Drill 00 — Foundation

Status: **DRAFT — awaiting approval** · Covers rungs 0.2–0.4

## 1. Goal

A launchable app with Compose, Koin, Room and Ktor wired in, **a working unit
test harness**, and a single command that provably fails when something breaks.

## 2. Options — library wiring order

**A — Big bang.** Add all four libraries plus the test harness, then build.

**B — Incremental, green between each.** Compose → Koin → Ktor → Room, building
after every addition, each library's test companion added with it.

**C — Throwaway spike, then redo properly.**

| | Pros | Cons |
|---|---|---|
| A | One build cycle if it works | On failure there are four suspects and no bisect; this toolchain already surprised us once |
| B | A failure names its own cause immediately; matches the ladder rule | 4 build cycles, ~2 min each |
| C | Fastest discovery of a blocking incompatibility | The work is thrown away — and B discovers the same thing on step one |

**Recommendation: B.** Correcting Drill 0: I proposed a timeboxed spike first,
but incremental addition *is* the spike, and the work survives.

## 3. Options — test framework

**A — JUnit 4.** **B — JUnit 5.** **C — kotlin.test.**

- **A** — Pros: what AGP ships by default; `androidx.test` rules and
  `createComposeRule()` are JUnit4-based; zero configuration. Cons: dated API.
- **B** — Pros: nicer parameterised tests, which suits §6.4's matrices. Cons:
  needs a third-party Gradle plugin, and **does not work for instrumented
  tests** — so the project would run two frameworks. On a brand-new AGP that is
  gratuitous risk.
- **C** — Pros: multiplatform-friendly assertions. Cons: still needs a runner
  underneath; solves nothing here.

**Recommendation: A.** §6.4's matrices are expressible as plain loops or
repeated cases; that is not worth a second framework and a plugin.

## 4. Options — test dependency placement

**A — All test libraries up front.** **B — Each with its production library.**

**Recommendation: B**, matching §2. `ktor-client-mock` arrives with Ktor,
`room-testing` with Room. The JVM core — `junit`, `kotlinx-coroutines-test`,
`turbine` — goes in first, because rung 1.1's tests need nothing else.

Consequence worth stating: `koin-test` is only needed for the `verify()` check
from spec §2.5, since every other test uses constructor injection directly.

## 5. Harness pieces that are easy to forget

These are not optional extras — each causes a confusing failure if missed.

- **`MainDispatcherRule`.** `Dispatchers.Main` does not exist on the JVM. Without
  a rule calling `Dispatchers.setMain(...)`, every ViewModel test fails with
  "Module with the Main dispatcher had failed to initialize". Needs writing once
  in `src/test`.
- **Core library desugaring.** Drill 01 §2 — `java.time` at minSdk 24. Enable it
  in this pass; it affects tests too.
- **A canary verification.** Rung 0.4 is not satisfied by tests passing. It is
  satisfied by *deliberately breaking one*, observing a non-zero exit, and
  reverting.
- **Fixture on the test classpath.** §6 reads
  `docs/sdlc/api-sample-response.json`. Either copy it to
  `src/test/resources/` or point a source set at it — decide at the build.

## 6. Backtick test names break instrumented tests

Spec §6.3 specifies backtick sentence names. On the JVM that is fine. In
`androidTest` it is **not**: method names containing spaces were rejected by the
platform before API 30, and **minSdk here is 24**.

Options:

**A — Backticks in `src/test`, camelCase in `src/androidTest`.**
**B — camelCase everywhere.**
**C — Backticks everywhere, run instrumented tests only on API 30+.**

**Recommendation: A.** Unit tests are the overwhelming majority (§6.1) and keep
the readable names; instrumented tests take a naming convention that runs
anywhere. C silently narrows the devices the suite can run on, which is a poor
trade for a naming style.

Spec §6.3 is amended accordingly.

## 7. Affects

Blocks all six other intents. Locks library *and test library* versions into
`libs.versions.toml`. Does not foreclose any architectural decision in spec §2.

## 8. Open questions

**Blocking:** none. Fully unblocked.

**Non-blocking:**
- Compose BOM version — newest compatible, decide at the build.
- The generated `ExampleUnitTest` / `ExampleInstrumentedTest`: lean **keep both
  until the real equivalents exist** — they prove each harness runs at all —
  then delete. Deleting first removes the only evidence the harness works.
- JVM toolchain pinning for reproducible test runs — nice, not required.

## 9. Estimate

**2–3.5h, low-to-medium confidence.** Raised from 1.5–3h: the test harness is
roughly 30–45 min that was previously unaccounted for.

Split: library wiring 60–90 min · test harness + `MainDispatcherRule` + fixture
30–45 min · gate verification 15 min · troubleshooting buffer 30–60 min.

Blows up if: no Compose-compiler or KSP release matches AGP 9.1.1 / Gradle
9.3.1. That is a version-matrix problem resolved by moving AGP, not by trying
harder.

## 10. Risks

| Risk | Early warning |
|---|---|
| **Room requires KSP** — see below | KSP plugin fails to resolve against Gradle 9.3.1 |
| Compose compiler plugin mismatch | First Compose build fails at plugin application |
| Gate passes without running tests | The canary check in §5 is the only thing that proves otherwise |
| Desugaring missed | Date code compiles, then crashes at runtime on API 24 |

**Room brings KSP back.** Spec §2.5 justified Koin partly by avoiding annotation
processing — true *for DI*, but Room needs a KSP processor regardless. The stack
is not KSP-free, and this is the most likely thing to fail. Fallback in Drill 03
§4, trigger 45 minutes.

## 11. Priority

**First.** Blocks everything and is the lowest-confidence item on the board.
Failing here at hour 1 is recoverable; at hour 6 it is not.
