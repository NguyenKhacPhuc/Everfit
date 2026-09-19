# Drill protocol

**No implementation begins without an approved drill.**

This is the approval gate between Stage 2 (Design) and Stage 3 (Build). The
playbook's own rule is that plan mode comes first and the plan is interrogated
before it is approved; this defines what that interrogation must cover here.

## Where this sits

| Document | Answers |
|---|---|
| `intents/` | What must be true for this to be done |
| `spec.md` | What was decided, and why |
| `plans/` (drills) | How it will be executed, at what cost, with what risk |
| `CLAUDE.md` | Rules that must not be broken |

A drill is the only one of these that carries estimates, risks and sequencing.
Keeping them out of the spec is what stops the spec ageing — an estimate is
stale the moment work starts.

## The rule

> Code is written only after the drill for that intent has been explicitly
> approved. "Looks good", "go ahead", or an equivalent clear agreement. Silence,
> a question, or a reply about something else is **not** approval.

Applies to production code, build files and dependency changes. It does not
apply to reading, investigating, or writing documents.

## Granularity

Drill **per intent**, not per rung. Seven intents is a workable number of
gates; thirty-five would spend more time on the protocol than on the work.

Rungs are sequenced and estimated *inside* the intent's drill. A rung that
turns out to need its own decision gets escalated back to a drill rather than
being decided mid-flight.

## What a drill must contain

| Section | Must answer |
|---|---|
| **1. Goal** | Which rungs this covers, and what "done" means |
| **2. Options** | At least two candidate approaches — one is not a choice |
| **3. Pros / cons** | Per option, with the real cost named, not a token drawback |
| **4. Recommendation** | One option, with the reason it wins |
| **5. Affects** | Files, other intents, and decisions this locks in or forecloses |
| **6. Open questions** | Split into **blocking** and **non-blocking** |
| **7. Estimate** | Range plus confidence, and what would blow the estimate |
| **8. Risks** | What could fail, and the early warning sign for each |
| **9. Priority** | Where this sits in the order, and why |

### On options

If only one approach is genuinely viable, the drill says so and explains what
was ruled out and why. A fabricated alternative presented to satisfy the format
is worse than none — it disguises the fact that no choice existed.

### On estimates

Ranges, never points. State the confidence and the assumption that would break
it. An estimate without a named failure mode is a guess wearing a suit.

### On open questions

**Blocking** means work cannot start. **Non-blocking** means work can start and
the answer is needed before the intent closes. Mislabelling a blocker as
non-blocking is how a deadline is lost.

## After approval

Drills live in `docs/sdlc/plans/NN-<intent>.md` (see
[`plans/README.md`](plans/README.md) — they are dated records and are not
rewritten as the code moves) and carry a status header:
**DRAFT — awaiting approval**, then **APPROVED** with the date. They are
written up front, as a set, so the whole board can be prioritised against real
estimates rather than one intent at a time. They always retain the **rejected
options**. Recording what was not chosen, and why, is most of
the value — it stops a settled question being relitigated at hour 20, and it is
the raw material for the video walkthrough.

Execution then follows the rungs in the approved order. A rung ends green and
committed on its own.

## Changing course mid-intent

If execution reveals the approved approach is wrong, **stop and re-drill**. Do
not quietly substitute a different design. The gate exists precisely for the
moment when continuing feels faster than asking.
