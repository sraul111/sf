# Run report: Feature 2 ADR revision (parts identified by SKU)

| Field | Value |
|---|---|
| Pipeline type | Architecture and planning (`adr-agent` → `adr-review-agent`) |
| Trigger | The user chose option B: treat the change from `P001`–`P004` to SKUs as a requirements change, update `requirements.md`, then rerun the ADR author and ADR review against `pipeline/feature2/requirements.md` |
| Date | 2026-09-29 |
| Final status | **BLOCKED**: the review verdict is *Changes required*, and the pipeline is waiting for human decisions |

## Execution order

1. Orchestrator: updated `pipeline/feature2/requirements.md`, as the user directed.
   - Replaced `P001`–`P004` with the Feature 3 SKU catalog (`CMP-CLUTCH`, `FLT-16`, `BRK-20A`, `VLV-BALL-075`).
   - Changed the `Parts Used` format to `<SKU>(qty)`, ordered by the part's numeric id.
   - Added Feature 3 as a dependency and added a revision note.
2. `adr-agent`: revised `architecture-adr.md` and `implementation-plan.md` in place.
3. `adr-review-agent`: rewrote `adr-review.md` in place. Verdict: **Changes required**.

## Files changed

- `pipeline/feature2/requirements.md`
- `pipeline/feature2/architecture-adr.md`
- `pipeline/feature2/implementation-plan.md`
- `pipeline/feature2/adr-review.md`
- `pipeline/runs/2026-09-29-adr-feature2-parts-sku.md` (this report)

No application code was changed.

## Commands run

None.

## Key design decisions (ADR revision)

- **Part identifier in the API:** parts are identified by SKU in the path (`POST /api/parts/{sku}/issues` and `/returns`). Internally, `PartIssue.partId` stores the numeric `Long` id.
- **`Part` model:** keeps its existing 3-argument constructor and gains a `totalQuantity` field and a 4-argument constructor.
- **Proposed seed totals:** CMP-CLUTCH 3, FLT-16 10, BRK-20A 5, VLV-BALL-075 3.
- **`GET /api/parts`:** keeps `id`, `sku` and `name`, and adds `inUse`, `available` and `total`. The change only adds fields.
- **`JobResponse`:** keeps every existing field, including `requiredSkill`, and adds `partsUsed`.
- **One allocation per job and part.**
- **Buffer constant:** `SchedulingWindowPolicy` becomes the only place the 45-minute buffer is defined. `TRAVEL_BUFFER_MINUTES` stays as an alias.

## Review verdict: Changes required

- **Major**
  - **M1 (still open):** it isn't decided whether the part hold is in effect at exactly 45 minutes before start and 45 minutes after end. Booking stays end-exclusive either way.
  - **M2 (still open):** no time zone rule for comparing `Clock.instant()` with `LocalDateTime` job times.
  - **M3 (still open):** no way to edit a job's times, yet rebooking is required.
  - **M4 (new):** the restock/return rules are narrower than the Definition of Done.
- **Minor**
  - **m2 (still open):** no concurrency test.
  - **m3–m7 (new):**
    - m3: the calendar component spec needs a parts service provider.
    - m4: the mapping of errors to 400 and 404 isn't defined.
    - m5: the lock doesn't cover job-list writes, and overriding the `Clock` bean in tests needs setup.
    - m6: the "one `45` literal" check should cover main code only.
    - m7: the parts table shows availability now, but issuing checks the whole job window.
- **Resolved:** m1 (one allocation per job and part).
- **Feature 3 tests:** the reviewer found by reading the code, without running tests, that the design keeps them passing without changes, provided nulls are still serialized in JSON and booking stays end-exclusive.

## Decisions the human must make

1. Is the part hold in effect at exactly 45 minutes before start and 45 minutes after end?
2. Is job editing/rebooking part of Feature 2, or deferred?
3. Which time zone and clock rules apply?
4. Does restock mean only returning issued parts, or can it also add to a part's total?
5. Can parts be issued only against `SCHEDULED` jobs, and what happens to held parts when a job is completed or cancelled?
6. Are there issue/return controls in the UI, or is this API-only?
7. Should SKU matching be case-sensitive? Should the SKU appear in the parts table? Are the proposed seed totals acceptable?
8. What status does an issue or return against another technician's job get: 404, 403 or 409?

The next step is not implementation. Once these are answered, the requirements get updated and the ADR author and reviewer run again until the review is Approved, and only then is implementation approved.

## Memory Operations

None performed.
