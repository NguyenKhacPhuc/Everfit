# Workflow

How work happens here. Read this first; it is short on purpose.

## Reading order

1. **`CLAUDE.md`** (repo root) — rules you must not break
2. **This file** — the loop and where things stand
3. **`plans/NN-*.md`** — the drill for the intent you are on
4. **`spec.md`** — only the sections your rung touches
5. `intents/NN-*.md` — the rung's definition of done

Do not read all of `spec.md` before starting. Read the section your rung needs.

## The loop — test first

```
pick rung -> drill approved? -> write test -> SEE IT FAIL -> implement -> SEE IT PASS -> gate -> commit
                   │ no
                   └──> STOP. Ask. Do not write code.
```

1. **Pick the next rung** from the current intent, in order.
2. **Check the drill is approved.** `plans/NN-*.md` says `APPROVED`, not
   `DRAFT`. If DRAFT, stop and ask.
3. **Write the failing test first.** The cases are already specified — the
   matrices in [`testing.md`](testing.md) are the test list, not a summary of
   one. Transcribe, do not invent.
4. **Run it and watch it fail.** Not optional, and not a formality — see §Why
   below.
5. **Implement only that rung.** Not the next one, not a refactor you noticed.
6. **Run it and watch it pass**, then the gate:
   ```bash
   ./gradlew build > /tmp/b.log 2>&1; echo "exit=$?"
   ```
7. **Commit that rung alone**, green. Test and implementation in one commit.
8. **Repeat.** When the intent's rungs are done, open a PR and stop.

### Why "see it fail" is the load-bearing step

A test that passes before the implementation exists is asserting nothing, and
looks identical to one that works. This project has two known cases where that
happens silently:

- **Cache-before-network ordering** (rung 3.3) — a network fake that returns
  instantly passes even when the implementation awaits the network first. Only
  a fake the test resumes explicitly can fail.
- **Future days grey out even when completed** (§4.2) — a single-step status
  implementation passes every other row in the matrix.

Both are caught by running red before green, and by nothing else.

### Where TDD applies

Honestly scoped — it is not uniform, and pretending otherwise invites ignoring
the rule where it does fit.

| Rung type | Test first? | Why |
|---|---|---|
| Reducer transitions (Tier 1) | **Always** | Truth tables already written in spec §4.2, §5.2 |
| Domain rules — week, status | **Always** | Matrices already written, with verified dates |
| Parsing and mappers | **Always** | Fixture is committed |
| Repository | **Always** | The three load-bearing tests live here |
| ViewModel pipelines (Tier 2) | **Always** | Operator choice is the assertion |
| DAO (instrumented) | Test-after acceptable | Device loop too slow for red/green |
| Build wiring (0.2–0.4) | N/A | Nothing to assert beyond the gate itself |
| Visual fidelity (4.x) | N/A | "Matches the design" is not expressible as an assertion; screenshot-compare after |

For the two N/A rows the discipline is the gate, not a test.

## Definition of done — a rung

- Its test was **seen to fail** before the implementation existed
- The gate exits **0**
- The rung's stated verification passes
- Its tests are in the right tier (`testing.md`)
- It is committed on its own

A rung is not done because it compiles.

## When you are blocked

| Situation | Do |
|---|---|
| Drill still DRAFT | Stop. Ask for approval. |
| Approved approach turns out wrong | Stop. Re-drill. Do not silently substitute. |
| Blocking open question in the drill | Stop. Ask. |
| Non-blocking open question | Proceed, name the assumption in one place |
| A rung needs a decision the drill did not make | Escalate to a drill |

The gate exists for the moment when continuing feels faster than asking.

## Git

- Branch per intent: `feat/NN-<intent>`, or `fix/`, `docs/`, `chore/`
- Conventional commits: `feat(calendar): ...`, `fix: ...`, `docs(sdlc): ...`
- Commit body says **why**, not what — the diff says what
- One PR per intent; never commit to `main` directly

## Status

**Stages 1–4 complete.** The app builds, runs against the live endpoint, and is
verified on device. 58 JVM unit tests; no instrumented suite by decision
(see [`testing.md`](testing.md)).

| Intent | Drill | Rungs |
|---|---|---|
| 00 Foundation | ✅ approved | ✅ 0.1–0.6 |
| 01 Week grid | ✅ approved | ✅ 1.1–1.4 |
| 02 Data layer | ✅ approved | ✅ 2.1–2.5 |
| 03 Local cache | ✅ approved | ✅ 3.1–3.5 (3.1 verified on device) |
| 04 Cell UI | ✅ approved | ✅ 4.1–4.5 |
| 05 Toggle | ✅ approved | ✅ 5.1–5.6 |
| 06 Submission | ✅ approved | ✅ 6.1–6.5 · ⬜ 6.6 video |
| 07 First-load feedback | ✅ approved (option E) | ✅ 7.1–7.5 |

### Verified on device, not only in tests

- The week renders to the design against the live endpoint.
- A tap marks a workout complete, and **the mark survives an app restart during
  which a refresh reports the opposite status** — rungs 5.4 and 5.5 end to end.
- Offline, cached content stays on screen with a message rather than an error
  page — rung 3.4, demonstrated by a real DNS outage on the emulator.
- Cold start shows shimmering placeholders and crossfades to content; a warm
  start shows none, because the cache answers before the 150ms gate opens.

### Decisions taken during the build

- **Status enum corrected** from the design: `0=ASSIGNED, 1=MISSED, 2=COMPLETED`.
  The intuitive reading has 1 and 2 reversed, and the fixture has three `status=1`
  items to one `status=2`, so guessing would have made the common case wrong.
- **Layout confirmed** as seven rows, not seven columns.
- **Room kept**: KSP did clash with AGP 9, but the documented
  `android.disallowKotlinSourceSets=false` flag fixed it in about ten minutes,
  so Drill 03's DataStore fallback never triggered.
- **Type scale taken from the Figma spec** and checked by measuring glyph ink
  against the export, not by eye.

### Remaining

1. **Record the 3–5 minute video** (rung 6.6) and replace the `TODO` link in
   [`../../README.md`](../../README.md).
2. Optional: a type spec for the card subtitle, currently inferred at 14sp/400.
