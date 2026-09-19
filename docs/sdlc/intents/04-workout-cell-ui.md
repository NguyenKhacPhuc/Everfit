# Intent 04: Workout cell UI

**Outcome:** each workout renders to the design, in every state.

> "Workout name (truncate with '...' if the name is too long). Total number of
> exercises belonging to that workout. Status indicator. Colors should
> dynamically reflect these states according to the provided design."

| Rung | Outcome | Verified by |
|---|---|---|
| 4.1 | A cell shows title and exercise count | Preview + device screenshot |
| 4.2 | An over-long title truncates with an ellipsis on one line | UI test with a deliberately long title |
| 4.3 | Status indicator renders per state, with the design's colours | Preview per state + visual comparison |
| 4.4 | A day holding multiple workouts stacks them; an empty day renders empty, not broken | Device check against days 4 and 2 of the fixture |
| 4.5 | During loading, dates show and workout areas show a placeholder | UI test |

## Notes

**Rung 4.2 needs a hostile fixture.** The longest title in the sample data is
"Squat, press, power clean" — which may well fit. Truncation must be tested
with a string long enough to force it, or the requirement is untested while
appearing to pass.

**Rung 4.4 uses the real fixture's shape.** Day 4 has two assignments and days
2 and 5 have none, so the committed sample already covers both cases without
inventing data.

**Rung 4.5 depends on nothing.** Because Intent 01 built the grid independently
of the data layer, the loading state is a rendering concern only.

## Open question

- [ ] Blocked on Figma for: purple value, status colours per state, corner
      radii, spacing, type scale, checkmark iconography. Rungs 4.1–4.5 can be
      built structurally and refined once the design is available, but
      "pixel-perfect" cannot be claimed until then.
