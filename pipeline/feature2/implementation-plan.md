# Feature 2 Parts Reservation Implementation Plan

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-09-29 |
| **Requirements** | `requirements.md` |
| **Architecture** | `architecture-adr.md` |
| **Revision** | 2026-09-29 — revised in place: `P001`–`P004` replaced by Feature 3's SKU catalog; tasks now extend Feature 3's `Part`, `MockDataStore` seed, `PartService`, and `PartController` instead of creating new ones; Feature 3 regression checks added. Originally proposed 2026-09-22. |

## Task 1: Resolve Contract Questions

**Scope:** Get human decisions on: the buffered end boundary; whether the Angular UI needs issue/return controls; SKU case sensitivity; whether the SKU appears in the parts table; and the seed totals (ADR Open Questions 1–6). Record the answers in the requirements or an approved decision artifact before coding. Note: job rescheduling/editing is out of scope for Feature 2 per the human decision; remove rebooking tests from later tasks.

**Expected artifacts:** Updated approved requirement/decision documentation; no application code.

**Acceptance checks:** Each open question in the ADR has an explicit answer; API and UI test expectations no longer conflict.

**Dependencies:** None.

**Effort:** Small.

## Task 2: Introduce the Shared Scheduling Window Policy

**Scope:** Create `SchedulingWindowPolicy`, which owns the only `45` literal and derives buffered job windows. Keep `TechnicianAvailabilityService.TRAVEL_BUFFER_MINUTES` as a public compile-time alias (`= SchedulingWindowPolicy.TRAVEL_BUFFER_MINUTES`) and have `bookJob` use the policy without changing the active no-overlap rule's behavior. Update the "Where it lives in code" pointer in `pipeline/decisions/feature-1-decisions.md`.

**Expected artifacts:** Shared policy class; constructor-injection update; focused booking conflict tests at overlap, before-buffer, exact-boundary, and after-buffer cases; decision-log pointer update.

**Acceptance checks:** There is exactly one `45` literal in backend code under `backend/src/main/**`; `TechnicianAvailabilityControllerTest` and `Feature3DispatchControllerTest` (including DoD 14 buffer cases and DoD 15 `TRAVEL_BUFFER_MINUTES == 45`) pass unchanged; policy boundary tests reflect the Task 1 decision.

**Dependencies:** Task 1.

**Effort:** Small.

## Task 3: Extend Feature 3's Part Model and Add Issue Persistence

**Scope:** Load `.claude/skills/migration-safety-skill/SKILL.md`. Extend the existing `model/Part` with a final `int totalQuantity`, a `(Long, String, String, int)` constructor, and `getTotalQuantity()`. Keep the `(Long, String, String)` constructor, which delegates with `0`, and keep `setSku`/`setName`. Change the four existing seed lines in `MockDataStore` to the 4-arg constructor with the same ids, SKUs, names, and order, and the totals approved in Task 1 (proposed: `CMP-CLUTCH` 3, `FLT-16` 10, `BRK-20A` 5, `VLV-BALL-075` 3). Add a `PartIssue` model (`partId: Long`, unique per `(jobId, partId)`). Extend `MockDataStore` with `findPart(Long)`, all-job lookup/`findJob(Long)`, issue query and upsert/remove operations, issue id generation, and an inventory mutation lock. Keep `getAllParts()`, `findPartBySku(String)`, technician behavior, and job behavior compatible. Grep every `Part` consumer in `backend/` and `frontend/src/app/`.

**Expected artifacts:** Modified `Part`; new `PartIssue`; `MockDataStore` extensions; focused data-store seed and lookup tests.

**Acceptance checks:** A fresh store has exactly 4 parts with ids `1`–`4` and SKUs `CMP-CLUTCH`, `FLT-16`, `BRK-20A`, `VLV-BALL-075` in that order; every total is positive; at least one total is 3; repeated test setup does not duplicate seed data; `Feature3ModelTest`, `MockDataStoreFeature3Test`, and Feature 1 tests pass unchanged.

