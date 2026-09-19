# Intent: Training Calendar

Status: **Draft** · Date: 2026-09-19 · Stage 1 (Plan)

## Problem

Users following a training programme cannot see what they are meant to do this
week, or record what they have finished. They need a single screen that shows
the week at a glance and lets them tick work off.

## Proposed outcome

A one-week (Monday–Sunday) training calendar showing each day's assigned
workouts with their title, exercise count and status, where tapping a workout
toggles its completion locally.

## Affected systems

- New Android application (no existing code — the repository was an empty
  scaffold; see `docs/sdlc/intents/00-foundation.md`)
- Read-only mock HTTP API for workout assignments
- On-device persistence for cache and local completion state

## Source of truth

The assignment brief from Everfit, received 2026-09-19. Product requirements
are reproduced in the individual intents rather than paraphrased, to avoid
drift between the brief and what gets built.

## Constraints

| Constraint | Detail |
|---|---|
| Deadline | 24 hours from receipt |
| Platform | Android (see *Known conflicts*) |
| Fidelity | "Pixel-perfect" against the Figma design is assessment criterion #1 |
| Authorship | All submitted code must be understood and explainable by the author |
| Deliverables | Public repo, README with build instructions + AI Collaboration section, 3–5 min video walkthrough |

## Known conflicts in the brief

These are recorded here rather than silently resolved, and should be noted in
the final README as evidence of attention to detail.

1. **Platform mismatch.** Every technical requirement names an Apple
   technology (Swift Concurrency, Realm/CoreData/SwiftData, UIKit/SwiftUI,
   MVVM-C), but this is an Android submission. The *product* requirements
   translate directly; the *technical* ones are mapped to Android equivalents
   in `spec.md`.

2. **Two mock API URLs, one dead.**
   - `https://mock.internalef.com/workouts` — live, returns valid JSON. **Use this.**
   - `http://demo6732818.mockable.io/workouts` — unreachable (connection failed).

3. **Undocumented status enum.** The API returns `status` values `0`, `1`, `2`
   with no stated meaning. See `intents/02-workout-data-layer.md`.

## Open questions

- [ ] **Figma access.** Required for colour, spacing and status-label fidelity.
      Blocks visual rungs (1.4, 4.1–4.4).
- [ ] **Status enum semantics.** Working assumption `0=Assigned, 1=Completed,
      2=Missed`; must be confirmed against the design.
- [ ] **Repository visibility.** Currently private; submission requires public.

## Decomposition

| # | Intent | Blocks |
|---|---|---|
| 00 | [Foundation](intents/00-foundation.md) | everything |
| 01 | [Week calendar grid](intents/01-week-calendar-grid.md) | 04, 06 |
| 02 | [Workout data layer](intents/02-workout-data-layer.md) | 03, 04 |
| 03 | [Local cache](intents/03-local-cache.md) | 05 |
| 04 | [Workout cell UI](intents/04-workout-cell-ui.md) | 05 |
| 05 | [Completion toggle](intents/05-completion-toggle.md) | — |
| 06 | [Submission package](intents/06-submission-package.md) | — |
