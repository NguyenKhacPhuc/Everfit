# Drill 07 — First-load feedback

Status: **APPROVED** 2026-09-19 (option E) · Covers rungs 7.1–7.5

## 1. Goal

A cold start reads as loading rather than as an empty week, without hiding the
dates the brief requires to stay visible.

## 2. Options

**A — Full-screen loading overlay** (as suggested: a cover over the screen).

**B — Per-day skeleton placeholders** in the workout areas; dates stay visible.

**C — Thin progress indicator** at the top of the list; layout otherwise
unchanged.

**D — No indicator; crossfade content in** so the transition is not abrupt.

**E — B + D**: skeletons on cold start, crossfaded out when content arrives.

## 3. Pros / cons

| | Pros | Cons |
|---|---|---|
| A | Unambiguous; hides all intermediate states | **Contradicts the brief** — "all day cells should still display the correct dates" — and contradicts `training - empty.png`. On a fast response it flashes, trading one flicker for another |
| B | Dates stay visible; the row skeleton sits exactly where content will land, so nothing jumps | Invents visual language the design does not provide |
| C | Cheap, honest, familiar | Does not stop the pop-in — the rows are still empty until content lands |
| D | No invented chrome at all | A slow network still looks like an empty week for seconds |
| E | Fixes both halves: *reads as loading* and *does not jump* | Most work of the five; two inventions instead of one |

## 4. Recommendation

**E, with B as the fallback if time is short.**

The report is really two defects. C and D each fix one. A fixes both but breaks an
explicit requirement the submission is graded against, and would be the wrong
answer even though it is what was asked for — so it is worth saying plainly
rather than implementing.

Two details that keep it restrained, since the design gives no guidance:

- **Only on cold start.** A background refresh over existing content must not
  replace it with skeletons (rung 7.4) — that would be a worse flicker than the
  one being fixed.
- **A short delay before showing skeletons** (~150ms). If the cache answers
  immediately, no loading treatment should appear at all. Otherwise a warm start
  gains a flash it does not have today.

## 5. Affects

- `CalendarState` — `Load.Idle` as the initial value is wrong; the first frame
  should be `Load.Refreshing`. This is rung 7.1 and is the smaller half of the
  fix, but it is a state-modelling correction, not cosmetics.
- `CalendarScreen`, `DayCell` — a skeleton composable, and `load` finally read.
- One reducer test currently asserts an initial state that is about to change.

Nothing in `domain` or `data` is touched.

## 6. Open questions

**Blocking:** none.

**Non-blocking:**
- Skeleton style — a flat rounded block in `cardBackground` matches the card
  geometry without inventing a shimmer. A shimmer is more "premium" and more
  invention; lean flat.
- Whether a genuinely empty week needs distinct messaging (rung 7.5). The fixture
  always returns workouts, so this only shows with an empty response. Lean: no
  empty-state copy, because the design shows none.

## 7. Estimate

**45–75 min, high confidence.**

Split: initial-state correction plus tests 15 min · skeleton composable and
previews 20 min · delay-and-crossfade wiring 15 min · device verification 15 min.

Blows up only if the delay/crossfade interacts badly with `LazyColumn` item keys —
mitigated by keying on the date, which is already stable.

## 8. Risks

| Risk | Early warning |
|---|---|
| Skeletons appear on every background refresh | Visible immediately on a second launch |
| A flash on fast/warm starts | The 150ms delay exists for this; check with a primed cache |
| Invented visuals diverge from the design | Kept to card geometry and existing tokens; no new colours |

## 9. Priority

**After rung 3.1, before the video.** It is a genuine defect found by testing,
and the first thing a reviewer sees on launch — but it is smaller than the
outstanding submission blockers (`main` still holds only the scaffold; the repo
is private).
