# Workflow

How work happens here. Read this first; it is short on purpose.

## Reading order

1. **`CLAUDE.md`** (repo root) — rules you must not break
2. **This file** — the loop and where things stand
3. **`plans/NN-*.md`** — the drill for the intent you are on
4. **`spec.md`** — only the sections your rung touches
5. `intents/NN-*.md` — the rung's definition of done

Do not read all of `spec.md` before starting. Read the section your rung needs.

## The loop

```
pick the next rung  ->  drill approved?  ->  implement  ->  verify  ->  commit  ->  repeat
                             │ no
                             └──>  STOP. Ask. Do not write code.
```

1. **Pick the next rung** from the current intent, in order.
2. **Check the drill is approved.** Status header in `plans/NN-*.md` says
   `APPROVED`, not `DRAFT`. If it says DRAFT, stop and ask.
3. **Implement only that rung.** Not the next one, not a refactor you noticed.
4. **Verify** by the rung's own "Verified by" column, then run the gate:
   ```bash
   ./gradlew build > /tmp/b.log 2>&1; echo "exit=$?"
   ```
5. **Commit that rung alone**, green. One rung, one commit.
6. **Repeat.** When the intent's rungs are done, open a PR and stop.

## Definition of done — a rung

- The gate exits **0**
- The rung's stated verification passes
- Its tests exist and are in the right tier (`testing.md`)
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
