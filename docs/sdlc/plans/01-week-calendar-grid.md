# Drill 01 — Week calendar grid

Status: **APPROVED** 2026-09-19 · Covers rungs 1.1–1.4

## 1. Goal

The correct Monday–Sunday dates render before any data loads, with today
highlighted.

## 2. Options — date library

**minSdk is 24, and `java.time` is API 26+.** This is a real constraint, not a
detail, and it must be settled before a line of date code is written.

| Option | Mechanism | Verdict |
|---|---|---|
| **A** `java.time` + core library desugaring | One `isCoreLibraryDesugaringEnabled` line + a dependency | Standard, well-supported |
| **B** `kotlinx-datetime` | Multiplatform API | Still needs desugaring below API 26 — same cost, extra dependency |
| **C** ThreeTenABP | Backport, no desugaring | An extra library and a second date API in the codebase |
| **D** `java.util.Calendar` | Always available | Mutable, error-prone, month indices from zero; exactly the API whose sharp edges rung 1.1 exists to avoid |

## 3. Pros / cons

- **A** — Pros: the standard API, immutable, `TemporalAdjusters` handles
  Monday-of-week directly, `Clock` is injectable so §6.4's matrix is testable.
  Cons: adds a desugaring step to the build, and a small method-count cost.
- **B** — Pros: nicer Kotlin ergonomics. Cons: pays A's cost *and* adds a
  dependency, for a single screen's worth of date maths.
- **C** — Pros: no desugaring. Cons: a parallel date API, and needs
  initialisation in `Application`.
- **D** — Pros: zero cost. Cons: the whole reason rung 1.1's edge cases exist.

## 4. Recommendation

**A.** `LocalDate` + `TemporalAdjusters.previousOrSame(MONDAY)` expresses the
Sunday-boundary rule in one line, which is precisely the case that silently
breaks one day in seven. Desugaring is one build-file line and should be
enabled during Intent 00 rather than as a separate change.

## 5. Affects

`WeekProvider`, every date in the UI, and the whole §6.4 date matrix. Enabling
desugaring touches `app/build.gradle.kts`, so it belongs in Intent 00's pass.

## 6. Open questions

**Blocking rung 1.4 only:** purple value, cell shape, typography — pending PNGs.

**Non-blocking:** day-label format (`Mon` vs `M`) and whether the month is shown
anywhere; both read off the design when it arrives.

Rungs 1.1–1.3 are fully unblocked.

## 7. Estimate

- Rungs 1.1–1.2 (`WeekProvider` + full test matrix): **45–60 min, high confidence.**
  Pure Kotlin, no Android, no device.
- Rung 1.3 (static grid): **30–45 min, high confidence.**
- Rung 1.4 (today highlight): **30 min, medium** — trivial logic, gated on design.

Blows up only if desugaring was not enabled in Intent 00.

## 8. Risks

| Risk | Early warning |
|---|---|
| Desugaring missed → `NoClassDefFoundError` at runtime on API 24 | Unit tests pass, app crashes on an old-API device |
| Week-start locale drift (`WeekFields.of(locale)` yields Sunday in some locales) | Use `previousOrSame(MONDAY)` explicitly, never a locale-derived week start |

The locale risk is worth naming: a locale-aware week start is *correct* general
behaviour and *wrong* here, because the brief mandates Monday–Sunday.

## 9. Priority

**Second.** Highest confidence work on the board, unblocked, and it de-risks
the loading-state requirement early.
