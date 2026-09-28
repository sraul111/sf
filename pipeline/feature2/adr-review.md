# ADR Review: Job-Linked, Time-Derived Parts Reservations

| Field | Value |
|---|---|
| **Verdict** | Changes required |
| **ADR** | `pipeline/feature2/architecture-adr.md` |
| **Requirements** | `pipeline/feature2/requirements.md` |

## Findings
### Critical
None.

### Major
- **M1 — Resolve the buffered-window end boundary before approval.** `pipeline/feature2/requirements.md`, Story 3 says the unit becomes available starting exactly at 3:45pm, while its Edge cases section says both exact buffer boundaries are in-use. The ADR adopts inclusive end semantics and calls that consistent with Feature 1, but `TechnicianAvailabilityService.bookJob(...)` uses strict `isBefore` comparisons, which permits a booking at the exact buffered end and therefore behaves as a half-open interval. The same instant can consequently be counted as in-use by parts while scheduling permits a booking at that boundary, contrary to the ADR's assertion that the semantics match. **Impact:** availability and capacity checks can disagree at a stock-critical boundary. **Correction:** obtain the human decision, make the requirement, shared policy, capacity algorithm, and tests use one explicit endpoint convention, and remove the unsupported claim that inclusive semantics match current booking behavior.
- **M2 — Define how local job times map to the injected clock.** The ADR proposes obtaining `Clock.instant()` and comparing it with buffered job windows, but `Job.startTime` and `Job.endTime` are `LocalDateTime`; seeded jobs use `LocalDateTime.now()`, and the frontend submits `datetime-local` values. No zone or conversion rule is specified. An `Instant` cannot be compared to these local timestamps without choosing a `ZoneId`, and daylight-saving transitions can make local times ambiguous or invalid. **Impact:** the live in-use projection may change with host configuration or calculate the wrong hold window, while fixed-clock tests may not represent production behavior. **Correction:** record the intended time-zone contract and explicitly define conversion in the shared policy and test setup before implementation; preserve the existing API/model contract unless a separately approved migration is needed.
- **M3 — Make the required rebooking behavior implementable and verifiable.** Requirements Edge cases mandates that a reservation follow a job when its time is edited, and the ADR's traceability table promises a rebooking test. In the current implementation, `Job.startTime` and `Job.endTime` are final, `MockDataStore` has no job-update operation, and there is no rebooking endpoint. The plan correctly raises endpoint scope as a human question, but its Task 4 acceptance check still requires rebooking-followed behavior without a testable mutation path. **Impact:** the mandatory behavior cannot be demonstrated by the planned implementation as currently scoped; if a mutation path is added, a concurrent time change could also race the inventory capacity check because the proposed inventory lock does not coordinate job updates. **Correction:** have the human decide whether Feature 2 includes job editing/rebooking; then align the approved requirement and plan, define a testable way to change job times, and coordinate that mutation with inventory reservation checks if it can run concurrently.

### Minor
- **m1 — Specify repeated issue aggregation and return selection.** The model gives each `PartIssue` an id, while the return API identifies an allocation only by `(technicianId, jobId, partId)` and the ADR does not explicitly state whether repeated issues for the same job and part update one allocation or create multiple records. **Impact:** a return can be ambiguous if multiple matching records exist, and response/`Parts Used` aggregation may differ. **Correction:** state and test a uniqueness/upsert invariant for one outstanding allocation per job and part, or define deterministic multi-record return behavior.
- **m2 — Carry the promised concurrency test into plan acceptance checks.** The ADR's risk mitigation calls for a concurrency-focused service test, but the plan's test lists specify overlapping windows and atomic mutation without explicitly requiring simultaneous competing requests. **Impact:** sequential tests would not verify that the lock prevents two requests from both consuming the same remaining capacity. **Correction:** add a coordinated concurrent issue test for a final shared unit, asserting one success, one `409`, and no over-allocation.

## Requirement Coverage

