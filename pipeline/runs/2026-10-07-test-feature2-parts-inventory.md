# Feature 2 Test Plan Run

- **Pipeline:** Tester verification
- **Trigger:** Execute the updated `pipeline/feature2/test-plan.md`, add its pre-test-plan reading rule to the tester-agent role, and implement tests.
- **Date:** 2026-10-07
- **Final status:** FAIL (test failures; substantial cases BLOCKED because the corresponding product surfaces are absent)
- **Subagent order:** `tester-agent` (single delegated step)
- **Product code:** Not changed.
- **Existing unrelated change:** `.github/outputs/reports/jira_agent_telemetry.json` was present and left untouched.

## Files Changed Or Produced

- `.claude/agents/tester-agent.md` — tool list narrowed to `Read, Edit, Write, Bash`; added the generic requirement to read the active feature directory's `test-plan.md` before writing tests. Existing core testing instructions retained.
- `backend/src/test/java/com/serviceforge/data/MockDataStoreFeature3Test.java` — AC-1 seeded part catalog test.
- `backend/src/test/java/com/serviceforge/controller/Feature2PartsReservationTest.java` — reservation API tests for AC-2b and EC-1/2/3/5/6/7.
- `pipeline/feature2/mcp_server/test_server.py` — mocked stock-tool response, mock memory travel-buffer fact, MCP validation/lookup/error mapping, and tool contract tests.
- Raw command transcripts were captured under `pipeline/feature2/test-results/`. They match `*.log` ignore rules and therefore do not appear in normal Git status, but remain available in the workspace:
  - [Raw backend Maven output](../feature2/test-results/backend-maven.log)
  - [Raw frontend output](../feature2/test-results/frontend-npm.log)
  - [Raw MCP output](../feature2/test-results/mcp-unittest.log)
  - [Raw memory pipeline output](../feature2/test-results/memory-node-test.log)

## Commands And Results

All commands were run by `tester-agent`; command working directories are noted.

| Working directory | Command | Result |
|---|---|---|
| `backend` | `mvn test` | FAIL — 43 tests, 2 failures, 0 errors. New failures: EC-3 missing job returned `200` and reduced stock; EC-5 completed/cancelled jobs returned `200` and reduced stock. Existing Feature 1/3 suites passed. |
| `frontend` | `npm test` | PASS — 8 tests. |
| repository root | `python -B -m unittest discover -s pipeline/feature2/mcp_server -p test_*.py -v` | FAIL — 7 tests, 2 failures: extra tool arguments accepted, and boolean `true` accepted as reservation quantity. |
| repository root | `node --test pipeline/feature2/memory_pipeline/memory_pipeline.test.js` | PASS — 9 tests. |
| repository root | `git diff --check` | PASS — no whitespace errors; Git reported a line-ending warning for the pre-existing telemetry file. |

## Plan Coverage

`PASS` means the named behavior was asserted and passed. `FAIL` means an executable assertion exposed a mismatch. `PARTIAL` means only the stated subset was tested. `NOT COVERED` means the current product has no matching behavior/surface to test; no passing result is claimed.

