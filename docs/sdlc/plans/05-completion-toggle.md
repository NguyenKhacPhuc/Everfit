# Drill 05 — Completion toggle

Status: **DRAFT — awaiting approval** · Covers rungs 5.1–5.6

## 1. Goal

Tapping a workout toggles completion by id, shows a checkmark, survives
restart, and is never overwritten by a refresh.

## 2. Options — where the local mark lives

**A — Separate `completion_overrides` table** (spec §3.2).
**B — Mutable `is_completed` column on `workout_assignments`.**
**C — In-memory only.**

## 3. Pros / cons

- **A** — Pros: refresh replaces server data wholesale and structurally cannot
  touch overrides; the invariant holds without anyone remembering it. Cons: a
  merge at read time; two tables for one concept.
- **B** — Pros: simplest to write; one table, one flag. Cons: **every refresh
  must remember to preserve the column.** Forget once and the user's tap
  silently reverts — a bug that only appears after a refresh, so it survives
  casual testing and shows up in the demo.
- **C** — Pros: trivial. Cons: fails rung 5.4 outright; the brief requires
  persistence.

## 4. Recommendation

**A**, as already specified. Restated here because it is the decision most
likely to be "simplified" to B by someone who has not hit the failure — which
is exactly why it is also written into CLAUDE.md as an invariant.

## 5. Options — update feedback

**A — Write to store, let the observable query re-emit.**
**B — Optimistic UI state, then write.**

**Recommendation: A.** A local write plus re-emission is single-digit
milliseconds; B adds a second source of truth and a rollback path to solve a
latency problem that does not exist offline.

## 6. Options — merge semantics

When an override exists and the server later reports `COMPLETED` too:

**A — Override always wins while present.**
**B — Delete the override once the server agrees.**

**Recommendation: B**, as a `WHERE` clause during refresh: drop overrides that
the server has caught up with. Keeps the table from growing forever and means
"has a local override" stays meaningful. **A is acceptable** if B proves fiddly
— it is never *wrong*, only untidy.

This is the one genuinely open design question in this intent.

## 7. Affects

Depends on Intent 03's storage choice. Under the DataStore fallback, the
override blob is written independently of the workouts blob — invariant intact,
atomicity weaker.

## 8. Open questions

**Blocking rung 5.3 only:** the checkmark icon and its placement. A Material
`Icons.Default.Check` is a fine stand-in; swapping it later is one line.

Rungs 5.1, 5.2, 5.4, 5.5, 5.6 are fully unblocked.

**Non-blocking:** should tapping a *future* workout be allowed? The brief says
tapping a non-empty cell toggles it, with no exception for future days. Lean:
allow it, keep the cell greyed (§6.4 already specifies `UPCOMING` regardless of
completion). Worth one line in the README as an interpretation.

## 9. Estimate

**1.5–2.5h, medium-high confidence.** Toggle + tests 45 min; persistence 30 min;
refresh-preservation test 30 min; checkmark 20 min.

Blows up if the merge semantics (§6) turn out to need transaction work.

## 10. Risks

| Risk | Early warning |
|---|---|
| Wrong row toggles | Only visible with two workouts on one day — test against day 4 |
| Refresh reverts the mark | Write rung 5.5's test *before* the refresh logic |
| Toggling an empty cell crashes | Rung 5.6 |

## 11. Priority

**Sixth.** Logic is unblocked and could run earlier if Intent 04 stalls on the
design — a useful schedule hedge.
