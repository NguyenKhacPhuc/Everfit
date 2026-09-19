# Drills

One per intent. Each records the options considered, the trade-offs, the
estimate with its failure mode, the risks, and the decision — see
[`../drill-protocol.md`](../drill-protocol.md).

## These are dated records, not living documents

A drill is a snapshot of what was known and decided **at the moment it was
approved**. They are deliberately not rewritten afterwards, because the value is
in showing what was weighed and what was rejected — including where a prediction
turned out wrong.

Two consequences when reading them:

- **File paths reflect the package structure at approval time.** The app was
  later reorganised into `core/`, `data/`, `feature/`, so a drill mentioning
  `ui/theme` means what is now `core/ui/theme`. The current structure is in
  [`../spec.md`](../spec.md) §2.2.
- **Some decisions were superseded.** Where that happened it is marked inline —
  see Drill 03 §4, where the DataStore fallback is recorded as resolved once
  Room's KSP problem turned out to be a one-line fix.

For what is true *now*, read [`../spec.md`](../spec.md),
[`../testing.md`](../testing.md) and [`../../../CLAUDE.md`](../../../CLAUDE.md).
