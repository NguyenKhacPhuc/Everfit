# Intent 07: First-load feedback

**Outcome:** a cold start reads as *loading*, not as *an empty week*, and content
does not pop in abruptly.

## The defect

Reported from device testing: on first launch the screen shows seven dated rows
with no workouts, then content appears suddenly. Two causes, both real:

1. **No loading affordance exists.** `CalendarState.isRefreshing` is defined but
   **never read by the UI**. `CalendarScreen` consumes `load` only for
   `showsFullScreenError`.
2. **The first frame claims to be idle.** `initialState` leaves `load = Load.Idle`
   while a refresh is about to start, so "never loaded" is indistinguishable from
   "loaded, nothing scheduled this week".

| Rung | Outcome | Verified by |
|---|---|---|
| 7.1 | The first frame reports loading, not idle | Reducer/ViewModel test on initial state |
| 7.2 | A cold start shows a loading treatment in the workout areas, dates still correct | Device + preview |
| 7.3 | Content replaces the loading treatment without an abrupt jump | Device |
| 7.4 | A background refresh over existing content does **not** show the cold-start treatment | ViewModel test + device |
| 7.5 | A genuinely empty week is distinguishable from a loading week | Device with an empty response |

## Constraint that shapes this

> "When the API request is loading, all day cells should still display the
> correct dates, but show an empty/loading state for the data."

The dates must stay visible. `docs/design/training - empty.png` shows exactly
that: dated rows, no cards. So the design's empty export **is** the loading
state, and anything that covers the screen contradicts both it and the brief.

## Open question

- [ ] The design provides no shimmer or spinner. Whatever is added is an
      invention, so it should be the most restrained thing that fixes the defect.
