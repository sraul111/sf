# Feature 2 — Parts Reservation

| Field | Value |
|---|---|
| **Status** | Candidate-built / in progress |
| **One-line intent** | Let a technician issue parts from a seeded stock repository — automatically holding stock around a booked job's time window — and return unused parts to restock it |
| **Depends on** | `pipeline/decisions/feature-1-decisions.md` — the "standard travel buffer per job is 45 minutes" decision (`TRAVEL_BUFFER_MINUTES = 45`). This feature now reuses that exact 45-minute buffer to compute when a part is held for a job, so the two values must never drift apart. The overlap-detection fix does not apply here. It also reuses the existing `Technician` and `Job` identities from Feature 1 structurally (to record who issued a part and, now, which booking it's held against). |
| **In scope** | A parts repository (mock, in-memory) that **boots seeded with exactly 4 parts — `P001`, `P002`, `P003`, `P004`** — each with a fixed total quantity (some seeded with a total of 3); listing all parts with their name, in-use count, available count, and total; a technician issuing a quantity of a part against a booked job, which reduces its available quantity for the job's scheduled window; a part automatically counting as "in use" from 45 minutes before its linked job's start time to 45 minutes after its linked job's end time, without any extra manual step; rejecting an issue request in full (no partial fulfillment) when the requested quantity exceeds what's available, with a clear insufficient-stock error; a technician returning/restocking a quantity of a part, which increases its available quantity; a parts-availability panel in the technician booking UI, directly below the existing booking div, showing **Part Name, In Use, Available, Total** columns, backed by a real endpoint (not static/mock UI data); a **`Parts Used` column on the existing technician booking grid**, listing each job's issued parts and quantities in a compact `P001(2); P003(1)` format |
| **Out of scope** | Adding brand-new part types at runtime — the catalog is fixed to the 4 seeded parts (`P001`–`P004`); any inventory-manager/admin role or UI for managing the part catalog; low-stock alerting or notifications; any concept of "tool" as a distinct model from "part" (tools are out of scope for now — everything is a `Part`); billing/costing a part to a job; a part being linked to more than one job at a time |
| **Definition of Done** | The backend boots with exactly 4 parts (`P001`–`P004`) already in stock, at least one with a total of 3; `GET` of the parts list returns each part's name, in-use quantity, available quantity, and total quantity; a technician can issue a quantity of a part against a specific booked job, and from 45 minutes before that job's start to 45 minutes after its end, the issued quantity shows as in-use and unavailable to others; outside that window the quantity is available again; an issue request for more than the available quantity is rejected in full with a clear insufficient-stock error and the part's counts are left unchanged; a technician can restock/return a quantity of a part, immediately increasing its available quantity (and reducing its in-use quantity when the return closes out an active issue); the technician booking UI renders a live parts table (Part Name / In Use / Available / Total) below the booking panel; the technician booking grid shows a `Parts Used` column per job, rendering issued parts as `P00X(qty)` entries separated by `; `, blank when no parts are issued against that job |

## User stories

1. As the system, I want to boot with 4 known parts already stocked (`P001`, `P002`, `P003`, `P004`, with some totaling 3 units), so that technicians have real inventory to work with immediately, with no setup step.
2. As a technician, I want to issue a quantity of a part against a job I've booked, so that the repository automatically holds that quantity for me around my appointment time.
3. As a technician, I want the part I issued for a job to show as "in use" starting 45 minutes before that job and ending 45 minutes after it, so that the stock reflects a realistic pickup/drop-off window, not just the exact appointment time.
4. As a technician, I want to return/restock a quantity of a part I didn't use, so that the repository's available count stays accurate for others.
5. As anyone viewing the technician booking screen, I want to see a live parts-availability table (Part Name, In Use, Available, Total) below the booking panel, so I know what's on hand before booking or issuing.
6. As anyone viewing the technician booking grid, I want to see which parts (and quantities) are issued against each job right in the grid, so that I don't have to cross-reference the parts table to know what a job is holding.

## Acceptance criteria

**Story 1**
- Given a fresh application start, when the parts repository initializes, then exactly 4 parts (`P001`, `P002`, `P003`, `P004`) exist, each with a positive total quantity, and at least one part's total is 3.

**Story 2**
- Given a part with N available and a technician's booked job, when the technician issues a quantity Q ≤ N against that job, then the part's available quantity becomes N − Q and its in-use quantity increases by Q for the job's held window (see Story 3).
- Given a part with N available, when a technician issues a quantity Q > N, then the request is rejected in full with a clear insufficient-stock error, and the part's available/in-use quantities remain unchanged.

**Story 3**
- Given a job booked tomorrow from 12:00pm to 3:00pm with 1 unit of `P001` issued against it, then `P001`'s available quantity is reduced by 1 starting at 11:15am (45 minutes before 12:00pm) and that unit becomes available again starting at 3:45pm (45 minutes after 3:00pm).
- Before 11:15am or after 3:45pm on that day, the reserved unit does not count against availability.
- The reservation is held for that technician specifically — it is not releasable to, or issuable by, another technician while inside the window.

**Story 4**
- Given a part with N available (and an active reservation), when a technician restocks a quantity Q, then the part's available quantity increases by Q; if the restock corresponds to an active issue being returned early, the in-use quantity for that issue is reduced accordingly and the hold ends immediately rather than waiting for the 45-minute-after window to lapse.

**Story 5**
- Given any number of parts in the repository, when the technician booking screen loads, then a table below the booking panel lists every part's name, in-use quantity, available quantity, and total quantity, and reflects the current time-windowed state (not a stale snapshot).

**Story 6**
- Given a job with 2 units of `P001` and 1 unit of `P003` issued against it, when the technician booking grid renders that job's row, then its `Parts Used` column shows `P001(2); P003(1)`.
- Given a job with no parts issued against it, when the grid renders that row, then the `Parts Used` column is empty (not a placeholder like "None" or an error).
- Given a part issued against a job is later restocked/returned, when the grid re-renders, then the `Parts Used` column reflects the reduced or removed entry for that part.

## Edge cases

- Issuing a quantity of zero or a negative quantity is rejected as invalid input.
- Restocking a quantity of zero or a negative quantity is rejected as invalid input.
- Issuing or restocking against a part id that doesn't exist is rejected with a not-found error.
- Issuing against a job id that doesn't exist, or a job that doesn't belong to the requesting technician, is rejected with a clear error.
- Issuing a quantity equal to exactly what's available succeeds and leaves the part at zero available (zero is a valid, in-stock state — not an error).
- An insufficient-stock rejection never partially deducts — the part's available/in-use quantities are provably unchanged after a rejected request.
- Two different technicians' jobs whose 45-minute-buffered windows overlap and both draw on the same part are evaluated against the same shared `Available` count — if the combined requested quantity would exceed the total, the second (later) issue request is rejected with insufficient-stock, exactly as if it were a single pool.
- At the exact boundary instants (precisely 45 minutes before start, precisely 45 minutes after end), the part is treated as in-use (the window is inclusive), consistent with how `TRAVEL_BUFFER_MINUTES` is applied in Feature 1's overlap check.
- If a job's time is edited/rebooked after a part was issued against it, the part's held window must move with the job — a stale window (tied to the job's original time) is a bug, not acceptable behavior.
- Restocking more than the currently in-use quantity for that issue is rejected as invalid input (can't return more than was issued).
- A part's `available` and `in-use` counts must never go negative, and `in-use + available` must always equal `total`.
- The `Parts Used` column's ordering of multiple entries is stable (e.g. by part id) so the same job doesn't render its entries in a different order between page loads.
- If a returned/restocked quantity fully zeroes out a part's issued amount for a job, that part's entry is dropped from the `Parts Used` string entirely rather than showing `P001(0)`.

## Success metrics

- The parts table on the technician booking screen always matches backend state — refreshing never shows a stale in-use/available count for the current time.
- A technician issuing a part against a job when stock is sufficient always succeeds, and the repository's in-use/available counts visibly reflect the hold starting exactly 45 minutes before the job and ending exactly 45 minutes after it.
- A technician attempting to issue more than what's in stock always receives a clear rejection (never a silent success, never a partial issue), and the repository's counts are provably unaffected.
- A part's available quantity, observed over any sequence of issue/restock operations and across any time window, never goes negative, and `available + in-use` always reconciles to `total`.

## Human review notes (BA-agent interventions confirmed by a human)

The raw intent for this feature was ambiguous on several points. Rather than guess, the following were raised as clarifying questions and confirmed by a human before this spec was finalized:

- **Part vs. Tool:** intent mentioned "parts/tools" together — confirmed to model everything as a single `Part` concept only, for now; a distinct `Tool` model is explicitly out of scope.
- **Restocking:** confirmed restocking/returning a part's quantity is in scope for this feature, not deferred to a later one.
- **Insufficient-stock behavior:** confirmed the whole request is rejected (no partial fulfillment) when requested quantity exceeds availability, with a clear error message.

**Revised in this update** (superseding the original decisions below, per new direction):

- **Seeding vs. admin-added parts:** originally, an inventory manager added parts manually with a starting quantity. **Changed:** the catalog is now fixed and seeded at boot with exactly 4 parts (`P001`–`P004`, some totaling 3 units); adding new part types at runtime is now out of scope.
- **Issuing scope:** originally, issuing only ever referenced a `Technician`, explicitly not a `Job`. **Changed:** issuing now must reference the technician's booked `Job`, because the in-use window (45 minutes before/after the job) is computed from that job's start/end time. This directly reuses Feature 1's `TRAVEL_BUFFER_MINUTES = 45` decision rather than introducing a new, separately-tunable number.
- **UI requirement (new):** a parts-availability table (Part Name / In Use / Available / Total) must render on the technician booking screen, below the existing booking div, backed by a live backend endpoint.
- **UI requirement (new):** the existing technician booking grid gets a new `Parts Used` column, rendering each job's issued parts/quantities as a compact `P00X(qty); P00Y(qty)` string, backed by the same live parts-issue data (not a second, divergent source of truth from the availability table).

## Artifacts this feature touches
<filled in as the developer and tester agents do their work>