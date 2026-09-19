# Drill 02 — Workout data layer

Status: **DRAFT — awaiting approval** · Covers rungs 2.1–2.5

## 1. Goal

The payload becomes a typed domain model with unambiguous status semantics, and
failures arrive as typed errors rather than exceptions.

## 2. Options — JSON

| Option | Codegen | Ktor integration |
|---|---|---|
| **A** kotlinx.serialization | Compiler plugin, **no KSP** | First-party — `ContentNegotiation` is built for it |
| **B** Moshi | KSP or reflection | Needs a converter |
| **C** Gson | Reflection | Needs a converter; no null-safety with Kotlin |

## 3. Pros / cons

- **A** — Pros: the Ktor-native choice; `@SerialName("_id")` handles the wire
  naming directly; `ignoreUnknownKeys` is one flag; compile-time generation with
  no annotation processor. Cons: another Gradle plugin (not KSP).
- **B** — Pros: mature. Cons: adds KSP *or* reflection, plus a Ktor converter,
  for no gain here.
- **C** — Pros: ubiquitous. Cons: reflection-based, and cheerfully produces
  `null` in a non-null Kotlin field — a real hazard given `status` drives colour.

## 4. Recommendation

**A.** It is what Ktor expects, it avoids KSP, and `ignoreUnknownKeys = true`
directly satisfies the parsing rung's "unknown fields are ignored, not fatal".

## 5. Options — status modelling

**A — `enum class StoredStatus(val raw: Int)` with an exhaustive `from(Int)`.**
**B — Keep the raw `Int` through to the UI layer.**
**C — Sealed interface with per-status data.**

**Recommendation: A.** B pushes an undocumented magic number into every layer
and makes §6.4's matrix untestable in isolation. C buys extensibility that
three statuses do not need.

**Unknown values:** the enum must handle an `Int` outside 0–2. Mapping to
`ASSIGNED` hides a server change; a typed `UNKNOWN` that renders greyed is
honest and cannot crash. **Recommend `UNKNOWN`.**

## 6. Affects

Locks the wire→domain boundary and the error type. `StatusResolver` (Intent 04's
colours, Intent 05's completion merge) depends on this enum. Getting the
semantics wrong propagates everywhere.

## 7. Open questions

**Blocking rung 2.2:** the enum meaning. `0=ASSIGNED, 1=COMPLETED, 2=MISSED` is
still an assumption, settled by the PNGs.

Work can proceed with the assumption **named in one place** so a correction is a
one-line change. Rungs 2.1, 2.4, 2.5 are fully unblocked.

**Non-blocking:** retry policy on refresh failure — lean none for a take-home;
a manual retry action is enough.

## 8. Estimate

**2–3h, medium-high confidence.** Split: DTOs + parsing 45 min; status model +
matrix 45 min; Ktor + `MockEngine` tests 45 min; repository 30 min.

Blows up if the endpoint changes shape mid-run — mitigated, since tests read the
committed fixture rather than the network.

## 9. Risks

| Risk | Early warning |
|---|---|
| Enum assumption wrong | Every status colour is wrong at once — loud, not silent |
| `status` arrives as a string in some response | Fixture says `Int`; `UNKNOWN` prevents a crash either way |
| Cleartext HTTP blocked | The live URL is `https`, so not an issue unless the dead `http` URL is used |

## 10. Priority

**Third.** Unblocked, feeds Intents 03/04/05, and independent of the design
apart from the enum labels.
