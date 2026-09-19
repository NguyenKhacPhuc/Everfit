# Intent 06: Submission package

**Outcome:** the work is submittable and the submission satisfies every stated
requirement.

| Rung | Outcome | Verified by |
|---|---|---|
| 6.1 | README documents how to build and run from a clean clone | Follow it on a fresh clone |
| 6.2 | README has an "AI Collaboration" section naming tools and 2–3 real prompts | Present and accurate |
| 6.3 | Non-obvious AI-generated logic carries an explanatory comment | Review of the diff |
| 6.4 | README describes the end-to-end feature process (bonus) | Present |
| 6.5 | Repository is **public** | Confirm visibility |
| 6.6 | Video walkthrough recorded and linked | Link resolves |

## Requirements that are easy to fail

**Rung 6.5 — the repo is currently private.** The brief says "Push your code to
a public GitHub repository." A private repo means the reviewer sees a 404.

**Rung 6.2 must be honest.** The brief asks for "2-3 specific prompts you used
to solve non-trivial parts of this test." These have to be prompts genuinely
used, not reconstructed afterwards. Capturing them *as the work happens* is far
easier than reverse-engineering them at the deadline.

**Rung 6.6 cannot be delegated.** The video is mandatory, must be 3–5 minutes,
and must explain the architecture and data flow in the author's own words.

**The brief's warning is explicit:**

> "In a follow-up interview, we may ask you to explain specific blocks of code.
> If you cannot explain code that AI generated for you, it will be considered a
> red flag."

This ladder is built to make that survivable: every rung is small enough to
hold in your head, and each is committed separately, so the git history is a
readable narrative of how the app was constructed rather than one opaque drop.

## Worth mentioning in the README

The two API-URL discrepancy and the undocumented status enum (see
[`../intent.md`](../intent.md)) are findings, not complaints. Reporting them
demonstrates the attention to detail the brief says it is assessing.
