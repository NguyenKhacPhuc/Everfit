# Intent 03: Local cache

**Outcome:** the calendar renders instantly from local data on launch, then
reconciles with the server.

> "The data must be cached locally. When the user opens the app the next time,
> it should load from the cache immediately while fetching updates (if any)."

| Rung | Outcome | Verified by |
|---|---|---|
| 3.1 | Workouts persist across process death | Write, restart, read back |
| 3.2 | Cold start (empty cache) shows loading, then content | Repository test |
| 3.3 | Warm start emits cached content **before** any network call completes | Repository test asserting emission order |
| 3.4 | A failed refresh leaves cached content on screen, not an error screen | Repository test with a failing source |
| 3.5 | Screen state is modelled explicitly (loading / content / error) | State-holder unit tests with a stubbed repository |

## Rung 3.3 is the requirement

"Load from the cache immediately **while** fetching" is an ordering guarantee,
not a vague aspiration. The test must assert that cached content is emitted
before the network result — an implementation that awaits the network and then
picks whichever arrived satisfies a naive test but fails the requirement on a
slow connection, which is exactly when it matters.

## Rung 3.4 is where offline-first is won or lost

A refresh failure must be non-destructive. Showing an error screen over
perfectly good cached data is the most common way this requirement is failed,
and it is trivially demonstrable in a demo by enabling airplane mode — which
makes it worth getting right for the video walkthrough.
