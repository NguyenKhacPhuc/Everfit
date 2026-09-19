# Intent 05: Completion toggle

**Outcome:** tapping a workout toggles its completion, and that choice sticks.

> "When tapping on a non-empty cell (a workout), it should toggle the
> mark/unmark state locally (based on the item ID). If marked as completed, a
> checkmark icon should appear on the right side of the cell."

| Rung | Outcome | Verified by |
|---|---|---|
| 5.1 | Tapping a workout toggles its state, addressed by item id | Tier 2 ViewModel test — intent in, DAO write out |
| 5.2 | Only the tapped workout changes | DAO/repository test, two workouts sharing a day and a title |
| 5.3 | A checkmark appears on the right when completed, and disappears when unmarked | UI test + device |
| 5.4 | The local choice survives app restart | Restart test |
| 5.5 | A server refresh does not silently overwrite a local choice | Repository test |
| 5.6 | Tapping an empty cell does nothing | UI test |

## Rung 5.2 exists because of the fixture

Day 4 holds two workouts, and the title "Legs day" appears on both day 0 and
day 4. Any implementation keyed on title or list position rather than `_id`
will toggle the wrong row — and will still look correct in a casual demo. The
brief says "based on the item ID" precisely because of this.

## Rung 5.5 is the real design decision

Intents 03 and 05 pull in opposite directions:

- Intent 03 says the cache refreshes from the server.
- Intent 05 says completion is toggled **locally**.

So what happens when a user marks a workout complete, the app refreshes, and
the server still reports it as assigned? If the refresh overwrites blindly, the
user's tap silently reverts — a bug that appears only after a refresh and is
easy to miss in testing.

Local completion must therefore be stored as an **overlay** that survives
server reconciliation, not as a mutation of the cached server status. This is
the single most important structural decision in the build, and the one most
worth explaining in the video walkthrough.
