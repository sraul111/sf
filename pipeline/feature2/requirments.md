# Feature 2 — Parts Reservation

| Field | Value |
|---|---|
| **Status** | Candidate-built / in progress |
| **One-line intent** | Let a technician issue parts from a stocked repository, reducing available quantity, and return unused parts to restock it |
| **Depends on** | None — checked `pipeline/decisions/feature-1-decisions.md`; neither the 45-minute travel-buffer decision nor the overlap-detection fix apply here, since this feature does not touch job scheduling. It reuses the existing `Technician` identity from Feature 1 structurally (to record who issued a part), which is not itself a named decision. |
| **In scope** | A parts repository (mock, in-memory, seeded at startup) where each part has a name and a quantity available; adding a new part with an initial quantity; listing all parts and their current available quantity; a technician issuing a quantity of a part, which reduces its available quantity; rejecting an issue request in full (no partial fulfillment) when the requested quantity exceeds what's available, with a clear insufficient-stock error; a technician returning/restocking a quantity of a part, which increases its available quantity |
| **Out of scope** | Associating an issued part with a specific job/work order (issuing only ever references a technician, per this feature's scope); low-stock alerting or notifications; any concept of "tool" as a distinct model from "part" (tools are out of scope for now — everything is a `Part`) |
| **Definition of Done** | A new part can be added to the repository with an initial quantity; all parts and their current available quantity can be listed; a technician can issue a quantity of a part and the part's available quantity is reduced by that amount; an issue request for more than the available quantity is rejected in full with a clear insufficient-stock error and the part's available quantity is left unchanged; a technician can restock/return a quantity of a part and the part's available quantity is increased by that amount |

## User stories

1. As an inventory manager, I want to add a new part to the repository with an initial quantity, so that it becomes available for technicians to issue.
2. As a technician, I want to issue a quantity of a part, so that I can use it and the repository reflects what's left.
3. As a technician, I want to return/restock a quantity of a part I didn't use, so that the repository's available count stays accurate for others.
4. As anyone checking stock, I want to list all parts and their current available quantity, so that I can see what's on hand before requesting more.

## Acceptance criteria

**Story 1**
- Given a part name and an initial quantity, when it's added to the repository, then it appears in the parts listing with that quantity available.

**Story 2**
- Given a part with N available, when a technician issues a quantity Q ≤ N, then the part's available quantity becomes N − Q.
- Given a part with N available, when a technician issues a quantity Q > N, then the request is rejected in full with a clear insufficient-stock error, and the part's available quantity remains N.

**Story 3**
- Given a part with N available, when a technician restocks a quantity Q, then the part's available quantity becomes N + Q.

**Story 4**
- Given any number of parts in the repository, when they are listed, then each part's name and current available quantity are returned.

## Edge cases

- Issuing a quantity of zero or a negative quantity is rejected as invalid input.
- Restocking a quantity of zero or a negative quantity is rejected as invalid input.
- Issuing or restocking against a part id that doesn't exist is rejected with a not-found error.
- Issuing a quantity equal to exactly what's available succeeds and leaves the part at zero available (zero is a valid, in-stock state — not an error).
- An insufficient-stock rejection never partially deducts — the part's available quantity is provably unchanged after a rejected request.

## Success metrics

- A technician issuing a part when stock is sufficient always succeeds and the repository's available count visibly reflects the deduction on the next listing.
- A technician attempting to issue more than what's in stock always receives a clear rejection (never a silent success, never a partial issue) and the repository's count is provably unaffected.
- A part's available quantity, observed over any sequence of issue/restock operations, never goes negative.

## Human review notes (BA-agent interventions confirmed by a human)

The raw intent for this feature was ambiguous on several points. Rather than guess, the following were raised as clarifying questions and confirmed by a human before this spec was finalized:

- **Part vs. Tool:** intent mentioned "parts/tools" together — confirmed to model everything as a single `Part` concept only, for now; a distinct `Tool` model is explicitly out of scope.
- **Issuing scope:** confirmed issuing only ever references a `Technician`, not a `Job` — this feature does not link issued parts to a specific job/work order.
- **Restocking:** confirmed restocking/returning a part's quantity is in scope for this feature, not deferred to a later one.
- **Insufficient-stock behavior:** confirmed the whole request is rejected (no partial fulfillment) when requested quantity exceeds availability, with a clear error message.

## Artifacts this feature touches
<filled in as the developer and tester agents do their work>