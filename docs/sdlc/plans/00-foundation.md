# Drill 00 — Foundation

Status: **DRAFT — awaiting approval** · Covers rungs 0.2, 0.3

## 1. Goal

A launchable app with Compose, Koin, Room and Ktor wired in, and a single
command that provably fails when something breaks.

## 2. Options

**A — Big bang.** Add all four libraries plus the test harness, then build.

**B — Incremental, green between each.** Compose → Koin → Ktor → Room, building
after every addition.

**C — Throwaway spike, then redo properly.** Prove the version matrix in a
scratch project first.

## 3. Pros / cons

| | Pros | Cons |
|---|---|---|
| A | One build cycle if it works | On failure there are four suspects and no bisect; this toolchain already surprised us once |
| B | A failure names its own cause immediately; matches the ladder rule | 4 build cycles, ~2 min each |
| C | Fastest possible discovery of a blocking incompatibility | The work is thrown away — and B already discovers the same thing on the first step |

## 4. Recommendation

**B.** Correcting what I proposed in Drill 0: I suggested a timeboxed spike
first, but incremental addition *is* the spike. If Compose fails against AGP
9.1.1 it fails at step one, just as fast as C would reveal it, and the work
survives. C only wins if we expect to throw the whole stack away.

## 5. Affects

Everything — this blocks all six other intents. Locks library versions into
`libs.versions.toml`. Does not foreclose any architectural decision in spec §2.

## 6. Open questions

**Blocking:** none. This intent is fully unblocked.

**Non-blocking:**
- Compose BOM version — take the newest compatible, decide at the build.
- Whether `ExampleInstrumentedTest`/`ExampleUnitTest` survive or are replaced
  by real tests (lean: delete once rung 1.1 lands).

## 7. Estimate

**1.5–3h, low-to-medium confidence.**

Blows up if: the Compose compiler plugin or KSP has no release matching AGP
9.1.1 / Gradle 9.3.1. That is a version-matrix problem, not a coding problem,
and it is resolved by moving AGP rather than by trying harder.

## 8. Risks

| Risk | Early warning |
|---|---|
| **Room requires KSP** — see below | KSP plugin fails to resolve against Gradle 9.3.1 |
| Compose compiler plugin mismatch | First Compose build fails at plugin application |
| Desugaring needed for `java.time` at minSdk 24 | Drill 01 §2 — cheaper to enable now, in the same pass |

**Room brings KSP back.** Spec §2.5 justified Koin partly by avoiding
annotation processing, which is true *for DI* — but Room needs a KSP processor
regardless. The stack is therefore not KSP-free, and this is the single most
likely thing to fail. If KSP will not resolve, Drill 03 §2 has the fallback.

## 9. Priority

**First.** Blocks everything, and is the lowest-confidence item on the board.
Failing here at hour 1 is recoverable; failing at hour 6 is not.
