# SDLC Artifacts

This project follows an adapted version of the
[AI-Native SDLC Playbook](https://claude.com/blog/the-ai-native-sdlc-playbook).

Each stage writes a version-controlled artifact that the next stage reads, so
the intent, the spec, the plan and the diff together form the audit trail.

## Scope

**Working here? Start with [`WORKFLOW.md`](WORKFLOW.md)**, not this file.

| Stage | Status | Artifact |
|---|---|---|
| 1. Plan | ✅ Complete | [`intent.md`](intent.md) + [`intents/`](intents/) |
| 2. Design | ✅ Complete | [`spec.md`](spec.md), [`testing.md`](testing.md) |
| 3. Build | Awaiting drill approval | [`plans/`](plans/), `CLAUDE.md` |
| 4. Test | Not started | Single-command gate, on-device verification |
| 5. Deploy | **Dropped** | Deliberate — see below |
| 6. Maintain | **Dropped** | Deliberate — see below |

Stages 5 and 6 (PR review automation, branch protection, deploy gating,
closed-loop monitoring, scheduled scans) were scoped out on purpose. This is a
24-hour take-home with no production environment, no metrics store and no
incident history, so that machinery would consume hours the brief scores at
zero while measuring nothing real. Stages 1–4 are kept because the
intent -> spec -> plan chain and the self-verifying test loop genuinely improve
the delivered code.

## Execution gate

Implementation is gated on an approved drill per intent — options, trade-offs,
impact, open questions, estimate, risks and priority. See
[`drill-protocol.md`](drill-protocol.md). Approved drills are committed to
`plans/`.

## The ladder

Intents decompose into numbered **rungs**. A rung is the smallest unit of work
that can be independently verified. The rule for every rung:

> A rung is done when the build is green, its verification passes, and it is
> committed on its own.

This is what makes diagnosis cheap — a regression bisects to a single rung
rather than to a large feature commit.

## Technology neutrality

Intents and rungs describe **outcomes**, not implementations. Framework,
persistence and networking choices are deliberately deferred to `spec.md`
(Stage 2) so the ladder survives those decisions unchanged.
