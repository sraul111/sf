# Feature 2 Changelog

This file records the checked-in parts-reservation implementation, not the complete target behavior in `requirements.md` or `architecture-adr.md`.

## Versioning

Use Git tag names in the form `vMAJOR.MINOR.PATCH`:

- **MAJOR**: incompatible change to a published API or behavior.
- **MINOR**: backward-compatible feature addition.
- **PATCH**: backward-compatible defect or documentation fix.

Before `1.0.0`, the API is under active development and may change incompatibly; increment the minor version for notable development milestones and the patch version for compatible corrections. After `1.0.0`, follow the normal SemVer compatibility rules above.

**Current documented baseline: `v0.1.0` (development; 2026-10-12).** This is a proposed version label for the implementation described below, not a claim that Feature 2 meets its requirements or is safe for production. No Git tag is created by this document. In particular, the current reservation path is **NO-GO for production notes-enabled reservation or auto-approval**; see [the risk assessment](assessments/risk-assessment-report.md).

## [v0.1.0] - 2026-10-12

### Added

- Seeded in-memory catalog containing `CMP-CLUTCH` (3), `FLT-16` (10), `BRK-20A` (5), and `VLV-BALL-075` (3).
- Catalog reads at `GET /api/parts` and `GET /api/parts/{sku}`.
- Basic reservation endpoint at `POST /api/parts/{sku}/reservations`, returning a reservation identifier and request details.
- A Python stdio adapter using a line-delimited JSON request/response envelope and a default local backend URL.

### Current behavior and limitations

- A successful backend reservation subtracts from `Part.totalQuantity`; it does not create a job-linked issue record. Counts are not projected as `inUse` and `available`.
- The reservation route does not validate job existence, ownership, or status. There is no REST return endpoint.
- The adapter's documented session-token and confirmation checks are not implemented; it forwards reservation requests without a token.
- Missing-job and completed/cancelled-job rejection behavior is not satisfied by the current endpoint. The focused `Feature2PartsReservationTest` currently reports these mismatches.
- The proposed windowed inventory, atomic job-linked issues/returns, live availability UI, and production authorization controls remain target behavior, not shipped guarantees.

## Configuration Surface

“Tunable” below means the running application reads the value from a supported property or environment variable. A value that can only be changed by editing source or seed data is not an operator-facing runtime setting.

| Concern | Current source and value | Tunable at runtime? | Classification and notes |
|---|---|---:|---|
| Backend runtime | Java release `17`; Spring Boot `3.2.5`; Maven `3.9.9` onboarding verification baseline | No | `backend/pom.xml` pins Java release and Spring Boot. Maven `3.9.9` was observed for onboarding validation but is not enforced by a Maven Wrapper or project lock. The validated JDK was `17.0.20.1`. |
| Adapter runtime | CPython `3.10+` minimum; Feature 2 adapter compatibility baseline `v0.1.0` | No | Python 3.10 is the minimum contract; the Python patch version is not locked. `pipeline/feature2/mcp_server/server.py` is repository-managed source, not a separately released or runtime-version-reporting server. There is no MCP SDK or protocol version dependency. The adapter speaks a custom newline-delimited JSON format, not the standard MCP JSON-RPC protocol. |
| Adapter Python dependencies | Standard library only; optional `requests` import | No | No Python dependency manifest/lock exists. `requests` is not required, and no version is pinned; do not treat an incidental installed version as supported. |
| Frontend toolchain | Angular dependency ranges in `frontend/package.json`; exact npm dependency graph in `frontend/package-lock.json` (lockfile format 3) | No | Node.js/npm executable versions are not pinned in the repository. Use an Angular 17-compatible Node.js release and `npm ci`; the frontend is optional for the backend/adapter. |
| Backend listen port | Spring `server.port`, default `8080` in `backend/src/main/resources/application.properties`; standard environment override `SERVER_PORT` | Yes | Transport configuration. Changing it also requires updating callers. |
| CORS allowed origin | `http://localhost:4200` hard-coded in `backend/src/main/java/com/serviceforge/ServiceForgeApplication.java` | No | `serviceforge.cors.allowed-origin` is present in `application.properties` but is not read by the CORS configuration. Do not assume changing that property changes CORS. |
| Frontend API base URL | `http://localhost:8080/api` in `frontend/src/environments/environment.ts` | No | Build-time source configuration; change the Angular environment and rebuild to target a different backend. |
| Python adapter backend URL | `SERVICEFORGE_BACKEND_URL`, default `http://localhost:8080`, in `pipeline/feature2/mcp_server/server.py` | Yes | Adapter process environment. A trailing slash is removed before paths are appended. |
| Adapter HTTP request timeout | `5` seconds in the `requests` and `urllib` branches of `server.py` | No | Fixed transport timeout, not a reservation business timeout. There is no separate timeout setting on the backend reservation operation. |
| Technician travel buffer | `45` minutes in `TechnicianAvailabilityService.TRAVEL_BUFFER_MINUTES`; adapter availability logic also uses a hard-coded 45-minute `timedelta` | No | Fixed policy, not a supported setting. The backend and adapter currently define it separately; the proposed shared policy is not implemented. Do not change one side alone. |
| Seed catalog and starting quantities | Four part entries and quantities in `MockDataStore.seed()` | No | Fixed in-memory seed data. Change source and tests together; there is no inventory-admin configuration or persisted stock. Restarting the backend reseeds state. |
| Reservation expiry / hold duration | No expiry or configurable reservation lifetime exists | No | The target design's job-window holds are not implemented. Do not treat the 45-minute scheduling buffer as a reservation timeout. |
| MCP idempotency storage | Process-local `IDEMPOTENCY_CACHE` dictionary in `server.py` | No | No configured TTL or durable store; entries last only for the adapter process lifetime. This is not a backend idempotency guarantee. |

### Adapter and Backend API Contract Pin

The `v0.1.0` compatibility contract is the custom adapter request/response envelope and the current backend routes documented in [onboarding.md](onboarding.md#adapter-version-and-wire-contract). The adapter calls `GET /api/technicians/{id}`, `GET /api/technicians/{id}/jobs`, `GET /api/parts`, `GET /api/parts/{sku}`, and `POST /api/parts/{sku}/reservations`. The reservation body is `{ "jobId": <integer>, "quantity": <positive integer> }`; the success body is `{ "ok": true, "result": { "reservationId", "jobId", "sku", "quantity" } }` with HTTP `200`. Adapter line envelopes, response shapes, and all corresponding backend fields are enumerated in the onboarding contract table.

This pin describes the checked-in contract only. It does not include the proposed `POST /api/parts/{sku}/issues` or `/returns` endpoints, does not imply that the adapter implements standard MCP, and does not change the documented security limitations. Any contract change must update this version entry, the onboarding contract table, and relevant adapter/backend tests together. The adapter and backend must be taken from the same repository revision; no independent server artifact or compatibility negotiation exists.

### Fixed policy versus configuration

The following are domain rules, not operator tuning knobs: positive integer quantities; all-or-nothing rejection when current stored quantity is insufficient; case-insensitive seeded SKU lookup; and, in the target Feature 2 requirements, job ownership/status checks, UTC job-window accounting, return semantics, and automatic release rules. Several target rules are currently absent from runtime code, as listed above; describing a rule here does not mean it is enforced.

Do not add environment variables for policy values such as the travel buffer or job-status eligibility unless a reviewed requirements/ADR change explicitly makes them operationally configurable. If the intended 45-minute policy is implemented, centralize it in one backend policy and make the adapter consume authoritative backend results rather than maintain a second value.

## Unreleased

No changes recorded.