# Drill 03 — Local cache

Status: **APPROVED** 2026-09-19 · Covers rungs 3.1–3.5

## 1. Goal

The calendar renders from local data immediately on launch, then reconciles,
and a failed refresh never destroys what is on screen.

## 2. Options — persistence

The dataset is **six workouts**. That makes options normally dismissed as toys
genuinely viable, so they are weighed rather than waved away.

| Option | Codegen | Query support |
|---|---|---|
| **A** Room | **KSP** | Full SQL, observable `Flow` |
| **B** SQLDelight | Gradle plugin, no KSP | Full SQL, typed from `.sq` files |
| **C** DataStore + kotlinx.serialization | None | Whole-blob read/write only |
| **D** Raw SQLite | None | Full SQL, all boilerplate by hand |

## 3. Pros / cons

- **A** — Pros: the expected answer; first-party; observable queries make §5.1's
  cache-first flow nearly free; the two-table design in spec §3.2 is natural.
  Cons: **KSP**, which Drill 00 §8 flags as the most likely toolchain failure.
- **B** — Pros: no KSP, compile-time-verified SQL. Cons: less familiar to a
  reviewer; `.sq` files are extra surface under a deadline.
- **C** — Pros: zero codegen, trivially satisfies "cached locally". Cons: no
  queries — the override merge becomes in-memory work, and two independent blobs
  lose the atomicity that makes rung 5.5 structural.
- **D** — Pros: no dependency. Cons: hand-written boilerplate is exactly the
  "clean, maintainable, modular" criterion being graded.

## 4. Recommendation

**A, with C as a pre-agreed fallback.** Room is what the brief's persistence
requirement expects, and spec §3.2's two-table design is built on observable
queries.

But the fallback is chosen **now, not in a panic**: if KSP cannot resolve
against AGP 9.1.1 / Gradle 9.3.1, switch to **C** rather than losing hours to a
version matrix. C still satisfies "the data must be cached locally"; it costs
the query layer, and the override merge moves into the repository, which is
where rung 5.5's logic is tested anyway.

**Trigger for the fallback: 45 minutes lost to KSP resolution.**

> **RESOLVED 2026-09-19, Intent 00.** KSP did fail against AGP 9.1.1 — it
> registers generated sources via `kotlin.sourceSets`, which built-in Kotlin
> rejects — but the documented flag `android.disallowKotlinSourceSets=false`
> fixes it, and a spike confirmed Room's processor emits `_Impl` classes. The
> trigger never fired; cost was about ten minutes. **Room stands; option C is
> not needed.**

## 5. Affects

Rung 5.5 depends on the two-table design. Under the fallback, "two tables"
becomes "two independently written blobs" — the *invariant* survives (refresh
never writes overrides) but atomicity is weaker.

## 6. Open questions

**Blocking:** none.

**Non-blocking:**
- Cache invalidation — lean none. The fixture has no freshness signal, and a
  24-hour take-home has no staleness problem worth solving.
- Sync timestamp — a column if Room, a field if the fallback. Not DataStore
  (spec §7).

## 7. Estimate

**2–3h, medium confidence** on Room. Add **1h** if the KSP fallback triggers.

Blows up on: KSP/AGP mismatch (the named failure mode), or Room's
`@Transaction` behaviour with the two-table refresh proving fiddlier than
expected.

## 8. Risks

| Risk | Early warning |
|---|---|
| KSP will not resolve | Fails at Intent 00, not here — by design |
| Refresh wipes overrides | Rung 5.5's test catches it; write that test *before* the refresh logic |
| Ordering test passes falsely | Needs a suspended fake the test resumes — CLAUDE.md flags this |

## 9. Priority

**Fourth**, immediately after the data layer. Unblocked by design assets.
