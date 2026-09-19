# Intent 02: Workout data layer

**Outcome:** the remote payload becomes a typed domain model with unambiguous
status semantics.

| Rung | Outcome | Verified by |
|---|---|---|
| 2.1 | The recorded API response parses into typed models | Unit test against the committed fixture |
| 2.2 | `status` 0/1/2 map to named states | Unit tests over the full matrix |
| 2.3 | Displayed status is derived from stored status **and** the day's position relative to today | Unit tests over the matrix |
| 2.4 | Data is fetched asynchronously off the main thread; failures surface as a typed error | Tests against a stubbed server, including non-200 and malformed body |
| 2.5 | A repository exposes the domain model to callers | Unit test with a stubbed source |

## The payload

A verbatim capture is committed at [`../api-sample-response.json`](../api-sample-response.json)
so tests do not depend on a live third-party endpoint.

```json
{ "data": [ { "_id": "...", "day": 0,
    "assignments": [ { "_id": "...", "title": "Legs day",
                       "status": 1, "total_exercise": 5 } ] } ] }
```

Observed properties, all of which the tests should pin:

- Exactly 7 entries, `day` 0–6.
- **No date field anywhere.** The week is implicit — hence rung 1.2.
- Days 2 and 5 have empty `assignments`; day 4 has **two**. The fixture
  genuinely exercises the empty-cell and multiple-workout cases.
- `status` ∈ {0, 1, 2}.

## Rung 2.3 is the subtle one

The brief defines status *by temporal position*, not by the raw value:

> Past: Missed or Completed · Today: Assigned or Completed · Future: greyed out

So the same stored value renders differently depending on the day. An
implementation that maps `status` straight to a label will be wrong for future
days, which must be greyed out regardless of what the server says. This
deserves its own rung and its own test matrix.

## Open question

- [ ] **Status semantics are undocumented.** Working assumption:
      `0 = Assigned, 1 = Completed, 2 = Missed`. Must be confirmed against the
      Figma design before rung 2.2 is considered done — every status colour in
      the UI depends on it.
