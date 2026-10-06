# Run report — Feature 3: Dispatch skills and parts SKU

| Field | Value |
|---|---|
| Pipeline type | Feature development (BA → Developer → Tester) |
| Trigger | "Develop this feature: Update the Java backend models and MockDataStore to support the new dispatch workflow requirements (Technician.skills, Job.requiredSkill, Part.sku, seed data, DTO exposure, preserve Feature 1 45-minute overlap logic)" |
| Date | 2026-09-28 |
| Final status | **PASS** |

## Subagents run (in order)

1. `ba-agent` — wrote the spec, then the pipeline paused for human review.
2. Human checkpoint — the spec was approved as written, including A1–A5.
3. `developer-agent` — implemented the spec with no deviations.
4. `tester-agent` — all 17 Definition of Done items passed.

## Files changed or produced

**Spec and report**
- `pipeline/features/feature-3-dispatch-skills-and-parts-sku.md` — new spec; the developer filled in its "Artifacts this feature touches" section.
- `pipeline/runs/2026-09-28-feature-dispatch-skills-and-parts-sku.md` — this report.

**Backend, new files** (under `backend/src/main/java/com/serviceforge/`)
- `model/Part.java`
- `service/PartService.java`
- `controller/PartController.java` — `GET /api/parts`.

**Backend, modified files**
- `model/Technician.java` — `skills`, a new 4-argument constructor, and a getter and setter. The 3-argument constructor stays and gives an empty list.
- `model/Job.java` — `requiredSkill`, a new 7-argument constructor, and a getter with no setter (A1). The 6-argument constructor stays and gives `null`.
- `data/MockDataStore.java` — seed data for skills, required skills and 4 parts, plus `getAllParts()` and `findPartBySku()`.
- `dto/BookJobRequest.java` — optional `requiredSkill`.
- `service/TechnicianAvailabilityService.java` — a `bookJob` overload; the 45-minute buffer and the conflict check are unchanged.
- `controller/JobController.java` — passes `requiredSkill` through.

**Frontend**
- `models/technician.model.ts` and `models/job.model.ts` — optional `skills?` and `requiredSkill?` fields.

**Tests, new files**
- `backend/src/test/java/com/serviceforge/model/Feature3ModelTest.java` (5 tests)
- `backend/src/test/java/com/serviceforge/data/MockDataStoreFeature3Test.java` (3 tests)
- `backend/src/test/java/com/serviceforge/controller/Feature3DispatchControllerTest.java` (11 tests)
- `frontend/src/app/models/feature3-models.spec.ts` (3 specs)

## Commands run

| Agent | Command | Result |
|---|---|---|
| developer | `mvn -q test-compile` | Exit 0 |
| developer | `mvn test` | Exit 0; 12 of 12 passed |
| developer | `npx ng build` | Exit 0 |
| developer | `npx ng test --watch=false --browsers=ChromeHeadless` | Exit 0; 5 of 5 passed |
| tester | `mvn test` (first run) | Exit 1; 26 context-load errors caused by a stale class file the IDE wrote to `target/` |
| tester | `mvn clean test` | Exit 0; 31 of 31 passed |
| tester | `mvn test` (rerun) | Exit 0; 31 of 31 passed |
| tester | `npx ng test --watch=false --browsers=ChromeHeadless` | 8 of 8 passed; exit code not captured |
| tester | `npx ng build` | Exit 0 |

## Verdicts

- All 17 Definition of Done items pass.
- The rule `no-overlap-booking.md` and the Feature 1 45-minute buffer decision still hold. The existing tests pass unedited, and new tests show `requiredSkill` has no effect on conflict detection.
- No new Maven or npm dependencies were added.

## Notes

- **Stale build output:** if a plain `mvn test` fails with `ClassNotFoundException` for the new classes, run `mvn clean test` instead.
- **Earlier startup failures:** `mvn spring-boot:run` and `npm start` both exited with code 1 in the user's terminals. The agents didn't investigate them; a port already in use or the stale `target/` output are the likely causes.
- **Not committed:** none of the changes are committed to git. No `feature-3-decisions.md` was created, because neither the spec nor the role files ask for one.

## Dependencies identified

- `pipeline/decisions/feature-1-decisions.md`: 45-minute travel buffer and buffered-interval conflict detection.
- `pipeline/feature2/rules/no-overlap-booking.md`.
- No dependency on Feature 2 code; Feature 2 didn't change backend or frontend code.

## Assumptions (approved by human)

- **A1:** `Job.requiredSkill` has no setter, to match the other fields on `Job` that can't be changed. The request asked for setters on every new field.
- **A2:** The new `Part` model has only `id`, `sku` and `name`. It has no stock, price or link to jobs. No Part model existed before this feature.
- **A3:** The API has no response DTOs; it returns the model classes directly. Only `BookJobRequest` gets a new field: an optional `requiredSkill`.
- **A4:** The spec proposes a read-only `GET /api/parts` endpoint so `sku` shows up in API JSON. The request didn't ask for it. Definition of Done item 12 applies only if you approve it.
- **A5:** The frontend TypeScript interfaces get the new fields as optional properties. The request didn't ask for this. There are no UI changes.
- Seed values: the part names, the extra SKUs `BRK-20A` and `VLV-BALL-075`, and which technician gets which skills were chosen by the BA agent.
- Skills are free text. Nothing checks that a technician has a job's required skill; skill matching is left for a later feature.

## Memory Operations

None performed.
