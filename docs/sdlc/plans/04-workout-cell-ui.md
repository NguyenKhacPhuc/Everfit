# Drill 04 — Workout cell UI

Status: **DRAFT — awaiting approval** · Covers rungs 4.1–4.5

## 1. Goal

Each workout renders to the design in every state, including truncation,
multiple workouts per day, empty days and loading.

## 2. Options — achieving fidelity

**A — Measure from PNG exports.** Read spacing and colour off the images.

**B — Figma MCP / dev-mode values.** Exact tokens read from the file.

**C — Approximate now, refine later.** Build structurally with placeholder
styling; correct once the design lands.

## 3. Pros / cons

- **A** — Pros: works with what is actually available; a colour picker gives
  exact hex. Cons: spacing is inferred, so a few px of drift; needs exports at
  a known scale (@2x/@3x) or measurements are guesses.
- **B** — Pros: exact values, no inference. Cons: needs the Figma connector
  authorised, which this session cannot do.
- **C** — Pros: unblocks immediately. Cons: **the visual work is done twice**,
  and "pixel-perfect" is assessment criterion #1 — the refinement pass is the
  graded one, and it lands last, under the most time pressure.

## 4. Recommendation

**A, with C strictly limited to structure.** Build layout, state handling and
truncation behaviour without the design — those are structural and will not
change. Do **not** invent colours or spacing and then correct them; leave tokens
as named placeholders in `ui/theme` so applying the design is editing one file,
not hunting inline values.

This is what spec §2.8's "no inline hex in composables" rule is for.

## 5. Options — loading state (rung 4.5)

**A — Shimmer/skeleton placeholders. B — Empty cells. C — Spinner over the grid.**

**Recommendation: B**, unless the design shows otherwise. The brief says cells
"show an empty/loading state for the data" while dates stay correct — C
contradicts that by obscuring the dates. A is polish the design has not asked
for.

## 6. Affects

`ui/theme` tokens, every composable, and the screenshot comparison. Locks
nothing architecturally — this intent is pure presentation.

## 7. Open questions

**Blocking — all of rungs 4.1, 4.3, 4.5 and most of 4.2:**
- Purple value, and the four status colours
- Corner radii, padding, cell height, type scale
- Checkmark icon and its placement
- Whether the status indicator is a text label, a coloured dot, or both
- What an empty day looks like

**Non-blocking:** exact ellipsis behaviour — Compose's `TextOverflow.Ellipsis`
with `maxLines = 1` is almost certainly right.

## 8. Estimate

**2–4h, low confidence** — the widest range and the least certain on the board,
because fidelity iteration is open-ended and its cost is set by a design nobody
has seen yet.

Blows up if: the design uses a custom font (adds asset work), or the cell layout
is materially more complex than the fixture suggests.

**Recommend capping this at 3h** and shipping a close approximation rather than
letting it consume Intents 05 and 06.

## 9. Risks

| Risk | Early warning |
|---|---|
| PNGs arrive late | Everything else finishes and this is all that is left |
| Truncation untested | Longest fixture title is "Squat, press, power clean" and may not overflow — rung 4.2 needs a deliberately long string |
| Endless pixel-chasing | The 3h cap |

## 10. Priority

**Fifth — but its *input* is on the critical path now.** The build order lets
this come last; the PNG export does not. Every hour the design is unavailable
compresses the one estimate that is already the least reliable.
