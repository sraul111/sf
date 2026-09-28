# Feature 2 Parts Reservation Implementation Plan

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-09-22 |
| **Requirements** | `requirements.md` |
| **Architecture** | `architecture-adr.md` |

## Task 1: Resolve Contract Questions

**Scope:** Obtain human decisions for the buffered end boundary, whether issue/return controls are required in the Angular UI, and whether a job-edit endpoint is part of this feature. Record the answers in the requirements or an approved decision artifact before coding.

**Expected artifacts:** Updated approved requirement/decision documentation; no application code.

**Acceptance checks:** Each open question in the ADR has an explicit answer; API and UI test expectations no longer conflict.

**Dependencies:** None.

**Effort:** Small.

## Task 2: Introduce the Shared Scheduling Window Policy

**Scope:** Create one backend policy abstraction that owns the 45-minute buffer and derives buffered job windows. Update `TechnicianAvailabilityService` to use it without changing the active no-overlap rule's behavior.

**Expected artifacts:** Shared policy class; constructor-injection update; focused booking conflict tests at overlap, before-buffer, exact-boundary, and after-buffer cases.

**Acceptance checks:** There is exactly one authoritative 45-minute definition; all existing booking tests pass; policy boundary tests reflect the Task 1 decision.

**Dependencies:** Task 1.

**Effort:** Small.

## Task 3: Add In-Memory Parts and Issue Persistence

**Scope:** Add `Part` and `PartIssue` models and extend `MockDataStore` with exactly four seeded parts, issue id generation, all-job lookup, part/issue query methods, and an inventory mutation lock. Keep existing technician and job behavior compatible.

**Expected artifacts:** Backend model classes; `MockDataStore` extensions; focused data-store seed and lookup tests.

**Acceptance checks:** A fresh store contains only `P001`-`P004`; every total is positive; at least one total is 3; repeated test setup does not duplicate seed data; existing Feature 1 tests pass.

**Dependencies:** None.

**Effort:** Medium.

## Task 4: Implement Inventory Domain Logic

**Scope:** Add `PartsInventoryService` with injected `Clock`, shared policy, reference/ownership validation, peak overlapping-window capacity calculation, atomic issue, atomic return, current inventory projections, and stable per-job parts formatting. Remove zero-quantity issue records rather than retaining `P00X(0)`.

**Expected artifacts:** Inventory service; response projection types or mappers; service unit tests using a fixed clock.

**Acceptance checks:** Tests cover successful issue, exact-capacity issue, non-positive quantities, missing references, wrong owner, overlapping technicians sharing stock, no partial mutation on rejection, partial/full/excessive return, rebooking-followed windows, stable part ordering, and `available + inUse = total` after operation sequences.

**Dependencies:** Tasks 2 and 3.

**Effort:** Large.

## Task 5: Expose Parts REST APIs

**Scope:** Add validated issue/return request DTOs, part availability and issue response DTOs, and `PartController`. Map invalid input to `400`, missing resources to `404`, and insufficient stock to `409`, all with `ApiError` bodies.

**Expected artifacts:** Controller and DTO classes; controller integration tests.

**Acceptance checks:** `GET /api/parts` returns all required fields in stable part-id order; issue returns `201`; return returns `200`; error responses have the required status and message; rejected issue leaves subsequent GET counts unchanged.

**Dependencies:** Task 4.

**Effort:** Medium.

## Task 6: Add Parts Used to Job Responses

**Scope:** Introduce a job response DTO preserving existing fields and adding `partsUsed`. Adapt technician job queries to project issue data through `PartsInventoryService`, without adding inventory collections to `Job`.

**Expected artifacts:** Job response DTO/mapper; controller changes; backend integration tests.

**Acceptance checks:** Jobs with no issues return an empty string; multiple parts render as `P001(2); P003(1)` in part-id order; partial and full returns update or remove entries; existing job fields remain API-compatible.

**Dependencies:** Task 4.

**Effort:** Medium.

## Task 7: Add Frontend Models and Parts Service

**Scope:** Add typed Angular models for part availability and issue/return requests, plus a `PartsService` that owns all parts HTTP calls. Extend the `Job` model with `partsUsed`.

**Expected artifacts:** TypeScript model files; Angular service; service HTTP tests.

**Acceptance checks:** Requests target the ADR-defined endpoints with typed payloads; no component calls `HttpClient` directly; job deserialization accepts `partsUsed`.

**Dependencies:** Tasks 5 and 6.

**Effort:** Small.

## Task 8: Render Live Inventory and Job Parts

**Scope:** Update the technician calendar to load current parts, render the four-column table directly below the booking panel, and add `Parts Used` to the jobs grid. Refresh jobs and parts after successful inventory commands. Add issue/return controls only if Task 1 confirms they are in scope.

**Expected artifacts:** Calendar component TypeScript/template/styles; component tests.

**Acceptance checks:** The table shows Part Name, In Use, Available, and Total from the API; job rows show the backend string or a blank cell; selecting technicians does not replace shared inventory with local mock data; successful commands refresh both views; layout remains usable at desktop and mobile widths.

**Dependencies:** Task 7 and Task 1 UI decision.

**Effort:** Medium, or Large if command controls are included.

## Task 9: Verify End-to-End Requirements and Regression Safety

**Scope:** Run backend and frontend suites and add only missing acceptance-level coverage. Verify current-time transitions with a controlled backend clock rather than wall-clock sleeps. Exercise the live UI/API flow for inventory and job projections.

**Expected artifacts:** Final focused tests; test results; updated `Artifacts this feature touches` section in `requirements.md`; Feature 2 decision log if implementation establishes reusable decisions.

**Acceptance checks:** Every Definition of Done and edge case maps to a passing test or documented manual check; Feature 1 booking behavior remains green; no database or external service is introduced; rejected commands are demonstrably atomic.

**Dependencies:** Tasks 5, 6, and 8.

**Effort:** Medium.