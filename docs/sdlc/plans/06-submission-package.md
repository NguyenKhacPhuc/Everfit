# Drill 06 — Submission package

Status: **DRAFT — awaiting approval** · Covers rungs 6.1–6.6

## 1. Goal

The work is submittable and satisfies every stated requirement — including the
ones that are not about code.

## 2. Options — the AI Collaboration section

The brief requires naming the tools and sharing "2-3 specific prompts you used
to solve non-trivial parts of this test."

**A — Capture prompts as the work happens.**
**B — Reconstruct at the end from memory and history.**
**C — Quote the SDLC artefacts as the collaboration record.**

## 3. Pros / cons

- **A** — Pros: accurate, and they are genuinely the prompts used. Cons: needs
  discipline during the build.
- **B** — Pros: no overhead during the work. Cons: reconstructed prompts read as
  reconstructed, and the brief is explicitly testing prompt-engineering skill.
- **C** — Pros: this repo already has an unusually strong record — the intents,
  spec and drills show the collaboration rather than describing it. Cons: not
  what was literally asked for; needs actual prompts alongside.

## 4. Recommendation

**A + C.** Quote 2–3 real prompts as required, and link the drill documents as
supporting evidence. The drills demonstrate the thing the section is probing —
options weighed, trade-offs named, decisions recorded — far better than three
prompts alone.

**Start capturing now.** Every drill approval in this conversation is a
candidate.

## 5. Options — README structure

**A — Single README with everything.**
**B — Short README linking to `docs/sdlc/`.**

**Recommendation: B, with the required sections inline.** Build instructions, AI
Collaboration and the video link must be *in* the README — the brief says so.
Architecture gets a summary plus a link; a reviewer on a deadline should not
have to read 600 lines of spec to find the build command.

## 6. Options — repository visibility timing

**A — Flip to public now. B — Flip at submission.**

**Recommendation: B**, but *set a reminder*. The repo is currently private and
submission requires public — a reviewer hitting a 404 fails the assignment
regardless of the code. It is one command and the single highest-consequence,
lowest-effort item on the board.

## 7. Affects

Nothing technical. Pure deliverable.

## 8. Open questions

**Blocking:** none.

**Non-blocking but required before submission:**
- Video hosting and link (Loom, or a screen recording uploaded somewhere)
- Whether to note the two brief conflicts in the README — **lean yes**, it
  demonstrates the attention to detail being graded
- The end-to-end process bonus: the SDLC artefacts *are* the answer

## 9. Estimate

**1–2h, medium confidence**, excluding the video.

**The video is 3–5 minutes of runtime and cannot be delegated** — realistically
30–60 minutes with setup and a retake. Budget it explicitly; it is the item most
likely to be squeezed, and it is mandatory.

## 10. Risks

| Risk | Early warning |
|---|---|
| **Repo left private** | Reviewer sees a 404. Highest consequence, lowest effort |
| Video skipped for time | Mandatory — non-negotiable |
| Prompts reconstructed | Reads as reconstructed; capture as you go |
| Build instructions untested | Verify on a fresh clone, not from the working directory |

## 11. Priority

**Last to finish, first to start.** Prompt capture and the video outline run
alongside the build. Leaving the whole intent to the end is how the mandatory
items get dropped.
