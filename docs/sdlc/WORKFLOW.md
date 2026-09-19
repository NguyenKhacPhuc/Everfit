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

Stages 1 (Plan) and 2 (Design) are complete. Stage 3 has **not started** —
no production code exists.

| Intent | Drill | Rungs |
|---|---|---|
| 00 Foundation | DRAFT | 0.1 ✅ · 0.5 ✅ · 0.2–0.4 not started |
| 01 Week grid | DRAFT | not started |
| 02 Data layer | DRAFT | not started |
| 03 Local cache | DRAFT | not started |
| 04 Cell UI | DRAFT | not started |
| 05 Toggle | DRAFT | not started |
| 06 Submission | DRAFT | not started |

**All seven drills await approval. No code may be written.**

Open blockers: design PNGs (`docs/design/`) block rungs 1.4 and 4.1–4.5; the
status enum meaning (spec §7) is settled by the same exports.

> Update this table when a rung lands. It is the only place status lives — an
> agent should not have to open seven files to learn what is next.