| AC / Ref | Test(s) and result |
|---|---|
| Story 1 / AC-1 | `MockDataStoreFeature3Test.ac1FreshStoreSeedsExactlyFourPartsInIdOrderWithExpectedCatalogValues` — PASS; checks count, order, IDs, SKUs, names, and quantities. |
| Story 2 / AC-2a | NOT COVERED — no job-owned issue/outstanding quantity or hold-window availability implementation. |
| Story 2 / AC-2b | `Feature2PartsReservationTest.ac2bAggregateInsufficientStockReturnsConflictWithoutPartialMutation` — PARTIAL PASS; aggregate stock rejection and unchanged stock pass, but candidate-window capacity and issue-record invariance are not represented. |
| Story 3 / AC-3a | NOT COVERED — no allocation hold-window projection for the exact buffered boundary instants. |
| Story 3 / AC-3b | NOT COVERED — no fixed-clock inventory projection, shared overlapping reservation pool, or ownership-aware issue/release behavior. The memory-fact mock test does not validate these reservation behaviors. |
| Story 4 / AC-4a | NOT COVERED — no return command or outstanding-issue records. |
| Story 4 / AC-4b | `Feature2PartsReservationTest.ec5CompletedAndCancelledJobIssuesReturnConflictWithoutChangingStock` — FAIL: expected `409, 409`, observed `200, 200`; stock expected `3`, observed `1`. Release of prior holdings and omission from job parts are not implemented. |
| Story 5 / AC-5 | NOT COVERED — no technician booking-screen parts inventory display or parts service. Frontend suite passed but has no test for this absent screen behavior. |
| Story 6 / AC-6a | NOT COVERED — no `Parts Used` projection. |
| Story 6 / AC-6b; API-only | NOT COVERED — no booking-grid parts projection to render; issue/return UI scope cannot be assessed against that projection. |
| EC-1 | PARTIAL / FAIL: `Feature2PartsReservationTest.ec1NegativeIssueQuantityReturnsBadRequestWithoutMutation` — PASS for negative Java API input. MCP `test_ec1McpRejectsNonPositiveAndNonIntegerReservationQuantitiesBeforeBackendCall` — FAIL for `True`, which is accepted; the remaining supplied invalid values are rejected. Return quantity validation is absent. |
| EC-2 | PARTIAL PASS: `Feature2PartsReservationTest.ec2LowercaseSkuIssueReturnsCanonicalSku` and MCP `test_ec2StockLookupReturnsCanonicalSkuAndMapsUnknownSku` pass lowercase canonicalization and mocked unknown-SKU mapping. Unknown-SKU return behavior is absent. |
| EC-3 | FAIL / PARTIAL: `Feature2PartsReservationTest.ec3MissingJobIssueReturnsNotFoundWithoutChangingStock` — expected `404` and unchanged stock; observed `200` and stock decreased from `3` to `2`. MCP `test_ec3AvailabilityMapsMissingTechnicianFromMockedBackend` passes mocked missing-technician error mapping. |
| EC-4 | NOT COVERED — no authenticated caller ownership/session checks and no return API. |
| EC-5 | FAIL / PARTIAL: the AC-4b test above fails for completed/cancelled issue status and stock mutation. Existing allocation release on lifecycle transition is absent. |
| EC-6 | PARTIAL PASS: `Feature2PartsReservationTest.ec6ConcurrentAggregateReservationsCannotExceedSeededStock` yields one `200`, one `409`, and zero aggregate stock. It does not model distinct technicians or buffered windows. |
| EC-7 | PARTIAL PASS: `Feature2PartsReservationTest.ec7ExactAggregateCapacityExhaustsStockWithoutGoingNegative` passes exact aggregate capacity and subsequent rejection. `inUse + available == total` and issue/return sequences cannot be checked without projections/returns. |
| EC-8 | NOT COVERED — no peak-capacity-over-candidate-window calculation. |
| EC-9 | NOT COVERED — no return operation, injectable reservation mutation failure, or issue records to verify rollback and retry. |
| EC-10 | PARTIAL: `test_crosscutAvailabilityUsesWindowMatchingMockedMemoryTravelBufferFact` passes a UTC buffered-window expectation using a mocked memory fact. Touching reservation windows, parts projection ordering/zero omission, and rescheduling scope have no matching implementation. |
| Cross-cutting / Contract | PARTIAL / FAIL: `test_crosscut_stock_checkUsesMockedStockToolResponse` and `test_crosscut_reservationMapsMockedConflictWithoutRetryingOrMutating` pass with mocked stock/backend responses. `test_crosscutToolContractRejectsUnexpectedArguments` fails because an extra argument is accepted; EC-1 also fails on boolean input. Full success/error schema and return contract cases are absent. |
| Cross-cutting / Correctness | NOT COVERED: no shared reservation scheduling-window policy is implemented. The mocked travel-buffer fact check only verifies the availability tool's response against test-provided data; it does not prove reservation code consumes a shared policy or avoids the Feature 1 constant. |
| Cross-cutting / Security bypass | NOT COVERED — the listed orchestrator/critic/policy-grant/authorization-guardrail workflow is not present as a testable in-process surface in this repository. |
| Cross-cutting / Integration path | NOT COVERED — no schedule/check/critic/reserve orchestrator or reservation projection handoff exists. |
| Known risk / Feature 1 overlap bug | PASS for regression run: existing `TechnicianAvailabilityControllerTest` and related backend suite passed unchanged. No overlap behavior was modified. |

## Blockers And Assumptions

- The plan spans a broader reservation application than the existing product surfaces. Tests were limited to current backend catalog/reservation endpoints, MCP server functions, frontend suite, and memory pipeline. No product behavior was added to make missing cases testable.
- The mocked memory travel-buffer fact is synthetic test input; the MCP server does not consume memory-skill output at runtime. Its test result is not evidence of a shared backend scheduling policy.
- Full Definition-of-Plan coverage is not achieved. The test suite intentionally retains red assertions for observed contract failures; this run does not certify the plan as passing.