**Dependencies:** None.

**Effort:** Medium.

## Task 4: Implement Inventory Domain Logic in PartService

**Scope:** Extend Feature 3's existing `PartService` rather than adding a parallel service. Inject `Clock` and `SchedulingWindowPolicy` by constructor. Add: SKU-to-part resolution at the boundary; reference and ownership validation; peak overlapping-window capacity calculation; an atomic issue that increments the single `(jobId, partId)` allocation; an atomic return; current availability projections sorted by `Part.id`; and `Parts Used` formatting as `<SKU>(qty)` joined by `; `, sorted by `Part.id`. Remove zero-quantity issue records rather than rendering `CMP-CLUTCH(0)`. Register a `Clock` bean that tests can override.

**Expected artifacts:** Extended `PartService`; projection mappers; service unit tests using a fixed clock.

**Acceptance checks:** Tests cover: successful issue; exact-capacity issue; repeated issue for the same job and SKU aggregating into one allocation; non-positive quantities; unknown SKU; missing technician/job; wrong owner (`403`); overlapping technicians sharing stock; no partial mutation on rejection; partial, full, and excessive returns; rebooking-followed windows are removed from acceptance (job rescheduling is out of scope); id-ordered (not alphabetical) formatting; and `available + inUse = total` after operation sequences. Include a focused concurrency acceptance test that races two requests for the last available unit and asserts exactly one success and one `409` failure.

**Dependencies:** Tasks 2 and 3.

**Effort:** Large.

## Task 5: Extend the Parts REST API

**Scope:** Extend Feature 3's `PartController` in place. Switch `GET /api/parts` from `List<Part>` to `PartAvailabilityResponse[]` (`id`, `sku`, `name`, `inUse`, `available`, `total`, sorted by `id`). Add `POST /api/parts/{sku}/issues` and `POST /api/parts/{sku}/returns` with validated `{ technicianId, jobId, quantity }` request DTOs and a `PartIssueResponse` (`partId`, `sku`, `jobId`, `technicianId`, `outstandingQuantity`). Map invalid input to `400`, wrong-owner to `403`, unknown SKU and missing technician/job/allocation to `404`, and insufficient stock or illegal issue against non-SCHEDULED job to `409`, all with `ApiError` bodies.

**Expected artifacts:** Modified `PartController`; new request/response DTO classes; controller integration tests.

**Acceptance checks:** `GET /api/parts` returns all six fields for exactly 4 parts in id order; Feature 3 DoD 12 test (`id`/`sku`/`name` present) passes unchanged; issuing against `CMP-CLUTCH` returns `201`; return returns `200`; `/api/parts/NOPE/issues` returns `404`; wrong-owner attempts return `403`; issuing against a non-`SCHEDULED` job returns `409`; error responses have the required status and message; a rejected issue leaves subsequent GET counts unchanged.

**Dependencies:** Task 4.

**Effort:** Medium.

## Task 6: Add Parts Used to Job Responses

**Scope:** Add a `JobResponse` DTO for `GET /api/technicians/{id}/jobs` that keeps every current `Job` JSON field, including `requiredSkill` serialized as `null` when absent (no `NON_NULL` inclusion), and adds `partsUsed`. Adapt the technician jobs query to project issue data through `PartService` without adding inventory collections to `Job`. `POST /api/jobs` stays unchanged.

**Expected artifacts:** `JobResponse` DTO/mapper; `TechnicianController` change; backend integration tests.

**Acceptance checks:** Jobs with no issues return `""`; a job with 2 `CMP-CLUTCH` and 1 `BRK-20A` renders exactly `CMP-CLUTCH(2); BRK-20A(1)` even if `BRK-20A` was issued first; `FLT-16` renders before `BRK-20A`; partial returns reduce entries and full returns remove them; Feature 3 DoD 9 (`requiredSkill` on seeded jobs) and DoD 11 (`POST /api/jobs` `requiredSkill` key is `null`) pass unchanged.

