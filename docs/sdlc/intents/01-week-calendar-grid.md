# Intent 01: Week calendar grid

**Outcome:** the correct Monday–Sunday week renders with correct dates,
independent of whether any workout data has loaded.

> "The screen should show 1 week of day containers. The day of the week (Mon,
> Tue, etc.) should show up on the top left of each cell. The day of the month
> for this current week should show up just below the day of the week. The date
> for 'Today' should be highlighted in purple."

| Rung | Outcome | Verified by |
|---|---|---|
| 1.1 | Given any instant, the seven dates of that Monday–Sunday week are derivable | Unit tests |
| 1.2 | API `day` index 0–6 maps onto those dates by an explicit, documented rule | Unit tests |
| 1.3 | Seven cells render with day-of-week above day-of-month | Preview + device screenshot |
| 1.4 | Today's cell is highlighted per the design | Unit test on the predicate + visual comparison |

## Why this intent comes before the data layer

The brief requires that during loading, "all day cells should still display the
correct dates, but show an empty/loading state for the data." If the grid is
built on top of the network response, that requirement becomes a retrofit. Built
first, it falls out for free — the loading state is simply this intent with no
data yet.

## Edge cases that must be covered by rung 1.1

These are where date handling usually breaks:

- **Sunday.** Many week calculations treat Sunday as the first day; the brief
  requires it to be the *last*. A naive implementation shows the wrong week for
  one day in seven.
- **Month and year boundaries.** A week spanning 31 Dec → 1 Jan must produce
  seven consecutive correct dates.
- **Timezone and DST.** "Today" is a local-calendar concept. Deriving it from a
  UTC instant puts the highlight on the wrong cell near midnight.
- **Injectable clock.** "Today" must be parameterisable, or none of the above is
  testable and the highlight can only be checked by changing the device date.

## Open question

- [ ] Exact purple, cell shape and typography — blocked on Figma access.