| Requirement area | Proposed coverage | Review |
|---|---|---|
| Story 1: exactly four seeded parts, positive totals, one total of 3 | `MockDataStore` seed and fresh-context test | Covered in design and plan; verify seed initialization is single-shot per store/context. |
| Stories 2–3: issue against an owned job; shared stock; 45-minute window; no partial fulfillment | `PartsInventoryService`, shared scheduling policy, peak-overlap capacity check, issue endpoint | Core path is covered, but exact end-boundary semantics conflict (M1), and current-time conversion is undefined (M2). |
| Story 4: partial/full return and restock | Atomic outstanding-quantity reduction/removal and return endpoint | Covered in outline; repeated matching issue behavior needs a defined invariant (m1). |
| Story 5: live parts panel with four required columns | `GET /api/parts`, Angular service and table | Covered; whether the UI also needs issue/return controls remains for human decision. |
| Story 6: stable per-job `Parts Used` projection | Job DTO projection from issue records, sorted by part id | Covered in design and plan; repeated-allocation aggregation needs clarification (m1). |
| Edge: rebooking moves the reservation | Resolve each issue against current job times | Conceptually supported, but no current job mutation path exists and the plan's test cannot be implemented as written (M3). |
| Edge: rejected issue is atomic; overlapping technicians share stock | Capacity check and mutation under inventory lock | Designed, but simultaneous-request verification is missing from plan acceptance (m2). |
| Edge: invalid quantities/references and error statuses | `400`/`404`/`409` with `ApiError` | Covered in the REST contract and planned controller tests. |
| Edge: no negative counts, reconciliation, stable order, zero entries omitted | Derived counts, sorted projection, remove zero issues | Covered in design and plan. |

## ServiceForge Compatibility

- **Confirmed compatible:** Java 17, Spring Boot 3, package-by-layer classes, constructor injection, `ResponseEntity` controller responses, `ApiError` conventions, Angular service-owned HTTP, and mock/in-memory storage all match `AGENTS.md` and current backend/frontend patterns.
- **Confirmed compatible:** the active `pipeline/feature2/rules/no-overlap-booking.md` requires preserving the 45-minute no-overlap booking behavior. Current `TechnicianAvailabilityService` applies `TRAVEL_BUFFER_MINUTES`; a shared policy can replace the duplicated ownership of that value only if booking behavior remains unchanged. The active rule's buffered intervals are end-exclusive under the current strict comparisons, contrary to the ADR's inclusive-boundary rationale (M1).
- **Confirmed compatible:** Feature 1's decision log names 45 minutes as the standard travel buffer. A single shared definition is appropriate and no database or external service is proposed.
- **Compatibility obligation:** current `GET /api/technicians/{id}/jobs` serializes `Job` objects with `id`, `technicianId`, `customerName`, `startTime`, `endTime`, and `status`; the Angular `Job` interface consumes these fields. Replacing raw `Job` output with a DTO must preserve those JSON names and values while adding `partsUsed`, as the ADR and plan state.
- **Unverified/needs decision:** current job times are local, zone-free timestamps and the current model has no job-time update operation. The clock comparison and rebooking changes therefore need the clarifications listed in M2 and M3; they are not established facts of the existing implementation.

## Riskiest Decision

The highest-risk choice is making outstanding issue quantities job-linked and deriving their active windows dynamically while enforcing shared capacity under a lock. This avoids stale copied timestamps and counters, but correctness depends on one precise time-boundary/time-zone contract and on job-time mutations being consistent with the inventory check. M1–M3 must be resolved before this design is ready to implement.

## Required Changes

1. Resolve the inclusive-versus-available-at-end contradiction and align the requirement, policy definition, capacity calculation, and boundary tests (M1).
2. Define the zone/conversion contract between `Clock` instants and the existing `LocalDateTime` job API/model; make fixed-clock tests use that same contract (M2).
3. Decide whether job rebooking is in Feature 2. Update the requirement/plan and provide a testable mutation path; if mutations are supported, serialize or otherwise coordinate them with reservation capacity checks (M3).
4. Define repeated issue aggregation/return selection and add the concurrency acceptance test (m1, m2).

## Questions for Human Review

1. At exactly `job.endTime + 45 minutes`, is the quantity still in use, or is it available at that instant? Should the start boundary follow the same convention?
2. Should technicians be able to issue and return parts from the Angular screen, or are those operations API-only in this feature?
3. Is adding a job edit/rebooking operation part of Feature 2, or should this feature only guarantee that any supported future time mutation is followed by the derived reservation?
4. Which time zone governs the existing local job timestamps and the inventory observation clock, especially around daylight-saving transitions?
5. May parts be issued only against `SCHEDULED` jobs, and what should happen to an outstanding issue if a job becomes `COMPLETED` or `CANCELLED`?