**Dependencies:** Task 4.

**Effort:** Medium.

## Task 7: Add Frontend Models and Parts Service

**Scope:** Add a new `part.model.ts` with `PartAvailability { id, sku, name, inUse, available, total }`, `PartIssueRequest`, and `PartIssueResponse` (Feature 3 added no Angular part model). Add a `PartsService` that owns all parts HTTP calls and addresses commands by SKU. Extend the `Job` model with optional `partsUsed?: string`.

**Expected artifacts:** TypeScript model file; `Job` model update; Angular service; service HTTP tests.

**Acceptance checks:** Requests target `/api/parts`, `/api/parts/{sku}/issues`, and `/api/parts/{sku}/returns` with typed payloads; no component calls `HttpClient` directly; job deserialization accepts `partsUsed`; `feature3-models.spec.ts` still compiles and passes.

**Test guidance (spec change):** Update `frontend/src/app/technician-calendar/technician-calendar.component.spec.ts` to provide a `PartsService` spy/provider alongside the existing `TechnicianService` spy to avoid `NullInjectorError` when the component injects `PartsService`.

**Dependencies:** Tasks 5 and 6.

**Effort:** Small.

## Task 8: Render Live Inventory and Job Parts

**Scope:** Update the technician calendar to load current parts and render the four-column table (Part Name / In Use / Available / Total) directly below the booking panel. `Part Name` shows `name`; SKU display follows the Task 1 decision. Add `Parts Used` to the jobs grid, rendering the backend string as-is. Refresh jobs and parts after successful inventory commands. Add issue/return controls, addressed by SKU, only if Task 1 confirms they are in scope.

**Expected artifacts:** Calendar component TypeScript/template/styles; component tests.

**Acceptance checks:** The table shows Part Name, In Use, Available, and Total from the API; job rows show the backend string or a blank cell; selecting technicians does not replace shared inventory with local mock data; successful commands refresh both views; layout remains usable at desktop and mobile widths.

**Dependencies:** Task 7 and Task 1 UI decision.

**Effort:** Medium, or Large if command controls are included.

## Task 9: Verify End-to-End Requirements and Regression Safety

**Scope:** Run backend and frontend suites and add only missing acceptance-level coverage. Verify current-time transitions with a controlled backend clock rather than wall-clock sleeps. Exercise the live UI/API flow for inventory and job projections.

**Expected artifacts:** Final focused tests; test results; updated `Artifacts this feature touches` section in `requirements.md`; a Feature 2 decision log recording reusable decisions (SKU-addressed part commands, `Part.totalQuantity`, `SchedulingWindowPolicy` as the buffer's home, and that Feature 2 supersedes Feature 3's parts-stock and parts-write-endpoint exclusions).

**Acceptance checks:** Every Definition of Done and edge case maps to a passing test or documented manual check; Feature 1 booking behavior and all Feature 3 backend tests (`Feature3DispatchControllerTest`, `MockDataStoreFeature3Test`, `Feature3ModelTest`) and `feature3-models.spec.ts` remain green; no `P001`–`P004` identifiers appear in code or tests; no database or external service is introduced; rejected commands are demonstrably atomic.

**Test infrastructure guidance:**
- Clock override: register production `Clock` as `Clock.systemUTC()`. In tests, override the `Clock` bean with a fixed UTC `Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)` via `@TestConfiguration` with `@Primary` or use `@MockBean Clock` to ensure deterministic boundary tests.
- Test isolation: Feature 2 tests that mutate `MockDataStore` should either use `@DirtiesContext` or explicitly reset the singleton store between tests to avoid cross-test state leakage.
- Scope the "one 45 literal" check to backend source: when searching for uses of `45` to validate centralization, limit the grep to `backend/src/main/**` so test code and comments are excluded.

**Dependencies:** Tasks 5, 6, and 8.

**Effort:** Medium.